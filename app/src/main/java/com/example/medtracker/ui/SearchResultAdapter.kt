package com.example.medtracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medtracker.R

class SearchResultAdapter(private val onSelect: (SearchResult) -> Unit) : ListAdapter<SearchResult, SearchResultAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.tv_search_label)
        private val tvSource: TextView = view.findViewById(R.id.tv_search_source)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onSelect(getItem(pos))
                }
            }
        }

        fun bind(item: SearchResult) {
            tv.text = item.label
            if (item.isLocal) {
                tvSource.visibility = View.VISIBLE
                tvSource.text = itemView.context.getString(R.string.search_source_local)
            } else {
                tvSource.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                // Use label+isLocal as stable identifier
                return oldItem.label == newItem.label && oldItem.isLocal == newItem.isLocal
            }

            override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                return oldItem == newItem
            }
        }
    }
}
