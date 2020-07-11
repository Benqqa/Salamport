package com.newpage.salamport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.location.aravind.getlocation.GeoLocator
import com.newpage.salamport.services.FilterActivity
import com.newpage.salamport.services.ServicesActivity
import com.newpage.salamport.services.TinderStart
import com.vk.api.sdk.VK
import java.nio.charset.StandardCharsets


class UserActivity : Activity() {

    private lateinit var grishaToken: String
    private lateinit var grishaSession: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        grishaToken = intent.getStringExtra("token")
        grishaSession = intent.getStringExtra("session")
        val slide =
            TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right)
        window.exitTransition = slide


        val goToMessages = findViewById<ImageView>(R.id.goToMessages)
        goToMessages.setOnClickListener {
            ChatActivity.startFrom(
                this, token = grishaToken,
                session = grishaSession
            )
        }

        val logoutButton = findViewById<ImageView>(R.id.logout)
        logoutButton.setOnClickListener {
            if (VK.isLoggedIn())
                VK.logout()
            saveSessionAndToken("-1", "-1")
            MainActivity.startFrom(this)
            finish()
        }

        findViewById<ImageView>(R.id.goToProfile).setOnClickListener {
            ProfileActivity.startFrom(
                this@UserActivity, token = grishaToken, session = grishaSession
            )
        }

        findViewById<ImageView>(R.id.goToFriends).setOnClickListener {
            FriendsActivity.startFrom(
                this@UserActivity, token = grishaToken, session = grishaSession
            )
        }

        findViewById<ImageView>(R.id.services).setOnClickListener {
            ServicesActivity.startFrom(
                this, session = grishaSession,
                token = grishaToken
            )
        }

        findViewById<ImageView>(R.id.goToTinderStart).setOnClickListener {
            TinderStart.startFrom(this, session = grishaSession, token = grishaToken)
        }

        initGeolocation()
    }

    private fun initGeolocation() {
        SingletonGeolocation.init(applicationContext, this, grishaSession, grishaToken)
    }

    private fun saveSessionAndToken(session: String, token: String) {
        this.openFileOutput("session", Context.MODE_PRIVATE).use {
            it.write(session.toByteArray(charset = StandardCharsets.UTF_8))
            it.flush()
        }
        this.openFileOutput("token", Context.MODE_PRIVATE).use {
            it.write(token.toByteArray(StandardCharsets.UTF_8))
            it.flush()
        }
    }


    override fun onBackPressed() {

    }


    companion object {
        fun startFrom(
            context: AppCompatActivity, grishaToken: String,
            grishaSession: String
        ) {
            val intent = Intent(context, UserActivity::class.java)
            intent.putExtra("token", grishaToken)
            intent.putExtra("session", grishaSession)
            context.startActivity(intent)
        }
    }
}
