package com.oalevel.resources.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.oalevel.resources.ui.viewmodel.PdfViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

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
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(uiState.splitSearchResults) { result ->
                                ListItem(
                                    headlineContent = {
                                        Text(result.name, maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium)
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
                        Text("No PDFs found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp))
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(displayName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text("│", color = MaterialTheme.colorScheme.outline)
                            Text(uiState.secondName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        }
                    } else {
                        Column {
                            Text(displayName, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                            if (uiState.totalPages > 0) {
                                Text("Page ${uiState.currentPage + 1} of ${uiState.totalPages}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    IconButton(onClick = viewModel::toggleFavourite) {
                        Icon(
                            if (uiState.isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
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
                                text = { Text(if (uiState.isSplitView) "Close Split View" else "Open Side by Side") },
                                leadingIcon = {
                                    Icon(if (uiState.isSplitView) Icons.Filled.CloseFullscreen
                                         else Icons.Filled.SpaceDashboard, null)
                                },
                                onClick = {
                                    showMenu = false
                                    if (uiState.isSplitView) viewModel.closeSplitView()
                                    else viewModel.openSplitSearch()
                                }
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
                uiState.isLoading -> PdfLoadingState()
                uiState.error != null -> PdfErrorState(uiState.error!!) { viewModel.loadPdf(nodeId, displayName) }
                uiState.pdfUrl != null -> {
                    if (uiState.isSplitView) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            PdfPane(url = uiState.pdfUrl!!, onLoaded = viewModel::onPdfLoaded,
                                modifier = Modifier.weight(1f).fillMaxHeight())
                            VerticalDivider()
                            when {
                                uiState.secondIsLoading -> PdfLoadingState()
                                uiState.secondPdfUrl != null ->
                                    PdfPane(url = uiState.secondPdfUrl!!, onLoaded = viewModel::onSecondPdfLoaded,
                                        modifier = Modifier.weight(1f).fillMaxHeight())
                                else -> Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) {
                                    Text("Second PDF loading…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        PdfPane(url = uiState.pdfUrl!!, onLoaded = viewModel::onPdfLoaded,
                            modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPane(
    url: String,
    onLoaded: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pages by remember(url) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        isLoading = true
        error = null
        withContext(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "pdf_${url.hashCode()}.pdf")
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    URL(url).openStream().use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                }
                val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val bitmaps = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val bmp = Bitmap.createBitmap(
                            page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bmp)
                    }
                }
                renderer.close()
                fd.close()
                pages = bitmaps
                onLoaded(bitmaps.size)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load PDF"
            } finally {
                isLoading = false
            }
        }
    }

    when {
        isLoading -> PdfLoadingState()
        error != null -> PdfErrorState(error!!) {}
        else -> {
            val listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = modifier) {
                items(pages) { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfLoadingState() {
    Column(modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Loading PDF…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PdfErrorState(error: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.ErrorOutline, null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
