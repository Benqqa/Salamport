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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
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

class MessagesAdapter(
    private val context: Context,
    private val messages: ArrayList<Message>
) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val content: TextView = view.findViewById(R.id.messageContent)
        val senderID: TextView = view.findViewById(R.id.messageSenderId)
        val msgDatetime: TextView = view.findViewById(R.id.messageDatetime)
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
            return messages.size-1
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
        holder.content.text = msg.content
        holder.msgDatetime.text = msg.datetime.toString()
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

    private var map: HashMap<String, Boolean> = HashMap()

    private val client: OkHttpClient = OkHttpClient.Builder().build()

    private val messages = ArrayList<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        grishaToken = intent.getStringExtra("token")
        grishaSession = intent.getStringExtra("session")
        chat_id = intent.getStringExtra("chat_id")
        handler = Handler()

        messagesHolder = MessagesAdapter(this, messages)
        mlayoutManager = LinearLayoutManager(this)
        messagesRecycler = findViewById<RecyclerView>(R.id.messagesRecycler).apply {
            setHasFixedSize(true)
            layoutManager = mlayoutManager
            adapter = messagesHolder
        }
        loadMessagesFromServer()

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

                    runOnUiThread {
                        if (!map.containsKey(id)) {
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
        fun startFrom(context: Context, session: String, token: String, chat_id: String) {
            val intent = Intent(context, MessagesActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.putExtra("chat_id", chat_id)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
