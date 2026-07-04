package com.oalevel.resources.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.oalevel.resources.data.remote.*
import com.oalevel.resources.data.repository.ResourceRepository
import javax.inject.Inject

data class HomeUiState(
    val isLoadingLevels: Boolean = true,
    val levels: List<ResourceNode> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val recentResources: List<ResourceItem> = emptyList(),
    val config: PublicConfig? = null,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ResourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLevels = true, error = null)

            // Load all in parallel
            val levelsResult = repository.getLevels()
            val announcementsResult = repository.getAnnouncements()
            val recentResult = repository.getRecentResources(10)
            val configResult = repository.getPublicConfig()

            _uiState.value = _uiState.value.copy(
                isLoadingLevels = false,
                levels = levelsResult.getOrDefault(emptyList()),
                announcements = announcementsResult.getOrDefault(emptyList()),
                recentResources = recentResult.getOrDefault(emptyList()),
                config = configResult.getOrNull(),
                error = levelsResult.exceptionOrNull()?.message
            )
        }
    }

    fun refresh() = loadData()
}
