package com.newpage.salamport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
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

class UsersSearchAdapter(
    var users: ArrayList<Friend>,
    private val context: UserSearchResult,
    private val session: String, private val token: String
) :
    RecyclerView.Adapter<UsersSearchAdapter.ViewHolder>() {

    private val client: OkHttpClient = OkHttpClient.Builder().build()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameAndUsername: TextView = view.findViewById(R.id.friendNameAndUsername)
        val friendAvatar: ImageView = view.findViewById(R.id.friendAvatar)
        val friendToChat: ImageView = view.findViewById(R.id.friendToChat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(
            inflater.inflate(
                R.layout.request_friends_holder,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return this.users.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.nameAndUsername.text = user.name + " " + user.surname
        Glide
            .with(context)
            .load(user.photo)
            .apply(
                RequestOptions
                    .bitmapTransform(RoundedCorners(250))
            )
            .into(holder.friendAvatar)

        holder.friendToChat.setOnClickListener {
            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("session", session)
                    .add("token", token)
                    .add("friend_id", user.id)
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/add_friend.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val response =
                    withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
                if (response != "false")
                    Toast.makeText(context, "Заявка отправлена", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class UserSearchResult : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var fadapter: UsersSearchAdapter

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_search_result)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")
        val query = intent.getStringExtra("query")

        recyclerView = findViewById(R.id.searchRecyclerView)
        val mLayoutAdapter = LinearLayoutManager(this)
        fadapter = UsersSearchAdapter(ArrayList(), this, session, token)

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

        recyclerView.apply {
            layoutManager = mLayoutAdapter
            setHasFixedSize(true)
            adapter = fadapter
        }
        makeQuery(query)
    }

    private fun makeQuery(query: String) {
        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .add("query", query)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/search_people.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response =
                withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
            try {
                val usersArray = JSONArray(response)

                val users = ArrayList<Friend>()
                for (i in 0 until usersArray.length()) {
                    val requestBodyForProfile = FormBody.Builder()
                        .add("session", session)
                        .add("token", token)
                        .add("user_id", usersArray.getString(i))
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

                    users.add(newFriend)
                }
                fadapter.users = users
                this@UserSearchResult.runOnUiThread {
                    fadapter.notifyDataSetChanged()
                }
            } catch (j: JSONException) {
                Log.e("e", "ead")
            }
        }
    }

    companion object {
        fun startFrom(
            context: Context, token: String, session: String,
            searchQuery: String
        ) {
            val intent = Intent(context, UserSearchResult::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.putExtra("query", searchQuery)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
