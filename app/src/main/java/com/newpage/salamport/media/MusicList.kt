package com.newpage.salamport.media

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.newpage.salamport.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.io.File
import java.lang.Exception


class TrackAdapter(
    private val context: MusicList, private var tracks: ArrayList<Track>,
    private val session: String, private val token: String, private val client: OkHttpClient
) :
    RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photo: ImageView = view.findViewById(R.id.trackLogo)
        val text: TextView = view.findViewById(R.id.trackTitle)
        val goToMessages: TextView = view.findViewById(R.id.playTrack)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(inflater.inflate(R.layout.music_list_holder, parent, false))
    }

    override fun getItemCount(): Int {
        return tracks.size
    }

    fun addChat(tracks: ArrayList<Track>) {
        this.tracks = tracks
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = tracks[position]
        holder.text.text = track.title
        val photo = track.logo

        loadWithGlide(photo!!, holder)
        holder.goToMessages.setOnClickListener {
            MediaPlayerActivity.startFrom(context, session, token, position)

        }
    }

    private fun loadWithGlide(photo: String, holder: ViewHolder) {

        Glide
            .with(context)
            .load(photo)
            .apply(
                RequestOptions
                    .bitmapTransform(RoundedCorners(250))
            )
            .into(holder.photo)

    }
}


class MusicList : AppCompatActivity() {

    val ACTIVITY_CHOOSE_FILE = 1

    private var isMusicLoaded = false
    private var isLogoLoaded = false

    private lateinit var pathToMusic: String
    private lateinit var pathToLogo: String

    private lateinit var session: String
    private lateinit var token: String

    private lateinit var trackAdapter: TrackAdapter

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_list)



        if (intent?.action == Intent.ACTION_SEND) {
            if (intent?.type!!.startsWith("audio")) {
                val grishaTokenAndSession = loadGrishaTokenAndSession()
                val u: Uri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                Toast.makeText(this, u.toString(), Toast.LENGTH_LONG).show()
            }
        }

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )

        findViewById<ImageView>(R.id.goToMessages).setOnClickListener {
            ChatActivity.startFrom(
                this, token = token,
                session = session
            )
        }

        findViewById<ImageView>(R.id.goToFriends).setOnClickListener {
            FriendsActivity.startFrom(
                this, token = token, session = session
            )
        }

        loadUI()
        loadTrackList()
        trackAdapter = TrackAdapter(this, ArrayList(), session, token, client)
        val mlayoutManager = LinearLayoutManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.musicContainer).apply {
            setHasFixedSize(true)
            layoutManager = mlayoutManager
            adapter = trackAdapter
        }
    }

    private fun loadGrishaTokenAndSession(): ArrayList<String> {
        val a = ArrayList<String>()
        return try {
            a.add(this.openFileInput("token").bufferedReader().readLine())
            a.add(this.openFileInput("session").bufferedReader().readLine())
            a
        } catch (e: Exception) {
            a.add("-1"); a.add("-1")
            a
        }
    }

    private fun loadTrackList() {
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
                val authore = responseMusicJSON.getString("author")
                tracks.add(
                    Track(
                        R.drawable.exo_controls_pause, title = title,
                        url = url, logo = photo, author = authore
                    )
                )
            }

            trackAdapter.addChat(tracks)
            runOnUiThread {
                trackAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadUI() {
        val plusButton = findViewById<Button>(R.id.loadMusicToServer)
        plusButton.setOnClickListener {
            onBrowse()
        }

        val sendMusic = findViewById<Button>(R.id.sendMusicToServer)
        sendMusic.setOnClickListener {
            sendFile(pathToMusic, pathToLogo)
        }
    }

    private fun sendFile(pathToMusic: String, pathToLogo: String) {
        val mpFile = File(pathToMusic)
        val logoFile = File(pathToLogo)
        GlobalScope.launch {
            val formBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

                .addFormDataPart(
                    "music", mpFile.name,
                    RequestBody.create("audio/mpeg".toMediaTypeOrNull(), mpFile)
                )
                .addFormDataPart(
                    "photo", logoFile.name,
                    RequestBody.create("image/png".toMediaTypeOrNull(), logoFile)
                )
                .addFormDataPart("session", session)
                .addFormDataPart("token", token)
                .addFormDataPart("name", "Twilight")
                .addFormDataPart("author", "Boa")
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/add_music.php")
                .post(formBody)
                .build()
            val response =
                withContext(Dispatchers.IO) {
                    client.newCall(request).await().body?.string()
                }
            val b = JSONArray(response)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] === PackageManager.PERMISSION_GRANTED
                ) {
                    loadUI()

                } else {

                    Toast.makeText(
                        this,
                        "Permission denied to read your External storage",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //this when button click
    fun onBrowse() {
        val intent: Intent
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE)
        if (!isMusicLoaded)
            chooseFile.type = "audio/mpeg"
        else chooseFile.type = "image/png"
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, ACTIVITY_CHOOSE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode !== Activity.RESULT_OK) return
        if ((requestCode === ACTIVITY_CHOOSE_FILE) && (!isMusicLoaded)) {
            val uri = data!!.data
            val FilePath = uri?.path
            pathToMusic = getPath(uri!!)
            isMusicLoaded = true
        }
        if ((requestCode === ACTIVITY_CHOOSE_FILE) && (!isLogoLoaded) && (isMusicLoaded)) {
            val uri = data!!.data
            val FilePath = uri?.path
            pathToLogo = getPath(uri!!)
            isLogoLoaded = true
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getPath(path: Uri): String {
        val downloadFolder = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val filename = path.path?.replace("/document/raw:/storage/emulated/0/Download/", "")
        return downloadFolder!!.path + "/" + filename
    }

    companion object {
        fun startFrom(context: Context, session: String, token: String, uri: Uri? = null) {
            val intent = Intent(context, MusicList::class.java)
            Toast.makeText(context, "t", Toast.LENGTH_LONG)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
