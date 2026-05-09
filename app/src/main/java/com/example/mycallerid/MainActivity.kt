// Location: <YourProject>/app/src/main/java/com/example/callerid/MainActivity.kt

package com.example.callerid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_READ_CONTACTS = 100
    }

    private lateinit var btnSyncContacts: Button
    private lateinit var tvStatus: TextView

    // Firebase Realtime Database reference
    private val database = FirebaseDatabase.getInstance()
    private val contactsRef = database.getReference("GlobalContacts")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSyncContacts = findViewById(R.id.btnSyncContacts)
        tvStatus = findViewById(R.id.tvStatus)

        btnSyncContacts.setOnClickListener {
            checkAndRequestContactsPermission()
        }
    }

    // Step 1: Check if READ_CONTACTS permission is granted; request it if not
    private fun checkAndRequestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            readAndSyncContacts()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_READ_CONTACTS
            )
        }
    }

    // Step 2: Handle the permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readAndSyncContacts()
            } else {
                Toast.makeText(this, "Permission denied. Cannot read contacts.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Step 3: Read contacts from the device and upload to Firebase
    private fun readAndSyncContacts() {
        tvStatus.text = "Reading contacts..."
        btnSyncContacts.isEnabled = false

        val contactsMap = mutableMapOf<String, String>()

        // Query the device contacts using ContentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: continue
                val rawNumber = it.getString(numberIndex) ?: continue

                // Clean phone number: keep only digits and leading '+'
                val cleanedNumber = cleanPhoneNumber(rawNumber)

                if (cleanedNumber.isNotEmpty()) {
                    // Use cleaned number as the key, name as the value
                    // Firebase keys cannot contain '.', '#', '$', '[', ']'
                    // so we use the number as key since it's already clean digits
                    contactsMap[cleanedNumber] = name
                }
            }
        }

        if (contactsMap.isEmpty()) {
            tvStatus.text = "No contacts found on device."
            btnSyncContacts.isEnabled = true
            return
        }

        tvStatus.text = "Syncing ${contactsMap.size} contacts to Firebase..."

        // Step 4: Upload the map to Firebase under GlobalContacts
        contactsRef.setValue(contactsMap)
            .addOnSuccessListener {
                tvStatus.text = "✓ Successfully synced ${contactsMap.size} contacts to Firebase."
                Toast.makeText(this, "Contacts synced!", Toast.LENGTH_SHORT).show()
                btnSyncContacts.isEnabled = true
            }
            .addOnFailureListener { error ->
                tvStatus.text = "✗ Sync failed: ${error.message}"
                Toast.makeText(this, "Sync failed: ${error.message}", Toast.LENGTH_LONG).show()
                btnSyncContacts.isEnabled = true
            }
    }

    // Cleans a raw phone number string:
    // - Keeps only digits and a leading '+' for international format
    // - e.g. "+1 (555) 123-4567" → "+15551234567"
    // - e.g. "0712 345 678"      → "0712345678"
    private fun cleanPhoneNumber(raw: String): String {
        val stripped = raw.trim()
        val hasPlus = stripped.startsWith("+")
        val digitsOnly = stripped.filter { it.isDigit() }
        return if (hasPlus) "+$digitsOnly" else digitsOnly
    }
}
