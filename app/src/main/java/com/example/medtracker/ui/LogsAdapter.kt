package com.example.medtracker.ui

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import androidx.recyclerview.widget.RecyclerView
import com.example.medtracker.R
import java.util.Date

class LogsAdapter(private val showButtons: Boolean = true): RecyclerView.Adapter<LogsAdapter.LogVH>() {
    private var items: MutableList<LogItem> = mutableListOf()

    var onTake: ((LogItem) -> Unit)? = null
    var onSkip:  ((LogItem) -> Unit)? = null

    fun submit(list: List<LogItem>) {
        items = list.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogVH {
        // Use the shared med card so med-list and logs-list have identical appearance
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_med_shared, parent, false)
        return LogVH(v)
    }

    override fun onBindViewHolder(holder: LogVH, position: Int) {
        val entry = items[position]
        holder.bind(entry)
    }

    override fun getItemCount (): Int = items.size

    inner class LogVH(view: View) : RecyclerView.ViewHolder(view) {
        val btnTake: Button = view.findViewById(R.id.btnTake)
        val btnSkip: Button = view.findViewById(R.id.btnSkip)
        private val tvBrand: TextView = view.findViewById(R.id.tvMedBrand)
        private val tvName: TextView = view.findViewById(R.id.tvMedName)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val tvDose: TextView = view.findViewById(R.id.tvDoseInfo)
        private val tvNote : TextView = view.findViewById(R.id.tvNote)
        private val buttonRow: View? = view.findViewById(R.id.buttonRow)
        private val metaRow: View? = view.findViewById(R.id.metaRow)

        fun bind(item: LogItem) {
            // hide med-list only meta row (form/strength/unit) for logs
            metaRow?.visibility = View.GONE
             val context = itemView.context
            val drug = item.drug
            val log = item.doseLog

            val brand = drug.brandName?.trim().orEmpty()
            val generic = drug.name.trim()
            val fallback = context.getString(R.string.unknown_drug)
            val accent = Color.parseColor("#3D3D99")

            // Title logic: mirror MedAdapter behavior
            if (brand.isNotBlank()) {
                tvBrand.text = brand
                tvBrand.visibility = View.VISIBLE
                tvBrand.setTypeface(null, Typeface.BOLD)
                tvBrand.setTextColor(accent)

                if (generic.isNotBlank()) {
                    tvName.text = generic
                    tvName.visibility = View.VISIBLE
                    tvName.setTypeface(null, Typeface.NORMAL)
                } else {
                    tvName.text = ""
                    tvName.visibility = View.GONE
                }
            } else {
                if (generic.isNotBlank()) {
                    tvBrand.text = generic
                    tvBrand.visibility = View.VISIBLE
                    tvBrand.setTypeface(null, Typeface.BOLD)
                    tvBrand.setTextColor(accent)

                    tvName.text = ""
                    tvName.visibility = View.GONE
                } else {
                    tvBrand.text = fallback
                    tvBrand.visibility = View.VISIBLE
                    tvBrand.setTypeface(null, Typeface.BOLD)
                    tvBrand.setTextColor(accent)

                    tvName.text = ""
                    tvName.visibility = View.GONE
                }
            }

             // Time:
             val timeMs = log.plannedTime ?: log.takenTime
             if (timeMs != null && timeMs > 0L) {
                 tvTime.visibility = View.VISIBLE
                 val timeStr = DateFormat.getTimeFormat(context).format(Date(timeMs))
                 tvTime.text = timeStr
             } else {
                 tvTime.visibility = View.GONE

             }

             // Dose info: quantity + unit
             val qty = log.quantity?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
             val unit = log.unit?.trim().orEmpty()
             val doseText = listOf(qty, unit).filter { it.isNotEmpty() }.joinToString(" ")
             if (doseText.isNotBlank()) {
                 tvDose.visibility = View.VISIBLE
                 tvDose.text = doseText
             } else {
                 tvDose.visibility = View.GONE
             }

             // Note:
             val note = log.note?.trim().orEmpty()
             if (note.isNotBlank()) {
                 tvNote.visibility = View.VISIBLE
                 tvNote.text = note
             } else {
                 tvNote.visibility = View.GONE
             }

            // Buttons: show only when adapter configured to show them
            if (showButtons) {
                buttonRow?.visibility = View.VISIBLE

                // Enable buttons
                btnTake.isEnabled = true
                btnSkip.isEnabled = true
                btnTake.isClickable = true
                btnSkip.isClickable = true

                // Set listeners using current bindingAdapterPosition to avoid stale references
                btnTake.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                        onTake?.invoke(items[pos])
                    }
                }

                btnSkip.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                        onSkip?.invoke(items[pos])
                    }
                }
            } else {
                // Hide/disable buttons for read-only history view
                buttonRow?.visibility = View.GONE
                btnTake.setOnClickListener(null)
                btnSkip.setOnClickListener(null)
                btnTake.isEnabled = false
                btnSkip.isEnabled = false
                btnTake.isClickable = false
                btnSkip.isClickable = false
            }
         }


     }
 }
