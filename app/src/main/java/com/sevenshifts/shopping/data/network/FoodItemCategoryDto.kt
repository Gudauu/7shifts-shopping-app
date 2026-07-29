package com.sevenshifts.shopping.data.network

import kotlinx.serialization.Serializable

@Serializable
data class FoodItemCategoryDto(val uuid: String, val name: String)
