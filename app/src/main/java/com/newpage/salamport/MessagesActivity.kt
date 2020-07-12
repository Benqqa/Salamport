package com.newpage.salamport

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.LinearLayoutCompat
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class Message(
    val id: String,
    val owner: String,
    val content: String,
    val datetime: Date
)

class MessagesAdapter : RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private val context: Context
    private val messages: ArrayList<Message>
    private val avatarsMap: HashMap<String, String>?
    private val myID: String

    constructor(
        context: Context, messages: ArrayList<Message>,
        avatars: HashMap<String, String>? = null, myID: String
    ) : super() {
        this.context = context
        this.messages = messages
        this.avatarsMap = avatars
        this.myID = myID
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val content: TextView = view.findViewById(R.id.messageContent)
        val rightContent: TextView = view.findViewById(R.id.messageContentRight)
    }

    fun addMessage(msg: Message): Int {
        if (messages.size == 0) {
            messages.add(msg)
            return 0
        }
        if (msg.datetime.before(messages[0].datetime)) {
            messages.add(0, msg)
            return 0
        }
        if (msg.datetime.after(messages[messages.size - 1].datetime)) {
            messages.add(msg)
            return messages.size - 1
        }
        for (i in 1 until messages.size - 1) {
            if ((msg.datetime.after(messages[i].datetime)) &&
                (msg.datetime.before(messages[i + 1].datetime))
            ) {
                messages.add(i, msg)
                return i
            }
        }
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(inflater.inflate(R.layout.message_view_holder, parent, false))
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.content.text = ""
        holder.rightContent.text = ""
        if (msg.owner != myID) {
            holder.rightContent.text = msg.content
            holder.rightContent.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            holder.rightContent.layoutParams = LinearLayoutCompat.LayoutParams(
                0, LinearLayoutCompat.LayoutParams.MATCH_PARENT, 0.8f
            )
            holder.content.background = context.getDrawable(R.color.white)
            holder.rightContent.background = context.getDrawable(R.drawable.background)
            holder.content.layoutParams = LinearLayoutCompat.LayoutParams(
                0, LinearLayoutCompat.LayoutParams.MATCH_PARENT, 0.2f
            )
        } else {
            holder.content.text = msg.content
            holder.content.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            holder.content.layoutParams = LinearLayoutCompat.LayoutParams(
                0, LinearLayoutCompat.LayoutParams.MATCH_PARENT, 0.8f
            )
            holder.rightContent.background = context.getDrawable(R.color.white)
            holder.content.background = context.getDrawable(R.drawable.background)
            holder.rightContent.layoutParams = LinearLayoutCompat.LayoutParams(
                0, LinearLayoutCompat.LayoutParams.MATCH_PARENT, 0.2f
            )
        }
    }
}

class MessagesActivity : AppCompatActivity() {

    private lateinit var grishaToken: String
    private lateinit var grishaSession: String
    private lateinit var chat_id: String

    private lateinit var messagesRecycler: RecyclerView
    private lateinit var messagesHolder: MessagesAdapter
    private lateinit var mlayoutManager: RecyclerView.LayoutManager

    private lateinit var handler: Handler

    private var map: Hashtable<String, Boolean> = Hashtable()

    private val client: OkHttpClient = OkHttpClient.Builder().build()

    private val messages = ArrayList<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        grishaToken = intent.getStringExtra("token")
        grishaSession = intent.getStringExtra("session")
        chat_id = intent.getStringExtra("chat_id")
        val isPrivate = intent.getStringExtra("is_private")


        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", grishaSession)
                .add("token", grishaToken)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/lk.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response =
                withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }
            try {
                val responseJSON = JSONObject(response)
                val myID = responseJSON.getString("id")
                this@MessagesActivity.runOnUiThread {
                    if (isPrivate == "true") {
                        val arrayUsers = intent.getStringArrayListExtra("array_users")
                        val arrayAvatars = intent.getStringArrayListExtra("array_avatars")
                        val usersAvatars = HashMap<String, String>()

                        for (i in 0 until arrayUsers.size) {
                            usersAvatars[arrayUsers[i]] = arrayAvatars[i]
                        }
                        Glide
                            .with(this@MessagesActivity)
                            .load(arrayAvatars[0])
                            .apply(
                                RequestOptions
                                    .bitmapTransform(RoundedCorners(250))
                            )
                            .into(findViewById(R.id.messagesAvatar))

                        messagesHolder =
                            MessagesAdapter(
                                this@MessagesActivity,
                                messages,
                                usersAvatars,
                                myID = myID
                            )
                    } else {
                        messagesHolder =
                            MessagesAdapter(this@MessagesActivity, messages, myID = myID)
                    }
                    mlayoutManager = LinearLayoutManager(this@MessagesActivity)
                    messagesRecycler = findViewById<RecyclerView>(R.id.messagesRecycler).apply {
                        setHasFixedSize(true)
                        layoutManager = mlayoutManager
                        adapter = messagesHolder
                    }
                    handler = Handler()
                    loadMessagesFromServer()
                }
            } catch (j: JSONException) {
                Log.e("j", "sorru")
            }
        }


        findViewById<Button>(R.id.messageSend).setOnClickListener {
            val typed: EditText = findViewById(R.id.typedMessage)

            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("session", grishaSession)
                    .add("token", grishaToken)
                    .add("chat_id", chat_id)
                    .add("text", typed.text.toString())
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/add_message.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).await() }
                runOnUiThread {
                    typed.text.clear()
                    loadMessagesFromServer()
                }
            }
        }

    }

    private fun loadMessagesFromServer() {
        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", grishaSession)
                .add("token", grishaToken)
                .add("chat_id", chat_id)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/chat_messages.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response = withContext(Dispatchers.IO) { client.newCall(request).await() }

            try {
                val messagesArray = JSONArray(response.body?.string())
                for (i in 0 until messagesArray.length()) {
                    val id = messagesArray.getJSONObject(i).getString("id")
                    val content = messagesArray.getJSONObject(i).getString("text")
                    val ownerID = messagesArray.getJSONObject(i).getString("sender_id")
                    val datetime = messagesArray.getJSONObject(i).getString("date")

                    val formatter = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss")

                    val message = Message(id, ownerID, content, formatter.parse(datetime))
                    if (!map.containsKey(id)) {
                        runOnUiThread {
                            val pos = this@MessagesActivity.messagesHolder.addMessage(message)
                            this@MessagesActivity.messagesHolder.notifyDataSetChanged()
                            this@MessagesActivity.messagesRecycler.scrollToPosition(pos)
                            map[id] = true
                        }
                    }
                }

            } catch (j: JSONException) {
                Log.e("error", "json error")
            }
        }

        handler.postDelayed({ loadMessagesFromServer() }, 5000)
    }

    companion object {
        fun startFrom(
            context: Context,
            session: String,
            token: String,
            chat_id: String,
            isPrivate: String
        ) {
            val intent = Intent(context, MessagesActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.putExtra("chat_id", chat_id)

            val client = OkHttpClient.Builder().build()

            val arrayUsers = ArrayList<String>()
            val arrayAvatars = ArrayList<String>()

            if (isPrivate != "null") {
                GlobalScope.launch {
                    val requestBody = FormBody.Builder()
                        .add("token", token)
                        .add("session", session)
                        .add("chat_id", chat_id)
                        .build()
                    val request = Request.Builder()
                        .url("https://salamport.newpage.xyz/api/chat_members.php")
                        .post(requestBody)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                    val responseString =
                        withContext(Dispatchers.IO) {
                            client.newCall(request).await().body?.string()
                        }
                    val jsonResponse = JSONArray(responseString)
                    for (i in 0 until jsonResponse.length()) {
                        val userID = jsonResponse.getString(i)

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
                        val userProfilePhoto = withContext(Dispatchers.IO) {
                            client.newCall(requestForProfile).await().body?.string()
                        }
                        val userProfilePhotoString = JSONObject(userProfilePhoto).getString("photo")
                        arrayUsers.add(jsonResponse.getString(i))
                        arrayAvatars.add(userProfilePhotoString)
                    }

                    intent.putExtra("is_private", "true")
                    intent.putStringArrayListExtra("array_users", arrayUsers)
                    intent.putStringArrayListExtra("array_avatars", arrayAvatars)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    context.startActivity(intent)
                }
            } else {
                intent.putExtra("is_private", "false")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(intent)
            }
        }
    }
}
