package com.oalevel.resources.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.oalevel.resources.data.local.*
import com.oalevel.resources.ui.viewmodel.DownloadsViewModel
import com.oalevel.resources.ui.viewmodel.FavouritesViewModel
import com.oalevel.resources.ui.viewmodel.RecentViewModel
import com.oalevel.resources.ui.viewmodel.ContinueReadingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onOpenPdf: (Download) -> Unit,
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsState(emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            EmptyState(Modifier.padding(padding), Icons.Filled.Download, "No downloads yet")
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(downloads) { dl ->
                    DownloadItem(dl, { onOpenPdf(dl) }, { viewModel.deleteDownload(dl.id) })
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(download: Download, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(download.name) },
        supportingContent = {
            Column {
                Text(download.status, style = MaterialTheme.typography.labelSmall, color = statusColor(download.status))
                if (download.status == "downloading") {
                    LinearProgressIndicator(
                        progress = { download.progress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        },
        leadingContent = {
            Icon(Icons.Filled.PictureAsPdf, null,
                tint = if (download.status == "completed") MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline)
        },
        trailingContent = {
            Row {
                if (download.status == "completed") {
                    IconButton(onClick = onClick) { Icon(Icons.Filled.OpenInNew, "Open") }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        modifier = Modifier.clickable(enabled = download.status == "completed") { onClick() }
    )
}

@Composable
private fun statusColor(status: String) = when (status) {
    "completed" -> MaterialTheme.colorScheme.primary
    "error" -> MaterialTheme.colorScheme.error
    "downloading" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    onItemClick: (Favourite) -> Unit,
    onBack: () -> Unit,
    viewModel: FavouritesViewModel = hiltViewModel()
) {
    val favourites by viewModel.favourites.collectAsState(emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favourites") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (favourites.isEmpty()) {
            EmptyState(Modifier.padding(padding), Icons.Outlined.FavoriteBorder,
                "No favourites yet. Tap the heart icon on any PDF.")
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(favourites) { fav ->
                    ListItem(
                        headlineContent = { Text(fav.name) },
                        supportingContent = {
                            Text(fav.parentPath, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingContent = {
                            Icon(
                                if (fav.type == "folder") Icons.Filled.Folder else Icons.Filled.PictureAsPdf,
                                null,
                                tint = if (fav.type == "folder") MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.remove(fav.resourceId) }) {
                                Icon(Icons.Outlined.FavoriteBorder, null, tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onItemClick(fav) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(
    onItemClick: (RecentViewed) -> Unit,
    onBack: () -> Unit,
    viewModel: RecentViewModel = hiltViewModel()
) {
    val items by viewModel.recentItems.collectAsState(emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Viewed") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(Modifier.padding(padding), Icons.Filled.History, "Nothing viewed yet")
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(items) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        leadingContent = {
                            Icon(
                                if (item.type == "folder") Icons.Filled.Folder else Icons.Filled.PictureAsPdf,
                                null,
                                tint = if (item.type == "folder") MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.clickable { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinueReadingScreen(
    onPdfClick: (com.oalevel.resources.data.local.ReadingProgress) -> Unit,
    onBack: () -> Unit,
    viewModel: ContinueReadingViewModel = hiltViewModel()
) {
    val items by viewModel.progressItems.collectAsState(emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Continue Reading") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(Modifier.padding(padding), Icons.Filled.MenuBook,
                "Start reading a PDF to track your progress")
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(items) { prog ->
                    ListItem(
                        headlineContent = { Text(prog.name) },
                        supportingContent = {
                            Column {
                                Text("Page ${prog.currentPage + 1} of ${prog.totalPages}",
                                    style = MaterialTheme.typography.labelSmall)
                                LinearProgressIndicator(
                                    progress = {
                                        if (prog.totalPages > 0)
                                            (prog.currentPage + 1f) / prog.totalPages
                                        else 0f
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        },
                        leadingContent = {
                            Icon(Icons.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable { onPdfClick(prog) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                ListItem(
                    headlineContent = { Text("Backend URL") },
                    supportingContent = { Text("oalevelresources.onrender.com") },
                    leadingContent = { Icon(Icons.Filled.Cloud, null) }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Clear Cache") },
                    leadingContent = { Icon(Icons.Filled.CleaningServices, null) },
                    modifier = Modifier.clickable { }
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("About") },
                    supportingContent = { Text("O/A Level Resources v1.0.0") },
                    leadingContent = { Icon(Icons.Filled.Info, null) }
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
