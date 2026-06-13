package com.pawix.caltimeline.data

/** A calendar account/collection exposed by the system Calendar Provider. */
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
    val isVisible: Boolean,
)

/** A single event instance resolved from the Calendar Provider. */
data class AgendaEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    /** Resolved display color (event color if set, otherwise calendar color). */
    val color: Int,
)

/** Events that belong to the same calendar day, used to render one timeline row. */
data class AgendaDay(
    val dayStartMillis: Long,
    val events: List<AgendaEvent>,
)
