package com.sevenshifts.shopping.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.testing.FakeCatalogRepository
import com.sevenshifts.shopping.testing.catalog
import com.sevenshifts.shopping.ui.cart.CartViewModel
import com.sevenshifts.shopping.ui.catalog.CatalogViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ShoppingNavHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    // Both view models share one cart, mirroring the production @Singleton binding.
    private val cart = Cart()

    private fun setContent() {
        val catalogViewModel = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(catalog()))), cart)
        val cartViewModel = CartViewModel(cart)
        composeRule.setContent {
            ShoppingNavHost(catalogViewModel = catalogViewModel, cartViewModel = cartViewModel)
        }
    }

    @Test
    fun `the app opens on the food items screen`() {
        setContent()

        composeRule.onNodeWithText("Food items").assertIsDisplayed()
    }

    @Test
    fun `viewing the cart navigates to the cart screen`() {
        setContent()

        composeRule.onNodeWithText("View cart").performClick()

        composeRule.onNodeWithText("Your cart").assertIsDisplayed()
    }

    @Test
    fun `going back from the cart returns to the food items screen`() {
        setContent()
        composeRule.onNodeWithText("View cart").performClick()

        composeRule.onNodeWithText("Back").performClick()

        composeRule.onNodeWithText("Food items").assertIsDisplayed()
    }

    // The tall window keeps the card's add button inside the touchable viewport; in the
    // default 470dp-high window the tap would land below the fold and never register.
    @Test
    @Config(qualifiers = "w320dp-h2000dp")
    fun `the cart badge survives navigating to the cart and back`() {
        setContent()
        composeRule.onNodeWithContentDescription("Add Bananas to the cart").performClick()

        composeRule.onNodeWithText("View cart").performClick()
        composeRule.onNodeWithText("Back").performClick()

        composeRule
            .onNode(
                hasText("View cart") and
                    hasText("1") and
                    hasStateDescription("1 item in cart"),
            )
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h2000dp")
    fun `decreasing in the cart immediately lowers its total and the catalog badge`() {
        setContent()
        repeat(2) { composeRule.onNodeWithContentDescription("Add Bananas to the cart").performClick() }
        composeRule.onNodeWithText("View cart").performClick()

        composeRule.onNodeWithContentDescription("Decrease Bananas quantity").performClick()

        composeRule.onNodeWithText("$1.49 / $1.49").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove Bananas from the cart").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule
            .onNode(
                hasText("View cart") and
                    hasText("1") and
                    hasStateDescription("1 item in cart"),
            )
            .assertIsDisplayed()
    }

    // End to end across the two screens: adds made on the catalog are what the cart
    // screen lists, because both view models observe the same cart.
    @Test
    @Config(qualifiers = "w320dp-h2000dp")
    fun `items added on the catalog appear on the cart screen with their quantity`() {
        setContent()
        repeat(2) { composeRule.onNodeWithContentDescription("Add Bananas to the cart").performClick() }

        composeRule.onNodeWithText("View cart").performClick()

        composeRule.onNodeWithText("$1.49 / $2.98").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("Total").assertIsDisplayed()
    }
}
