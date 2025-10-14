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
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import android.view.View
import com.example.medtracker.data.db.dao.DrugDao
import com.example.medtracker.data.db.entities.Drug
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firstName = intent.getStringExtra("firstName")
        val userId = intent.getStringExtra("uid")
        val namePart = if (!firstName.isNullOrBlank()) firstName else userId ?: "User"
        findViewById<TextView>(R.id.tvWelcome).text = "Welcome, $namePart!"

        val rv = findViewById<RecyclerView>(R.id.rvMeds)
        val adapter = MedAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        val rootView = findViewById<View>(android.R.id.content)

        val db = DbBuilder.getDatabase(applicationContext)
        val drugDao = db.drugDao()

        // Open bottom sheet on item click
        adapter.onItemClick = { drug ->
            showMedBottomSheet(drugDao, drug)
        }

        // Swipe to delete (left only)
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
                    Snackbar.make(rootView, "Deleted ${removedDrug.name}", Snackbar.LENGTH_LONG)
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
                    val paint = Paint().apply { color = Color.parseColor("#D32F2F") }
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
                    Log.d("MainActivity", "Med rows for $uid = ${list.size}")
                    adapter.submit(list)
                }
            }
        }

        BottomBarHelper.setup(this, R.id.btnMeds)
    }

    private fun showMedBottomSheet(drugDao: DrugDao, drug: Drug) {
        val view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_med, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        // Configure draggable, expandable behavior
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (sheet != null) {
                // Allow full-screen expansion
                sheet.layoutParams = sheet.layoutParams.apply { height = ViewGroup.LayoutParams.MATCH_PARENT }
                sheet.requestLayout()

                val behavior = BottomSheetBehavior.from(sheet)
                behavior.peekHeight = resources.getDimensionPixelSize(R.dimen.med_sheet_peek)
                behavior.isFitToContents = false
                behavior.expandedOffset = 0
                behavior.skipCollapsed = false
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                behavior.isDraggable = true
            }
        }

        val tvBrand = view.findViewById<TextView>(R.id.tvSheetBrand)
        val tvName = view.findViewById<TextView>(R.id.tvSheetName)
        val tvForm = view.findViewById<TextView>(R.id.tvSheetForm)
        val tvStrength = view.findViewById<TextView>(R.id.tvSheetStrength)
        val tvUnit = view.findViewById<TextView>(R.id.tvSheetUnit)

        val brand = drug.brandName?.trim().orEmpty()
        val generic = drug.name.trim()
        if (brand.isNotBlank()) {
            tvBrand.text = brand
            tvBrand.visibility = View.VISIBLE
        } else {
            tvBrand.visibility = View.GONE
        }
        tvName.text = if (generic.isNotBlank()) generic else brand.ifBlank { "(Unknown drug)" }

        val hasForm = !drug.form.isNullOrBlank()
        val hasStrength = drug.strength != null
        val hasUnit = !drug.unit.isNullOrBlank()
        tvForm.text = drug.form ?: ""
        tvForm.visibility = if (hasForm) View.VISIBLE else View.GONE
        tvStrength.text = drug.strength?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
        tvStrength.visibility = if (hasStrength) View.VISIBLE else View.GONE
        tvUnit.text = drug.unit ?: ""
        tvUnit.visibility = if (hasUnit) View.VISIBLE else View.GONE

        dialog.show()
    }
}
