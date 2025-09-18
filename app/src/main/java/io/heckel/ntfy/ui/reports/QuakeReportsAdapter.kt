package io.heckel.ntfy.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.heckel.ntfy.R

class QuakeReportsAdapter : ListAdapter<QuakeReport, QuakeReportsAdapter.ViewHolder>(DiffCallback) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quake_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.title.text = item.headline
        holder.subtitle.isVisible = !item.subline.isNullOrBlank()
        holder.subtitle.text = item.subline
        if (item.details.isEmpty()) {
            holder.details.isVisible = false
            holder.details.text = ""
        } else {
            holder.details.isVisible = true
            holder.details.text = item.details.joinToString(separator = "\n") { (label, value) ->
                if (label.isBlank()) {
                    value
                } else {
                    "• $label: $value"
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.report_title)
        val subtitle: TextView = view.findViewById(R.id.report_subtitle)
        val details: TextView = view.findViewById(R.id.report_details)
    }

    private object DiffCallback : DiffUtil.ItemCallback<QuakeReport>() {
        override fun areItemsTheSame(oldItem: QuakeReport, newItem: QuakeReport): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: QuakeReport, newItem: QuakeReport): Boolean {
            return oldItem == newItem
        }
    }
}
