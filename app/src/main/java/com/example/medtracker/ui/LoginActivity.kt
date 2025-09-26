package com.example.medtracker.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.medtracker.R
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.medtracker.data.db.AppDatabase
import com.example.medtracker.data.db.entities.UserProfile
import kotlinx.coroutines.launch
import android.widget.Toast
import java.util.UUID

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etDob = findViewById<EditText>(R.id.etDob)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "medtracker-db"
        ).build()
        val userProfileDao = db.userProfileDao()

        btnLogin.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val dob = etDob.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use UUID for uid for robust uniqueness
            val userProfile = UserProfile(uid = UUID.randomUUID().toString(), firstName = firstName, lastName = lastName, dob = dob)
            lifecycleScope.launch {
                try {
                    userProfileDao.upsert(userProfile)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "User saved!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    }

}