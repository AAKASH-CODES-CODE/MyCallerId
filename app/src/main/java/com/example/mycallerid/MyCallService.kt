package com.example.mycallerid

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class MyCallService : InCallService() {

    companion object {
        var currentCall: Call? = null
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        
        // Multi-call safety: Agar pehle se baat chal rahi hai, toh naya call abhi handle nahi karenge
        if (currentCall != null && currentCall!!.state == Call.STATE_ACTIVE) {
            return 
        }

        currentCall = call

        val intent = Intent(this, CallActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (currentCall == call) {
            currentCall = null
        }
    }
}