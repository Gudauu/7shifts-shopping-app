package com.sevenshifts.shopping.data.network

import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `category_uuid` and `image_url` are optional. The identity fields stay required,
 * because an unidentifiable or unpriced item is not purchasable, but payloads decode
 * element by element, so a missing required field drops that element, never the catalog.
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
