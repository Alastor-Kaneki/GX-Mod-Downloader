package dev.alastorkaneki.gxmods

import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GxStoreJsonTest {
    @Test
    fun explicitNullOptionalMetadataUsesSafeDefaults() {
        val payload = """
            {
              "data": {
                "mods": [
                  {
                    "modId": "mod-1",
                    "modShortId": "abc123",
                    "title": "Nullable metadata test",
                    "studio": {
                      "studioId": "studio-1",
                      "name": "Bigcheif",
                      "owner": {
                        "username": "Bigcheif",
                        "avatarUrl": null
                      }
                    },
                    "covers": [
                      {
                        "coverUrl": null,
                        "variants": [],
                        "aspectRatio": null,
                        "type": null
                      }
                    ],
                    "icons": [
                      {
                        "name": null,
                        "iconUrl": null
                      }
                    ],
                    "contentUrl": null,
                    "creationDate": null,
                    "lastModified": null
                  }
                ],
                "pagination": {
                  "currPage": 0,
                  "numPerPage": 24,
                  "totalPages": 1,
                  "totalItems": 1
                }
              },
              "errors": []
            }
        """.trimIndent()

        val envelope = GxNetwork.json.decodeFromString<GxEnvelope<GxModsData>>(payload)
        val dto = assertNotNull(envelope.data).mods.single()

        assertEquals("", dto.studio?.owner?.avatarUrl)
        assertEquals("", dto.covers.single().coverUrl)
        assertEquals("", dto.icons.single().iconUrl)
        assertEquals("", dto.contentUrl)
        assertEquals("", dto.creationDate)
        assertEquals("", dto.lastModified)
    }
}
