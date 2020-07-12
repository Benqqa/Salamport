package com.newpage.salamport.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.newpage.salamport.*
import com.newpage.salamport.groups.NewsActivity
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

class MatchesActivity : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var fadapter: FriendsAdapter

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matches)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")
        val mLayoutManager = LinearLayoutManager(this)
        fadapter = FriendsAdapter(
            ArrayList(), this,
            session = session, token = token
        )
        recyclerView = findViewById(R.id.yourMatchesRecyclerView)
        recyclerView.apply {
            setHasFixedSize(true)
            adapter = fadapter
            layoutManager = mLayoutManager
        }
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
        loadMatches()
    }

    private fun loadMatches() {
        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/get_matches.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response =
                withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
            val friendsArray = JSONArray(response)

            val friends = ArrayList<Friend>()
            for (i in 0 until friendsArray.length()) {
                val requestBodyForProfile = FormBody.Builder()
                    .add("session", session)
                    .add("token", token)
                    .add("user_id", friendsArray.getString(i))
                    .build()
                val requestForProfile = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/view_profile.php")
                    .post(requestBodyForProfile)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseForProfile = withContext(Dispatchers.IO) {
                    client.newCall(requestForProfile).await().body?.string()
                }
                val friendProfile = JSONObject(responseForProfile)
                val newFriend = Friend(
                    friendProfile.getString("id"),
                    friendProfile.getString("name"), friendProfile.getString("surname"),
                    friendProfile.getString("photo")
                )

                friends.add(newFriend)
            }

            runOnUiThread {
                updateRecyclerView(friends)
            }
        }
    }

    private fun updateRecyclerView(friends: ArrayList<Friend>) {
        fadapter.friends = friends
        fadapter.notifyDataSetChanged()
    }

    companion object {
        fun startFrom(context: Context, session: String, token: String) {
            val intent = Intent(context, MatchesActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
