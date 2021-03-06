package ru.dyatel.tsuschedule.utilities

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import hirondelle.date4j.DateTime
import ru.dyatel.tsuschedule.R
import ru.dyatel.tsuschedule.events.Event
import ru.dyatel.tsuschedule.events.EventBus
import java.util.TimeZone

private const val DATA_PREFERENCES = "data_preferences"

private const val DATA_GROUP = "group"
private const val DATA_GROUPS = "groups"

private const val DATA_LAST_UPDATE_CHECK = "last_auto_update"
private const val DATA_LAST_RELEASE_URL = "last_release"
private const val DATA_LAST_USED_VERSION = "last_used_version"
private const val DATA_PENDING_CHANGELOG_DISPLAY = "pending_changelog_display"

class SchedulePreferences(private val context: Context) {

    private val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)
    private val dataPreferences: SharedPreferences
        get() = context.getSharedPreferences(DATA_PREFERENCES, Context.MODE_PRIVATE)

    val connectionTimeout: Int
        get() {
            val preference = context.getString(R.string.preference_timeout)
            val fallback = context.getString(R.string.preference_timeout_default)
            return preferences.getString(preference, fallback).toInt() * 1000
        }

    val historySize: Int
        get() {
            val preference = context.getString(R.string.preference_history_size)
            val fallback = context.getString(R.string.preference_history_size_default)
            return preferences.getString(preference, fallback).toInt()
        }

    val autoupdate: Boolean
        get() {
            val preference = context.getString(R.string.preference_update_auto)
            val fallback = context.getString(R.string.preference_update_auto_default)!!.toBoolean()
            return preferences.getBoolean(preference, fallback)
        }

    val allowPrerelease: Boolean
        get() {
            val preference = context.getString(R.string.preference_update_allow_prerelease)
            val fallback = context.getString(R.string.preference_update_allow_prerelease_default)!!.toBoolean()
            return preferences.getBoolean(preference, fallback)
        }

    var group: String?
        get() = dataPreferences.getString(DATA_GROUP, null)?.takeIf { it.isNotBlank() }
        set(value) = dataPreferences.editAndApply { putString(DATA_GROUP, value) }

    val groups: List<String>
        get() = dataPreferences.getStringSet(DATA_GROUPS, emptySet()).sorted()

    fun addGroup(group: String) {
        dataPreferences.editAndApply {
            putStringSet(DATA_GROUPS, HashSet(groups).apply { add(group) })
        }
    }

    fun removeGroup(group: String) {
        dataPreferences.editAndApply {
            putStringSet(DATA_GROUPS, HashSet(groups).apply { remove(group) })
        }
    }

    var pendingChangelogDisplay: Boolean
        get() = dataPreferences.getBoolean(DATA_PENDING_CHANGELOG_DISPLAY, false)
        set(value) = dataPreferences.editAndApply { putBoolean(DATA_PENDING_CHANGELOG_DISPLAY, value) }

    var lastUpdateCheck: DateTime?
        get() {
            return dataPreferences.getLong(DATA_LAST_UPDATE_CHECK, -1)
                    .takeUnless { it == -1L }
                    ?.let { DateTime.forInstant(it, TimeZone.getDefault()) }
        }
        set(value) {
            val timestamp = value?.getMilliseconds(TimeZone.getDefault()) ?: -1
            dataPreferences.editAndApply { putLong(DATA_LAST_UPDATE_CHECK, timestamp) }
        }

    var lastReleaseUrl: String?
        get() = dataPreferences.getString(DATA_LAST_RELEASE_URL, null)
        set(value) {
            dataPreferences.editAndApply { putString(DATA_LAST_RELEASE_URL, value) }
            EventBus.broadcast(Event.PREFERENCES_LATEST_VERSION_CHANGED, value)
        }

    var lastUsedVersion: Int?
        get() = dataPreferences.getInt(DATA_LAST_USED_VERSION, -1).takeIf { it > 0 }
        set(value) = dataPreferences.editAndApply { putInt(DATA_LAST_USED_VERSION, value ?: -1) }

}

val Context.schedulePreferences: SchedulePreferences
    get() = SchedulePreferences(this)

fun SharedPreferences.editAndApply(editor: SharedPreferences.Editor.() -> Unit) {
    val edit = edit()
    edit.editor()
    edit.apply()
}