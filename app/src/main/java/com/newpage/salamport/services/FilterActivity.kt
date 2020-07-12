package com.newpage.salamport.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.newpage.salamport.ChatActivity
import com.newpage.salamport.FriendsActivity
import com.newpage.salamport.R
import com.newpage.salamport.groups.NewsActivity

class FilterActivity : Activity() {

    private lateinit var session: String
    private lateinit var token: String

    private var age = 0
    private var range = 0
    private var sex = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")

        val ageSeeker = findViewById<SeekBar>(R.id.ageSeeker)
        val rangeSeeker = findViewById<SeekBar>(R.id.rangeSeeker)

        val isMale = findViewById<RadioButton>(R.id.radioButton1)
        var isFemale = findViewById<RadioButton>(R.id.radioButton2)

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

        isMale.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) sex = "1"
        }

        isFemale.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) sex = "2"
        }

        ageSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                age = progress
                findViewById<TextView>(R.id.ageTextView).text = age.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rangeSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                range = progress
                findViewById<TextView>(R.id.rangeTextView).text = range.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val goToTinderButton = findViewById<Button>(R.id.goToTinder)
        goToTinderButton.setOnClickListener {
            TinderActivity.startFrom(
                this, session = session,
                token = token, radius = range.toString(), sex = sex, min_b = "0",
                max_b = "3000"
            )
        }
    }

    companion object {
        fun startFrom(context: Context, session: String, token: String) {
            val intent = Intent(context, FilterActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
