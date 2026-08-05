package dev.alastorkaneki.gxmods

import kotlinx.serialization.Serializable

@Serializable
data class GxEnvelope<T>(
    val data: T? = null,
    val errors: List<GxApiError> = emptyList(),
)

@Serializable
data class GxApiError(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class GxModsData(
    val mods: List<GxModDto> = emptyList(),
    val pagination: GxPagination = GxPagination(),
)

@Serializable
data class GxPagination(
    val currPage: Int = 0,
    val numPerPage: Int = 0,
    val totalPages: Int = 0,
    val totalItems: Int = 0,
)

@Serializable
data class GxModDto(
    val modId: String = "",
    val modVersion: String = "",
    val modShortId: String = "",
    val crxId: String = "",
    val releaseId: String = "",
    val releaseVersion: String = "",
    val packageVersion: String = "",
    val title: String = "Untitled GX Mod",
    val description: String = "",
    val longDescription: String = "",
    val ageRating: String = "",
    val studio: GxStudio? = null,
    val covers: List<GxCover> = emptyList(),
    val mangledTitle: String = "",
    val modTags: List<GxTag> = emptyList(),
    val modTypeTags: List<GxTag> = emptyList(),
    val modWallpaperTypeTags: List<GxTag> = emptyList(),
    val modPlatformTags: List<GxTag> = emptyList(),
    val modSigningKeyTags: List<GxTag> = emptyList(),
    val size: Long = 0,
    val contentUrl: String = "",
    val contentFiles: List<GxContentFile> = emptyList(),
    val icons: List<GxIcon> = emptyList(),
    val creationDate: String = "",
    val lastModified: String = "",
    val numberOfDownloads: Long = 0,
    val allowedFeedback: Boolean = false,
)

@Serializable
data class GxStudio(
    val studioId: String = "",
    val name: String = "Unknown creator",
    val owner: GxOwner? = null,
)

@Serializable
data class GxOwner(
    val username: String = "",
    val avatarUrl: String = "",
)

@Serializable
data class GxCover(
    val coverUrl: String = "",
    val variants: List<GxMediaVariant> = emptyList(),
    val aspectRatio: String = "",
    val type: String = "",
)

@Serializable
data class GxMediaVariant(
    val variantKey: String = "",
    val url: String = "",
    val mimeType: String = "",
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class GxTag(
    val tagId: String = "",
    val alias: String = "",
    val parentTagAlias: String? = null,
    val title: GxLocalizedTitle = GxLocalizedTitle(),
    val numberOfMods: Long = 0,
)

@Serializable
data class GxLocalizedTitle(
    val value: String = "",
)

@Serializable
data class GxContentFile(
    val archivePath: String = "",
    val fileType: String = "",
    val mediaType: String? = null,
    val variants: List<GxMediaVariant> = emptyList(),
)

@Serializable
data class GxIcon(
    val name: String = "",
    val iconUrl: String = "",
)

data class GxMod(
    val id: String,
    val shortId: String,
    val crxId: String,
    val title: String,
    val slug: String,
    val description: String,
    val creator: String,
    val packageVersion: String,
    val downloads: Long,
    val sizeBytes: Long,
    val coverUrl: String?,
    val iconUrl: String?,
    val tags: List<String>,
    val tagAliases: Set<String>,
    val components: List<String>,
    val platforms: List<String>,
    val contentUrl: String,
    val creationDate: String,
    val lastModified: String,
    val official: Boolean,
    val contentFileCount: Int,
) {
    val storeUrl: String
        get() = "https://store.gx.me/mods/$shortId/$slug/"

    val suggestedFileName: String
        get() = "${sanitizeFileName(slug.ifBlank { title })}-${packageVersion.ifBlank { "latest" }}.crx"

    val hasActiveContent: Boolean
        get() = tagAliases.any { it in setOf("page-style", "shader") }
}

data class GxPage(
    val mods: List<GxMod>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
)

enum class GxSort(val apiValue: String, val label: String) {
    MOST_DOWNLOADED("total-downloads-desc", "Most downloaded"),
    LAST_UPDATED("last-modified-desc", "Last updated"),
    NEWEST("creation-date-desc", "Newest"),
    TITLE_ASC("title-asc", "Title A–Z"),
    TITLE_DESC("title-desc", "Title Z–A"),
}

data class GxBrowseQuery(
    val search: String = "",
    val sort: GxSort = GxSort.MOST_DOWNLOADED,
    val platformTag: String? = "desktop",
    val typeTag: String? = null,
)

data class DownloadRecord(
    val modTitle: String,
    val fileName: String,
    val destination: String,
    val completed: Boolean,
    val note: String,
)

fun GxModDto.toDomain(): GxMod {
    val allTags = (modTypeTags + modPlatformTags + modTags).distinctBy { it.alias }
    val bestCover = covers.firstOrNull()?.let { cover ->
        cover.variants.maxByOrNull { it.width ?: 0 }?.url?.takeIf { it.isNotBlank() }
            ?: cover.coverUrl.takeIf { it.isNotBlank() }
    }
    return GxMod(
        id = modId,
        shortId = modShortId,
        crxId = crxId,
        title = title,
        slug = mangledTitle.ifBlank { sanitizeFileName(title) },
        description = longDescription.ifBlank { description },
        creator = studio?.name ?: "Unknown creator",
        packageVersion = packageVersion,
        downloads = numberOfDownloads,
        sizeBytes = size,
        coverUrl = bestCover,
        iconUrl = icons.firstOrNull()?.iconUrl?.takeIf { it.isNotBlank() },
        tags = allTags.mapNotNull { it.title.value.takeIf(String::isNotBlank) },
        tagAliases = allTags.mapNotNull { it.alias.takeIf(String::isNotBlank) }.toSet(),
        components = modTypeTags.mapNotNull { it.title.value.takeIf(String::isNotBlank) },
        platforms = modPlatformTags.mapNotNull { it.title.value.takeIf(String::isNotBlank) },
        contentUrl = contentUrl,
        creationDate = creationDate,
        lastModified = lastModified,
        official = modSigningKeyTags.any { it.alias == "opera-gx-official" },
        contentFileCount = contentFiles.size,
    )
}

fun sanitizeFileName(value: String): String {
    val normalized = value.lowercase().map { char ->
        when {
            char.isLetterOrDigit() -> char
            char in setOf('.', '_', '-') -> char
            else -> '-'
        }
    }.joinToString("")
    return normalized.replace(Regex("-+"), "-").trim('-', '.').ifBlank { "gx-mod" }
}
