package dev.alastorkaneki.gxmods

import io.ktor.http.URLProtocol
import io.ktor.http.Url

object GxPackageSecurity {
    private val allowedHosts = setOf(
        "mods.store.gx.me",
        "play.gxc.gg",
        "play.gx.games",
    )

    fun resolvePackageUrl(contentUrl: String): String? {
        val clean = contentUrl.substringBefore('?').trimEnd('/')
        val base = when {
            clean.endsWith("/contents", ignoreCase = true) -> clean.dropLast("/contents".length)
            clean.contains("/contents/", ignoreCase = true) -> clean.substringBefore("/contents/")
            else -> return null
        }
        val candidate = "$base/mod.crx"
        return candidate.takeIf(::isAllowedPackageUrl)
    }

    fun isAllowedPackageUrl(candidate: String): Boolean = runCatching {
        val parsed = Url(candidate)
        parsed.protocol == URLProtocol.HTTPS &&
            parsed.host.lowercase() in allowedHosts &&
            parsed.encodedPath.lowercase().endsWith("/mod.crx")
    }.getOrDefault(false)

    fun isAllowedAssetUrl(candidate: String): Boolean = runCatching {
        val parsed = Url(candidate)
        parsed.protocol == URLProtocol.HTTPS && parsed.host.lowercase() in allowedHosts
    }.getOrDefault(false)
}

data class CrxHeader(
    val version: Int,
    val zipOffset: Int,
)

object CrxHeaderParser {
    private const val MAX_HEADER_BYTES = 16 * 1024 * 1024

    fun parse(bytes: ByteArray): CrxHeader {
        require(bytes.size >= 16) { "CRX package is too small" }
        require(bytes[0] == 'C'.code.toByte() && bytes[1] == 'r'.code.toByte() &&
            bytes[2] == '2'.code.toByte() && bytes[3] == '4'.code.toByte()) {
            "Missing CRX magic header"
        }

        val version = bytes.readLittleEndianInt(4)
        val zipOffset = when (version) {
            2 -> {
                val publicKeyLength = bytes.readLittleEndianInt(8)
                val signatureLength = bytes.readLittleEndianInt(12)
                require(publicKeyLength >= 0 && signatureLength >= 0) { "Invalid CRX2 header" }
                16L + publicKeyLength + signatureLength
            }
            3 -> {
                val headerSize = bytes.readLittleEndianInt(8)
                require(headerSize in 0..MAX_HEADER_BYTES) { "CRX3 header is unreasonably large" }
                12L + headerSize
            }
            else -> error("Unsupported CRX version $version")
        }

        require(zipOffset <= Int.MAX_VALUE && zipOffset + 4 <= bytes.size) { "CRX ZIP payload is truncated" }
        val offset = zipOffset.toInt()
        require(bytes[offset] == 'P'.code.toByte() && bytes[offset + 1] == 'K'.code.toByte()) {
            "CRX payload is not a ZIP archive"
        }
        return CrxHeader(version, offset)
    }

    private fun ByteArray.readLittleEndianInt(offset: Int): Int {
        require(offset >= 0 && offset + 4 <= size) { "Truncated CRX integer" }
        return (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16) or
            ((this[offset + 3].toInt() and 0xff) shl 24)
    }
}
