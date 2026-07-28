package com.sevenshifts.shopping.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ShoppingNavHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the app opens on the food items screen`() {
        composeRule.setContent { ShoppingNavHost() }

        composeRule.onNodeWithText("Food items").assertIsDisplayed()
    }

    @Test
    fun `viewing the cart navigates to the cart screen`() {
        composeRule.setContent { ShoppingNavHost() }

        composeRule.onNodeWithText("View cart").performClick()

        composeRule.onNodeWithText("Your cart").assertIsDisplayed()
    }

    @Test
    fun `going back from the cart returns to the food items screen`() {
        composeRule.setContent { ShoppingNavHost() }
        composeRule.onNodeWithText("View cart").performClick()

        composeRule.onNodeWithText("Back").performClick()

        composeRule.onNodeWithText("Food items").assertIsDisplayed()
    }
}
