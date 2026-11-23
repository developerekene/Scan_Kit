package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.HistoryEntity
import com.example.data.model.QrContentType
import com.example.data.model.QrSource
import com.example.data.repository.QrHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter(val label: String) {
    ALL("All"),
    SCANNED("Scanned"),
    CREATED("Created"),
    FAVORITES("Favorites"),
    URL("Links"),
    WIFI("Wi-Fi"),
    CONTACT("Contacts")
}

data class HistoryUiState(
    val entries: List<HistoryEntity> = emptyList(),
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val selectedEntry: HistoryEntity? = null
)

class HistoryViewModel(
    private val repository: QrHistoryRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(HistoryFilter.ALL)
    private val selectedEntry = MutableStateFlow<HistoryEntity?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allEntries,
        searchQuery,
        selectedFilter,
        selectedEntry
    ) { all, query, filter, activeEntry ->
        val filteredByType = when (filter) {
            HistoryFilter.ALL -> all
            HistoryFilter.SCANNED -> all.filter { it.parsedSource() == QrSource.SCANNED }
            HistoryFilter.CREATED -> all.filter { it.parsedSource() == QrSource.CREATED }
            HistoryFilter.FAVORITES -> all.filter { it.isFavorite }
            HistoryFilter.URL -> all.filter {
                it.parsedType() == QrContentType.URL || it.parsedType() == QrContentType.SOCIAL
            }
            HistoryFilter.WIFI -> all.filter { it.parsedType() == QrContentType.WIFI }
            HistoryFilter.CONTACT -> all.filter { it.parsedType() == QrContentType.CONTACT }
        }

        val finalItems = if (query.isBlank()) {
            filteredByType
        } else {
            val q = query.trim().lowercase()
            filteredByType.filter { entry ->
                entry.displayTitle.lowercase().contains(q) ||
                    entry.displaySubtitle.lowercase().contains(q) ||
                    entry.rawContent.lowercase().contains(q)
            }
        }

        val refreshedSelected = activeEntry?.let { sel -> all.find { it.id == sel.id } }

        HistoryUiState(
            entries = finalItems,
            totalCount = all.size,
            searchQuery = query,
            selectedFilter = filter,
            selectedEntry = refreshedSelected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectFilter(filter: HistoryFilter) {
        selectedFilter.value = filter
    }

    fun selectEntry(entry: HistoryEntity?) {
        selectedEntry.value = entry
    }

    fun toggleFavorite(entry: HistoryEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(entry.id, entry.isFavorite)
        }
    }

    fun deleteEntry(entry: HistoryEntity) {
        viewModelScope.launch {
            if (selectedEntry.value?.id == entry.id) {
                selectedEntry.value = null
            }
            repository.deleteById(entry.id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            selectedEntry.value = null
            repository.clearAll()
        }
    }

    companion object {
        fun factory(repository: QrHistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(repository) as T
                }
            }
    }
}
