package com.newpage.salamport.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.media.session.MediaControllerCompat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.newpage.salamport.ChatActivity
import com.newpage.salamport.FriendsActivity
import com.newpage.salamport.R
import com.newpage.salamport.groups.NewsActivity
import com.newpage.salamport.media.PlaybackService.PlaybackServiceBinder


class MediaPlayerActivity : AppCompatActivity() {

    private var binder: PlaybackServiceBinder? = null
    private var controller: MediaControllerCompat? = null

    private lateinit var session: String
    private lateinit var token: String

    private lateinit var prevButton: ImageView
    private lateinit var pauseButton: ImageView
    private lateinit var playButton: ImageView
    private lateinit var nextButton: ImageView
    private lateinit var stopButton: ImageView

    private lateinit var textAuthor: TextView
    private lateinit var textTitle: TextView

    private lateinit var imageLogo: ImageView

    private lateinit var repository: MusicRepository
    private var position: Int = 0

    private var handler = Handler()

    //private var firstTime = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)

        prevButton = findViewById(R.id.goToPrev)
        pauseButton = findViewById(R.id.goToPause)
        playButton = findViewById(R.id.goToPlay)
        nextButton = findViewById(R.id.goToNext)
        stopButton = findViewById(R.id.goToStop)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")
        position = intent.getIntExtra("position", 0)

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
        findViewById<ImageView>(R.id.goToFeed).setOnClickListener {
            NewsActivity.startFrom(
                this, token = token, session = session
            )
        }

        imageLogo = findViewById(R.id.MediaTrackLogo)
        textAuthor = findViewById(R.id.MusicAuthor)
        textTitle = findViewById(R.id.MusicTitle)

        val intent = Intent(this, PlaybackService::class.java)
        intent.putExtra("session", session)
        intent.putExtra("token", token)
        intent.putExtra("position", position)
        bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder = service as PlaybackServiceBinder
                try {
                    controller = MediaControllerCompat(
                        this@MediaPlayerActivity, binder!!.token
                    )
                    repository = binder!!.repository

                    handler.postDelayed({ updateInterface() }, 500)

                } catch (e: RemoteException) {
                    controller = null
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                binder = null
                controller = null
            }
        }, Context.BIND_AUTO_CREATE)


        playButton.setOnClickListener { v: View? ->
            if (controller != null) {
                //if (!firstTime) {
                controller!!.transportControls.play()
                updateInterface()
                //} else {
                //   controller!!.transportControls.skipToNext()
                //    controller!!.transportControls.skipToPrevious()
                //   updateInterface()
                //   firstTime = false
                //}
            }
        }

        pauseButton.setOnClickListener { v: View? ->
            if (controller != null) {
                controller!!.transportControls.pause()
                updateInterface()
            }
        }

        stopButton.setOnClickListener { v ->
            if (controller != null) controller!!.transportControls.stop()
            updateInterface()
        }

        nextButton.setOnClickListener { v: View? ->
            if (controller != null && repository.canGoNext()) {
                controller!!.transportControls.skipToNext()
                updateInterface()
            }
        }

        prevButton.setOnClickListener { v: View? ->
            if (controller != null && repository.canGoBack()) {
                controller!!.transportControls.skipToPrevious()
                updateInterface()
            }
        }
    }

    private fun updateInterface() {
        Glide.with(this)
            .load(repository.current.logo)
            .into(imageLogo)
        textAuthor.text = repository.current.author
        textTitle.text = repository.current.title
    }


    companion object {
        fun startFrom(context: Context, session: String, token: String, position: Int) {
            val intent = Intent(context, MediaPlayerActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.putExtra("position", position)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
