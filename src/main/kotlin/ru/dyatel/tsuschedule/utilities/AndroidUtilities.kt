package ru.dyatel.tsuschedule.utilities

import android.app.Activity
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.preference.Preference
import android.view.View
import com.wealthfront.magellan.Screen
import org.jetbrains.anko.inputMethodManager
import org.json.JSONArray
import org.json.JSONObject

class NumberPreferenceValidator(
        private val acceptEmptyInput: Boolean = false,
        private val constraint: IntRange? = null
) : Preference.OnPreferenceChangeListener {

    override fun onPreferenceChange(preference: Preference?, newValue: Any): Boolean {
        if (newValue == "")
            return acceptEmptyInput && (constraint == null || 0 in constraint)

        return try {
            val number = (newValue as String).toInt()
            constraint == null || number in constraint
        } catch (e: NumberFormatException) {
            false
        }
    }

}

fun Activity.hideKeyboard() {
    val view = currentFocus ?: return

    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    view.clearFocus()
}

fun View.hideIf(condition: () -> Boolean) {
    visibility = if (condition()) View.GONE else View.VISIBLE
}

fun View.setBackgroundTintResource(colorRes: Int) {
    ViewCompat.setBackgroundTintList(this, ContextCompat.getColorStateList(context, colorRes))
}

inline fun <reified T> JSONObject.find(name: String): T =
        get(name) as T? ?: throw NoSuchElementException()

operator fun JSONArray.iterator() = object : Iterator<Any> {

    private var current = 0

    override fun hasNext() = current < this@iterator.length()

    override fun next() = this@iterator[current++]

}

val Screen<*>.ctx: Context?
    get() = getActivity()

val Fragment.ctx: Context?
    get() = context
