package com.newpage.salamport.groups

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        findViewById<ImageView>(R.id.subscribeGroup).setOnClickListener {
            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("session", session)
                    .add("token", token)
                    .add("query", "false")
                    .add("group_id", groupID)
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/sub_group.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val response =
                    withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
                val o = response?.length
            }
        }

        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .add("groud_id", groupID)
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
                    val response = withContext(Dispatchers.IO) {
                        client.newCall(requestForPost).await().body?.string()
                    }
                    val postJSON = JSONObject(response)
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
                    val groupName = groupJSON.getString("name");
                    val groupPhoto = groupJSON.getString("photo")
                    post.groupName = groupName; post.groupPhoto = groupPhoto; post.groupID =
                        responseJSON.getString(i)
                }
                runOnUiThread {
                    postAdapter.posts = posts
                    postAdapter.notifyDataSetChanged()
                }
            } catch (j: JSONException) {
            }
        }
    }

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
