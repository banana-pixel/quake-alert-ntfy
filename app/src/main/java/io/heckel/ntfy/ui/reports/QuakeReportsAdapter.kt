package io.heckel.ntfy.ui.reports

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDivider
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
        holder.subtitle.text = item.subline.orEmpty()
        holder.subtitle.isVisible = holder.subtitle.text.isNotBlank()

        if (item.details.isEmpty()) {
            holder.details.isVisible = false
            holder.details.text = ""
            holder.divider.isVisible = false
        } else {
            val gapWidth = holder.itemView.resources.getDimensionPixelSize(R.dimen.report_detail_bullet_gap)
            val builder = SpannableStringBuilder()
            item.details.forEach { (label, value) ->
                if (label.isBlank() && value.isBlank()) {
                    return@forEach
                }
                if (builder.isNotEmpty()) {
                    builder.append('\n')
                }
                val lineStart = builder.length
                if (label.isBlank()) {
                    builder.append(value)
                } else {
                    val labelStart = builder.length
                    builder.append(label)
                    builder.setSpan(StyleSpan(Typeface.BOLD), labelStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.append(": ")
                    builder.append(value)
                }
                builder.setSpan(BulletSpan(gapWidth), lineStart, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            holder.details.isVisible = builder.isNotEmpty()
            if (holder.details.isVisible) {
                holder.details.text = builder
            } else {
                holder.details.text = ""
            }
            holder.divider.isVisible = holder.details.isVisible && holder.subtitle.isVisible
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.report_title)
        val subtitle: TextView = view.findViewById(R.id.report_subtitle)
        val divider: MaterialDivider = view.findViewById(R.id.report_divider)
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
