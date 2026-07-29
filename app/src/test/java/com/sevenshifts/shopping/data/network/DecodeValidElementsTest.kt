package com.sevenshifts.shopping.data.network

import java.math.BigDecimal
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodeValidElementsTest {
    private fun jsonArrayOf(raw: String) = shoppingJson.parseToJsonElement(raw).jsonArray

    @Test
    fun `a malformed element is dropped while valid elements survive`() {
        val elements = jsonArrayOf(
            """
            [
              {"uuid": "u1", "name": "Bananas", "price": 1.49},
              {"uuid": "u2"},
              {"uuid": "u3", "name": "Milk", "price": 4.99}
            ]
            """.trimIndent(),
        ).decodeValidElements(FoodItemDto.serializer())

        assertEquals(listOf("u1", "u3"), elements.map { it.uuid })
    }

    @Test
    fun `an element with an unparseable price is dropped`() {
        val elements = jsonArrayOf("""[{"uuid": "u1", "name": "Bananas", "price": "cheap"}]""")
            .decodeValidElements(FoodItemDto.serializer())

        assertTrue(elements.isEmpty())
    }

    @Test
    fun `a fully valid payload keeps every element with exact prices`() {
        val elements = jsonArrayOf("""[{"uuid": "u1", "name": "Bananas", "price": 1.49}]""")
            .decodeValidElements(FoodItemDto.serializer())

        assertEquals(BigDecimal("1.49"), elements.single().price)
    }
}
