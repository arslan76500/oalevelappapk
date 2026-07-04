package com.oalevel.resources.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.oalevel.resources.data.remote.SearchResult
import com.oalevel.resources.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onResultClick: (SearchResult) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search PDFs, folders, subjects...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearQuery) {
                                    Icon(Icons.Filled.Clear, "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter chips
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all", "pdf", "folder").forEach { type ->
                    FilterChip(
                        selected = uiState.selectedType == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(type.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            HorizontalDivider()

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.query.isEmpty() -> {
                    // Recent searches
                    if (uiState.recentSearches.isNotEmpty()) {
                        LazyColumn {
                            item {
                                ListItem(
                                    headlineContent = {
                                        Text("Recent Searches",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    trailingContent = {
                                        TextButton(onClick = viewModel::clearRecentSearches) {
                                            Text("Clear")
                                        }
                                    }
                                )
                            }
                            items(uiState.recentSearches) { search ->
                                ListItem(
                                    headlineContent = { Text(search) },
                                    leadingContent = {
                                        Icon(Icons.Filled.History, null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    modifier = Modifier.clickable {
                                        viewModel.onQueryChange(search)
                                    }
                                )
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Type to search...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                uiState.results.isEmpty() && !uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.SearchOff, null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(8.dp))
                            Text("No results for \"${uiState.query}\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(8.dp)) {
                        item {
                            Text(
                                "${uiState.totalResults} results",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        items(uiState.results) { result ->
                            SearchResultItem(result = result, onClick = { onResultClick(result) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(result.name) },
        supportingContent = {
            Text(
                result.breadcrumb.dropLast(1).joinToString(" > ") { it.name },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                if (result.type == "folder") Icons.Filled.Folder else Icons.Filled.PictureAsPdf,
                null,
                tint = if (result.type == "folder") MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
        },
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }
    )
}
