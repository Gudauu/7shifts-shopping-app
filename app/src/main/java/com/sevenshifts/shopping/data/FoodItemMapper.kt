package com.sevenshifts.shopping.data

import android.util.Log
import com.sevenshifts.shopping.data.network.FoodItemCategoryDto
import com.sevenshifts.shopping.data.network.FoodItemDto
import com.sevenshifts.shopping.domain.FoodItem

private const val TAG = "FoodItemMapper"

/**
 * Joins items to their category names on `category_uuid` and enforces the catalog data
 * rules: an item is shown only with a non-blank uuid and name and a non-negative price;
 * anything else is dropped and logged rather than shown. When two elements share a uuid
 * the later one overrides the earlier. The two endpoints have no guaranteed consistency,
 * so an item whose category is unknown or dropped keeps a null category name rather than
 * being dropped itself.
 */
fun joinCatalog(items: List<FoodItemDto>, categories: List<FoodItemCategoryDto>): List<FoodItem> {
    val categoryNamesByUuid = categories
        .filter { it.uuid.isNotBlank() && it.name.isNotBlank() }
        .associate { it.uuid to it.name }
    val (sellable, dropped) = items.partition { it.isSellable() }
    dropped.forEach { Log.w(TAG, "Dropping item that violates the catalog assumptions: uuid='${it.uuid}'") }
    return sellable
        .associateBy { it.uuid }
        .values
        .map { item ->
            FoodItem(
                id = item.uuid,
                name = item.name,
                price = item.price,
                categoryName = item.categoryUuid?.let { categoryNamesByUuid[it] },
                imageUrl = item.imageUrl,
            )
        }
}

private fun FoodItemDto.isSellable(): Boolean = uuid.isNotBlank() && name.isNotBlank() && price.signum() >= 0
