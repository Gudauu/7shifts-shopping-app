package com.sevenshifts.shopping.domain

/**
 * Keeps the items belonging to any of the selected categories; the filter is a union
 * because an item has exactly one category. An empty selection means no filter. When any
 * filter is active, an item with no resolved category belongs to no selected category and
 * is excluded. Relative order is preserved, so filtering composes with sorting in either
 * order.
 */
fun List<FoodItem>.filteredByCategories(selectedCategoryIds: Set<String>): List<FoodItem> =
    if (selectedCategoryIds.isEmpty()) this else filter { it.category?.id in selectedCategoryIds }
