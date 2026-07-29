package com.sevenshifts.shopping.ui.catalog

import com.sevenshifts.shopping.domain.FoodItem

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Content(val items: List<FoodItem>) : CatalogUiState

    data object Error : CatalogUiState
}
