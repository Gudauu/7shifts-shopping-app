package com.sevenshifts.shopping.ui.catalog

import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sevenshifts.shopping.testing.FakeCatalogRepository
import com.sevenshifts.shopping.testing.foodItem
import java.io.IOException
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CatalogScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `items render with name, price, and category name`() {
        composeRule.setContent {
            CatalogScreen(
                state = CatalogUiState.Content(
                    listOf(foodItem(name = "Bananas", price = BigDecimal("1.49"), categoryName = "Produce")),
                ),
                onRetry = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
        composeRule.onNodeWithText("$1.49").assertIsDisplayed()
        composeRule.onNodeWithText("Produce").assertIsDisplayed()
    }

    @Test
    fun `prices always display two decimals`() {
        composeRule.setContent {
            CatalogScreen(
                state = CatalogUiState.Content(listOf(foodItem(name = "Milk", price = BigDecimal("4.9")))),
                onRetry = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("$4.90").assertIsDisplayed()
    }

    @Test
    fun `an item without an image still renders`() {
        composeRule.setContent {
            CatalogScreen(
                state = CatalogUiState.Content(listOf(foodItem(name = "Plain oats", imageUrl = null))),
                onRetry = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("Plain oats").assertIsDisplayed()
    }

    @Test
    fun `an item without a category still renders`() {
        composeRule.setContent {
            CatalogScreen(
                state = CatalogUiState.Content(listOf(foodItem(name = "Mystery snack", categoryName = null))),
                onRetry = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("Mystery snack").assertIsDisplayed()
    }

    @Test
    fun `loading shows a spinner`() {
        composeRule.setContent {
            CatalogScreen(state = CatalogUiState.Loading, onRetry = {}, onViewCart = {})
        }

        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun `an empty catalog shows the empty state`() {
        composeRule.setContent {
            CatalogScreen(state = CatalogUiState.Content(emptyList()), onRetry = {}, onViewCart = {})
        }

        composeRule.onNodeWithText("No food items to show").assertIsDisplayed()
    }

    @Test
    fun `the error state shows a retry that recovers`() {
        val viewModel = CatalogViewModel(
            FakeCatalogRepository(
                listOf(
                    Result.failure(IOException("boom")),
                    Result.success(listOf(foodItem(name = "Bananas"))),
                ),
            ),
        )
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CatalogScreen(state = state, onRetry = viewModel::retry, onViewCart = {})
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()

        composeRule.onNodeWithText("Retry").performClick()

        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
    }
}
