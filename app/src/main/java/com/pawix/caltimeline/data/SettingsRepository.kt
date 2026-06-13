package com.pawix.caltimeline.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "cal_timeline_settings")

/**
 * Persists which calendars the widget should show. A `null` selection means the user
 * has never configured it, in which case every visible calendar is shown by default.
 */
object SettingsRepository {

    private val ENABLED_IDS = stringSetPreferencesKey("enabled_calendar_ids")

    /** Emits the explicitly stored selection, or `null` when never configured. */
    fun enabledIdsFlow(context: Context): Flow<Set<Long>?> =
        context.settingsDataStore.data.map { prefs ->
            prefs[ENABLED_IDS]?.mapNotNull(String::toLongOrNull)?.toSet()
        }

    suspend fun storedEnabledIds(context: Context): Set<Long>? =
        enabledIdsFlow(context).first()

    /** The selection actually used by the widget: stored set, or all visible calendars. */
    suspend fun effectiveEnabledIds(context: Context): Set<Long> {
        storedEnabledIds(context)?.let { return it }
        return CalendarRepository.queryCalendars(context)
            .filter { it.isVisible }
            .map { it.id }
            .toSet()
    }

    suspend fun setEnabledIds(context: Context, ids: Set<Long>) {
        context.settingsDataStore.edit { prefs ->
            prefs[ENABLED_IDS] = ids.map(Long::toString).toSet()
        }
    }
}
