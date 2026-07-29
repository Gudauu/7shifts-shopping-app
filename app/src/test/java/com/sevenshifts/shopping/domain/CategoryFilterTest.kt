package com.sevenshifts.shopping.domain

import com.sevenshifts.shopping.testing.foodCategory
import com.sevenshifts.shopping.testing.foodItem
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryFilterTest {
    private val produce = foodCategory(id = "cat-produce", name = "Produce")
    private val dairy = foodCategory(id = "cat-dairy", name = "Dairy")
    private val meat = foodCategory(id = "cat-meat", name = "Meat")

    private val bananas = foodItem(id = "bananas", name = "Bananas", price = BigDecimal("1.49"), category = produce)
    private val milk = foodItem(id = "milk", name = "Milk", price = BigDecimal("4.90"), category = dairy)
    private val steak = foodItem(id = "steak", name = "Steak", price = BigDecimal("12.99"), category = meat)
    private val mystery = foodItem(id = "mystery", name = "Mystery snack", price = BigDecimal("0.99"), category = null)

    private val all = listOf(steak, milk, bananas, mystery)

    @Test
    fun `an empty selection means no filter`() {
        assertEquals(all, all.filteredByCategories(emptySet()))
    }

    @Test
    fun `selecting one category keeps only its items`() {
        assertEquals(listOf(milk), all.filteredByCategories(setOf(dairy.id)))
    }

    @Test
    fun `selecting many categories keeps their union in the original order`() {
        assertEquals(listOf(milk, bananas), all.filteredByCategories(setOf(produce.id, dairy.id)))
    }

    @Test
    fun `a selection matching no items yields an empty list`() {
        val frozen = foodCategory(id = "cat-frozen", name = "Frozen")

        assertTrue(all.filteredByCategories(setOf(frozen.id)).isEmpty())
    }

    @Test
    fun `an item without a resolved category is excluded whenever any filter is active`() {
        assertEquals(listOf(steak), all.filteredByCategories(setOf(meat.id)))
    }

    @Test
    fun `filter and sort applied in either order produce the same list`() {
        val selection = setOf(produce.id, dairy.id, meat.id)

        val filteredThenSorted = all.filteredByCategories(selection).sortedByPrice(PriceSortOrder.ASCENDING)
        val sortedThenFiltered = all.sortedByPrice(PriceSortOrder.ASCENDING).filteredByCategories(selection)

        assertEquals(listOf(bananas, milk, steak), filteredThenSorted)
        assertEquals(filteredThenSorted, sortedThenFiltered)
    }
}
