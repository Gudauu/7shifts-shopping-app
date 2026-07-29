package com.sevenshifts.shopping.ui.cart

import app.cash.turbine.test
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.testing.MainDispatcherRule
import com.sevenshifts.shopping.testing.foodItem
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CartViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bananas = foodItem(id = "bananas", name = "Bananas", price = BigDecimal("1.49"))
    private val milk = foodItem(id = "milk", name = "Milk", price = BigDecimal("4.90"))

    @Test
    fun `an empty cart renders as empty lines and a zero total`() = runTest {
        val viewModel = CartViewModel(Cart())

        viewModel.uiState.test {
            assertEquals(CartUiState(), awaitItem())
        }
    }

    @Test
    fun `the state mirrors the cart's lines and total as items are added`() = runTest {
        val cart = Cart()
        val viewModel = CartViewModel(cart)

        viewModel.uiState.test {
            assertEquals(CartUiState(), awaitItem())

            cart.add(bananas)
            assertEquals(
                CartUiState(
                    lines = listOf(CartLine(bananas, quantity = 1)),
                    orderTotal = BigDecimal("1.49"),
                ),
                awaitItem(),
            )

            cart.add(milk)
            cart.add(bananas)
            assertEquals(
                CartUiState(
                    lines = listOf(CartLine(bananas, quantity = 2), CartLine(milk, quantity = 1)),
                    orderTotal = BigDecimal("7.88"),
                ),
                awaitItem(),
            )
        }
    }

    // Arriving on the cart screen constructs the view model after the shopper has been
    // adding items on the catalog; the first emission must already hold them.
    @Test
    fun `a cart filled before construction is visible in the first state`() = runTest {
        val cart = Cart()
        repeat(2) { cart.add(bananas) }

        val viewModel = CartViewModel(cart)

        viewModel.uiState.test {
            assertEquals(
                CartUiState(
                    lines = listOf(CartLine(bananas, quantity = 2)),
                    orderTotal = BigDecimal("2.98"),
                ),
                awaitItem(),
            )
        }
    }
}
