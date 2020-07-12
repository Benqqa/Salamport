package com.newpage.salamport.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.newpage.salamport.ChatActivity
import com.newpage.salamport.FriendsActivity
import com.newpage.salamport.R
import com.newpage.salamport.UserActivity
import com.newpage.salamport.support.GestureHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await


data class Dating(
    val id: String, val name: String, val surname: String,
    val gender: String, val nativeCity: String, val radius: String, val avatar: String
)

class TinderActivity : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String
    private lateinit var radius: String
    private lateinit var sex: String
    private lateinit var min_b: String
    private lateinit var max_b: String

    private val client = OkHttpClient.Builder().build()
    private val datings = ArrayList<Dating>()

    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tinder)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")
        radius = intent.getStringExtra("radius")
        sex = intent.getStringExtra("sex")
        min_b = intent.getStringExtra("min_b")
        max_b = intent.getStringExtra("max_b")

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


        GlobalScope.launch {
            try {
                val requestBody = FormBody.Builder()
                    .add("token", token)
                    .add("session", session)
                    .add("radius", radius)
                    .add("sex", sex)
                    .add("min_b", min_b)
                    .add("max_b", max_b)
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/get_dating.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseString = withContext(Dispatchers.IO) {
                    client.newCall(request).await().body?.string()
                }

                val datingsJSON = JSONArray(responseString)
                for (i in 0 until datingsJSON.length()) {
                    val userID = datingsJSON.getJSONObject(i).getString("id")
                    val radius = datingsJSON.getJSONObject(i).getString("radius")
                    val requestBodyForProfile = FormBody.Builder()
                        .add("token", token)
                        .add("session", session)
                        .add("user_id", userID)
                        .build()
                    val requestForProfile = Request.Builder()
                        .url("https://salamport.newpage.xyz/api/view_profile.php")
                        .post(requestBodyForProfile)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                    val userProfileString = withContext(Dispatchers.IO) {
                        client.newCall(requestForProfile).await().body?.string()
                    }
                    val userProfileJson = JSONObject(userProfileString)
                    val userAvatar = userProfileJson.getString("photo")
                    val userName = userProfileJson.getString("name")
                    val userSurname = userProfileJson.getString("surname")
                    val gender = userProfileJson.getString("sex")
                    val nativeCity = userProfileJson.getString("native_city")
                    val dating = Dating(
                        id = userID, name = userName, surname = userSurname, avatar = userAvatar,
                        radius = radius, gender = gender, nativeCity = nativeCity
                    )
                    datings.add(dating)
                }
                runOnUiThread {
                    renderDatings()
                }
            } catch (e: Exception) {
                Log.e("e", "ee")
            }
        }
        loadGestures()
    }

    private fun loadGestures() {
        val imgView = findViewById<ImageView>(R.id.imageOfT).setOnTouchListener(
            object : GestureHandler(this@TinderActivity) {
                override fun onSwipeRight() {
                    GlobalScope.launch {
                        val requestBody = FormBody.Builder()
                            .add("session", session)
                            .add("token", token)
                            .add("query", "true")
                            .add("user_id", datings[currentIndex - 1].id)
                            .build()
                        val request = Request.Builder()
                            .url("https://salamport.newpage.xyz/api/confirm_match.php")
                            .post(requestBody)
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .build()
                        val responseString = withContext(Dispatchers.IO) {
                            client.newCall(request).await().body?.string()
                        }
                        val r = responseString?.length
                    }
                    currentIndex++
                    renderDatings()
                }

                override fun onSwipeLeft() {
                    GlobalScope.launch {
                        val requestBody = FormBody.Builder()
                            .add("session", session)
                            .add("token", token)
                            .add("query", "false")
                            .add("user_id", datings[currentIndex - 1].id)
                            .build()
                        val request = Request.Builder()
                            .url("https://salamport.newpage.xyz/api/confirm_match.php")
                            .post(requestBody)
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .build()
                        val responseString = withContext(Dispatchers.IO) {
                            client.newCall(request).await()
                        }
                    }
                    currentIndex++
                    renderDatings()
                }
            }
        )
    }

    private fun renderDatings() {
        if (currentIndex >= datings.size) {
            Toast.makeText(this, "Никого нет", Toast.LENGTH_SHORT).show()
            UserActivity.startFrom(
                this, grishaSession = session,
                grishaToken = token

            )
            return
        }
        val upperText = findViewById<TextView>(R.id.textTINDER)
        val imgView = findViewById<ImageView>(R.id.imageOfT)
        val rangeText = findViewById<TextView>(R.id.tinderRange)
        val tinderNativeCity = findViewById<TextView>(R.id.tinderNativeCity)
        val genderAv = findViewById<ImageView>(R.id.tinderGender)

        when (datings[currentIndex].gender) {
            "1" -> genderAv.setImageResource(R.drawable.ic_muzh_01)
            "0" -> genderAv.setImageResource(R.drawable.ic_zhen_01)
        }
        Glide.with(this)
            .load(datings[currentIndex].avatar)
            .into(imgView)

        upperText.text = datings[currentIndex].name + " " + datings[currentIndex].surname
        rangeText.text = datings[currentIndex].radius + " метров"
        tinderNativeCity.text = "Родной город: " + datings[currentIndex].nativeCity
    }


    companion object {
        fun startFrom(
            context: Context, session: String, token: String,
            radius: String, sex: String, min_b: String, max_b: String
        ) {
            val intent = Intent(context, TinderActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.putExtra("radius", radius)
            intent.putExtra("sex", sex)
            intent.putExtra("min_b", min_b)
            intent.putExtra("max_b", max_b)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
