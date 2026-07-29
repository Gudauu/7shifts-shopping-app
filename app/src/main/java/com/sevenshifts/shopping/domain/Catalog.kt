package com.sevenshifts.shopping.domain

/**
 * The full catalog: every food item plus every known category, both in the API's order.
 * Categories are exposed on their own because the filter offers all of them, including any
 * that no current item references.
 */
data class Catalog(val items: List<FoodItem>, val categories: List<FoodCategory>)
