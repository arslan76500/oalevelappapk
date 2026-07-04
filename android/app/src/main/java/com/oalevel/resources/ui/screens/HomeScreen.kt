package com.oalevel.resources.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.oalevel.resources.data.remote.Announcement
import com.oalevel.resources.data.remote.ResourceItem
import com.oalevel.resources.data.remote.ResourceNode
import com.oalevel.resources.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLevelClick: (ResourceNode) -> Unit,
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onFavouritesClick: () -> Unit,
    onRecentClick: () -> Unit,
    onContinueReadingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAiChatClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "O/A Level Resources",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Your study companion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick action buttons
            item {
                QuickActionsRow(
                    onSearchClick = onSearchClick,
                    onDownloadsClick = onDownloadsClick,
                    onFavouritesClick = onFavouritesClick,
                    onRecentClick = onRecentClick,
                    onContinueReadingClick = onContinueReadingClick,
                    onAiChatClick = onAiChatClick
                )
            }

            // Announcements
            if (uiState.announcements.isNotEmpty()) {
                item {
                    AnnouncementsSection(announcements = uiState.announcements)
                }
            }

            // Level cards
            item {
                Text(
                    text = "Browse Resources",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Levels from API
            if (uiState.isLoadingLevels) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.levels.isNotEmpty()) {
                items(uiState.levels.chunked(2)) { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { level ->
                            LevelCard(
                                node = level,
                                modifier = Modifier.weight(1f),
                                onClick = { onLevelClick(level) }
                            )
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                // Default cards when no data yet
                item { DefaultLevelCards(onAiChatClick = onAiChatClick) }
            }

            // WhatsApp channel
            if (uiState.config?.whatsappChannel?.isNotBlank() == true) {
                item {
                    WhatsAppChannelCard(url = uiState.config!!.whatsappChannel)
                }
            }

            // Recent resources
            if (uiState.recentResources.isNotEmpty()) {
                item {
                    Text(
                        text = "Latest Resources",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(uiState.recentResources.take(5)) { resource ->
                    RecentResourceItem(
                        resource = resource,
                        onClick = { /* navigate */ }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onFavouritesClick: () -> Unit,
    onRecentClick: () -> Unit,
    onContinueReadingClick: () -> Unit,
    onAiChatClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        QuickActionChip(Icons.Filled.Search, "Search", onSearchClick)
        QuickActionChip(Icons.Filled.Download, "Downloads", onDownloadsClick)
        QuickActionChip(Icons.Filled.Favorite, "Favourites", onFavouritesClick)
        QuickActionChip(Icons.Filled.History, "Recent", onRecentClick)
        QuickActionChip(Icons.Filled.MenuBook, "Continue", onContinueReadingClick)
        QuickActionChip(Icons.Filled.SmartToy, "AI Chat", onAiChatClick)
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
private fun LevelCard(
    node: ResourceNode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    )

    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Icon(
                    Icons.Filled.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if ((node.childCount ?: 0) > 0) {
                    Text(
                        text = "${node.childCount} subjects",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultLevelCards(onAiChatClick: () -> Unit) {
    val levels = listOf(
        Triple("O Level", Icons.Filled.School, "Cambridge O Level"),
        Triple("IGCSE", Icons.Filled.AutoStories, "Cambridge IGCSE"),
        Triple("AS Level", Icons.Filled.School, "AS Level"),
        Triple("A2 Level", Icons.Filled.School, "A2 Level"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        levels.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (name, icon, subtitle) ->
                    Card(
                        modifier = Modifier.weight(1f).height(110.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(name, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // AI Card
        Card(
            onClick = onAiChatClick,
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.SmartToy, null, tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp))
                Column {
                    Text("O/A Level AI", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("Ask questions, solve MCQs, analyze PDFs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.7f))
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun AnnouncementsSection(announcements: List<Announcement>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Announcements",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        announcements.take(3).forEach { ann ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(ann.title, style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(ann.message, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(0.8f))
                }
            }
        }
    }
}

@Composable
private fun WhatsAppChannelCard(url: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        onClick = {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url))
            context.startActivity(intent)
        },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF25D366)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Chat, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text("Join WhatsApp Channel", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text("Get updates and resources", style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.85f))
            }
            Icon(Icons.Filled.OpenInNew, null, tint = Color.White)
        }
    }
}

@Composable
private fun RecentResourceItem(resource: ResourceItem, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(resource.name, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = {
            Text(
                resource.parentName ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(Icons.Filled.PictureAsPdf, null,
                tint = MaterialTheme.colorScheme.error)
        },
        trailingContent = {
            resource.size?.let {
                Text(formatSize(it), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }
    )
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
