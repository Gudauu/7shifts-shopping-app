package com.sevenshifts.shopping.data.network

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodItemDtoTest {
    // The production configuration, so these tests cannot drift from what the app parses with.
    private val json = shoppingJson

    @Test
    fun `a JSON float price parses to the exact BigDecimal`() {
        val dto = json.decodeFromString<FoodItemDto>(
            """
            {
              "uuid": "a1f7b3e5-4c1d-42e9-8f2a-8cbb8b1f6f01",
              "name": "Bananas",
              "price": 1.49,
              "category_uuid": "b1f6d8a5-0e29-4d70-8d4f-1f8c1d7a5b12",
              "image_url": "https://example.test/bananas.png"
            }
            """.trimIndent(),
        )

        assertEquals(BigDecimal("1.49"), dto.price)
    }

    @Test
    fun `a whole number price parses without invented decimals`() {
        val dto = json.decodeFromString<FoodItemDto>(
            """{"uuid": "u", "name": "Flour", "price": 3, "category_uuid": "c", "image_url": "https://example.test/flour.png"}""",
        )

        assertEquals(BigDecimal("3"), dto.price)
    }

    @Test
    fun `an item without an image url key parses instead of failing the array`() {
        val dto = json.decodeFromString<FoodItemDto>(
            """{"uuid": "u", "name": "Oats", "price": 2.99, "category_uuid": "c"}""",
        )

        assertNull(dto.imageUrl)
    }

    @Test
    fun `an item without a category uuid key parses instead of failing the array`() {
        val dto = json.decodeFromString<FoodItemDto>(
            """{"uuid": "u", "name": "Oats", "price": 2.99, "image_url": "https://example.test/oats.png"}""",
        )

        assertNull(dto.categoryUuid)
    }
}
