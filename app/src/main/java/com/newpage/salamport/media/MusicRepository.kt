package com.newpage.salamport.media

import com.newpage.salamport.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.util.*

data class Track(
    val bitmapResId: Int, val title: String, var url: String?,
    val logo: String? = null, val author: String
)

class MusicRepository {
    private var currentPos = 0
    private var client = OkHttpClient.Builder().build()
    private var tracks =
        ArrayList<Track>()

    fun size(): Int {
        return tracks.size
    }

    fun add(t: Track) {
        tracks.add(t)
    }

    val current: Track
        get() = tracks[currentPos]

    fun next() {
        if (canGoNext()) {
            currentPos++
        }
    }

    fun canGoNext(): Boolean {
        return currentPos < tracks.size - 1
    }

    fun canGoBack(): Boolean {
        return currentPos > 0
    }

    fun prev() {
        if (canGoBack()) {
            currentPos--
        }
    }

    fun init(session: String, token: String, position: Int) {
        currentPos = position
        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("token", token)
                .add("session", session)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/all_music.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val responseString = withContext(Dispatchers.IO) { client.newCall(request).await() }
            val responseJSON = JSONArray(responseString.body?.string())
            val tracks = ArrayList<Track>()

            for (i in 0 until responseJSON.length()) {
                val requestBodyForMusic = FormBody.Builder()
                    .add("token", token)
                    .add("session", session)
                    .add("music_id", responseJSON.getString(i))
                    .build()
                val requestForMusic = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/music_info.php")
                    .post(requestBodyForMusic)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseStringForMusic =
                    withContext(Dispatchers.IO) { client.newCall(requestForMusic).await() }
                val responseMusicJSON = JSONObject(responseStringForMusic.body?.string())
                val title = responseMusicJSON.getString("name")
                val url = responseMusicJSON.getString("file")
                val photo = responseMusicJSON.getString("photo")
                val author = responseMusicJSON.getString("author")
                tracks.add(
                    Track(
                        R.drawable.exo_controls_pause, title = title, url = url, logo = photo,
                        author = author
                    )
                )
            }

            this@MusicRepository.tracks = tracks
        }
    }
}