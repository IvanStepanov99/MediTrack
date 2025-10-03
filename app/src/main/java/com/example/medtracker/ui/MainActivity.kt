package com.example.medtracker.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup.LayoutParams

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = intent.getStringExtra("uid")
        val firstName = intent.getStringExtra("firstName")
        val namePart = if (!firstName.isNullOrBlank()) firstName else "User"
        val uidPart = uid ?: "(no uid)"

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        val tvWelcome = TextView(this).apply {
            textSize = 30f
            text = "Welcome, $namePart\nuid: $uidPart"
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        container.addView(tvWelcome)
        setContentView(container)
    }
}
