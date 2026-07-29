package com.sevenshifts.shopping.testing

import com.sevenshifts.shopping.domain.FoodItem
import java.math.BigDecimal

fun foodItem(
    id: String = "uuid-1",
    name: String = "Bananas",
    price: BigDecimal = BigDecimal("1.49"),
    categoryName: String? = "Produce",
    imageUrl: String = "https://example.test/bananas.png",
) = FoodItem(
    id = id,
    name = name,
    price = price,
    categoryName = categoryName,
    imageUrl = imageUrl,
)
