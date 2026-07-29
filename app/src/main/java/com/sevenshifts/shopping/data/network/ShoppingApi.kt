package com.sevenshifts.shopping.data.network

import retrofit2.http.GET

interface ShoppingApi {
    @GET("api/food_items.json")
    suspend fun getFoodItems(): List<FoodItemDto>

    @GET("api/food_item_categories.json")
    suspend fun getFoodItemCategories(): List<FoodItemCategoryDto>
}
