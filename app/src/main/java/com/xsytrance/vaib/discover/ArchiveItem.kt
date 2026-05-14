package com.xsytrance.vaib.discover

data class ArchiveItem(
    val id: String,
    val title: String,
    val creator: String,
)

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Success(val items: List<ArchiveItem>) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}
