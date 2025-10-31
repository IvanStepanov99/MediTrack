package com.example.medtracker.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medtracker.R
import com.example.medtracker.data.db.entities.Drug

class MedAdapter : RecyclerView.Adapter<MedAdapter.VH>() {
    private var items: MutableList<Drug> = mutableListOf()

    var onItemClick: ((Drug) -> Unit)? = null

    fun submit(list: List<Drug>) {
        items = list.toMutableList()
        notifyDataSetChanged()
    }

    fun removeItem(position: Int): Drug? {
        if (position in items.indices) {
            val removed = items.removeAt(position)
            notifyItemRemoved(position)
            return removed
        }
        return null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Inflate the shared card layout so med-list and logs-list share the same structure
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_med_shared, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onItemClick?.invoke(items[pos])
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBrand: TextView = itemView.findViewById(R.id.tvMedBrand)
        private val tvName: TextView = itemView.findViewById(R.id.tvMedName)
        private val tvForm: TextView = itemView.findViewById(R.id.tvForm)
        private val tvStrength: TextView = itemView.findViewById(R.id.tvStrength)
        private val tvUnit: TextView = itemView.findViewById(R.id.tvUnit)
        private val metaRow: View = itemView.findViewById(R.id.metaRow)
        private val buttonRow: View? = itemView.findViewById(R.id.buttonRow)

        fun bind(d: Drug) {
            val brand = d.brandName?.trim().orEmpty()
            val generic = d.name.trim()

            // Use the requested primary accent for brand/generic
            val accentColor = Color.parseColor("#3D3D99")
            val defaultColor = tvName.currentTextColor

            // Apply the same display rules as the bottom sheet
            if (brand.isNotBlank()) {
                // Brand exists: brand above (bold + accent), generic below (regular + default)
                tvBrand.text = brand
                tvBrand.visibility = View.VISIBLE
                tvBrand.setTypeface(null, Typeface.BOLD)
                tvBrand.setTextColor(accentColor)

                if (generic.isNotBlank()) {
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
                    // No brand: generic takes brand slot (bold + accent), hide secondary
                    tvBrand.text = generic
                    tvBrand.visibility = View.VISIBLE
                    tvBrand.setTypeface(null, Typeface.BOLD)
                    tvBrand.setTextColor(accentColor)

                    tvName.text = ""
                    tvName.visibility = View.GONE
                } else {
                    // Neither present: fallback localized string
                    tvBrand.text = itemView.context.getString(R.string.unknown_drug)
                    tvBrand.visibility = View.VISIBLE
                    tvBrand.setTypeface(null, Typeface.BOLD)
                    tvBrand.setTextColor(accentColor)

                    tvName.text = ""
                    tvName.visibility = View.GONE
                }
            }

            // Form / strength / unit
            val hasStrength = d.strength != null
            val hasUnit = !d.unit.isNullOrBlank()
            val hasForm = !d.form.isNullOrBlank()

            if (hasForm || hasStrength || hasUnit) {
                tvForm.text = d.form ?: ""
                tvForm.visibility = if (hasForm) View.VISIBLE else View.GONE

                tvStrength.text = d.strength?.let { formatStrength(it) } ?: ""
                tvStrength.visibility = if (hasStrength) View.VISIBLE else View.GONE

                tvUnit.text = d.unit ?: ""
                tvUnit.visibility = if (hasUnit) View.VISIBLE else View.GONE

                metaRow.visibility = View.VISIBLE
            } else {
                metaRow.visibility = View.GONE
            }

            // Ensure button row is hidden for med-list cards
            buttonRow?.visibility = View.GONE
        }

        private fun formatStrength(v: Double): String =
            if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
    }
}
