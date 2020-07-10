package com.newpage.salamport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        if (VK.isLoggedIn()) {
            val tokenAndId = loadTokenAndId()
            goToUserActivity(token = tokenAndId[0], userId = tokenAndId[1])
        }

        setContentView(R.layout.activity_main)

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

        try {
            val usernameAndPassword = loadUsernameAndPassword()
            val buttonWithoutVk = findViewById<Button>(R.id.loginWithoutVk)
            buttonWithoutVk.setOnClickListener {
                if (usernameAndPassword[0] != null)
                LoginPassword.startFrom(
                    this, usernameAndPassword[0],
                    usernameAndPassword[1]
                ) else {
                    LoginPassword.startFrom(
                        this, "-1", "-1")
                }
            }
        } catch (e: Exception) {
            val buttonWithoutVk = findViewById<Button>(R.id.loginWithoutVk)
            buttonWithoutVk.setOnClickListener {
                LoginPassword.startFrom(this) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                Log.i("Izi",token.accessToken)

                val filename = "token"; val emailFilename = "email"; val phoneFilename = "phone"
                val content = token.accessToken
                val email = token.email
                val userIdFilename = "id"

                this@MainActivity.openFileOutput(filename, Context.MODE_PRIVATE).use {
                    it.write(content.toByteArray(charset = StandardCharsets.UTF_8))
                    it.flush()
                }
                this@MainActivity.openFileOutput(userIdFilename, Context.MODE_PRIVATE).use {
                    it.write(token.userId.toString().toByteArray(StandardCharsets.UTF_8))
                    it.flush()
                }
                goToUserActivity(token = token.accessToken, userId = token.userId.toString())
            }

            override fun onLoginFailed(errorCode: Int) {
                Log.e("problem", "Login failed")
            }
        }
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun loadTokenAndId(): ArrayList<String> {
        val a = ArrayList<String>()
        a.add(this.openFileInput("token").bufferedReader().readLine())
        a.add(this.openFileInput("id").bufferedReader().readLine())
        return a
    }

    private fun loadUsernameAndPassword(): ArrayList<String> {
        val a = ArrayList<String>()
        a.add(this.openFileInput("username").bufferedReader().readLine())
        a.add(this.openFileInput("password").bufferedReader().readLine())
        return a
    }

    private fun goToUserActivity(token: String, userId: String) {
        val handler = Handler()
        handler.postDelayed(Runnable {
            val requestBody = FormBody.Builder()
                .add("vk_id", userId)
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
                    UserActivity.startFrom(this@MainActivity, message = resBody)
                }

                override fun onFailure(call: Call, e: IOException) {
                    TODO("Not yet implemented")
                }
            })
        }, 1000)

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
