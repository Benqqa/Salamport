package com.newpage.salamport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
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

data class Friend(
    val id: String, val name: String, val surname: String,
    val photo: String? = null
)

class FriendsAdapter(
    var friends: ArrayList<Friend>,
    private val context: AppCompatActivity,
    private val session: String, private val token: String,
    private val mode: String = ""
) :
    RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {

    private val client: OkHttpClient = OkHttpClient.Builder().build()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameAndUsername: TextView = view.findViewById(R.id.friendNameAndUsername)
        val friendAvatar: ImageView = view.findViewById(R.id.friendAvatar)
        val friendToChat: ImageView = view.findViewById(R.id.friendToChat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        if (mode != "request") {
            return ViewHolder(
                inflater.inflate(
                    R.layout.friends_view_holder,
                    parent,
                    false
                )
            )
        } else {
            return ViewHolder(
                inflater.inflate(
                    R.layout.request_friends_holder,
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return this.friends.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friends[position]
        holder.nameAndUsername.text = friend.name + " " + friend.surname
        Glide
            .with(context)
            .load(friend.photo)
            .apply(
                RequestOptions
                    .bitmapTransform(RoundedCorners(250))
            )
            .into(holder.friendAvatar)

        if (mode != "request") {
            holder.friendToChat.setOnClickListener {
                GlobalScope.launch {
                    val requestBody = FormBody.Builder()
                        .add("session", session)
                        .add("token", token)
                        .add("member_id", friend.id)
                        .build()
                    val request = Request.Builder()
                        .url("https://salamport.newpage.xyz/api/create_chat_priv.php")
                        .post(requestBody)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                    val response =
                        withContext(Dispatchers.IO) {
                            client.newCall(request).await().body?.string()
                        }
                    val chatID = JSONArray(response).getString(0)
                    MessagesActivity.startFrom(
                        context, session = session,
                        token = token, chat_id = chatID, isPrivate = "true"
                    )
                }
            }
        } else {
            holder.friendToChat.setOnClickListener {
                GlobalScope.launch {
                    val requestBody = FormBody.Builder()
                        .add("session", session)
                        .add("token", token)
                        .add("friend_id", friend.id)
                        .build()
                    val request = Request.Builder()
                        .url("https://salamport.newpage.xyz/api/add_friend.php")
                        .post(requestBody)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                    val response =
                        withContext(Dispatchers.IO) {
                            client.newCall(request).await().body?.string()
                        }
                    context.finish()
                    context.overridePendingTransition(0, 0)
                    context.startActivity(context.intent)
                    context.overridePendingTransition(0, 0)
                }
            }
        }
    }
}

class FriendsActivity : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var fadapter: FriendsAdapter

    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var radapter: FriendsAdapter

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")

        findViewById<ImageView>(R.id.goToMessages).setOnClickListener {
            ChatActivity.startFrom(
                this, token = token,
                session = session
            )
        }

        findViewById<ImageView>(R.id.goToFeed).setOnClickListener {
            NewsActivity.startFrom(this, token = token, session = session)
        }

        val mLayoutManager = LinearLayoutManager(this@FriendsActivity)
        fadapter = FriendsAdapter(
            ArrayList(), this@FriendsActivity,
            session = session, token = token
        )
        recyclerView = findViewById(R.id.friendsRecyclerView)
        recyclerView.apply {
            setHasFixedSize(true)
            adapter = fadapter
            layoutManager = mLayoutManager
        }

        requestsRecyclerView = findViewById(R.id.requestFriendRecyclerView)
        radapter = FriendsAdapter(
            ArrayList(), this,
            session = session, token = token, mode = "request"
        )

        requestsRecyclerView.apply {
            setHasFixedSize(true)
            adapter = radapter
            layoutManager = LinearLayoutManager(this@FriendsActivity)
        }

        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/friends.php")
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

        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/friends_mb.php")
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
                updateRequestRecyclerView(friends)
            }
        }

        findViewById<ImageView>(R.id.findUsersButton).setOnClickListener {
            UserSearchResult.startFrom(
                this, session = session,
                token = token, searchQuery = findViewById<EditText>(R.id.searchFriendsQuery)
                    .text.toString()
            )
        }
    }

    private fun updateRecyclerView(friends: ArrayList<Friend>) {
        fadapter.friends = friends
        fadapter.notifyDataSetChanged()
    }

    private fun updateRequestRecyclerView(friends: ArrayList<Friend>) {
        radapter.friends = friends
        radapter.notifyDataSetChanged()
    }

    companion object {
        fun startFrom(context: Context, token: String, session: String) {
            val intent = Intent(context, FriendsActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
