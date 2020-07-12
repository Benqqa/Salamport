package com.newpage.salamport.services

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import com.location.aravind.getlocation.GeoLocator
import com.newpage.salamport.ChatActivity
import com.newpage.salamport.FriendsActivity
import com.newpage.salamport.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
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

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )

        findViewById<Button>(R.id.goToTinderFilter).setOnClickListener {
            FilterActivity.startFrom(
                this, session = session,
                token = token
            )
        }

        findViewById<Button>(R.id.OTKLIKI).setOnClickListener {
            MatchesActivity.startFrom(this, session = session, token = token)
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
            val jsonResponse = JSONArray(responseString)
            val k = jsonResponse.getString(0)
            runOnUiThread {
                findViewById<SwitchCompat>(R.id.SWITCH).isChecked = !((k == "0") || (k == "null"))
            }
            runOnUiThread {
                findViewById<SwitchCompat>(R.id.SWITCH).setOnCheckedChangeListener { _, isChecked ->
                    var checks = ""
                    checks = if (isChecked) "1" else "0"

                    GlobalScope.launch {
                        val requestBodySWITCH = FormBody.Builder()
                            .add("token", token)
                            .add("session", session)
                            .add("use", checks)
                            .build()
                        val requestSWITCH = Request.Builder()
                            .url("https://salamport.newpage.xyz/api/set_dating_status.php")
                            .post(requestBodySWITCH)
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .build()
                        val responseStringSWITCH = withContext(Dispatchers.IO) {
                            client.newCall(requestSWITCH).await().body?.string()
                        }
                    }

                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val geoLocator = GeoLocator(applicationContext, this)
        val a = geoLocator.lattitude
        val b = geoLocator.longitude

        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("token", token)
                .add("session", session)
                .add("lat", a.toString())
                .add("lon", b.toString())
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/send_geo.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val responseString =
                withContext(Dispatchers.IO) {
                    client.newCall(request).await().body?.string()
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
