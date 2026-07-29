package com.sevenshifts.shopping.domain

import com.sevenshifts.shopping.testing.foodItem
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class CartTest {
    private val bananas = foodItem(id = "bananas", name = "Bananas", price = BigDecimal("1.49"))
    private val milk = foodItem(id = "milk", name = "Milk", price = BigDecimal("4.90"))

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

    // BigDecimal.equals distinguishes 4.47 from 4.470, so these assert the exact
    // unrounded amount, not a rounded rendering of it.
    @Test
    fun `a line total is exactly the unit price times the quantity`() {
        assertEquals(BigDecimal("4.47"), CartLine(bananas, quantity = 3).lineTotal)
    }

    @Test
    fun `the order total is exactly the sum of the line totals over a mixed cart`() {
        val cart = Cart()

        repeat(3) { cart.add(bananas) }
        cart.add(milk)

        assertEquals(BigDecimal("9.37"), cart.lines.value.orderTotal)
    }

    @Test
    fun `an empty cart's order total is zero`() {
        assertEquals(BigDecimal.ZERO, emptyList<CartLine>().orderTotal)
    }

    // 0.10 and 0.20 have no exact binary representation, so a cart summed in doubles
    // shows cent drift here; the exact total proves the money stays decimal.
    @Test
    fun `totals do not drift on amounts that binary floating point cannot represent`() {
        val cart = Cart()

        repeat(3) { cart.add(foodItem(id = "gum", price = BigDecimal("0.10"))) }
        cart.add(foodItem(id = "mints", price = BigDecimal("0.20")))

        assertEquals(BigDecimal("0.50"), cart.lines.value.orderTotal)
    }
}
