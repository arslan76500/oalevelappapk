package com.oalevel.resources.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.oalevel.resources.data.local.Download
import com.oalevel.resources.data.remote.BreadcrumbItem
import com.oalevel.resources.data.remote.ResourceNode
import com.oalevel.resources.ui.viewmodel.BrowseViewModel

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    nodeId: String,
    onNodeClick: (ResourceNode) -> Unit,
    onBack: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasPdfs = uiState.children.any { it.type == "pdf" }

    LaunchedEffect(nodeId) { viewModel.loadNode(nodeId) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(uiState.currentNode?.name ?: "Browse") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                    },
                    actions = {
                        // Download-all button — only when the folder has PDFs
                        if (hasPdfs) {
                            if (uiState.isFolderDownloading) {
                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else {
                                IconButton(onClick = viewModel::downloadFolder) {
                                    Icon(
                                        Icons.Filled.DownloadForOffline,
                                        contentDescription = "Download all PDFs in folder",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.loadNode(nodeId) }) {
                            Icon(Icons.Filled.Refresh, "Refresh")
                        }
                    }
                )
                if (uiState.breadcrumb.isNotEmpty()) {
                    BreadcrumbRow(breadcrumb = uiState.breadcrumb)
                    HorizontalDivider()
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(16.dp), Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ErrorOutline, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadNode(nodeId) }) { Text("Retry") }
                }
            }
            uiState.children.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding), Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FolderOpen, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("This folder is empty",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.children) { node ->
                    val download = if (node.type == "pdf") uiState.downloadMap[node.id] else null
                    ResourceNodeItem(
                        node = node,
                        download = download,
                        onClick = { onNodeClick(node) }
                    )
                }
            }
        }
    }
}

@Composable
fun BreadcrumbRow(breadcrumb: List<BreadcrumbItem>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumb.forEachIndexed { index, item ->
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (index == breadcrumb.lastIndex)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (index < breadcrumb.lastIndex) {
                Icon(Icons.Filled.ChevronRight, null,
                    Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ResourceNodeItem(
    node: ResourceNode,
    download: Download? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadStatus = download?.status // "completed" | "downloading" | "pending" | "error" | null

    ListItem(
        headlineContent = {
            Text(node.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.type == "folder") {
                    Text("${node.childCount ?: 0} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    node.size?.let {
                        Text(formatSize(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Download status chip
                    when (downloadStatus) {
                        "completed" -> DownloadBadge(
                            text = "Downloaded",
                            color = MaterialTheme.colorScheme.primary
                        )
                        "downloading" -> DownloadBadge(
                            text = "↓ ${download?.progress ?: 0}%",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        "pending" -> DownloadBadge(
                            text = "Pending",
                            color = MaterialTheme.colorScheme.outline
                        )
                        "error" -> DownloadBadge(
                            text = "Error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        leadingContent = {
            Box(contentAlignment = Alignment.BottomEnd) {
                Icon(
                    imageVector = if (node.type == "folder") Icons.Filled.Folder
                                  else Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    tint = if (node.type == "folder") MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                // Green checkmark overlay for downloaded files
                if (downloadStatus == "completed") {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        trailingContent = {
            if (node.type == "folder") {
                Icon(Icons.Filled.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }
    )
}

@Composable
private fun DownloadBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
