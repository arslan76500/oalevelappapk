package com.oalevel.resources.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.oalevel.resources.data.local.Download
import com.oalevel.resources.data.remote.*
import com.oalevel.resources.data.repository.DownloadRepository
import com.oalevel.resources.data.repository.ResourceRepository
import com.oalevel.resources.data.service.DownloadService
import javax.inject.Inject

data class BrowseUiState(
    val isLoading: Boolean = false,
    val currentNode: ResourceNode? = null,
    val children: List<ResourceNode> = emptyList(),
    val breadcrumb: List<BreadcrumbItem> = emptyList(),
    val error: String? = null,
    // resourceId -> Download (null = not downloaded)
    val downloadMap: Map<String, Download> = emptyMap(),
    val isFolderDownloading: Boolean = false
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: ResourceRepository,
    private val downloadRepository: DownloadRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        // Keep download status in sync with Room in real time
        viewModelScope.launch {
            downloadRepository.getAllDownloads().collect { downloads ->
                val map = downloads.associateBy { it.resourceId }
                _uiState.update { it.copy(downloadMap = map) }
            }
        }
    }

    fun loadNode(nodeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val node = repository.getNode(nodeId).getOrThrow()
                val children = repository.getNodeChildren(nodeId).getOrThrow()
                val breadcrumb = repository.getBreadcrumb(nodeId).getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentNode = node,
                        children = children.items,
                        breadcrumb = breadcrumb
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load") }
            }
        }
    }

    /** Download all PDFs in the current folder (one level only). */
    fun downloadFolder() {
        val pdfs = _uiState.value.children.filter { it.type == "pdf" }
        if (pdfs.isEmpty()) return

        _uiState.update { it.copy(isFolderDownloading = true) }
        viewModelScope.launch {
            pdfs.map { pdf ->
                async {
                    // Skip already-completed downloads
                    val existing = _uiState.value.downloadMap[pdf.id]
                    if (existing?.status == "completed") return@async

                    val urlResult = repository.getPdfUrl(pdf.id)
                    urlResult.getOrNull()?.let { urlResponse ->
                        DownloadService.start(
                            context = context,
                            resourceId = pdf.id,
                            driveId = pdf.driveId,
                            url = urlResponse.url,
                            name = pdf.name
                        )
                    }
                }
            }.awaitAll()
            _uiState.update { it.copy(isFolderDownloading = false) }
        }
    }
}
