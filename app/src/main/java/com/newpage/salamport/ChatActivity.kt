package com.newpage.salamport

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

data class Chat(
    val id: String,
    val name: String,
    val photo: String,
    val isPrivate: String,
    val members: ArrayList<Int>? = null
)

class ChatAdapter(
    private val context: ChatActivity, private val chats: ArrayList<Chat>,
    private val session: String, private val token: String, private val client: OkHttpClient
) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photo: ImageView = view.findViewById(R.id.chatListAvatar)
        val text: TextView = view.findViewById(R.id.chatListName)
        val goToMessages: LinearLayout = view.findViewById(R.id.chatListButton)
        val lastMessage: TextView = view.findViewById(R.id.chatListLastMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(inflater.inflate(R.layout.chat_view_holder, parent, false))
    }

    override fun getItemCount(): Int {
        return chats.size
    }

    fun addChat(chat: Chat) {
        this.chats.add(chat)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]
        holder.text.text = chat.name
        val photo = chat.photo


        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", session)
                .add("token", token)
                .add("chat_id", chat.id)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/chat_messages.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response = withContext(Dispatchers.IO) { client.newCall(request).await() }


            try {
                val messagesArray = JSONArray(response.body?.string())
                val lastMessage = messagesArray
                    .getJSONObject(0)
                    .getString("text")
                setLastMessage(lastMessage, holder)
            } catch (j: JSONException) {
                Log.e("e", "sorry")
            }
        }


        if (chat.isPrivate != "null") {
            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("token", token)
                    .add("session", session)
                    .add("chat_id", chat.id)
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/chat_members.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseString =
                    withContext(Dispatchers.IO) { client.newCall(request).await().body?.string() }

                try {
                    val jsonWithChatUsers = JSONArray(responseString)
                    val userID = jsonWithChatUsers.getString(0)

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

                    val username = JSONObject(userProfilePhoto).getString("name")
                    loadWithGlide(userProfilePhotoString, holder)
                    loadUserName(username, holder)
                } catch (j: JSONException) {
                    Log.e("e", "json error")
                }
            }
        } else {
            loadWithGlide(photo, holder)
        }
        holder.goToMessages.setOnClickListener {
            MessagesActivity.startFrom(context, session, token, chat.id, chat.isPrivate)
        }
    }

    private fun setLastMessage(lastMessage: String, holder: ViewHolder) {
        context.runOnUiThread {
            if (lastMessage.length < 30) holder.lastMessage.text = lastMessage else
                holder.lastMessage.text = lastMessage.substring(30) + "..."
        }
    }

    private fun loadUserName(username: String, holder: ViewHolder) {
        context.runOnUiThread {
            holder.text.text = username
        }
    }

    private fun loadWithGlide(photo: String, holder: ViewHolder) {
        context.runOnUiThread {
            Glide
                .with(context)
                .load(photo)
                .apply(
                    RequestOptions
                        .bitmapTransform(RoundedCorners(250))
                )
                .into(holder.photo)
        }
    }
}

class ChatActivity : AppCompatActivity() {

    private lateinit var grishaToken: String
    private lateinit var grishaSession: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var mlayoutManager: RecyclerView.LayoutManager


    private val chats = ArrayList<Chat>()

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        grishaToken = intent.getStringExtra("token")
        grishaSession = intent.getStringExtra("session")

        setContentView(R.layout.activity_chat)


        findViewById<ImageView>(R.id.goToFriends).setOnClickListener {
            FriendsActivity.startFrom(
                this, token = grishaToken, session = grishaSession
            )
        }

        chatAdapter = ChatAdapter(this, chats, grishaSession, grishaToken, client)
        mlayoutManager = LinearLayoutManager(this)
        recyclerView = findViewById<RecyclerView>(R.id.chatContainer).apply {
            setHasFixedSize(true)
            layoutManager = mlayoutManager
            adapter = chatAdapter
        }
        recyclerView.adapter = chatAdapter

        findViewById<ImageView>(R.id.chatListCreateChat).setOnClickListener {
            val li = LayoutInflater.from(this)
            val promptsView: View = li.inflate(R.layout.prompt, null)

            val mDialogBuilder = AlertDialog.Builder(this)
            mDialogBuilder.setView(promptsView)
            val userInput = promptsView.findViewById<EditText>(R.id.input_text)

            mDialogBuilder.setPositiveButton(
                "OK",
                DialogInterface.OnClickListener { dialog, which ->
                    GlobalScope.launch {
                        val requestBody = FormBody.Builder()
                            .add("token", grishaToken)
                            .add("session", grishaSession)
                            .add("name", userInput.text.toString())
                            .build()
                        val request = Request.Builder()
                            .url("https://salamport.newpage.xyz/api/create_chat.php")
                            .post(requestBody)
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .build()
                        val responseString =
                            withContext(Dispatchers.IO) { client.newCall(request).await() }

                        val a = responseString.body?.string()
                        Log.i("e", a)
                        if (!a.equals("")) Log.i("f", "successful create")
                    }
                })
            mDialogBuilder.create().show()
        }
        loadChatList()
    }

    private fun loadChatList() {
        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("token", grishaToken)
                .add("session", grishaSession)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/get_messages.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val responseString = withContext(Dispatchers.IO) { client.newCall(request).await() }
            try {
                val responseJSON = JSONArray(responseString.body?.string())
                runOnUiThread { renderChatList(responseJSON) }
            } catch (j: JSONException) {
                UserActivity.startFrom(
                    this@ChatActivity, grishaToken,
                    grishaSession
                )
            }
        }
    }

    private fun renderChatList(responseJSON: JSONArray) {
        for (chat in 0 until responseJSON.length()) {
            val id = responseJSON[chat].toString()
            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("token", grishaToken)
                    .add("session", grishaSession)
                    .add("chat_id", id)
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/chat_info.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseString = withContext(Dispatchers.IO) {
                    client.newCall(request).await()
                }
                try {
                    val chat = JSONObject(responseString.body?.string())
                    val newChat = Chat(
                        chat.getString("id"), chat.getString("name"),
                        chat.getString("photo"), chat.getString("private")
                    )
                    this@ChatActivity.chatAdapter.addChat(newChat)
                    runOnUiThread {
                        this@ChatActivity.chatAdapter.notifyDataSetChanged()
                    }
                } catch (j: JSONException) {
                    Log.e("eroe", "chat_info no info")
                }
            }
        }
    }

    //{"id":"1","name":"My_chat","private":null,"photo":null}
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        this.grishaSession = savedInstanceState.getString("session")!!
        this.grishaToken = savedInstanceState.getString("token")!!
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("token", grishaToken)
        outState.putString("session", grishaSession)
    }

    companion object {
        fun startFrom(context: Context, session: String, token: String) {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
