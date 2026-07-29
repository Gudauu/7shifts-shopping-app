package com.sevenshifts.shopping.data.network

import kotlinx.serialization.json.JsonArray
import retrofit2.http.GET

/**
 * Both endpoints return flat JSON arrays. They arrive as [JsonArray] so elements can be
 * decoded individually with [decodeValidElements]: a malformed element costs itself,
 * never the whole catalog.
 */
interface ShoppingApi {
    @GET("api/food_items.json")
    suspend fun getFoodItems(): JsonArray

    @GET("api/food_item_categories.json")
    suspend fun getFoodItemCategories(): JsonArray
}
