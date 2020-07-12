package com.newpage.salamport.groups

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
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

data class Group(
    var groupID: String, var groupAvatar: String? = null,
    var groupName: String? = null
)

class GroupAdapter(
    val context: Context, var groups: ArrayList<Group>,
    val session: String, val token: String
) : RecyclerView.Adapter<GroupAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupAvatar = view.findViewById<ImageView>(R.id.groupAvatarInList)
        val groupName = view.findViewById<TextView>(R.id.nameOfGroupInList)
        val groupLayout = view.findViewById<LinearLayout>(R.id.goToGroupLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(
            inflater.inflate(
                R.layout.group_view_holder,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return groups.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groups[position]

        holder.groupName.text = group.groupName
        Glide.with(context)
            .load(group.groupAvatar)
            .apply(
                RequestOptions
                    .bitmapTransform(RoundedCorners(250))
            )
            .into(holder.groupAvatar)

        holder.groupLayout.setOnClickListener {
            GroupActivity.startFrom(context, session, token, group.groupID!!)
        }
    }
}

class MyGroups : AppCompatActivity() {

    private lateinit var session: String
    private lateinit var token: String
    private val client = OkHttpClient.Builder().build()

    private lateinit var groupAdapter: GroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_groups)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")

        groupAdapter = GroupAdapter(this, ArrayList(), session, token)
        val mlayoutManager = LinearLayoutManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.groupContainer).apply {
            setHasFixedSize(true)
            layoutManager = mlayoutManager
            adapter = groupAdapter
        }

        findViewById<ImageView>(R.id.goToGroupSearch).setOnClickListener {
            SearchGroupsActivity.startFrom(
                this, session, token,
                findViewById<EditText>(R.id.searchGroupsQuery).text.toString()
            )
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
            val groups = ArrayList<Group>()
            val response =
                withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
            try {
                val responseJSON = JSONArray(response)
                for (i in 0 until responseJSON.length()) {
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
                    val responseForGroup = withContext(Dispatchers.IO) {
                        client.newCall(requestForGroup).await().body?.string()
                    }
                    val groupJSON = JSONObject(responseForGroup)
                    val group = Group(groupJSON.getString("id"), groupJSON.getString("photo"))
                    val groupName = groupJSON.getString("name");
                    val groupPhoto = groupJSON.getString("photo")
                    group.groupName = groupName; group.groupAvatar = groupPhoto; group.groupID =
                        responseJSON.getString(i)
                    groups.add(group)
                }
                runOnUiThread {
                    groupAdapter.groups = groups
                    groupAdapter.notifyDataSetChanged()
                }
            } catch (j: JSONException) {
                Log.e("e", "eee")
            }
        }
    }


    companion object {
        fun startFrom(context: Context, session: String, token: String) {
            val intent = Intent(context, MyGroups::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
