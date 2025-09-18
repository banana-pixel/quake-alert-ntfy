package io.heckel.ntfy.ui.reports

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

/**
 * Represents a single earthquake report fetched from the quakealert backend.
 */
data class QuakeReport(
    val headline: String,
    val subline: String?,
    val details: List<Pair<String, String>>
)

/**
 * Parses the JSON payload returned by https://quakealert.bananapixel.my.id/laporan into
 * a list of [QuakeReport]s. The server payload is not strictly defined, so the parser is
 * intentionally resilient: It supports objects, arrays and a range of common field names
 * that the endpoint is known to use. Unknown structures are rendered as readable text so
 * the app can still present something meaningful to the user.
 */
object QuakeReportParser {
    private val ROOT_ARRAY_KEYS = listOf("laporan", "reports", "data", "items", "result", "results")
    private val HEADLINE_KEYS = listOf("wilayah", "lokasi", "location", "area", "region", "kejadian", "keterangan", "title", "judul", "info")
    private val DATE_KEYS = listOf("tanggal", "date", "tgl")
    private val TIME_KEYS = listOf("jam", "time", "waktu")

    fun parse(rawBody: String): List<QuakeReport> {
        val body = rawBody.trim()
        if (body.isEmpty()) {
            return emptyList()
        }

        return try {
            when {
                body.startsWith("[") -> parseArray(JSONArray(body))
                body.startsWith("{") -> parseRootObject(JSONObject(body))
                else -> listOf(QuakeReport(body, null, emptyList()))
            }
        } catch (e: JSONException) {
            listOf(QuakeReport(body, null, emptyList()))
        }
    }

    private fun parseRootObject(root: JSONObject): List<QuakeReport> {
        ROOT_ARRAY_KEYS.forEach { key ->
            if (root.has(key)) {
                return parseArrayValue(root.get(key))
            }
        }
        return parseArrayValue(root)
    }

    private fun parseArrayValue(value: Any?): List<QuakeReport> {
        return when (value) {
            is JSONArray -> parseArray(value)
            is JSONObject -> listOf(parseObject(value))
            null -> emptyList()
            else -> listOf(QuakeReport(value.toString(), null, emptyList()))
        }
    }

    private fun parseArray(array: JSONArray): List<QuakeReport> {
        val list = mutableListOf<QuakeReport>()
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is JSONObject -> list.add(parseObject(item))
                is JSONArray -> list.addAll(parseArray(item))
                null -> Unit
                else -> list.add(QuakeReport(item.toString(), null, emptyList()))
            }
        }
        return list
    }

    private fun parseObject(obj: JSONObject): QuakeReport {
        val fields = mutableListOf<ReportField>()
        val iterator = obj.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val value = obj.opt(key)
            val formattedValue = formatValue(value)
            if (formattedValue.isNotBlank()) {
                fields.add(ReportField(key, formatKey(key), formattedValue))
            }
        }

        if (fields.isEmpty()) {
            return QuakeReport("Laporan", null, emptyList())
        }

        val headlineField = fields.firstOrNull { field ->
            HEADLINE_KEYS.any { candidate -> field.rawKey.equals(candidate, ignoreCase = true) }
        }
        val dateField = fields.firstOrNull { field ->
            DATE_KEYS.any { candidate -> field.rawKey.equals(candidate, ignoreCase = true) }
        }
        val timeField = fields.firstOrNull { field ->
            TIME_KEYS.any { candidate -> field.rawKey.equals(candidate, ignoreCase = true) }
        }

        val sublineParts = mutableListOf<String>()
        dateField?.value?.takeIf { it.isNotBlank() }?.let(sublineParts::add)
        timeField?.value?.takeIf { it.isNotBlank() }?.let(sublineParts::add)
        val subline = if (sublineParts.isEmpty()) null else sublineParts.joinToString(separator = " • ")

        val headline = headlineField?.value?.takeIf { it.isNotBlank() }
            ?: fields.firstOrNull { it !== dateField && it !== timeField }?.value
            ?: dateField?.value
            ?: "Laporan"

        val remainingFields = fields.filterNot { field ->
            headlineField != null && field.rawKey.equals(headlineField.rawKey, ignoreCase = true)
        }
        val details = mutableListOf<Pair<String, String>>()
        if (headlineField != null && headlineField.displayLabel.isNotBlank() && remainingFields.isNotEmpty()) {
            details.add(headlineField.displayLabel to headlineField.value)
        }
        details.addAll(remainingFields.map { field -> field.displayLabel to field.value })

        return QuakeReport(headline, subline, details)
    }

    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> ""
            is JSONArray -> buildString {
                for (i in 0 until value.length()) {
                    val item = value.opt(i)
                    val text = formatValue(item)
                    if (text.isNotBlank()) {
                        if (isNotEmpty()) append('\n')
                        append("• ")
                        append(text)
                    }
                }
            }
            is JSONObject -> formatNestedObject(value)
            else -> value.toString().trim()
        }
    }

    private fun formatNestedObject(obj: JSONObject): String {
        val builder = StringBuilder()
        val iterator = obj.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val value = formatValue(obj.opt(key))
            if (value.isNotBlank()) {
                if (builder.isNotEmpty()) {
                    builder.append('\n')
                }
                builder.append(formatKey(key))
                builder.append(':')
                builder.append(' ')
                builder.append(value)
            }
        }
        return builder.toString()
    }

    private fun formatKey(key: String): String {
        return key.replace('_', ' ')
            .split(' ', '-', '.', ':')
            .flatMap { segment ->
                segment.replace(Regex("(?<=[A-Za-z])(?=[A-Z0-9])"), " ")
                    .split(' ')
            }
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
            .ifEmpty { key }
    }

    private data class ReportField(
        val rawKey: String,
        val displayLabel: String,
        val value: String
    )
}
