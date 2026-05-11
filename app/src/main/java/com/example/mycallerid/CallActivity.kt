package com.example.mycallerid

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CallActivity : AppCompatActivity() {

    private lateinit var tvCallStatus: TextView
    private lateinit var btnAnswerCall: Button
    private lateinit var btnEndCall: Button

    // Yeh Android ka listener hai jo call kaatne par humein batayega
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            updateUI(state)
            if (state == Call.STATE_DISCONNECTED) {
                finish() // Doosre ne call kaata, toh app khud band ho jayegi
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // BUG FIX: Lock screen ke upar dikhane ke liye
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setContentView(R.layout.activity_call)

        val tvCallNumber = findViewById<TextView>(R.id.tvCallNumber)
        tvCallStatus = findViewById(R.id.tvCallStatus)
        btnAnswerCall = findViewById(R.id.btnAnswerCall)
        btnEndCall = findViewById(R.id.btnEndCall)

        val call = MyCallService.currentCall
        if (call != null) {
            val handle = call.details.handle
            tvCallNumber.text = handle?.schemeSpecificPart ?: "Unknown Number"
            
            // Listener ko zinda karna
            call.registerCallback(callCallback)
            updateUI(call.state)
        } else {
            tvCallNumber.text = "Unknown"
        }

        btnAnswerCall.setOnClickListener {
            MyCallService.currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        btnEndCall.setOnClickListener {
            MyCallService.currentCall?.disconnect()
            finish()
        }
    }

    private fun updateUI(state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                tvCallStatus.text = "Incoming Call..."
                tvCallStatus.setTextColor(android.graphics.Color.parseColor("#00E676"))
                btnAnswerCall.visibility = View.VISIBLE
            }
            Call.STATE_DIALING -> {
                tvCallStatus.text = "Calling..."
                tvCallStatus.setTextColor(android.graphics.Color.parseColor("#888888"))
                btnAnswerCall.visibility = View.GONE
            }
            Call.STATE_ACTIVE -> {
                tvCallStatus.text = "Call Active 🟢"
                tvCallStatus.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                btnAnswerCall.visibility = View.GONE // Baat shuru ho gayi, ab Answer button hide kar do
            }
            Call.STATE_DISCONNECTED -> {
                tvCallStatus.text = "Call Ended"
                tvCallStatus.setTextColor(android.graphics.Color.parseColor("#FF1744"))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MyCallService.currentCall?.unregisterCallback(callCallback)
    }
}