package com.sevenshifts.shopping.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sevenshifts.shopping.testing.FakeCatalogRepository
import com.sevenshifts.shopping.testing.foodItem
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

    private fun catalogViewModel() = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(listOf(foodItem())))))

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
}
