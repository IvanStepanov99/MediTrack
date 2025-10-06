package com.example.medtracker.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.medtracker.R
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medtracker.data.db.DbBuilder
import com.example.medtracker.data.db.entities.UserProfile
import kotlinx.coroutines.launch
import android.widget.Toast
import java.util.UUID
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.example.medtracker.data.session.SessionManager

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etDob = findViewById<EditText>(R.id.etDob)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val db = DbBuilder.getDatabase(applicationContext)
        val userProfileDao = db.userProfileDao()

        btnLogin.setOnClickListener {
            val firstNameRaw = etFirstName.text.toString()
            val lastNameRaw = etLastName.text.toString()
            val dobRaw = etDob.text.toString()

            val firstName = normalizeName(firstNameRaw)
            val lastName = normalizeName(lastNameRaw)

            if (firstName.isEmpty() || lastName.isEmpty() || dobRaw.isBlank()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dob = normalizeDob(dobRaw.trim())
            if (dob == null) {
                Toast.makeText(this, "Invalid date format. Use DD/MM/YYYY.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            lifecycleScope.launch {
                try {
                    // Exact match: name + normalized DOB => login existing
                    val exact = userProfileDao.findByNameAndDob(firstName, lastName, dob)
                    if (exact != null) {
                        userProfileDao.touchSignAt(exact.uid, System.currentTimeMillis())
                        // Persist current uid
                        SessionManager.setCurrentUid(applicationContext, exact.uid)
                        runOnUiThread {
                            navigateToMain(uid = exact.uid, firstName = exact.firstName ?: firstName)
                        }
                        return@launch
                    }

                    // Single-user rule: if any user already exists and this is not an exact match, block login
                    val any = userProfileDao.getAny()
                    if (any != null) {
                        runOnUiThread {
                            Toast.makeText(
                                this@LoginActivity,
                                "The user for this app is already created OR check given info.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    // No users exist yet  create new and login
                    val uid = UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    val newProfile = UserProfile(
                        uid = uid,
                        firstName = firstName,
                        lastName = lastName,
                        dob = dob,
                        createdAt = now,
                        lastSignAt = now
                    )
                    userProfileDao.upsert(newProfile)
                    // Persist current uid
                    SessionManager.setCurrentUid(applicationContext, uid)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "User created", Toast.LENGTH_SHORT).show()
                        navigateToMain(uid = uid, firstName = firstName)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    runOnUiThread { btnLogin.isEnabled = true }
                }
            }
        }

    }

    private fun normalizeName(s: String): String = s.trim().replace(Regex("\\s+"), " ")

    // Accepts d/M/yyyy, dd/MM/yyyy and separators '/', '-', '.' and normalizes to dd/MM/yyyy
    private fun normalizeDob(input: String): String? {
        val m = Regex("""^\s*(\d{1,2})[./-](\d{1,2})[./-](\d{4})\s*$""").find(input)
            ?: return null
        val day = m.groupValues[1].toInt()
        val month = m.groupValues[2].toInt()
        val year = m.groupValues[3].toInt()
        if (day !in 1..31 || month !in 1..12 || year !in 1900..2100) return null
        val dd = day.toString().padStart(2, '0')
        val mm = month.toString().padStart(2, '0')
        return "$dd/$mm/$year"
    }

    private fun navigateToMain(uid: String, firstName: String) {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.putExtra("uid", uid)
        intent.putExtra("firstName", firstName)
        startActivity(intent)
        finish()
    }

}