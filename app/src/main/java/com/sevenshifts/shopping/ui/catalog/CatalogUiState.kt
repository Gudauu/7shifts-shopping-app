package com.sevenshifts.shopping.ui.catalog

import com.sevenshifts.shopping.domain.FoodCategory
import com.sevenshifts.shopping.domain.FoodItem
import com.sevenshifts.shopping.domain.PriceSortOrder

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Content(
        /** Already filtered and sorted; the screen renders it as-is. */
        val items: List<FoodItem>,
        /** Null means the user has not chosen a sort and the items keep the API's order. */
        val sort: PriceSortOrder? = null,
        /** Every known category in the API's order, offered as filter chips. */
        val categories: List<FoodCategory> = emptyList(),
        /** Empty means no filter and every item shows. */
        val selectedCategoryIds: Set<String> = emptySet(),
        /** Total adds across the cart, so three of one item is 3; shown as the badge. */
        val cartItemCount: Int = 0,
    ) : CatalogUiState

    data object Error : CatalogUiState
}
