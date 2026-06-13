package com.pawix.caltimeline.widget

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pawix.caltimeline.MainActivity
import com.pawix.caltimeline.R
import com.pawix.caltimeline.data.AgendaDay
import com.pawix.caltimeline.data.AgendaEvent
import com.pawix.caltimeline.data.CalendarRepository
import com.pawix.caltimeline.data.DateLabels
import com.pawix.caltimeline.data.SettingsRepository

class CalendarTimelineWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hasPermission = CalendarRepository.hasReadPermission(context)
        val days: List<AgendaDay> = if (hasPermission) {
            val enabled = SettingsRepository.effectiveEnabledIds(context)
            CalendarRepository.queryAgendaDays(context, enabled, daysAhead = 30, maxEvents = 80)
        } else {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                WidgetRoot(hasPermission = hasPermission, days = days)
            }
        }
    }
}

@Composable
private fun WidgetRoot(hasPermission: Boolean, days: List<AgendaDay>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Header()
        Spacer(GlanceModifier.height(10.dp))
        when {
            !hasPermission -> CenteredCta(R.string.widget_grant_cta)
            days.isEmpty() -> CenteredCta(R.string.widget_no_events)
            else -> AgendaList(days)
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = DateLabels.monthTitle(),
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity<MainActivity>()),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        AddButton()
    }
}

@Composable
private fun AddButton() {
    val insert = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.secondaryContainer)
            .clickable(androidx.glance.appwidget.action.actionStartActivity(insert)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_add),
            contentDescription = LocalContext.current.getString(R.string.add_event),
            modifier = GlanceModifier.size(22.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
        )
    }
}

@Composable
private fun AgendaList(days: List<AgendaDay>) {
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(days, itemId = { it.dayStartMillis }) { day ->
            DayRow(day)
        }
    }
}

@Composable
private fun DayRow(day: AgendaDay) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        DateCell(day.dayStartMillis)
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            day.events.forEachIndexed { index, event ->
                if (index > 0) Spacer(GlanceModifier.height(6.dp))
                EventChip(event)
            }
        }
    }
}

@Composable
private fun DateCell(dayStartMillis: Long) {
    val isToday = DateLabels.isToday(dayStartMillis)
    val dayNum = DateLabels.dayOfMonth(dayStartMillis)
    if (isToday) {
        Box(
            modifier = GlanceModifier
                .size(46.dp)
                .cornerRadius(23.dp)
                .background(GlanceTheme.colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = DateLabels.weekdayNarrow(dayStartMillis),
                    style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 11.sp),
                )
                Text(
                    text = dayNum,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    } else {
        Box(modifier = GlanceModifier.width(46.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = DateLabels.weekdayShort(dayStartMillis),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                )
                Text(
                    text = dayNum,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EventChip(event: AgendaEvent) {
    val argb = if (event.color == 0) DEFAULT_EVENT_COLOR else event.color
    val chipColor = Color(argb)
    val onChip = if (chipColor.luminance() > 0.5f) Color.Black else Color.White

    val view = Intent(Intent.ACTION_VIEW)
        .setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(18.dp)
            .background(chipColor)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .clickable(androidx.glance.appwidget.action.actionStartActivity(view)),
    ) {
        Text(
            text = event.title,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(onChip),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (!event.allDay) {
            Text(
                text = DateLabels.timeRange(
                    begin = event.begin,
                    end = event.end,
                    allDay = false,
                    allDayLabel = "",
                ),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(onChip.copy(alpha = 0.85f)),
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

@Composable
private fun CenteredCta(textRes: Int) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = LocalContext.current.getString(textRes),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

private const val DEFAULT_EVENT_COLOR = 0xFF5A8A3C.toInt()
