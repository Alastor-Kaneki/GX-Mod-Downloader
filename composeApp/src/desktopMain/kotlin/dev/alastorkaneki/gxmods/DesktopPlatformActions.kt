package dev.alastorkaneki.gxmods

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

class DesktopPlatformActions : PlatformActions {
    override suspend fun download(url: String, suggestedFileName: String): DownloadResult =
        withContext(Dispatchers.IO) {
            if (!GxPackageSecurity.isAllowedPackageUrl(url)) {
                return@withContext DownloadResult(false, "", "Blocked a non-GX package URL.")
            }

            val downloadDirectory = Path.of(
                System.getProperty("user.home"),
                "Downloads",
                "GX Mod Downloader",
            )
            Files.createDirectories(downloadDirectory)
            val destination = uniqueDestination(downloadDirectory, sanitizeDownloadName(suggestedFileName))
            val partial = destination.resolveSibling("${destination.fileName}.part")

            runCatching {
                val connection = openSafeConnection(url)
                try {
                    BufferedInputStream(connection.inputStream).use { input ->
                        BufferedOutputStream(Files.newOutputStream(partial)).use { output ->
                            input.copyTo(output)
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                validateCrx(partial)
                runCatching {
                    Files.move(partial, destination, StandardCopyOption.ATOMIC_MOVE)
                }.getOrElse {
                    Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING)
                }
            }.fold(
                onSuccess = {
                    DownloadResult(
                        true,
                        destination.toAbsolutePath().toString(),
                        "Downloaded and validated ${destination.fileName}.",
                    )
                },
                onFailure = { error ->
                    Files.deleteIfExists(partial)
                    DownloadResult(
                        false,
                        "",
                        "Download failed: ${error.message ?: "unknown error"}",
                    )
                },
            )
        }

    override fun openExternalUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || !Desktop.isDesktopSupported()) {
            false
        } else {
            Desktop.getDesktop().browse(uri)
            true
        }
    }.getOrDefault(false)

    private fun openSafeConnection(initialUrl: String): HttpURLConnection {
        var current = initialUrl
        repeat(MAX_REDIRECTS + 1) { hop ->
            check(GxPackageSecurity.isAllowedPackageUrl(current)) { "Redirect left the official GX CDN" }
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 60_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", GxStoreRepository.USER_AGENT)
                setRequestProperty("Accept-Encoding", "identity")
            }
            val status = connection.responseCode
            if (status in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: run {
                        connection.disconnect()
                        error("GX CDN returned a redirect without a location")
                    }
                connection.disconnect()
                check(hop < MAX_REDIRECTS) { "Too many redirects" }
                current = URL(URL(current), location).toString()
            } else {
                check(status in 200..299) {
                    connection.disconnect()
                    "GX CDN returned HTTP $status"
                }
                return connection
            }
        }
        error("Too many redirects")
    }

    private fun validateCrx(path: Path) {
        RandomAccessFile(path.toFile(), "r").use { file ->
            val maxPrefix = (16L * 1024L * 1024L + 16L).coerceAtMost(file.length()).toInt()
            check(maxPrefix >= 16) { "Downloaded package is too small" }
            val prefix = ByteArray(maxPrefix)
            file.readFully(prefix)
            CrxHeaderParser.parse(prefix)
        }
    }

    private fun uniqueDestination(directory: Path, requestedName: String): Path {
        val initial = directory.resolve(requestedName)
        if (!initial.exists()) return initial
        val extension = requestedName.substringAfterLast('.', "")
        val stem = requestedName.removeSuffix(if (extension.isBlank()) "" else ".$extension")
        var counter = 2
        while (true) {
            val candidateName = if (extension.isBlank()) "$stem-$counter" else "$stem-$counter.$extension"
            val candidate = directory.resolve(candidateName)
            if (!candidate.exists()) return candidate
            counter++
        }
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
