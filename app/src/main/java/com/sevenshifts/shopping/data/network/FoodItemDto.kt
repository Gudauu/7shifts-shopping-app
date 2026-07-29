package com.sevenshifts.shopping.data.network

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `category_uuid` and `image_url` are optional: one item missing a required key would
 * fail decoding for the entire array and error-screen the whole catalog. Identity fields
 * stay required, because an unidentifiable or unpriced item is not purchasable.
 */
@Serializable
data class FoodItemDto(
    val uuid: String,
    val name: String,
    @Serializable(with = BigDecimalAsJsonNumberSerializer::class)
    val price: BigDecimal,
    @SerialName("category_uuid")
    val categoryUuid: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
)
