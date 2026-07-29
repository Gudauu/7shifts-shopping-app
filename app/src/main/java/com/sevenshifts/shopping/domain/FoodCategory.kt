package com.sevenshifts.shopping.domain

/** A food item category. [id] is the API's `uuid` and is the identity the filter matches on. */
data class FoodCategory(val id: String, val name: String)
