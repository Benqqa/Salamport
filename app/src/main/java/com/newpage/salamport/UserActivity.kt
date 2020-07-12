package com.newpage.salamport

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.transition.TransitionInflater
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.location.aravind.getlocation.GeoLocator
import com.newpage.salamport.groups.NewsActivity
import com.newpage.salamport.media.MediaStart
import com.newpage.salamport.services.FilterActivity
import com.newpage.salamport.services.ServicesActivity
import com.newpage.salamport.services.TinderStart
import com.vk.api.sdk.VK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.nio.charset.StandardCharsets


class UserActivity : Activity() {

    private lateinit var grishaToken: String
    private lateinit var grishaSession: String

    private val client = OkHttpClient.Builder().build()


    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        setContentView(R.layout.activity_user)

        grishaToken = intent.getStringExtra("token")
        grishaSession = intent.getStringExtra("session")
        val slide =
            TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right)
        window.exitTransition = slide



        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", grishaSession)
                .add("token", grishaToken)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/lk.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response =
                withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
            val imgView = findViewById<ImageView>(R.id.avatar)

            val responseJSON = JSONObject(response)
            val avatar = responseJSON.getString("photo")
            this@UserActivity.runOnUiThread {
                Glide.with(this@UserActivity)
                    .load(avatar)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(250)))
                    .into(imgView)
            }
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

        findViewById<ImageView>(R.id.goToMessages).setOnClickListener {
            ChatActivity.startFrom(
                this, token = grishaToken,
                session = grishaSession
            )
        }

        findViewById<ImageView>(R.id.goToFriends).setOnClickListener {
            FriendsActivity.startFrom(
                this@UserActivity, token = grishaToken, session = grishaSession
            )
        }

        findViewById<ImageView>(R.id.goToFeed).setOnClickListener {
            NewsActivity.startFrom(
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

        findViewById<ImageView>(R.id.goToMediaStart).setOnClickListener {
            MediaStart.startFrom(this, session = grishaSession, token = grishaToken)
        }
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
