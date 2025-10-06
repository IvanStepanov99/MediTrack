package com.example.medtracker.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medtracker.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.medtracker.data.db.DbBuilder
import kotlinx.coroutines.launch
import android.widget.ImageButton
import android.widget.Toast
import android.util.Log
import com.example.medtracker.data.session.SessionManager
import androidx.recyclerview.widget.ItemTouchHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firstName = intent.getStringExtra("firstName")
        val namePart = if (!firstName.isNullOrBlank()) firstName else "User"
        findViewById<TextView>(R.id.tvWelcome).text = "Welcome, $namePart!"

        val rv = findViewById<RecyclerView>(R.id.rvMeds)
        val adapter = MedAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val db = DbBuilder.getDatabase(applicationContext)
        val drugDao = db.drugDao()

        // Swipe to delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val removedDrug = adapter.removeItem(position)
                if (removedDrug != null) {
                    lifecycleScope.launch {
                        drugDao.delete(removedDrug)
                    }
                    Toast.makeText(this@MainActivity, "Medication deleted", Toast.LENGTH_SHORT).show()
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rv)

        val uidFromIntent = intent.getStringExtra("uid")
        val uid = uidFromIntent ?: SessionManager.getCurrentUid(applicationContext)
        if (uid == null) {
            Toast.makeText(this, "No user id. Please log in.", Toast.LENGTH_LONG).show()
            Log.w("MainActivity", "No uid in intent or session; cannot observe meds")
            return
        }
        Log.d("MainActivity", "Observing meds for uid=$uid")

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                drugDao.observeByUser(uid).collect { list ->
                    Log.d("MainActivity", "Med rows for $uid = ${list.size}")
                    adapter.submit(list)
                }
            }
        }

        // Bottom bar click handlers
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            Toast.makeText(this, "Home tapped", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btnMeds).setOnClickListener {
            Toast.makeText(this, "Meds tapped", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btnSchedule).setOnClickListener {
            Toast.makeText(this, "Schedule tapped", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
            Toast.makeText(this, "Search tapped", Toast.LENGTH_SHORT).show()
        }
    }
}
