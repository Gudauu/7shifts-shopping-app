package com.sevenshifts.shopping.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenshifts.shopping.domain.CatalogRepository
import com.sevenshifts.shopping.domain.FoodItem
import com.sevenshifts.shopping.domain.PriceSortOrder
import com.sevenshifts.shopping.domain.sortedByPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CatalogViewModel @Inject constructor(private val repository: CatalogRepository) : ViewModel() {
    private sealed interface LoadState {
        data object Loading : LoadState

        /** [items] stay in the API's order; sorting derives from them without mutating them. */
        data class Loaded(val items: List<FoodItem>) : LoadState

        data object Failed : LoadState
    }

    private val loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    private val sort = MutableStateFlow<PriceSortOrder?>(null)

    val uiState: StateFlow<CatalogUiState> = combine(loadState, sort) { load, sort ->
        when (load) {
            LoadState.Loading -> CatalogUiState.Loading

            LoadState.Failed -> CatalogUiState.Error

            is LoadState.Loaded -> CatalogUiState.Content(
                // Always sorting from the API-ordered list keeps the sort stable: items
                // with equal prices hold their relative API order in both directions.
                items = if (sort == null) load.items else load.items.sortedByPrice(sort),
                sort = sort,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CatalogUiState.Loading)

    private var loadJob: Job? = null

    init {
        // Fetching once here, rather than on every collection, is what lets the state
        // survive a configuration change without refetching.
        load()
    }

    fun retry() {
        load()
    }

    /** Null clears the sort and restores the API's order. */
    fun onSortSelected(order: PriceSortOrder?) {
        sort.value = order
    }

    private fun load() {
        loadJob?.cancel()
        loadState.value = LoadState.Loading
        loadJob = viewModelScope.launch {
            repository.loadCatalog()
                .onSuccess { items -> loadState.value = LoadState.Loaded(items) }
                .onFailure { loadState.value = LoadState.Failed }
        }
    }
}
