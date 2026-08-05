package dev.alastorkaneki.gxmods

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object GxNetwork {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        expectSuccess = true
    }
}

class GxStoreRepository(
    private val client: HttpClient = GxNetwork.client,
) {
    suspend fun browse(query: GxBrowseQuery, page: Int, pageSize: Int = 24): GxPage {
        val response: GxEnvelope<GxModsData> = client.get("$API_BASE/store/v3/mods") {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, USER_AGENT)
            parameter("page", page)
            parameter("pageSize", pageSize)
            parameter("sort", query.sort.apiValue)
            query.search.trim().takeIf(String::isNotBlank)?.let { parameter("search", it) }
            query.platformTag?.let { parameter("tagAlias", it) }
            query.typeTag?.let { parameter("tagAlias", it) }
        }.body()

        val data = response.data ?: throw GxStoreException(response.errorMessage())
        return GxPage(
            mods = data.mods.map(GxModDto::toDomain),
            currentPage = data.pagination.currPage,
            totalPages = data.pagination.totalPages,
            totalItems = data.pagination.totalItems,
        )
    }

    suspend fun details(shortId: String): GxMod {
        require(shortId.matches(Regex("[a-z0-9]+"))) { "Invalid GX mod identifier" }
        val response: GxEnvelope<GxModDto> = client.get("$API_BASE/store/v3/mods/$shortId") {
            accept(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, USER_AGENT)
        }.body()
        return response.data?.toDomain() ?: throw GxStoreException(response.errorMessage())
    }

    private fun GxEnvelope<*>.errorMessage(): String = errors
        .mapNotNull { it.message ?: it.code }
        .joinToString("; ")
        .ifBlank { "GX Store returned no data" }

    companion object {
        const val API_BASE = "https://api.gx.me"
        const val USER_AGENT = "GX-Mod-Downloader/0.1 (+https://github.com/Alastor-Kaneki/GX-Mod-Downloader)"
    }
}

class GxStoreException(message: String) : Exception(message)
