package com.example.medtracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medtracker.R
import com.example.medtracker.data.db.entities.Drug

class MedAdapter : RecyclerView.Adapter<MedAdapter.VH>() {
    private var items: MutableList<Drug> = mutableListOf()

    fun submit(list: List<Drug>) {
        items = list.toMutableList()
        notifyDataSetChanged()
    }

    fun removeItem(position: Int): Drug? {
        if (position in items.indices) {
            val removed = items.removeAt(position)
            notifyItemRemoved(position)
            return removed
        } else {
            return null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_med, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBrand: TextView = itemView.findViewById(R.id.tvMedBrand)
        private val tvName: TextView = itemView.findViewById(R.id.tvMedName)
        private val tvForm: TextView = itemView.findViewById(R.id.tvForm)
        private val tvStrength: TextView = itemView.findViewById(R.id.tvStrength)
        private val tvUnit: TextView = itemView.findViewById(R.id.tvUnit)
        private val metaRow: View = itemView.findViewById(R.id.metaRow)

        fun bind(d: Drug) {
            val brand = d.brandName?.trim().orEmpty()
            val generic = d.name.trim()

            // Brand (primary)
            if (brand.isBlank()) {
                tvBrand.visibility = View.GONE
            } else {
                tvBrand.text = brand
                tvBrand.visibility = View.VISIBLE
            }

            // Generic (secondary) with fallback to ensure something shows
            val displayName = when {
                generic.isNotBlank() -> generic
                brand.isNotBlank() -> brand
                else -> "(Unknown drug)"
            }
            tvName.text = displayName
            tvName.visibility = View.VISIBLE

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
        }

        private fun formatStrength(v: Double): String =
            if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
    }
}
