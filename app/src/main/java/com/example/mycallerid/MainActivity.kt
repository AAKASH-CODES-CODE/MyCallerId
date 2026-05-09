package com.example.mycallerid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ye line design file wale btnSync ko dhoondhegi
        val btnSync = findViewById<Button>(R.id.btnSync)

        btnSync.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 100)
            } else {
                syncContacts()
            }
        }
    }

    private fun syncContacts() {
        Toast.makeText(this, "Syncing Started...", Toast.LENGTH_SHORT).show()
        
        thread {
            val database = FirebaseDatabase.getInstance().reference.child("GlobalContacts")
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val number = it.getString(numberIndex) ?: ""

                    val cleanNumber = number.replace(Regex("[^0-9+]"), "")
                    
                    if (cleanNumber.isNotEmpty()) {
                        database.child(cleanNumber).setValue(name)
                    }
                }
            }
            
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Contacts Synced successfully!", Toast.LENGTH_LONG).show()
            }
        }
    }
}