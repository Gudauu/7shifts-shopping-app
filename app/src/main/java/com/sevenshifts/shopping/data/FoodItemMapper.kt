package com.sevenshifts.shopping.data

import com.sevenshifts.shopping.data.network.FoodItemCategoryDto
import com.sevenshifts.shopping.data.network.FoodItemDto
import com.sevenshifts.shopping.domain.FoodItem

/**
 * Joins items to their category names on `category_uuid`. The two endpoints have no
 * guaranteed consistency, so an item whose category is unknown keeps a null category
 * name rather than being dropped.
 */
fun joinCatalog(items: List<FoodItemDto>, categories: List<FoodItemCategoryDto>): List<FoodItem> {
    val categoryNamesByUuid = categories.associate { it.uuid to it.name }
    return items.map { item ->
        FoodItem(
            id = item.uuid,
            name = item.name,
            price = item.price,
            categoryName = categoryNamesByUuid[item.categoryUuid],
            imageUrl = item.imageUrl,
        )
    }
}
