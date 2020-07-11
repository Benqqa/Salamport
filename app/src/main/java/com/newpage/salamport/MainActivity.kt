package com.newpage.salamport

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.newpage.salamport.media.MusicList
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    val client = OkHttpClient.Builder().build()

    private lateinit var send: Intent

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (VK.isLoggedIn()) {
            val grishaTokenAndSession = loadGrishaTokenAndSession()

            UserActivity.startFrom(this, grishaToken = grishaTokenAndSession[0],
                grishaSession = grishaTokenAndSession[1])
        }

        val button: AppCompatButton = findViewById(R.id.login)
        button.setOnClickListener {
            if (!VK.isLoggedIn()) {
                VK.login(
                    this,
                    arrayListOf(VKScope.WALL,
                        VKScope.EMAIL,
                        VKScope.STATS,
                        VKScope.PHONE,
                        VKScope.OFFLINE)
                )
            }
        }

        val loginWithoutVK: AppCompatButton = findViewById(R.id.loginWithoutVk)
        loginWithoutVK.setOnClickListener {
            val grishaTokenAndSession = loadGrishaTokenAndSession()
            if ((grishaTokenAndSession[0] != "-1") &&
                (grishaTokenAndSession[1] != "-1")) {
                UserActivity.startFrom(this, grishaToken = grishaTokenAndSession[0],
                grishaSession = grishaTokenAndSession[1])
            } else {
                LoginPassword.startFrom(this)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                Log.i("Izi",token.accessToken)

                loginThroughVK(token = token.accessToken, vk_id = token.userId.toString())
            }

            override fun onLoginFailed(errorCode: Int) {
                Log.e("problem", "Login failed")
            }
        }
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun loadGrishaTokenAndSession(): ArrayList<String> {
        val a = ArrayList<String>()
        return try {
            a.add(this.openFileInput("token").bufferedReader().readLine())
            a.add(this.openFileInput("session").bufferedReader().readLine())
            a
        } catch (e : Exception) {
            a.add("-1"); a.add("-1")
            a
        }
    }


    private fun loginThroughVK(vk_id: String, token: String) {
        val handler = Handler()
        handler.postDelayed(Runnable {
            val requestBody = FormBody.Builder()
                .add("vk_id", vk_id)
                .add("token", token)
                .build()

            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/vk_auth.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val resBody = response.body!!.string()
                    try {
                        val jsonResponse = JSONObject(resBody)
                        val grishaSession = jsonResponse.getString("session")
                        val grishaToken = jsonResponse.getString("token")
                        saveSessionAndToken(grishaToken, grishaSession)
                        UserActivity.startFrom(
                            this@MainActivity,
                            grishaToken, grishaSession
                        )
                    } catch (j: JSONException) {
                        Log.e("error", "invalid vk user")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    TODO("Not yet implemented")
                }
            })
        }, 1000)

    }

    private fun saveSessionAndToken(token: String, session: String) {
        this.openFileOutput("session", Context.MODE_PRIVATE).use {
            it.write(session.toByteArray(charset = StandardCharsets.UTF_8))
            it.flush()
        }
        this.openFileOutput("token", Context.MODE_PRIVATE).use {
            it.write(token.toByteArray(StandardCharsets.UTF_8))
            it.flush()
        }
    }

    companion object {
        fun startFrom(context: Context, message: String = "") {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("msg", message)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
