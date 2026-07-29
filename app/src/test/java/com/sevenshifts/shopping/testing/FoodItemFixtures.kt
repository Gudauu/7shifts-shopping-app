package com.sevenshifts.shopping.testing

import com.sevenshifts.shopping.domain.Catalog
import com.sevenshifts.shopping.domain.FoodCategory
import com.sevenshifts.shopping.domain.FoodItem
import java.math.BigDecimal

fun foodCategory(id: String = "cat-produce", name: String = "Produce") = FoodCategory(id = id, name = name)

fun foodItem(
    id: String = "uuid-1",
    name: String = "Bananas",
    price: BigDecimal = BigDecimal("1.49"),
    category: FoodCategory? = foodCategory(),
    imageUrl: String? = "https://example.test/bananas.png",
) = FoodItem(
    id = id,
    name = name,
    price = price,
    category = category,
    imageUrl = imageUrl,
)

fun catalog(
    items: List<FoodItem> = listOf(foodItem()),
    /** Defaults to the items' own categories in first-appearance order. */
    categories: List<FoodCategory> = items.mapNotNull { it.category }.distinct(),
) = Catalog(items = items, categories = categories)
