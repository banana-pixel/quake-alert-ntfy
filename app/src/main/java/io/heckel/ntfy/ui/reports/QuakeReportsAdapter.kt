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
import java.util.Locale

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

        val context = holder.itemView.context
        val defaultIntensityLabel = context.getString(R.string.report_intensity_default_label)
        val defaultIdLabel = context.getString(R.string.report_id_default_label)

        var intensityLabel: String? = null
        var intensityValue: String? = null
        var idLabel: String? = null
        var idValue: String? = null
        val remainingDetails = mutableListOf<Pair<String, String>>()

        item.details.forEach { (rawLabel, rawValue) ->
            val label = rawLabel.trim()
            val value = rawValue.trim()
            if (label.isBlank() && value.isBlank()) {
                return@forEach
            }
            if (value.isBlank()) {
                return@forEach
            }
            val normalizedLabel = label.lowercase(Locale.getDefault())
            when {
                intensityValue == null && label.isNotBlank() && isIntensityLabel(normalizedLabel) -> {
                    intensityLabel = label
                    intensityValue = value
                }
                idValue == null && label.isNotBlank() && isIdLabel(normalizedLabel) -> {
                    idLabel = label
                    idValue = value
                }
                else -> {
                    val displayValue = if (label.isNotBlank() && isDurationLabel(normalizedLabel) && needsDurationUnit(value)) {
                        context.getString(R.string.report_duration_seconds, value)
                    } else {
                        value
                    }
                    remainingDetails.add(label to displayValue)
                }
            }
        }

        val intensityText = intensityValue?.takeIf { it.isNotBlank() }
        if (intensityText != null) {
            val labelText = intensityLabel?.takeIf { it.isNotBlank() } ?: defaultIntensityLabel
            holder.intensityLabel.text = labelText
            holder.intensityValue.text = intensityText
            holder.intensityContainer.isVisible = true
        } else {
            holder.intensityContainer.isVisible = false
            holder.intensityLabel.text = ""
            holder.intensityValue.text = ""
        }

        val gapWidth = holder.itemView.resources.getDimensionPixelSize(R.dimen.report_detail_bullet_gap)
        val builder = SpannableStringBuilder()
        remainingDetails.forEach { (label, value) ->
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

        val idText = idValue?.takeIf { it.isNotBlank() }
        if (idText != null) {
            val labelText = idLabel?.takeIf { it.isNotBlank() } ?: defaultIdLabel
            holder.reportId.text = context.getString(R.string.report_id_template, labelText, idText)
            holder.reportId.isVisible = true
        } else {
            holder.reportId.text = ""
            holder.reportId.isVisible = false
        }

        holder.divider.isVisible = holder.details.isVisible || holder.reportId.isVisible
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.report_title)
        val subtitle: TextView = view.findViewById(R.id.report_subtitle)
        val intensityContainer: View = view.findViewById(R.id.report_intensity_container)
        val intensityLabel: TextView = view.findViewById(R.id.report_intensity_label)
        val intensityValue: TextView = view.findViewById(R.id.report_intensity_value)
        val divider: MaterialDivider = view.findViewById(R.id.report_divider)
        val details: TextView = view.findViewById(R.id.report_details)
        val reportId: TextView = view.findViewById(R.id.report_id)
    }

    private object DiffCallback : DiffUtil.ItemCallback<QuakeReport>() {
        override fun areItemsTheSame(oldItem: QuakeReport, newItem: QuakeReport): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: QuakeReport, newItem: QuakeReport): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private val INTENSITY_KEYWORDS = listOf("intens", "magnit", "skala")
        private val DURATION_KEYWORDS = listOf("durasi", "duration", "lama")
        private val DURATION_NUMBER_REGEX = Regex("^[0-9]+([.,][0-9]+)?$")

        private fun isIntensityLabel(label: String): Boolean {
            return INTENSITY_KEYWORDS.any { keyword -> label.contains(keyword) }
        }

        private fun isDurationLabel(label: String): Boolean {
            return DURATION_KEYWORDS.any { keyword -> label.contains(keyword) }
        }

        private fun isIdLabel(label: String): Boolean {
            if (label == "id") {
                return true
            }
            if (label.startsWith("id ")) {
                return true
            }
            if (label.endsWith(" id")) {
                return true
            }
            if (label.contains("laporan") && label.contains("id")) {
                return true
            }
            if (label.contains("event") && label.contains("id")) {
                return true
            }
            if (label.contains("gempa") && label.contains("id")) {
                return true
            }
            return false
        }

        private fun needsDurationUnit(value: String): Boolean {
            return DURATION_NUMBER_REGEX.matches(value.trim())
        }
    }
}
