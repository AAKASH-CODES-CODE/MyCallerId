package com.example.mycallerid

import android.os.Bundle
import android.telecom.Call
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        val tvCallNumber = findViewById<TextView>(R.id.tvCallNumber)
        val btnEndCall = findViewById<Button>(R.id.btnEndCall)

        // Number nikal kar screen par dikhana
        val call = MyCallService.currentCall
        if (call != null) {
            val handle = call.details.handle
            tvCallNumber.text = handle?.schemeSpecificPart ?: "Unknown Number"
        } else {
            tvCallNumber.text = "Unknown"
        }

        // Call cut karne ka button
        btnEndCall.setOnClickListener {
            MyCallService.currentCall?.disconnect()
            finish() // Screen band kar do
        }
    }
}