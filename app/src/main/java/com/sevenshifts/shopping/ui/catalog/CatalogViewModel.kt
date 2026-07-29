package com.sevenshifts.shopping.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenshifts.shopping.domain.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CatalogViewModel @Inject constructor(private val repository: CatalogRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // Fetching once here, rather than on every collection, is what lets the state
        // survive a configuration change without refetching.
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = CatalogUiState.Loading
        loadJob = viewModelScope.launch {
            repository.loadCatalog()
                .onSuccess { items -> _uiState.value = CatalogUiState.Content(items) }
                .onFailure { _uiState.value = CatalogUiState.Error }
        }
    }
}
