package com.sevenshifts.shopping.ui.catalog

import com.sevenshifts.shopping.domain.FoodCategory
import com.sevenshifts.shopping.domain.FoodItem
import com.sevenshifts.shopping.domain.PriceSortOrder

/**
 * The one state the catalog screen renders. The cart fields sit beside the catalog
 * section rather than inside [CatalogState.Content] because the cart outlives catalog
 * loading: a future reload must not blank a non-empty badge while the list is away.
 */
data class CatalogUiState(
    val catalog: CatalogState = CatalogState.Loading,
    /** Total adds across the cart, so three of one item is 3; shown as the badge. */
    val cartItemCount: Int = 0,
    /** How many of each item are in the cart, keyed by item id; absent means none. */
    val cartQuantities: Map<String, Int> = emptyMap(),
)

/** The catalog portion of the screen, which loads independently of the cart. */
sealed interface CatalogState {
    data object Loading : CatalogState

    data class Content(
        /** Already filtered and sorted; the screen renders it as-is. */
        val items: List<FoodItem>,
        /** Null means the user has not chosen a sort and the items keep the API's order. */
        val sort: PriceSortOrder? = null,
        /** Every known category in the API's order, offered as filter chips. */
        val categories: List<FoodCategory> = emptyList(),
        /** Empty means no filter and every item shows. */
        val selectedCategoryIds: Set<String> = emptySet(),
    ) : CatalogState

    data object Error : CatalogState
}
