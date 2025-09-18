package io.heckel.ntfy.ui.reports

import io.heckel.ntfy.msg.ApiService
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object QuakeReportsService {
    private const val REPORTS_URL = "https://quakealert.bananapixel.my.id/laporan"

    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    fun fetchReports(): List<QuakeReport> {
        val request = Request.Builder()
            .url(REPORTS_URL)
            .addHeader("User-Agent", ApiService.USER_AGENT)
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected response ${'$'}{response.code}")
            }
            val body = response.body?.string().orEmpty()
            return QuakeReportParser.parse(body)
        }
    }
}
