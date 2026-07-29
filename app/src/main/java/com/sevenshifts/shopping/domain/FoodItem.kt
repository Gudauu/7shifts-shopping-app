package com.sevenshifts.shopping.domain

import java.math.BigDecimal

/**
 * A purchasable food item as the rest of the app sees it: joined to its category and with
 * the price already parsed as exact money.
 */
data class FoodItem(
    val id: String,
    val name: String,
    val price: BigDecimal,
    /**
     * Null when the item's `category_uuid` matches no known category. The item is still
     * shown; losing a purchasable item over a metadata gap is the worse failure.
     */
    val categoryName: String?,
    /** Null when the API omits the image; the card shows a placeholder instead of hiding the item. */
    val imageUrl: String?,
)
