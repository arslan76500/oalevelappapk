package com.oalevel.resources.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.oalevel.resources.ui.viewmodel.PdfViewerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    nodeId: String,
    displayName: String,
    onBack: () -> Unit,
    onOpenAnother: (String, String) -> Unit,
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(nodeId) { viewModel.loadPdf(nodeId, displayName) }

    // ── Split search dialog ───────────────────────────────────────────────────
    if (uiState.showSplitSearch) {
        AlertDialog(
            onDismissRequest = viewModel::closeSplitSearch,
            title = { Text("Open Side by Side") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.splitSearchQuery,
                        onValueChange = viewModel::onSplitSearchChange,
                        placeholder = { Text("Search for a PDF…") },
                        leadingIcon = {
                            if (uiState.isSplitSearching)
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else
                                Icon(Icons.Filled.Search, null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.splitSearchResults.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(uiState.splitSearchResults) { result ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            result.name,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    leadingContent = {
                                        Icon(Icons.Filled.PictureAsPdf, null,
                                            tint = MaterialTheme.colorScheme.error)
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.loadSecondPdf(result.id, result.name) }
                                )
                                HorizontalDivider()
                            }
                        }
                    } else if (uiState.splitSearchQuery.length >= 2 && !uiState.isSplitSearching) {
                        Text(
                            "No PDFs found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::closeSplitSearch) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSplitView) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text("│", color = MaterialTheme.colorScheme.outline)
                            Text(
                                uiState.secondName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column {
                            Text(displayName, maxLines = 1,
                                style = MaterialTheme.typography.titleSmall)
                            if (uiState.totalPages > 0) {
                                Text(
                                    "Page ${uiState.currentPage + 1} of ${uiState.totalPages}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (uiState.isSplitView) {
                        IconButton(onClick = viewModel::closeSplitView) {
                            Icon(Icons.Filled.CloseFullscreen, "Close split view",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = viewModel::toggleNightMode) {
                        Icon(
                            if (uiState.nightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            "Toggle night mode"
                        )
                    }
                    IconButton(onClick = viewModel::toggleFavourite) {
                        Icon(
                            if (uiState.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Favourite",
                            tint = if (uiState.isFavourite) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "More options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Rotate 90°") },
                                leadingIcon = { Icon(Icons.Filled.RotateRight, null) },
                                onClick = { showMenu = false; viewModel.rotate() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (uiState.isSplitView) "Close Split View" else "Open Side by Side")
                                },
                                leadingIcon = {
                                    Icon(
                                        if (uiState.isSplitView) Icons.Filled.CloseFullscreen
                                        else Icons.Filled.SpaceDashboard,
                                        null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    if (uiState.isSplitView) viewModel.closeSplitView()
                                    else viewModel.openSplitSearch()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Download") },
                                leadingIcon = { Icon(Icons.Filled.Download, null) },
                                onClick = { showMenu = false; viewModel.download() }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Filled.Share, null) },
                                onClick = {
                                    showMenu = false
                                    uiState.pdfUrl?.let { url ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, url)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share PDF"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open in Browser") },
                                leadingIcon = { Icon(Icons.Filled.OpenInBrowser, null) },
                                onClick = {
                                    showMenu = false
                                    uiState.pdfUrl?.let { url ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(uiState.error!!) { viewModel.loadPdf(nodeId, displayName) }
                uiState.pdfUrl != null -> {
                    if (uiState.isSplitView) {
                        // ── Side-by-side layout ───────────────────────────────────────
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Primary PDF
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                PdfPane(
                                    url = uiState.pdfUrl!!,
                                    nightMode = uiState.nightMode,
                                    rotation = uiState.rotation,
                                    startPage = uiState.currentPage,
                                    onPageChange = viewModel::onPageChange,
                                    onLoaded = viewModel::onPdfLoaded,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (uiState.totalPages > 0) {
                                    Text(
                                        "P ${uiState.currentPage + 1}/${uiState.totalPages}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                    )
                                }
                            }

                            VerticalDivider()

                            // Secondary PDF
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                when {
                                    uiState.secondIsLoading -> LoadingState()
                                    uiState.secondPdfUrl != null -> {
                                        PdfPane(
                                            url = uiState.secondPdfUrl!!,
                                            nightMode = uiState.nightMode,
                                            rotation = uiState.rotation,
                                            startPage = uiState.secondCurrentPage,
                                            onPageChange = viewModel::onSecondPageChange,
                                            onLoaded = viewModel::onSecondPdfLoaded,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (uiState.secondTotalPages > 0) {
                                            Text(
                                                "P ${uiState.secondCurrentPage + 1}/${uiState.secondTotalPages}",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                    else -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Filled.SpaceDashboard, null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.outline)
                                            Spacer(Modifier.height(8.dp))
                                            Text("Second PDF loading…",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ── Single PDF ────────────────────────────────────────────────
                        PdfPane(
                            url = uiState.pdfUrl!!,
                            nightMode = uiState.nightMode,
                            rotation = uiState.rotation,
                            startPage = uiState.currentPage,
                            onPageChange = viewModel::onPageChange,
                            onLoaded = viewModel::onPdfLoaded,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Progress bar (single view only)
            if (!uiState.isSplitView && uiState.totalPages > 0) {
                LinearProgressIndicator(
                    progress = { (uiState.currentPage + 1).toFloat() / uiState.totalPages },
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PdfPane(
    url: String,
    nightMode: Boolean,
    rotation: Int,
    startPage: Int,
    onPageChange: (Int) -> Unit,
    onLoaded: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            PDFView(ctx, null).apply {
                fromUrl(url)
                    .defaultPage(startPage)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .enableAntialiasing(true)
                    .nightMode(nightMode)
                    .onPageChange(OnPageChangeListener { page, _ -> onPageChange(page) })
                    .onLoad(OnLoadCompleteListener { nb -> onLoaded(nb) })
                    .load()
            }
        },
        update = { pdfView ->
            pdfView.setNightMode(nightMode)
            pdfView.setRotation(rotation.toFloat())
        },
        modifier = modifier
    )
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Loading PDF…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.ErrorOutline, null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
