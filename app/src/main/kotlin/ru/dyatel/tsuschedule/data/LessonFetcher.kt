package ru.dyatel.tsuschedule.data

import android.content.Context
import ru.dyatel.tsuschedule.R
import ru.dyatel.tsuschedule.getConnectionTimeout
import ru.dyatel.tsuschedule.parsing.Lesson
import ru.dyatel.tsuschedule.parsing.Parser
import java.io.IOException
import java.net.SocketTimeoutException

private val errorStrings = mapOf(
        LessonFetcher.Failure.UNKNOWN to R.string.unknown_failure,
        LessonFetcher.Failure.NO_GROUP to R.string.no_group_index,
        LessonFetcher.Failure.WRONG_GROUP to R.string.wrong_group_index,
        LessonFetcher.Failure.TIMEOUT to R.string.connection_timeout,
        LessonFetcher.Failure.CONNECTION_FAIL to R.string.load_failure
)

class LessonFetcher(private val context: Context) {

    enum class Failure {
        NONE, UNKNOWN, NO_GROUP, WRONG_GROUP, TIMEOUT, CONNECTION_FAIL
    }

    private var failure: Failure = Failure.NONE
    fun failed() = failure != Failure.NONE

    fun getError(): String? {
        val stringRes = errorStrings[failure]
        if (stringRes != null) return context.getString(stringRes)
        return ""
    }

    fun fetch(group: String): Set<Lesson>? {
        if (group.isBlank()) {
            failure = Failure.NO_GROUP
            return null
        }

        val parser = Parser();
        parser.setTimeout(getConnectionTimeout(context))

        var lessons: Set<Lesson>? = null
        try {
            lessons = parser.getLessons(group)
        } catch(e: IllegalArgumentException) {
            failure = Failure.WRONG_GROUP
        } catch(e: SocketTimeoutException) {
            failure = Failure.TIMEOUT
        } catch(e: IOException) {
            failure = Failure.CONNECTION_FAIL
        } catch(e: Exception) {
            failure = Failure.UNKNOWN
        }
        return lessons
    }

}