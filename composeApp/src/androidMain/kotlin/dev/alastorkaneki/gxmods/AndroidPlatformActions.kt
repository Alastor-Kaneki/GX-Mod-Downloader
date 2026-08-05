package dev.alastorkaneki.gxmods

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class AndroidPlatformActions(
    private val context: Context,
) : PlatformActions {
    override suspend fun download(url: String, suggestedFileName: String): DownloadResult =
        withContext(Dispatchers.IO) {
            if (!GxPackageSecurity.isAllowedPackageUrl(url)) {
                return@withContext DownloadResult(false, "", "Blocked a non-GX package URL.")
            }

            val finalUrl = runCatching { resolveSafeFinalUrl(url) }
                .getOrElse { error ->
                    return@withContext DownloadResult(
                        false,
                        "",
                        "Could not verify the GX download: ${error.message ?: "network error"}",
                    )
                }
            val fileName = sanitizeDownloadName(suggestedFileName)
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return@withContext DownloadResult(false, "", "Android Download Manager is unavailable.")

            val request = DownloadManager.Request(Uri.parse(finalUrl))
                .setTitle(fileName)
                .setDescription("Raw Opera GX mod package")
                .setMimeType("application/x-chrome-extension")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            runCatching { manager.enqueue(request) }
                .fold(
                    onSuccess = {
                        DownloadResult(
                            true,
                            "Downloads/$fileName",
                            "Downloading $fileName with Android Download Manager.",
                        )
                    },
                    onFailure = { error ->
                        DownloadResult(
                            false,
                            "",
                            "Could not start the download: ${error.message ?: "unknown error"}",
                        )
                    },
                )
        }

    override fun openExternalUrl(url: String): Boolean = runCatching {
        val uri = Uri.parse(url)
        if (uri.scheme != "https") {
            false
        } else {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }
    }.getOrDefault(false)

    private fun resolveSafeFinalUrl(initialUrl: String): String {
        var current = initialUrl
        repeat(MAX_REDIRECTS + 1) { hop ->
            check(GxPackageSecurity.isAllowedPackageUrl(current)) { "Redirect left the official GX CDN" }
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-15")
                setRequestProperty("User-Agent", GxStoreRepository.USER_AGENT)
                setRequestProperty("Accept-Encoding", "identity")
            }
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    check(hop < MAX_REDIRECTS) { "Too many redirects" }
                    val location = connection.getHeaderField("Location")
                        ?: error("GX CDN returned a redirect without a location")
                    current = URL(URL(current), location).toString()
                } else {
                    check(status in 200..299) { "GX CDN returned HTTP $status" }
                    return current
                }
            } finally {
                connection.disconnect()
            }
        }
        error("Too many redirects")
    }

    private fun sanitizeDownloadName(value: String): String {
        val base = value.substringAfterLast('/').substringAfterLast('\\')
        val safe = base.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-', '.')
        return (safe.ifBlank { "gx-mod.crx" }).let {
            if (it.endsWith(".crx", ignoreCase = true)) it else "$it.crx"
        }
    }

    private companion object {
        const val MAX_REDIRECTS = 5
    }
}
