package com.sevenshifts.shopping.ui.catalog

import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// The tall window keeps every card on screen: the grid renders one 320dp-wide column, so
// rendered order is assertable as top-to-bottom position and prices are never below the fold.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w320dp-h2000dp")
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
                onSortSelected = {},
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
                onSortSelected = {},
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
                onSortSelected = {},
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
                onSortSelected = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("Mystery snack").assertIsDisplayed()
    }

    @Test
    fun `loading shows a spinner`() {
        composeRule.setContent {
            CatalogScreen(state = CatalogUiState.Loading, onRetry = {}, onSortSelected = {}, onViewCart = {})
        }

        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun `an empty catalog shows the empty state`() {
        composeRule.setContent {
            CatalogScreen(
                state = CatalogUiState.Content(emptyList()),
                onRetry = {},
                onSortSelected = {},
                onViewCart = {},
            )
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
            CatalogScreen(
                state = state,
                onRetry = viewModel::retry,
                onSortSelected = viewModel::onSortSelected,
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()

        composeRule.onNodeWithText("Retry").performClick()

        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
    }

    @Test
    fun `toggling the sort control reorders the rendered list`() {
        val viewModel = sortableCatalogViewModel()
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CatalogScreen(
                state = state,
                onRetry = viewModel::retry,
                onSortSelected = viewModel::onSortSelected,
                onViewCart = {},
            )
        }

        assertRenderedOrder("Steak", "Milk", "Bananas")

        composeRule.onNodeWithText("Price: low to high").performClick()

        assertRenderedOrder("Bananas", "Milk", "Steak")

        composeRule.onNodeWithText("Price: high to low").performClick()

        assertRenderedOrder("Steak", "Milk", "Bananas")
    }

    @Test
    fun `the active sort is marked selected and deselecting it restores the API order`() {
        val viewModel = sortableCatalogViewModel()
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CatalogScreen(
                state = state,
                onRetry = viewModel::retry,
                onSortSelected = viewModel::onSortSelected,
                onViewCart = {},
            )
        }
        composeRule.onNodeWithText("Price: low to high").assertIsNotSelected()

        composeRule.onNodeWithText("Price: low to high").performClick()

        composeRule.onNodeWithText("Price: low to high").assertIsSelected()
        assertRenderedOrder("Bananas", "Milk", "Steak")

        composeRule.onNodeWithText("Price: low to high").performClick()

        composeRule.onNodeWithText("Price: low to high").assertIsNotSelected()
        assertRenderedOrder("Steak", "Milk", "Bananas")
    }

    // The short window fits roughly one card, so this test can observe where the
    // viewport lands. Without an explicit scroll the keyed grid follows the first
    // visible card to its new position at the bottom of the ascending order.
    @Test
    @Config(qualifiers = "+h800dp")
    fun `applying a sort scrolls the list back to the top`() {
        val viewModel = sortableCatalogViewModel()
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CatalogScreen(
                state = state,
                onRetry = viewModel::retry,
                onSortSelected = viewModel::onSortSelected,
                onViewCart = {},
            )
        }
        composeRule.onNodeWithText("Steak").assertIsDisplayed()

        composeRule.onNodeWithText("Price: low to high").performClick()

        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
    }

    /** The API order is deliberately not the ascending price order. */
    private fun sortableCatalogViewModel() = CatalogViewModel(
        FakeCatalogRepository(
            listOf(
                Result.success(
                    listOf(
                        foodItem(id = "steak", name = "Steak", price = BigDecimal("12.99")),
                        foodItem(id = "milk", name = "Milk", price = BigDecimal("4.90")),
                        foodItem(id = "bananas", name = "Bananas", price = BigDecimal("1.49")),
                    ),
                ),
            ),
        ),
    )

    private fun assertRenderedOrder(vararg names: String) {
        val tops = names.map { composeRule.onNodeWithText(it).getBoundsInRoot().top }
        assertTrue(
            "Expected top-to-bottom order ${names.toList()}, but their tops were $tops",
            tops.zipWithNext().all { (above, below) -> above < below },
        )
    }
}
