package com.example.medtracker.ui

import android.graphics.Typeface
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.medtracker.R
import com.example.medtracker.data.db.entities.Drug

object ButtonSheetHelper {
    fun show(activity: AppCompatActivity, drug: Drug, lifecycleScope: LifecycleCoroutineScope) {
        // Inflate with a parent to ensure layout params on the root are resolved
        val parent = activity.findViewById<ViewGroup>(android.R.id.content)
        val view = LayoutInflater.from(activity).inflate(R.layout.bottomsheet_med, parent, false)
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(view)

        // Configure draggable, expandable behavior
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (sheet != null) {
                // Allow full-screen expansion
                sheet.layoutParams = sheet.layoutParams.apply { height = ViewGroup.LayoutParams.MATCH_PARENT }
                sheet.requestLayout()

                val behavior = BottomSheetBehavior.from(sheet)
                behavior.peekHeight = activity.resources.getDimensionPixelSize(R.dimen.med_sheet_peek)
                behavior.isFitToContents = false
                behavior.expandedOffset = 0
                behavior.skipCollapsed = false
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                behavior.isDraggable = true
            }
        }

        // Populate basic drug fields (brand/name/form/strength/unit)
        val tvBrand = view.findViewById<android.widget.TextView>(R.id.tvSheetBrand)
        val tvName = view.findViewById<android.widget.TextView>(R.id.tvSheetName)
        val tvStrength = view.findViewById<android.widget.TextView>(R.id.tvSheetStrength)
        val tvUnit = view.findViewById<android.widget.TextView>(R.id.tvSheetUnit)
        val tvForm = view.findViewById<android.widget.TextView>(R.id.tvSheetForm)

        val brand = drug.brandName?.trim().orEmpty()
        val generic = drug.name.trim()

        // Use fixed primary color per request
        val brandColor = Color.parseColor("#3D3D99")
        val defaultColor = tvName.currentTextColor

        if (brand.isNotBlank()) {
            // Brand exists: brand above (bold + colored)
            tvBrand.text = brand
            tvBrand.visibility = View.VISIBLE
            tvBrand.setTypeface(null, Typeface.BOLD)
            tvBrand.setTextColor(brandColor)

            if (generic.isNotBlank()) {
                // show generic below as secondary
                tvName.text = generic
                tvName.visibility = View.VISIBLE
                tvName.setTypeface(null, Typeface.NORMAL)
                tvName.setTextColor(defaultColor)
            } else {
                tvName.text = ""
                tvName.visibility = View.GONE
            }
        } else {
            if (generic.isNotBlank()) {
                // No brand: use generic as the emphasized brand slot
                tvBrand.text = generic
                tvBrand.visibility = View.VISIBLE
                tvBrand.setTypeface(null, Typeface.BOLD)
                tvBrand.setTextColor(brandColor)

                tvName.text = ""
                tvName.visibility = View.GONE
            } else {
                // Neither present: fallback
                tvBrand.text = activity.getString(R.string.unknown_drug)
                tvBrand.visibility = View.VISIBLE
                tvBrand.setTypeface(null, Typeface.BOLD)
                tvBrand.setTextColor(brandColor)

                tvName.text = ""
                tvName.visibility = View.GONE
            }
        }

        val hasForm = !drug.form.isNullOrBlank()
        val hasStrength = drug.strength != null
        val hasUnit = !drug.unit.isNullOrBlank()
        tvForm.text = drug.form ?: ""
        tvForm.visibility = if (hasForm) View.VISIBLE else View.GONE
        tvStrength.text = drug.strength?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
        tvStrength.visibility = if (hasStrength) View.VISIBLE else View.GONE
        tvUnit.text = drug.unit ?: ""
        tvUnit.visibility = if (hasUnit) View.VISIBLE else View.GONE

        // Delegate schedule rendering/fetching to ScheduleBinder
        ScheduleBinder.bind(view, drug, lifecycleScope, activity.applicationContext)

        dialog.show()
    }
}
