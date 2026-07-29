package com.sevenshifts.shopping.domain

import com.sevenshifts.shopping.testing.foodItem
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class PriceSortOrderTest {
    private val cheap = foodItem(id = "cheap", price = BigDecimal("1.49"))
    private val mid = foodItem(id = "mid", price = BigDecimal("4.90"))
    private val dear = foodItem(id = "dear", price = BigDecimal("12.99"))

    @Test
    fun `ascending puts the lowest price first`() {
        val sorted = listOf(mid, dear, cheap).sortedByPrice(PriceSortOrder.ASCENDING)

        assertEquals(listOf(cheap, mid, dear), sorted)
    }

    @Test
    fun `descending puts the highest price first`() {
        val sorted = listOf(mid, dear, cheap).sortedByPrice(PriceSortOrder.DESCENDING)

        assertEquals(listOf(dear, mid, cheap), sorted)
    }

    @Test
    fun `equal prices keep their relative order in both directions`() {
        // Different scales, same value: 4.9 and 4.90 must compare as equal prices.
        val first = foodItem(id = "first", price = BigDecimal("4.9"))
        val second = foodItem(id = "second", price = BigDecimal("4.90"))
        val items = listOf(first, second, cheap)

        assertEquals(
            listOf(cheap, first, second),
            items.sortedByPrice(PriceSortOrder.ASCENDING),
        )
        assertEquals(
            listOf(first, second, cheap),
            items.sortedByPrice(PriceSortOrder.DESCENDING),
        )
    }
}
