package com.sevenshifts.shopping.ui.cart

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.domain.PurchaseFailure
import com.sevenshifts.shopping.domain.PurchaseItemFailure
import com.sevenshifts.shopping.domain.PurchaseItemFailureReason
import com.sevenshifts.shopping.domain.PurchaseResult
import com.sevenshifts.shopping.testing.FakePurchaseRepository
import com.sevenshifts.shopping.testing.foodItem
import java.math.BigDecimal
import org.junit.Assert.assertEquals
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
        composeRule.onNodeWithText("Purchase").assertIsNotEnabled()
    }

    @Test
    fun `a duplicated item renders one row with its quantity and line total`() {
        val cart = Cart()
        repeat(2) { cart.add(bananas) }

        setContent(cart)

        composeRule.onAllNodesWithText("Bananas").assertCountEquals(1)
        composeRule.onNodeWithText("$1.49 / $2.98").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("×", substring = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Decrease Bananas quantity").assertIsDisplayed()
        composeRule.onAllNodesWithText("$2.98").assertCountEquals(1)
    }

    @Test
    fun `the order total sums the line totals over a mixed cart`() {
        val cart = Cart()
        repeat(2) { cart.add(bananas) }
        cart.add(milk)

        setContent(cart)

        composeRule.onNodeWithText("$1.49 / $2.98").assertIsDisplayed()
        composeRule.onNodeWithText("$4.90 / $4.90").assertIsDisplayed()
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

    @Test
    fun `a row without an image still renders its name and totals`() {
        val cart = Cart()
        cart.add(foodItem(id = "oats", name = "Plain oats", price = BigDecimal("3.25"), imageUrl = null))

        setContent(cart)

        composeRule.onNodeWithText("Plain oats").assertIsDisplayed()
        composeRule.onNodeWithText("$3.25 / $3.25").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove Plain oats from the cart").assertIsDisplayed()
    }

    @Test
    fun `tapping decrease updates quantities and totals then removes the final unit`() {
        val cart = Cart()
        repeat(2) { cart.add(bananas) }
        setContent(cart)

        composeRule.onNodeWithContentDescription("Decrease Bananas quantity").performClick()

        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("$1.49 / $1.49").assertIsDisplayed()
        composeRule.onAllNodesWithText("$1.49").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Decrease Bananas quantity").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Remove Bananas from the cart").performClick()

        composeRule.onNodeWithText("Your cart is empty").assertIsDisplayed()
        composeRule.onNodeWithText("Bananas").assertDoesNotExist()
        composeRule.onNodeWithText("Total").assertDoesNotExist()
    }

    // The fixture price drops the trailing zero, so this fails if the amount is ever
    // rendered with toString instead of the two-decimal formatter.
    @Test
    fun `amounts always display two decimals`() {
        val cart = Cart()
        cart.add(foodItem(id = "soda", name = "Soda", price = BigDecimal("2.5")))

        setContent(cart)

        composeRule.onNodeWithText("$2.50 / $2.50").assertIsDisplayed()
        composeRule.onAllNodesWithText("$2.50").assertCountEquals(1)
    }

    @Test
    fun `purchase progress disables submission and cart changes but allows leaving`() {
        val lines = Cart().apply { add(bananas) }.lines.value
        var backCalls = 0
        composeRule.setContent {
            CartScreen(
                state = CartUiState(
                    lines = lines,
                    orderTotal = BigDecimal("1.49"),
                    purchase = PurchaseUiState.InFlight,
                ),
                onBack = { backCalls++ },
                onDecrease = {},
                onPurchase = {},
            )
        }

        composeRule.onNodeWithText("Purchasing...").assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Remove Bananas from the cart").assertIsNotEnabled()
        composeRule.onNodeWithText("Back").assertIsEnabled().performClick()
        assertEquals(1, backCalls)
    }

    @Test
    fun `purchase failure keeps the cart visible and offers retry`() {
        val lines = Cart().apply { add(bananas) }.lines.value
        var purchaseCalls = 0
        composeRule.setContent {
            CartScreen(
                state = CartUiState(
                    lines = lines,
                    orderTotal = BigDecimal("1.49"),
                    purchase = PurchaseUiState.Failed(PurchaseFailure.TemporarilyUnavailable),
                ),
                onBack = {},
                onDecrease = {},
                onPurchase = { purchaseCalls++ },
            )
        }

        composeRule.onNodeWithText("Purchase failed").assertIsDisplayed()
        composeRule
            .onNodeWithText("Purchases are temporarily unavailable. Your cart is unchanged.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
        composeRule.onNodeWithText("$1.49 / $1.49").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed().assertIsEnabled().performClick()
        assertEquals(1, purchaseCalls)
    }

    @Test
    fun `item failure shows actionable detail without an unsafe retry`() {
        val lines = Cart().apply { add(bananas) }.lines.value
        composeRule.setContent {
            CartScreen(
                state = CartUiState(
                    lines = lines,
                    orderTotal = BigDecimal("1.49"),
                    purchase = PurchaseUiState.Failed(
                        PurchaseFailure.ItemsRequireAttention(
                            items = listOf(
                                PurchaseItemFailure(
                                    foodItemId = bananas.id,
                                    reason = PurchaseItemFailureReason.PRICE_CHANGED,
                                    expectedUnitPrice = BigDecimal("1.49"),
                                    currentUnitPrice = BigDecimal("1.59"),
                                ),
                            ),
                        ),
                    ),
                ),
                onBack = {},
                onDecrease = {},
                onPurchase = {},
            )
        }

        composeRule
            .onNodeWithText("Some items need attention before another purchase. Your cart is unchanged.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Bananas changed from $1.49 to $1.59.").assertIsDisplayed()
        composeRule.onNodeWithText("Update cart to continue").assertIsNotEnabled()
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun `purchase success shows confirmation and a way back to shopping`() {
        var continueCalls = 0
        composeRule.setContent {
            CartScreen(
                state = CartUiState(
                    purchase = PurchaseUiState.Succeeded(
                        purchaseId = "purchase-1",
                        total = BigDecimal("2.98"),
                    ),
                ),
                onBack = { continueCalls++ },
                onDecrease = {},
                onPurchase = {},
            )
        }

        composeRule.onNodeWithText("Purchase complete").assertIsDisplayed()
        composeRule.onNodeWithText("Your $2.98 purchase is confirmed.").assertIsDisplayed()
        composeRule.onNodeWithText("Continue shopping").performClick()
        assertEquals(1, continueCalls)
    }

    private fun setContent(cart: Cart) {
        val viewModel = CartViewModel(
            cart,
            FakePurchaseRepository(
                listOf(PurchaseResult.Failed(PurchaseFailure.TemporarilyUnavailable)),
            ),
        )
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CartScreen(
                state = state,
                onBack = {},
                onDecrease = viewModel::onDecrease,
                onPurchase = viewModel::onPurchase,
            )
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
