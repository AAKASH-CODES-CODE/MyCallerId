package com.example.mycallerid

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
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
    private lateinit var btnMute: Button
    private lateinit var btnSpeaker: Button
    
    private var isMuted = false
    private var isSpeakerOn = false
    private lateinit var audioManager: AudioManager

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            updateUI(state)
            if (state == Call.STATE_DISCONNECTED) {
                // Call cut hone par audio reset karna zaroori hai
                audioManager.isMicrophoneMute = false
                audioManager.isSpeakerphoneOn = false
                finish() 
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val tvCallNumber = findViewById<TextView>(R.id.tvCallNumber)
        tvCallStatus = findViewById(R.id.tvCallStatus)
        btnAnswerCall = findViewById(R.id.btnAnswerCall)
        btnEndCall = findViewById(R.id.btnEndCall)
        btnMute = findViewById(R.id.btnMute)
        btnSpeaker = findViewById(R.id.btnSpeaker)

        val call = MyCallService.currentCall
        if (call != null) {
            val handle = call.details.handle
            tvCallNumber.text = handle?.schemeSpecificPart ?: "Unknown Number"
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
        }

        // Mute Logic
        btnMute.setOnClickListener {
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            btnMute.text = if (isMuted) "Unmuted ❌" else "Mute 🎙️"
            btnMute.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(if (isMuted) "#FF1744" else "#333333")
            )
        }

        // Speaker Logic
        btnSpeaker.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            audioManager.isSpeakerphoneOn = isSpeakerOn
            btnSpeaker.text = if (isSpeakerOn) "Speaker ON 🔊" else "Speaker 🔈"
            btnSpeaker.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(if (isSpeakerOn) "#00E676" else "#333333")
            )
        }
    }

    private fun updateUI(state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                tvCallStatus.text = "Incoming Call..."
                tvCallStatus.setTextColor(android.graphics.Color.parseColor("#00E676"))
                btnAnswerCall.visibility = View.VISIBLE
                btnMute.visibility = View.GONE
                btnSpeaker.visibility = View.GONE
            }
            Call.STATE_DIALING -> {
                tvCallStatus.text = "Calling..."
                tvCallStatus.setTextColor(android.graphics.Color.parseColor("#888888"))
                btnAnswerCall.visibility = View.GONE
                btnMute.visibility = View.VISIBLE
                btnSpeaker.visibility = View.VISIBLE
            }
            Call.STATE_ACTIVE -> {
                tvCallStatus.text = "Call Active 🟢"
                tvCallStatus.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                btnAnswerCall.visibility = View.GONE 
                btnMute.visibility = View.VISIBLE
                btnSpeaker.visibility = View.VISIBLE
            }
            Call.STATE_DISCONNECTED -> {
                tvCallStatus.text = "Call Ended"
                tvCallStatus.setTextColor(android.graphics.Color.parseColor("#FF1744"))
                btnMute.visibility = View.GONE
                btnSpeaker.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MyCallService.currentCall?.unregisterCallback(callCallback)
    }
}