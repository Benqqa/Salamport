package com.newpage.salamport.groups

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.newpage.salamport.ChatAdapter
import com.newpage.salamport.MessagesAdapter
import com.newpage.salamport.R
import com.newpage.salamport.services.TinderStart
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


data class Post(
    val id: String, val text: String, var groupName: String? = null,
    var groupPhoto: String? = null, var groupID: String? = null
)

class PostAdapter(
    val context: Context, var posts: ArrayList<Post>,
    val session: String, val token: String
) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupAvatar = view.findViewById<ImageView>(R.id.groupAvatar)
        val groupName = view.findViewById<TextView>(R.id.groupName)
        val postContent = view.findViewById<TextView>(R.id.postContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(
            inflater.inflate(
                R.layout.post_view_holder,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]

        holder.postContent.text = post.text
        holder.groupName.text = post.groupName
        Glide.with(context)
            .load(post.groupPhoto)
            .apply(
                RequestOptions
                    .bitmapTransform(RoundedCorners(250))
            )
            .into(holder.groupAvatar)

        holder.groupAvatar.setOnClickListener {
            GroupActivity.startFrom(context, session, token, post.groupID!!)
        }
    }
}

class NewsActivity : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String

    private lateinit var postAdapter: PostAdapter

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")

        postAdapter = PostAdapter(this, ArrayList(), session, token)
        val mlayoutManager = LinearLayoutManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.postContainer).apply {
            setHasFixedSize(true)
            layoutManager = mlayoutManager
            adapter = postAdapter
        }

        findViewById<Button>(R.id.goToGroupList).setOnClickListener {
            MyGroups.startFrom(this, session = session, token = token)
        }

        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/feed.php")
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
        fun startFrom(context: Context, session: String, token: String) {
            val intent = Intent(context, NewsActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
