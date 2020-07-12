package com.newpage.salamport.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.newpage.salamport.ChatActivity
import com.newpage.salamport.FriendsActivity
import com.newpage.salamport.R
import com.newpage.salamport.groups.NewsActivity

class ServicesActivity : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")

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

    }

    companion object {
        fun startFrom(context: Context, session: String, token: String) {
            val intent = Intent(context, ServicesActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
