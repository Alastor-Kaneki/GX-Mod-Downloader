package dev.alastorkaneki.gxmods

import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PackageSecurityTest {
    @Test
    fun resolvesOfficialContentUrlToCrx() {
        val content = "https://mods.store.gx.me/mods/abc/release/build/contents"
        assertEquals(
            "https://mods.store.gx.me/mods/abc/release/build/mod.crx",
            GxPackageSecurity.resolvePackageUrl(content),
        )
    }

    @Test
    fun blocksUntrustedOrInsecurePackages() {
        assertFalse(GxPackageSecurity.isAllowedPackageUrl("https://example.com/mod.crx"))
        assertFalse(GxPackageSecurity.isAllowedPackageUrl("http://mods.store.gx.me/mods/a/mod.crx"))
        assertFalse(GxPackageSecurity.isAllowedPackageUrl("https://mods.store.gx.me/mods/a/preview.png"))
    }

    @Test
    fun acceptsKnownGxPackageHosts() {
        assertTrue(GxPackageSecurity.isAllowedPackageUrl("https://mods.store.gx.me/mods/a/mod.crx"))
        assertTrue(GxPackageSecurity.isAllowedPackageUrl("https://play.gxc.gg/mods/a/mod.crx?token=1"))
        assertTrue(GxPackageSecurity.isAllowedPackageUrl("https://play.gx.games/mods/a/mod.crx"))
    }

    @Test
    fun parsesCrx3HeaderAndFindsZip() {
        val bytes = ByteArray(20)
        bytes[0] = 'C'.code.toByte()
        bytes[1] = 'r'.code.toByte()
        bytes[2] = '2'.code.toByte()
        bytes[3] = '4'.code.toByte()
        bytes.putLittleEndian(4, 3)
        bytes.putLittleEndian(8, 4)
        bytes[16] = 'P'.code.toByte()
        bytes[17] = 'K'.code.toByte()
        val header = CrxHeaderParser.parse(bytes)
        assertEquals(3, header.version)
        assertEquals(16, header.zipOffset)
    }

    @Test
    fun rejectsMalformedCrx() {
        assertFailsWith<IllegalArgumentException> { CrxHeaderParser.parse(ByteArray(8)) }
        val bytes = ByteArray(20)
        bytes[0] = 'C'.code.toByte()
        bytes[1] = 'r'.code.toByte()
        bytes[2] = '2'.code.toByte()
        bytes[3] = '4'.code.toByte()
        bytes.putLittleEndian(4, 3)
        bytes.putLittleEndian(8, 4)
        assertFailsWith<IllegalArgumentException> { CrxHeaderParser.parse(bytes) }
    }

    @Test
    fun parsesLiveListingShape() {
        val fixture = """
            {
              "data": {
                "mods": [{
                  "modId": "mod-1",
                  "modShortId": "abc123",
                  "crxId": "crx-id",
                  "packageVersion": "1.2.3",
                  "title": "Neon Test",
                  "description": "A fixture mod",
                  "studio": {"name": "Fixture Studio"},
                  "covers": [{
                    "coverUrl": "https://mods.store.gx.me/mod/mod-1/cover/image",
                    "variants": [{
                      "variantKey": "webp-640x360",
                      "url": "https://mods.store.gx.me/mod/mod-1/cover/image/webp-640x360",
                      "mimeType": "image/webp",
                      "width": 640,
                      "height": 360
                    }]
                  }],
                  "mangledTitle": "neon-test",
                  "modTypeTags": [{"alias": "theme", "title": {"value": "Theme"}}],
                  "modPlatformTags": [{"alias": "desktop", "title": {"value": "Desktop"}}],
                  "modSigningKeyTags": [{"alias": "opera-gx-official", "title": {"value": "Official"}}],
                  "size": 1024,
                  "contentUrl": "https://mods.store.gx.me/mods/mod-1/release/build/contents",
                  "numberOfDownloads": 42
                }],
                "pagination": {"currPage": 0, "numPerPage": 24, "totalPages": 1, "totalItems": 1}
              },
              "errors": []
            }
        """.trimIndent()
        val envelope = GxNetwork.json.decodeFromString<GxEnvelope<GxModsData>>(fixture)
        val mod = assertNotNull(envelope.data).mods.single().toDomain()
        assertEquals("Neon Test", mod.title)
        assertEquals("Fixture Studio", mod.creator)
        assertTrue(mod.official)
        assertEquals(listOf("Theme"), mod.components)
    }

    private fun ByteArray.putLittleEndian(offset: Int, value: Int) {
        this[offset] = value.toByte()
        this[offset + 1] = (value ushr 8).toByte()
        this[offset + 2] = (value ushr 16).toByte()
        this[offset + 3] = (value ushr 24).toByte()
    }
}
