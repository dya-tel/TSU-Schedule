package ru.dyatel.tsuschedule.updater

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import ru.dyatel.tsuschedule.GITHUB_REPOSITORY
import ru.dyatel.tsuschedule.MIME_APK
import ru.dyatel.tsuschedule.ParsingException
import ru.dyatel.tsuschedule.log
import ru.dyatel.tsuschedule.utilities.find
import ru.dyatel.tsuschedule.utilities.iterator
import java.io.IOException
import java.net.HttpURLConnection

class UpdaterApi {

    private val connection = Jsoup.connect("https://api.github.com/repos/$GITHUB_REPOSITORY/releases")
            .ignoreHttpErrors(true)
            .ignoreContentType(true)
            .method(Connection.Method.GET)

    fun setTimeout(timeout: Int) {
        connection.timeout(timeout)
    }

    fun getReleases(): List<ReleaseToken> {
        val response = connection.execute()
        if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw IOException("GitHub API didn't find the repository")
        }
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw IOException("Request failed with HTTP code ${response.statusCode()}")
        }

        val releases = try {
            JSONArray(response.body())
        } catch (e: JSONException) {
            throw ParsingException(e)
        }

        return releases.iterator().asSequence()
                .mapNotNull {
                    try {
                        parseRelease(it as JSONObject)
                    } catch (e: Exception) {
                        e.log()
                        null
                    }
                }
                .toList()
    }

    private fun parseRelease(json: JSONObject): ReleaseToken {
        val links = json.find<JSONArray>("assets").iterator().asSequence()
                .map { it as JSONObject }
                .filter { it.find<String>("content_type") == MIME_APK }
                .map { it.find<String>("browser_download_url") }
                .toList()
                .takeIf { it.any() } ?: throw ParsingException("No .apk files in assets")

        val url = links.singleOrNull() ?: throw ParsingException("Too many .apk files in assets")
        val release = Release(json.find("tag_name"))

        return ReleaseToken(release, json.find("prerelease"), url, json.find("body"))
    }

}
