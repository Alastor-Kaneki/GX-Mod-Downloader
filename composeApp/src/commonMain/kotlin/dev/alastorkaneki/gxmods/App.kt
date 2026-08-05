package dev.alastorkaneki.gxmods

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

private enum class AppSection(val label: String, val icon: ImageVector) {
    BROWSE("Browse", Icons.Outlined.GridView),
    DOWNLOADS("Downloads", Icons.Outlined.History),
    ABOUT("About", Icons.Outlined.Info),
}

private data class QuickFilter(val label: String, val alias: String?)

private val platformFilters = listOf(
    QuickFilter("All platforms", null),
    QuickFilter("Desktop", "desktop"),
    QuickFilter("Mobile", "mobile"),
)

private val typeFilters = listOf(
    QuickFilter("All components", null),
    QuickFilter("Wallpaper", "wallpaper"),
    QuickFilter("Theme", "theme"),
    QuickFilter("Music", "background-music"),
    QuickFilter("Sounds", "browser-sounds"),
    QuickFilter("Web modding", "page-style"),
    QuickFilter("Shader", "shader"),
    QuickFilter("Fonts", "fonts"),
    QuickFilter("Icons", "image-overrides"),
)

@Composable
fun App(platformActions: PlatformActions) {
    val controller = remember { GxStoreController() }
    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    GxTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 900.dp
                AppScaffold(controller, platformActions, wide)
            }
        }
    }
}

@Composable
private fun AppScaffold(
    controller: GxStoreController,
    platformActions: PlatformActions,
    wide: Boolean,
) {
    val state = controller.state
    var section by remember { mutableStateOf(AppSection.BROWSE) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbarHost.showSnackbar(it)
            controller.consumeNotice()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            if (!wide && state.selectedMod == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AppSection.entries.forEach { item ->
                        NavigationBarItem(
                            selected = section == item,
                            onClick = { section = item },
                            icon = { Icon(item.icon, null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        Row(Modifier.fillMaxSize().padding(contentPadding)) {
            if (wide && state.selectedMod == null) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        GxLogo(Modifier.padding(vertical = 20.dp))
                    },
                ) {
                    AppSection.entries.forEach { item ->
                        NavigationRailItem(
                            selected = section == item,
                            onClick = { section = item },
                            icon = { Icon(item.icon, null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    state.selectedMod != null -> DetailScreen(controller, platformActions, state.selectedMod)
                    state.loadingDetails -> LoadingOverlay("Loading mod details…")
                    section == AppSection.BROWSE -> BrowseScreen(controller, wide)
                    section == AppSection.DOWNLOADS -> DownloadsScreen(state.downloads)
                    else -> AboutScreen()
                }
            }
        }
    }
}

@Composable
private fun GxLogo(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("GX", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text("MOD//DL", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun BrowseScreen(controller: GxStoreController, wide: Boolean) {
    val state = controller.state
    var searchText by remember(state.query.search) { mutableStateOf(state.query.search) }

    Column(Modifier.fillMaxSize()) {
        GxHeader(
            title = "GX MOD DOWNLOADER",
            subtitle = "Search, inspect, and download raw Opera GX mod packages.",
            trailing = {
                IconButton(onClick = controller::refresh) {
                    Icon(Icons.Outlined.Refresh, "Refresh")
                }
            },
        )

        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (wide) 28.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Search GX Mods") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { controller.search(searchText) }),
                    shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
                )
                Button(
                    onClick = { controller.search(searchText) },
                    shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Icon(Icons.Outlined.Search, null)
                    if (wide) {
                        Spacer(Modifier.width(8.dp))
                        Text("SEARCH")
                    }
                }
            }

            FilterStrip(
                title = "Platform",
                filters = platformFilters,
                selected = state.query.platformTag,
                onSelected = controller::setPlatform,
            )
            FilterStrip(
                title = "Components",
                filters = typeFilters,
                selected = state.query.typeTag,
                onSelected = controller::setType,
            )
            SortStrip(state.query.sort, controller::setSort)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.loading) "CONNECTING TO GX STORE" else "${formatCount(state.totalItems.toLong())} MODS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Unofficial client • Downloads only",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> LoadingPanel("Loading GX Mods…")
            state.error != null && state.mods.isEmpty() -> ErrorPanel(state.error, controller::refresh)
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (wide) 280.dp else 220.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = if (wide) 28.dp else 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.mods, key = { it.id }) { mod ->
                        ModCard(mod = mod, onClick = { controller.select(mod.shortId) })
                    }
                    if (state.currentPage + 1 < state.totalPages) {
                        item(key = "load-more") {
                            OutlinedButton(
                                onClick = controller::loadMore,
                                enabled = !state.loadingMore,
                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
                            ) {
                                if (state.loadingMore) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("LOAD MORE")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterStrip(
    title: String,
    filters: List<QuickFilter>,
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        filters.forEach { filter ->
            FilterChip(
                selected = selected == filter.alias,
                onClick = { onSelected(filter.alias) },
                label = { Text(filter.label) },
                shape = CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp),
            )
        }
    }
}

@Composable
private fun SortStrip(selected: GxSort, onSelected: (GxSort) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("SORT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        GxSort.entries.forEach { sort ->
            FilterChip(
                selected = selected == sort,
                onClick = { onSelected(sort) },
                label = { Text(sort.label) },
                shape = CutCornerShape(topEnd = 9.dp, bottomStart = 9.dp),
            )
        }
    }
}

@Composable
private fun ModCard(mod: GxMod, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CutCornerShape(topEnd = 22.dp, bottomStart = 22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box {
            RemoteImage(
                url = mod.coverUrl ?: mod.iconUrl,
                contentDescription = mod.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop,
            )
            if (mod.official) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    shape = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                ) {
                    Text(
                        "OFFICIAL",
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                mod.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "by ${mod.creator}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                mod.description.ifBlank { "No description supplied." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(38.dp),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("↓ ${formatCount(mod.downloads)}", style = MaterialTheme.typography.labelMedium)
                Text(formatBytes(mod.sizeBytes), style = MaterialTheme.typography.labelMedium)
                Text("v${mod.packageVersion}", style = MaterialTheme.typography.labelMedium)
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                mod.components.take(3).forEach { component ->
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            component,
                            Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(
    controller: GxStoreController,
    actions: PlatformActions,
    mod: GxMod,
) {
    val state = controller.state
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = controller::clearSelection) {
                Icon(Icons.Outlined.ArrowBack, "Back")
            }
            GxLogo()
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { actions.openExternalUrl(mod.storeUrl) }) {
                Icon(Icons.Outlined.OpenInNew, null)
                Spacer(Modifier.width(6.dp))
                Text("GX STORE")
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            RemoteImage(
                url = mod.coverUrl ?: mod.iconUrl,
                contentDescription = mod.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(CutCornerShape(topEnd = 28.dp, bottomStart = 28.dp)),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mod.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("by ${mod.creator}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (mod.official) {
                    Icon(Icons.Outlined.CheckCircle, "Opera GX Official", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Text(mod.description.ifBlank { "No description supplied." }, style = MaterialTheme.typography.bodyLarge)

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                mod.platforms.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                mod.components.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
            }

            MetadataGrid(mod)

            if (mod.hasActiveContent) {
                SafetyPanel(
                    title = "ACTIVE CONTENT INCLUDED",
                    message = "This package contains web-modding CSS or shaders. The app downloads the raw file only and never executes mod content.",
                )
            } else {
                SafetyPanel(
                    title = "SAFE DOWNLOAD FLOW",
                    message = "The package URL is constrained to official GX content hosts. Opera GX still controls installation and activation.",
                )
            }

            Button(
                onClick = { controller.downloadSelected(actions) },
                enabled = !state.downloading,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (state.downloading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("STARTING DOWNLOAD…")
                } else {
                    Icon(Icons.Outlined.Download, null)
                    Spacer(Modifier.width(10.dp))
                    Text("DOWNLOAD RAW .CRX", fontWeight = FontWeight.Black)
                }
            }

            Text(
                "Downloads are not installed automatically. Review and enable mods through Opera GX.",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MetadataGrid(mod: GxMod) {
    Card(
        shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MetadataRow("VERSION", mod.packageVersion.ifBlank { "Unknown" })
            MetadataRow("DOWNLOADS", formatCount(mod.downloads))
            MetadataRow("PACKAGE SIZE", formatBytes(mod.sizeBytes))
            MetadataRow("FILES", mod.contentFileCount.toString())
            MetadataRow("CREATED", formatIsoDate(mod.creationDate))
            MetadataRow("UPDATED", formatIsoDate(mod.lastModified))
            MetadataRow("GX ID", mod.shortId)
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SafetyPanel(title: String, message: String) {
    Row(
        Modifier.fillMaxWidth()
            .clip(CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun DownloadsScreen(downloads: List<DownloadRecord>) {
    Column(Modifier.fillMaxSize()) {
        GxHeader("DOWNLOADS", "Packages started from this app during the current session.")
        if (downloads.isEmpty()) {
            EmptyPanel(Icons.Outlined.Download, "NO DOWNLOADS YET", "Choose a mod and download its raw .crx package.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(downloads) { record ->
                    Card(shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                if (record.completed) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                null,
                                tint = if (record.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            Column {
                                Text(record.modTitle, fontWeight = FontWeight.Bold)
                                Text(record.fileName, style = MaterialTheme.typography.bodySmall)
                                Text(record.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (record.destination.isNotBlank()) {
                                    Text(record.destination, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    Column(Modifier.fillMaxSize()) {
        GxHeader("ABOUT", "A native, unofficial GX Mods browser and downloader.")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GxLogo()
            Text("GX Mod Downloader", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(
                "Built with Kotlin and Compose Multiplatform for Android, Windows, and Linux. It talks directly to the GX Store catalog API and downloads only raw packages from allowlisted Opera GX content hosts.",
                style = MaterialTheme.typography.bodyLarge,
            )
            SafetyPanel(
                "UNOFFICIAL PROJECT",
                "This project is not affiliated with or endorsed by Opera. Opera, Opera GX, and GX Mods are trademarks of their respective owners.",
            )
            Text("Version 0.1.0-alpha", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GxHeader(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
private fun LoadingPanel(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingOverlay(label: String) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(label)
        }
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(42.dp))
            Text("GX STORE CONNECTION FAILED", fontWeight = FontWeight.Black)
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("RETRY")
            }
        }
    }
}

@Composable
private fun EmptyPanel(icon: ImageVector, title: String, message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Black)
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
