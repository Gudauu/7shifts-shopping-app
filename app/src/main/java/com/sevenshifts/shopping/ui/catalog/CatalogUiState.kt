package com.sevenshifts.shopping.ui.catalog

import com.sevenshifts.shopping.domain.FoodItem
import com.sevenshifts.shopping.domain.PriceSortOrder

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Content(
        val items: List<FoodItem>,
        /** Null means the user has not chosen a sort and the items keep the API's order. */
        val sort: PriceSortOrder? = null,
    ) : CatalogUiState

    data object Error : CatalogUiState
}
