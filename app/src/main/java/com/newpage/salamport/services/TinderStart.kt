package com.newpage.salamport.services

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.widget.SwitchCompat
import com.newpage.salamport.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await

class TinderStart : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String

    private var checked = false

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tinder_start)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")

        findViewById<Button>(R.id.goToTinderFilter).setOnClickListener {
            FilterActivity.startFrom(this, session = session,
            token = token)
        }

        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("token", token)
                .add("session", session)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/my_dating_status.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val responseString = withContext(Dispatchers.IO) {
                client.newCall(request).await().body?.string()
            }
            runOnUiThread {
                findViewById<SwitchCompat>(R.id.SWITCH).isChecked = responseString != "null"
            }
        }

        findViewById<SwitchCompat>(R.id.SWITCH).setOnCheckedChangeListener { _, isChecked ->
            checked = isChecked
            var checkedS = ""
            if (checked) checkedS = "1" else checkedS = "null"
            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("token", token)
                    .add("session", session)
                    .add("use", checkedS)
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/set_dating_status.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseString = withContext(Dispatchers.IO) {
                    client.newCall(request).await().body?.string()
                }
            }
        }
    }

            companion object {
            fun startFrom(context: Context, session: String, token: String) {
                val intent = Intent(context, TinderStart::class.java)
                intent.putExtra("session", session)
                intent.putExtra("token", token)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(intent)
            }
        }
        }
