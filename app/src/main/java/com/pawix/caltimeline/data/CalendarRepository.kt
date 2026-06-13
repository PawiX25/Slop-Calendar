package com.pawix.caltimeline.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.LocalDate

/**
 * Thin wrapper over the system Calendar Provider (CalendarContract). All queries are
 * read-only and require the READ_CALENDAR runtime permission; callers must check
 * [hasReadPermission] first (the widget renders a CTA when it is missing).
 */
object CalendarRepository {

    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private val CALENDAR_PROJECTION = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.CALENDAR_COLOR,
        CalendarContract.Calendars.VISIBLE,
    )

    fun queryCalendars(context: Context): List<CalendarInfo> {
        if (!hasReadPermission(context)) return emptyList()
        val result = ArrayList<CalendarInfo>()
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                CALENDAR_PROJECTION,
                null,
                null,
                "${CalendarContract.Calendars.ACCOUNT_NAME} ASC, " +
                    "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val visIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)
                while (c.moveToNext()) {
                    result += CalendarInfo(
                        id = c.getLong(idIdx),
                        displayName = c.getString(nameIdx) ?: "(?)",
                        accountName = c.getString(accIdx) ?: "",
                        color = c.getInt(colorIdx),
                        isVisible = c.getInt(visIdx) == 1,
                    )
                }
            }
        }
        return result
    }

    private val INSTANCE_PROJECTION = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.CALENDAR_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.DISPLAY_COLOR,
    )

    /**
     * Returns upcoming events grouped by calendar day, from the start of today through
     * [daysAhead] days, limited to [maxEvents] total. Only [enabledCalendarIds] are read.
     */
    fun queryAgendaDays(
        context: Context,
        enabledCalendarIds: Set<Long>,
        daysAhead: Int = 30,
        maxEvents: Int = 80,
    ): List<AgendaDay> {
        if (!hasReadPermission(context) || enabledCalendarIds.isEmpty()) return emptyList()

        val zone = DateLabels.zone()
        val todayStart = DateLabels.startOfToday(zone)
        val rangeEndExclusive = LocalDate.now(zone).plusDays(daysAhead.toLong())
            .atStartOfDay(zone).toInstant().toEpochMilli()

        // Query a slightly wider window than [todayStart, rangeEndExclusive) so that all-day
        // events (stored at UTC midnight) are not clipped by timezone offset, then filter
        // precisely by the locally-resolved day below.
        val queryStart = todayStart - DAY_MS
        val queryEnd = rangeEndExclusive + DAY_MS

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
            ContentUris.appendId(this, queryStart)
            ContentUris.appendId(this, queryEnd)
        }.build()

        val idList = enabledCalendarIds.joinToString(",")
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($idList) AND " +
            "(${CalendarContract.Instances.STATUS} IS NULL OR " +
            "${CalendarContract.Instances.STATUS} != ${CalendarContract.Instances.STATUS_CANCELED})"

        val events = ArrayList<AgendaEvent>()
        runCatching {
            context.contentResolver.query(
                uri,
                INSTANCE_PROJECTION,
                selection,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                val eventIdIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val calIdIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
                val titleIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val colorIdx = c.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)
                while (c.moveToNext()) {
                    events += AgendaEvent(
                        eventId = c.getLong(eventIdIdx),
                        calendarId = c.getLong(calIdIdx),
                        title = c.getString(titleIdx)?.takeIf { it.isNotBlank() } ?: "(bez tytułu)",
                        begin = c.getLong(beginIdx),
                        end = c.getLong(endIdx),
                        allDay = c.getInt(allDayIdx) == 1,
                        color = c.getInt(colorIdx),
                    )
                }
            }
        }

        // Group by locally-resolved day, keep only [today, rangeEnd), order days and events.
        val grouped = events
            .asSequence()
            .map { it to DateLabels.dayStartFor(it.begin, it.allDay, zone) }
            .filter { (_, dayStart) -> dayStart in todayStart until rangeEndExclusive }
            .groupBy({ it.second }, { it.first })

        return grouped.entries
            .sortedBy { it.key }
            .map { (dayStart, dayEvents) ->
                AgendaDay(
                    dayStartMillis = dayStart,
                    events = dayEvents.sortedWith(
                        compareByDescending<AgendaEvent> { it.allDay }.thenBy { it.begin },
                    ),
                )
            }
            .let { capEvents(it, maxEvents) }
    }

    /** Caps the flattened event count while keeping whole days intact. */
    private fun capEvents(days: List<AgendaDay>, maxEvents: Int): List<AgendaDay> {
        var count = 0
        val out = ArrayList<AgendaDay>()
        for (day in days) {
            if (count >= maxEvents) break
            out += day
            count += day.events.size
        }
        return out
    }

    private const val DAY_MS = 24L * 60L * 60L * 1000L
}
