package com.sevenshifts.shopping.domain

/** The user's price sort choice. Absence of a choice means the API's own order. */
enum class PriceSortOrder {
    ASCENDING,
    DESCENDING,
}

/**
 * Orders items by price. The sort is stable, so items with equal prices keep the relative
 * order of the receiver; callers sort from the API-ordered list so that order is the API's
 * in both directions.
 */
fun List<FoodItem>.sortedByPrice(order: PriceSortOrder): List<FoodItem> = when (order) {
    PriceSortOrder.ASCENDING -> sortedBy { it.price }
    PriceSortOrder.DESCENDING -> sortedByDescending { it.price }
}
