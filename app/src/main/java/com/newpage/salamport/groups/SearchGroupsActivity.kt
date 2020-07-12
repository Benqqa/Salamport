package com.newpage.salamport.groups

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import org.json.JSONException
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await

class SearchGroupsActivity : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String
    private lateinit var query: String

    private lateinit var groupAdapter: GroupAdapter

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_groups)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")
        query = intent.getStringExtra("query")

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

        groupAdapter = GroupAdapter(this, ArrayList(), session, token)
        val mlayoutManager = LinearLayoutManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.groupContainerWithSeacrh).apply {
            setHasFixedSize(true)
            layoutManager = mlayoutManager
            adapter = groupAdapter
        }
        makeQuery(query)
    }


    private fun makeQuery(query: String) {
        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .add("name", query)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/search_groups.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response =
                withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
            try {
                val usersArray = JSONArray(response)

                val users = ArrayList<Group>()
                for (i in 0 until usersArray.length()) {
                    val requestBodyForProfile = FormBody.Builder()
                        .add("session", session)
                        .add("token", token)
                        .add("group_id", usersArray.getString(i))
                        .build()
                    val requestForProfile = Request.Builder()
                        .url("https://salamport.newpage.xyz/api/group_info.php")
                        .post(requestBodyForProfile)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                    val responseForProfile = withContext(Dispatchers.IO) {
                        client.newCall(requestForProfile).await().body?.string()
                    }
                    val friendProfile = JSONObject(responseForProfile)
                    val newFriend = Group(
                        friendProfile.getString("id"), friendProfile.getString("photo"),
                        friendProfile.getString("name")
                    )
                    users.add(newFriend)
                }
                groupAdapter.groups = users
                this@SearchGroupsActivity.runOnUiThread {
                    groupAdapter.notifyDataSetChanged()
                }
            } catch (j: JSONException) {
                Log.e("e", "ead")
            }
        }
    }

    companion object {
        fun startFrom(context: Context, session: String, token: String, query: String) {
            val intent = Intent(context, SearchGroupsActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.putExtra("query", query)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
