package dev.alastorkaneki.gxmods

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class GxUiState(
    val query: GxBrowseQuery = GxBrowseQuery(),
    val mods: List<GxMod> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalItems: Int = 0,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val selectedMod: GxMod? = null,
    val loadingDetails: Boolean = false,
    val downloading: Boolean = false,
    val notice: String? = null,
    val downloads: List<DownloadRecord> = emptyList(),
)

class GxStoreController(
    private val repository: GxStoreRepository = GxStoreRepository(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var state by mutableStateOf(GxUiState())
        private set

    init {
        refresh()
    }

    fun close() = scope.cancel()

    fun refresh() {
        scope.launch {
            state = state.copy(loading = true, error = null, mods = emptyList(), currentPage = 0)
            runCatching { repository.browse(state.query, 0) }
                .onSuccess { page ->
                    state = state.copy(
                        mods = page.mods,
                        currentPage = page.currentPage,
                        totalPages = page.totalPages,
                        totalItems = page.totalItems,
                        loading = false,
                    )
                }
                .onFailure { error ->
                    state = state.copy(loading = false, error = error.userMessage())
                }
        }
    }

    fun search(text: String) {
        state = state.copy(query = state.query.copy(search = text.trim()))
        refresh()
    }

    fun setSort(sort: GxSort) {
        if (state.query.sort == sort) return
        state = state.copy(query = state.query.copy(sort = sort))
        refresh()
    }

    fun setPlatform(tag: String?) {
        if (state.query.platformTag == tag) return
        state = state.copy(query = state.query.copy(platformTag = tag))
        refresh()
    }

    fun setType(tag: String?) {
        if (state.query.typeTag == tag) return
        state = state.copy(query = state.query.copy(typeTag = tag))
        refresh()
    }

    fun loadMore() {
        if (state.loadingMore || state.loading || state.currentPage + 1 >= state.totalPages) return
        val nextPage = state.currentPage + 1
        scope.launch {
            state = state.copy(loadingMore = true, error = null)
            runCatching { repository.browse(state.query, nextPage) }
                .onSuccess { page ->
                    state = state.copy(
                        mods = (state.mods + page.mods).distinctBy { it.id },
                        currentPage = page.currentPage,
                        totalPages = page.totalPages,
                        totalItems = page.totalItems,
                        loadingMore = false,
                    )
                }
                .onFailure { error ->
                    state = state.copy(loadingMore = false, error = error.userMessage())
                }
        }
    }

    fun select(shortId: String) {
        scope.launch {
            state = state.copy(loadingDetails = true, error = null)
            runCatching { repository.details(shortId) }
                .onSuccess { mod -> state = state.copy(selectedMod = mod, loadingDetails = false) }
                .onFailure { error -> state = state.copy(loadingDetails = false, error = error.userMessage()) }
        }
    }

    fun clearSelection() {
        state = state.copy(selectedMod = null, loadingDetails = false)
    }

    fun downloadSelected(actions: PlatformActions) {
        val mod = state.selectedMod ?: return
        val packageUrl = GxPackageSecurity.resolvePackageUrl(mod.contentUrl)
        if (packageUrl == null) {
            state = state.copy(notice = "This mod does not expose a safe official GX package URL.")
            return
        }
        scope.launch {
            state = state.copy(downloading = true, notice = null)
            val result = runCatching { actions.download(packageUrl, mod.suggestedFileName) }
                .getOrElse { DownloadResult(false, "", it.userMessage()) }
            val record = DownloadRecord(
                modTitle = mod.title,
                fileName = mod.suggestedFileName,
                destination = result.destination,
                completed = result.success,
                note = result.message,
            )
            state = state.copy(
                downloading = false,
                notice = result.message,
                downloads = listOf(record) + state.downloads,
            )
        }
    }

    fun consumeNotice() {
        state = state.copy(notice = null)
    }

    private fun Throwable.userMessage(): String = message?.takeIf(String::isNotBlank)
        ?: this::class.simpleName
        ?: "Unknown error"
}
