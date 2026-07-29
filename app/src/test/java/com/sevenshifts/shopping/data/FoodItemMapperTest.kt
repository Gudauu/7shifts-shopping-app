package com.sevenshifts.shopping.data

import com.sevenshifts.shopping.data.network.FoodItemCategoryDto
import com.sevenshifts.shopping.data.network.FoodItemDto
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodItemMapperTest {
    private val produce = FoodItemCategoryDto(uuid = "cat-produce", name = "Produce")
    private val dairy = FoodItemCategoryDto(uuid = "cat-dairy", name = "Dairy")

    private fun itemDto(
        uuid: String = "item-1",
        name: String = "Bananas",
        price: BigDecimal = BigDecimal("1.49"),
        categoryUuid: String? = "cat-produce",
        imageUrl: String? = "https://example.test/bananas.png",
    ) = FoodItemDto(
        uuid = uuid,
        name = name,
        price = price,
        categoryUuid = categoryUuid,
        imageUrl = imageUrl,
    )

    @Test
    fun `items join to their category names by uuid`() {
        val items = joinCatalog(
            items = listOf(
                itemDto(uuid = "item-1", categoryUuid = "cat-produce"),
                itemDto(uuid = "item-2", name = "Milk", categoryUuid = "cat-dairy"),
            ),
            categories = listOf(produce, dairy),
        )

        assertEquals(listOf("Produce", "Dairy"), items.map { it.categoryName })
    }

    @Test
    fun `an item with an unknown category uuid is kept without a category name`() {
        val items = joinCatalog(
            items = listOf(itemDto(categoryUuid = "cat-missing")),
            categories = listOf(produce),
        )

        assertEquals(1, items.size)
        assertNull(items.single().categoryName)
    }

    @Test
    fun `an item with no category uuid at all is kept without a category name`() {
        val items = joinCatalog(
            items = listOf(itemDto(categoryUuid = null)),
            categories = listOf(produce),
        )

        assertEquals(1, items.size)
        assertNull(items.single().categoryName)
    }

    @Test
    fun `an item with a blank uuid is dropped`() {
        val items = joinCatalog(
            items = listOf(itemDto(uuid = " ")),
            categories = listOf(produce),
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `an item with a blank name is dropped`() {
        val items = joinCatalog(
            items = listOf(itemDto(uuid = "item-1", name = " "), itemDto(uuid = "item-2", name = "Milk")),
            categories = listOf(produce),
        )

        assertEquals(listOf("item-2"), items.map { it.id })
    }

    @Test
    fun `an item with a negative price is dropped`() {
        val items = joinCatalog(
            items = listOf(itemDto(price = BigDecimal("-0.01"))),
            categories = listOf(produce),
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `a duplicated item uuid resolves to the later element`() {
        val items = joinCatalog(
            items = listOf(
                itemDto(uuid = "item-1", name = "Old bananas"),
                itemDto(uuid = "item-1", name = "New bananas"),
            ),
            categories = listOf(produce),
        )

        assertEquals(listOf("New bananas"), items.map { it.name })
    }

    @Test
    fun `a duplicated category uuid resolves to the later name`() {
        val items = joinCatalog(
            items = listOf(itemDto(categoryUuid = "cat-produce")),
            categories = listOf(
                FoodItemCategoryDto(uuid = "cat-produce", name = "Old produce"),
                FoodItemCategoryDto(uuid = "cat-produce", name = "New produce"),
            ),
        )

        assertEquals("New produce", items.single().categoryName)
    }

    @Test
    fun `a category with a blank name is ignored and its items render unlabelled`() {
        val items = joinCatalog(
            items = listOf(itemDto(categoryUuid = "cat-produce")),
            categories = listOf(FoodItemCategoryDto(uuid = "cat-produce", name = "")),
        )

        assertNull(items.single().categoryName)
    }

    @Test
    fun `mapping preserves the exact price and the API order`() {
        val items = joinCatalog(
            items = listOf(
                itemDto(uuid = "item-2", name = "Milk", price = BigDecimal("4.99")),
                itemDto(uuid = "item-1", name = "Bananas", price = BigDecimal("1.49")),
            ),
            categories = listOf(produce),
        )

        assertEquals(listOf("item-2", "item-1"), items.map { it.id })
        assertEquals(BigDecimal("4.99"), items.first().price)
    }
}
