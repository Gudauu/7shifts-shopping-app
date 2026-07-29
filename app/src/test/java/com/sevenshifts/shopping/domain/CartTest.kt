package com.sevenshifts.shopping.domain

import com.sevenshifts.shopping.testing.foodItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CartTest {
    private val bananas = foodItem(id = "bananas", name = "Bananas")
    private val milk = foodItem(id = "milk", name = "Milk")

    @Test
    fun `the cart is empty on construction`() {
        val cart = Cart()

        assertEquals(emptyList<CartLine>(), cart.lines.value)
        assertEquals(0, cart.lines.value.totalQuantity)
    }

    @Test
    fun `adding distinct items keeps one line per item in first-added order`() {
        val cart = Cart()

        cart.add(bananas)
        cart.add(milk)

        assertEquals(
            listOf(CartLine(bananas, quantity = 1), CartLine(milk, quantity = 1)),
            cart.lines.value,
        )
    }

    @Test
    fun `adding the same item again raises its quantity instead of adding a line`() {
        val cart = Cart()

        cart.add(bananas)
        cart.add(milk)
        cart.add(bananas)

        assertEquals(
            listOf(CartLine(bananas, quantity = 2), CartLine(milk, quantity = 1)),
            cart.lines.value,
        )
    }

    @Test
    fun `the total quantity counts every add rather than distinct items`() {
        val cart = Cart()

        repeat(3) { cart.add(bananas) }
        cart.add(milk)

        assertEquals(4, cart.lines.value.totalQuantity)
    }
}
