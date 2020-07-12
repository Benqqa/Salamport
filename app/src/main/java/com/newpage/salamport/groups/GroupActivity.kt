package com.newpage.salamport.groups

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
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

class GroupActivity : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String
    private lateinit var groupID: String

    private lateinit var postAdapter: PostAdapter
    private val client = OkHttpClient.Builder().build()

    private var amISubscribed = false

    private lateinit var groupAvatar: ImageView
    private lateinit var groupName: TextView

    private var wasLogoAndNameLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")
        groupID = intent.getStringExtra("group_id")

        postAdapter = PostAdapter(this, ArrayList(), session, token)
        val mlayoutManager = LinearLayoutManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.postContainer).apply {
            setHasFixedSize(true)
            layoutManager = mlayoutManager
            adapter = postAdapter
        }

        groupAvatar = findViewById(R.id.groupAvatar)
        groupName = findViewById(R.id.groupName)

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

        checkAmISubscribed()

        findViewById<ImageView>(R.id.subscribeGroup).setOnClickListener {
            val query = (!amISubscribed).toString()
            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("session", session)
                    .add("token", token)
                    .add("query", query)
                    .add("group_id", groupID)
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/sub_group.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val response =
                    withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
                checkAmISubscribed()
            }
        }

        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .add("group_id", groupID)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/group_posts.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val posts = ArrayList<Post>()
            val response =
                withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
            try {
                val responseJSON = JSONArray(response)
                for (i in 0 until responseJSON.length()) {
                    val requestBodyForPost = FormBody.Builder()
                        .add("session", session)
                        .add("token", token)
                        .add("post_id", responseJSON.getString(i))
                        .build()
                    val requestForPost = Request.Builder()
                        .url("https://salamport.newpage.xyz/api/post_view.php")
                        .post(requestBodyForPost)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                    val responseForPost = withContext(Dispatchers.IO) {
                        client.newCall(requestForPost).await().body?.string()
                    }
                    val postJSON = JSONObject(responseForPost)
                    val post = Post(postJSON.getString("id"), postJSON.getString("text"))
                    val requestBodyForGroup = FormBody.Builder()
                        .add("session", session)
                        .add("token", token)
                        .add("group_id", responseJSON.getString(i))
                        .build()
                    val requestForGroup = Request.Builder()
                        .url("https://salamport.newpage.xyz/api/group_info.php")
                        .post(requestBodyForGroup)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                    val group = withContext(Dispatchers.IO) {
                        client.newCall(requestForGroup).await().body?.string()
                    }
                    val groupJSON = JSONObject(group)
                    val groupName = groupJSON.getString("name")
                    val groupPhoto = groupJSON.getString("photo")
                    post.groupName = groupName; post.groupPhoto = groupPhoto; post.groupID =
                        responseJSON.getString(i)
                    posts.add(post)
                    if (!this@GroupActivity.wasLogoAndNameLoaded) {
                        runOnUiThread {
                            this@GroupActivity.groupName.text = groupName
                            Glide.with(this@GroupActivity)
                                .load(post.groupPhoto)
                                .apply(
                                    RequestOptions
                                        .bitmapTransform(RoundedCorners(250))
                                )
                                .into(this@GroupActivity.groupAvatar)
                            wasLogoAndNameLoaded = true
                        }
                    }
                }
                runOnUiThread {
                    postAdapter.posts = posts
                    postAdapter.notifyDataSetChanged()
                }
            } catch (j: JSONException) {
            }
        }
    }

    private fun checkAmISubscribed() {
        runOnUiThread {
            if (amISubscribed) {
                findViewById<ImageView>(R.id.subscribeGroup).setImageResource(R.drawable.ic_clear_black_24dp)
            } else {
                findViewById<ImageView>(R.id.subscribeGroup).setImageResource(R.drawable.ic_plyus_01)
            }
        }
        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/show_subs.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response =
                withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
            val responseJSON = JSONArray(response)
            var temp = false
            for (i in 0 until responseJSON.length()) {
                if (responseJSON.getString(i) == groupID) {
                    amISubscribed = true
                    temp = true
                }
            }
            if (!temp) amISubscribed = false
        }
    }

    //{"id":"1","group_id":"1","member_id":"11","text":"\u0417\u0434\u0435\u0441\u044c \u0431\u0443\u0434\u0435\u0442 \u043d\u043e\u0440\u043c\u0430\u043b\u044c\u043d\u044b\u0439 \u0442\u0435\u043a\u0441\u0442, \u041e\u043b\u044f \u043f\u043e\u0442\u043e\u043c \u043d\u0430\u043f\u0438\u0448\u0435\u0442","video":"0","photo1":"0","photo2":"0","photo3":"0","photo4":"0","photo5":"0"}
    companion object {
        fun startFrom(context: Context, session: String, token: String, groupID: String) {
            val intent = Intent(context, GroupActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.putExtra("group_id", groupID)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
