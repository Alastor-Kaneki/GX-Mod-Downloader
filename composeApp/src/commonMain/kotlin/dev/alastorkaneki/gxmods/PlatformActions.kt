package dev.alastorkaneki.gxmods

data class DownloadResult(
    val success: Boolean,
    val destination: String,
    val message: String,
)

interface PlatformActions {
    suspend fun download(url: String, suggestedFileName: String): DownloadResult
    fun openExternalUrl(url: String): Boolean
}
