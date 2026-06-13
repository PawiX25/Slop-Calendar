package com.pawix.caltimeline.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

/**
 * All presentation-level date/time formatting for the timeline. Kept free of any
 * Glance/Compose imports so it can be reused by both the widget and the app.
 */
object DateLabels {

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun startOfToday(zone: ZoneId = zone()): Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Local start-of-day millis for an event, handling all-day events stored in UTC. */
    fun dayStartFor(begin: Long, allDay: Boolean, zone: ZoneId = zone()): Long {
        val date = if (allDay) {
            Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
        } else {
            Instant.ofEpochMilli(begin).atZone(zone).toLocalDate()
        }
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun localDate(dayStartMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(dayStartMillis).atZone(zone).toLocalDate()

    fun isToday(dayStartMillis: Long, zone: ZoneId = zone()): Boolean =
        localDate(dayStartMillis, zone) == LocalDate.now(zone)

    /**
     * Full month name of today in the *standalone* (nominative) form, capitalised
     * (e.g. "June" / "Czerwiec"). FULL_STANDALONE matters for Polish and similar
     * languages: plain FULL returns the genitive "czerwca" used in dates ("14 czerwca").
     */
    fun monthTitle(locale: Locale = Locale.getDefault(), zone: ZoneId = zone()): String {
        val name = LocalDate.now(zone).month.getDisplayName(JTextStyle.FULL_STANDALONE, locale)
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    /** Single-letter weekday, e.g. "S" — used inside the "today" circle. */
    fun weekdayNarrow(dayStartMillis: Long, locale: Locale = Locale.getDefault(), zone: ZoneId = zone()): String =
        localDate(dayStartMillis, zone).dayOfWeek.getDisplayName(JTextStyle.NARROW, locale)
            .replaceFirstChar { it.titlecase(locale) }

    /** Short weekday, e.g. "Mon" / "pon.". */
    fun weekdayShort(dayStartMillis: Long, locale: Locale = Locale.getDefault(), zone: ZoneId = zone()): String =
        localDate(dayStartMillis, zone).dayOfWeek.getDisplayName(JTextStyle.SHORT, locale)
            .replaceFirstChar { it.titlecase(locale) }

    fun dayOfMonth(dayStartMillis: Long, zone: ZoneId = zone()): String =
        localDate(dayStartMillis, zone).dayOfMonth.toString()

    private val timeFmt: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    /** "01:00–02:00" for timed events, or the supplied all-day label. */
    fun timeRange(
        begin: Long,
        end: Long,
        allDay: Boolean,
        allDayLabel: String,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = zone(),
    ): String {
        if (allDay) return allDayLabel
        val fmt = timeFmt.withLocale(locale)
        val from = Instant.ofEpochMilli(begin).atZone(zone).toLocalTime().format(fmt)
        val to = Instant.ofEpochMilli(end).atZone(zone).toLocalTime().format(fmt)
        return "$from–$to"
    }
}
