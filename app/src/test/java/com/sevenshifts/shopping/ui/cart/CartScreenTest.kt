package com.sevenshifts.shopping.ui.cart

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.testing.foodItem
import java.math.BigDecimal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// The tall window keeps every row and the total on screen, so rendered order is
// assertable as top-to-bottom position and no amount sits below the fold.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w320dp-h2000dp")
class CartScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bananas = foodItem(id = "bananas", name = "Bananas", price = BigDecimal("1.49"))
    private val milk = foodItem(id = "milk", name = "Milk", price = BigDecimal("4.90"))

    @Test
    fun `an empty cart shows the empty state instead of a blank screen`() {
        setContent(Cart())

        composeRule.onNodeWithText("Your cart is empty").assertIsDisplayed()
        composeRule.onNodeWithText("Total").assertDoesNotExist()
    }

    @Test
    fun `a duplicated item renders one row with its quantity and line total`() {
        val cart = Cart()
        repeat(2) { cart.add(bananas) }

        setContent(cart)

        composeRule.onAllNodesWithText("Bananas").assertCountEquals(1)
        composeRule.onNodeWithText("2 × $1.49").assertIsDisplayed()
        composeRule.onAllNodesWithText("$2.98").assertCountEquals(2)
    }

    @Test
    fun `the order total sums the line totals over a mixed cart`() {
        val cart = Cart()
        repeat(2) { cart.add(bananas) }
        cart.add(milk)

        setContent(cart)

        composeRule.onNodeWithText("$2.98").assertIsDisplayed()
        composeRule.onNodeWithText("1 × $4.90").assertIsDisplayed()
        composeRule.onNodeWithText("$4.90").assertIsDisplayed()
        composeRule.onNodeWithText("Total").assertIsDisplayed()
        composeRule.onNodeWithText("$7.88").assertIsDisplayed()
    }

    @Test
    fun `rows keep first-added order below the top bar`() {
        val cart = Cart()
        cart.add(milk)
        cart.add(bananas)
        cart.add(milk)

        setContent(cart)

        assertRenderedOrder("Milk", "Bananas", "Total")
    }

    // The fixture price drops the trailing zero, so this fails if the amount is ever
    // rendered with toString instead of the two-decimal formatter.
    @Test
    fun `amounts always display two decimals`() {
        val cart = Cart()
        cart.add(foodItem(id = "soda", name = "Soda", price = BigDecimal("2.5")))

        setContent(cart)

        composeRule.onNodeWithText("1 × $2.50").assertIsDisplayed()
        composeRule.onAllNodesWithText("$2.50").assertCountEquals(2)
    }

    private fun setContent(cart: Cart) {
        val viewModel = CartViewModel(cart)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CartScreen(state = state, onBack = {})
        }
    }

    private fun assertRenderedOrder(vararg texts: String) {
        val tops = texts.map { composeRule.onNodeWithText(it).getBoundsInRoot().top }
        assertTrue(
            "Expected top-to-bottom order ${texts.toList()}, but their tops were $tops",
            tops.zipWithNext().all { (above, below) -> above < below },
        )
    }
}
