package com.sevenshifts.shopping.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.domain.Catalog
import com.sevenshifts.shopping.domain.CatalogRepository
import com.sevenshifts.shopping.domain.FoodItem
import com.sevenshifts.shopping.domain.PriceSortOrder
import com.sevenshifts.shopping.domain.filteredByCategories
import com.sevenshifts.shopping.domain.sortedByPrice
import com.sevenshifts.shopping.domain.totalQuantity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CatalogViewModel @Inject constructor(private val repository: CatalogRepository, private val cart: Cart) :
    ViewModel() {
    private sealed interface LoadState {
        data object Loading : LoadState

        /** [catalog] stays in the API's order; filtering and sorting derive from it without mutating it. */
        data class Loaded(val catalog: Catalog) : LoadState

        data object Failed : LoadState
    }

    private val loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    private val sort = MutableStateFlow<PriceSortOrder?>(null)
    private val selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<CatalogUiState> =
        combine(loadState, sort, selectedCategoryIds, cart.lines) { load, sort, selected, cartLines ->
            CatalogUiState(
                catalog = when (load) {
                    LoadState.Loading -> CatalogState.Loading

                    LoadState.Failed -> CatalogState.Error

                    is LoadState.Loaded -> {
                        // Filtering preserves relative order, and always sorting from the
                        // API-ordered list keeps the sort stable: items with equal prices hold
                        // their relative API order in both directions. Neither control resets
                        // the other; both derive from the same untouched catalog.
                        val filtered = load.catalog.items.filteredByCategories(selected)
                        CatalogState.Content(
                            items = if (sort == null) filtered else filtered.sortedByPrice(sort),
                            sort = sort,
                            categories = load.catalog.categories,
                            selectedCategoryIds = selected,
                        )
                    }
                },
                cartItemCount = cartLines.totalQuantity,
                cartQuantities = cartLines.associate { it.item.id to it.quantity },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, CatalogUiState())

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

    /** Adding is silent and unbounded; the badge in the state is the feedback. */
    fun onAddToCart(item: FoodItem) {
        cart.add(item)
    }

    /** Selections are additive; toggling the last one off restores the full list. */
    fun onCategoryToggled(categoryId: String) {
        selectedCategoryIds.update { selected ->
            if (categoryId in selected) selected - categoryId else selected + categoryId
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadState.value = LoadState.Loading
        loadJob = viewModelScope.launch {
            repository.loadCatalog()
                .onSuccess { catalog -> loadState.value = LoadState.Loaded(catalog) }
                .onFailure { loadState.value = LoadState.Failed }
        }
    }
}
