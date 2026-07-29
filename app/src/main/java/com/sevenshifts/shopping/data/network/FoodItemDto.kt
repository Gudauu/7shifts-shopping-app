package com.sevenshifts.shopping.data.network

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FoodItemDto(
    val uuid: String,
    val name: String,
    @Serializable(with = BigDecimalAsJsonNumberSerializer::class)
    val price: BigDecimal,
    @SerialName("category_uuid")
    val categoryUuid: String,
    @SerialName("image_url")
    val imageUrl: String,
)
