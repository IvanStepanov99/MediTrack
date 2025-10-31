package com.example.medtracker.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import android.widget.Toast
import android.util.Log
import com.example.medtracker.data.session.SessionManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import android.view.View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firstName = intent.getStringExtra("firstName")
        val userId = intent.getStringExtra("uid")
        val namePart = if (!firstName.isNullOrBlank()) firstName else userId ?: "User"
        // Use string resource with placeholder to allow translation
        findViewById<TextView>(R.id.tvWelcome).text = getString(R.string.welcome_user, namePart)

        val rv = findViewById<RecyclerView>(R.id.rvMeds)
        val adapter = MedAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        val rootView = findViewById<View>(android.R.id.content)

        val db = DbBuilder.getDatabase(applicationContext)
        val drugDao = db.drugDao()

        // Open bottom sheet on item click (delegated to helper)
        adapter.onItemClick = { drug ->
            ButtonSheetHelper.show(this@MainActivity, drug, lifecycleScope)
        }

        // Swipe left to delete
        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val removedDrug = adapter.removeItem(position)
                if (removedDrug != null) {
                    lifecycleScope.launch { drugDao.delete(removedDrug) }
                    Snackbar.make(rootView, "Deleted ${'$'}{removedDrug.name}", Snackbar.LENGTH_LONG)
                        .setAction("Undo") { lifecycleScope.launch { drugDao.insert(removedDrug) } }
                        .show()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val itemView = viewHolder.itemView
                    val paint = Paint().apply { color = Color.parseColor("#3D3D99") }
                    c.drawRect(
                        itemView.right.toFloat() + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat(),
                        paint
                    )
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
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
                    Log.d("MainActivity", "Med rows for $uid = ${'$'}{list.size}")
                    adapter.submit(list)
                }
            }
        }

        BottomBarHelper.setup(this, R.id.btnMeds)
    }
}
