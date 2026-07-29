package com.sevenshifts.shopping.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.testing.FakeCatalogRepository
import com.sevenshifts.shopping.testing.catalog
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

    private fun catalogViewModel() = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(catalog()))), Cart())

    @Test
    fun `the app opens on the food items screen`() {
        val viewModel = catalogViewModel()
        composeRule.setContent { ShoppingNavHost(catalogViewModel = viewModel) }

        composeRule.onNodeWithText("Food items").assertIsDisplayed()
    }

    @Test
    fun `viewing the cart navigates to the cart screen`() {
        val viewModel = catalogViewModel()
        composeRule.setContent { ShoppingNavHost(catalogViewModel = viewModel) }

        composeRule.onNodeWithText("View cart").performClick()

        composeRule.onNodeWithText("Your cart").assertIsDisplayed()
    }

    @Test
    fun `going back from the cart returns to the food items screen`() {
        val viewModel = catalogViewModel()
        composeRule.setContent { ShoppingNavHost(catalogViewModel = viewModel) }
        composeRule.onNodeWithText("View cart").performClick()

        composeRule.onNodeWithText("Back").performClick()

        composeRule.onNodeWithText("Food items").assertIsDisplayed()
    }

    // The tall window keeps the card's add button inside the touchable viewport; in the
    // default 470dp-high window the tap would land below the fold and never register.
    @Test
    @Config(qualifiers = "w320dp-h2000dp")
    fun `the cart badge survives navigating to the cart and back`() {
        val viewModel = catalogViewModel()
        composeRule.setContent { ShoppingNavHost(catalogViewModel = viewModel) }
        composeRule.onNodeWithContentDescription("Add Bananas to the cart").performClick()

        composeRule.onNodeWithText("View cart").performClick()
        composeRule.onNodeWithText("Back").performClick()

        composeRule.onNodeWithText("1").assertIsDisplayed()
    }
}
