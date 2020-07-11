package com.newpage.salamport.media

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.newpage.salamport.R


class MediaStart : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_start)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")

        val goToMusicList = findViewById<Button>(R.id.goToMusicList)
        goToMusicList.setOnClickListener {
            MusicList.startFrom(this, session = session, token = token)
        }

    }

    companion object {
        fun startFrom(context: Context, session: String, token: String) {
            val intent = Intent(context, MediaStart::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
