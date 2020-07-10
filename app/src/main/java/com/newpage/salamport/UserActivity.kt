package com.newpage.salamport

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.newpage.salamport.requests.AvatarRequest
import com.newpage.salamport.requests.UserRequest
import com.newpage.salamport.requests.VKUser
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.api.sdk.exceptions.VKApiExecutionException
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets


class UserActivity : Activity() {

    var grishaToken = "";
    var grishaSession = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        val msg = intent.extras?.get("msg").toString()
        val slide =
            TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right)
        window.exitTransition = slide

        try {
            val msgJSON = JSONObject(msg)
            grishaToken = msgJSON.getString("token")
            grishaSession = msgJSON.getString("session")
            val goToMessages = findViewById<ImageView>(R.id.goToMessages)
            goToMessages.setOnClickListener {
                ChatActivity.startFrom(
                    this, token = grishaToken,
                    session = grishaSession
                )
            }
        } catch (j: JSONException) {
            Log.e("error", "COULD NOT PARSE SERVER RESPONSE")
            //MainActivity.startFrom(this)
        }

        val logoutButton = findViewById<ImageView>(R.id.logout)
        logoutButton.setOnClickListener {
            if (VK.isLoggedIn())
                VK.logout()
            saveLoginAndPassword("-1", "-1")
            MainActivity.startFrom(this)
            finish()
        }



        if (VK.isLoggedIn()) {
            VK.execute(UserRequest(), object : VKApiCallback<VKUser> {
                override fun success(result: VKUser) {
                    if (!isFinishing) {
                        val user = result

                        this@UserActivity.runOnUiThread {
                            this@UserActivity.findViewById<TextView>(R.id.city).text =
                                user.town + ", " + user.country
                            this@UserActivity.findViewById<TextView>(R.id.name).text = user.name
                            this@UserActivity.findViewById<TextView>(R.id.surname).text =
                                user.surname
                        }
                        val myId = loadSecrets().toInt()
                        VK.execute(AvatarRequest(intArrayOf(myId)), object : VKApiCallback<String> {
                            override fun success(result: String) {
                                val imgView = findViewById<ImageView>(R.id.avatar)
                                Glide.with(this@UserActivity)
                                    .load(result)
                                    .apply(RequestOptions.bitmapTransform(RoundedCorners(250)))
                                    .into(imgView)

                            }

                            override fun fail(error: VKApiExecutionException) {
                                TODO("Not yet implemented")
                            }
                        })
                    }
                }

                override fun fail(error: VKApiExecutionException) {
                    Log.e("error", error.toString())
                }
            })
        }
        //loadSecrets()

        findViewById<ImageView>(R.id.goToProfile).setOnClickListener {
            ProfileActivity.startFrom(
                this@UserActivity,
                grishaSession,
                grishaToken
            )
        }
    }

    private fun saveLoginAndPassword(username: String, password: String) {
        this.openFileOutput("username", Context.MODE_PRIVATE).use {
            it.write(username.toByteArray(charset = StandardCharsets.UTF_8))
            it.flush()
        }
        this.openFileOutput("password", Context.MODE_PRIVATE).use {
            it.write(password.toByteArray(StandardCharsets.UTF_8))
            it.flush()
        }
    }

    fun loadSecrets(): String {
        return openFileInput("id")
            .bufferedReader().readLine()
    }

    override fun onBackPressed() {

    }

    companion object {
        fun startFrom(context: AppCompatActivity, message: String? = "") {
            val intent = Intent(context, UserActivity::class.java)
            intent.putExtra("msg", message)
            context.startActivity(intent)
        }
    }
}
