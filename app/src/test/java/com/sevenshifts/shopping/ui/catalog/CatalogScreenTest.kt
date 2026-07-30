package com.sevenshifts.shopping.ui.catalog

import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.domain.FoodItem
import com.sevenshifts.shopping.testing.FakeCatalogRepository
import com.sevenshifts.shopping.testing.catalog
import com.sevenshifts.shopping.testing.foodCategory
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
                state = catalogContent(
                    foodItem(
                        name = "Bananas",
                        price = BigDecimal("1.49"),
                        category = foodCategory(name = "Produce"),
                    ),
                ),
                onRetry = {},
                onSortSelected = {},
                onCategoryToggled = {},
                onAddToCart = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("Shopping").assertIsDisplayed()
        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
        composeRule.onNodeWithText("$1.49").assertIsDisplayed()
        composeRule.onNodeWithText("Produce").assertIsDisplayed()
    }

    @Test
    fun `prices always display two decimals`() {
        composeRule.setContent {
            CatalogScreen(
                state = catalogContent(foodItem(name = "Milk", price = BigDecimal("4.9"))),
                onRetry = {},
                onSortSelected = {},
                onCategoryToggled = {},
                onAddToCart = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("$4.90").assertIsDisplayed()
    }

    @Test
    fun `an item without an image still renders`() {
        composeRule.setContent {
            CatalogScreen(
                state = catalogContent(foodItem(name = "Plain oats", imageUrl = null)),
                onRetry = {},
                onSortSelected = {},
                onCategoryToggled = {},
                onAddToCart = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("Plain oats").assertIsDisplayed()
    }

    @Test
    fun `an item without a category still renders`() {
        composeRule.setContent {
            CatalogScreen(
                state = catalogContent(foodItem(name = "Mystery snack", category = null)),
                onRetry = {},
                onSortSelected = {},
                onCategoryToggled = {},
                onAddToCart = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("Mystery snack").assertIsDisplayed()
    }

    @Test
    fun `loading shows a spinner`() {
        composeRule.setContent {
            CatalogScreen(
                state = CatalogUiState(),
                onRetry = {},
                onSortSelected = {},
                onCategoryToggled = {},
                onAddToCart = {},
                onViewCart = {},
            )
        }

        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun `an empty catalog shows the empty state`() {
        composeRule.setContent {
            CatalogScreen(
                state = catalogContent(),
                onRetry = {},
                onSortSelected = {},
                onCategoryToggled = {},
                onAddToCart = {},
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
                    Result.success(catalog(listOf(foodItem(name = "Bananas")))),
                ),
            ),
            Cart(),
        )
        setContent(viewModel)

        composeRule.onNodeWithText("Retry").assertIsDisplayed()

        composeRule.onNodeWithText("Retry").performClick()

        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
    }

    @Test
    fun `toggling the sort control reorders the rendered list`() {
        setContent(filterableCatalogViewModel())

        assertRenderedOrder("Steak", "Milk", "Bananas")

        composeRule.onNodeWithText("Price: low to high").performClick()

        assertRenderedOrder("Bananas", "Milk", "Steak")

        composeRule.onNodeWithText("Price: high to low").performClick()

        assertRenderedOrder("Steak", "Milk", "Bananas")
    }

    @Test
    fun `the active sort is marked selected and deselecting it restores the API order`() {
        setContent(filterableCatalogViewModel())
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
        setContent(filterableCatalogViewModel())
        composeRule.onNodeWithText("Steak").assertIsDisplayed()

        composeRule.onNodeWithText("Price: low to high").performClick()

        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
    }

    @Test
    fun `selecting a category chip narrows the list and deselecting it restores the full list`() {
        setContent(filterableCatalogViewModel())
        categoryChip("Produce").assertIsNotSelected()

        categoryChip("Produce").performClick()

        categoryChip("Produce").assertIsSelected()
        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
        composeRule.onNodeWithText("Steak").assertDoesNotExist()
        composeRule.onNodeWithText("Milk").assertDoesNotExist()

        categoryChip("Produce").performClick()

        categoryChip("Produce").assertIsNotSelected()
        assertRenderedOrder("Steak", "Milk", "Bananas")
    }

    @Test
    fun `selecting two category chips shows the union of both`() {
        setContent(filterableCatalogViewModel())

        categoryChip("Produce").performClick()
        categoryChip("Dairy").performClick()

        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
        composeRule.onNodeWithText("Milk").assertIsDisplayed()
        composeRule.onNodeWithText("Steak").assertDoesNotExist()
    }

    @Test
    fun `a selection matching no items shows the empty state and keeps the chips available`() {
        setContent(filterableCatalogViewModel())

        categoryChip("Frozen").performClick()

        composeRule.onNodeWithText("No items in the selected categories").assertIsDisplayed()
        categoryChip("Frozen").assertIsSelected()
    }

    @Test
    fun `a sort and a filter stay applied together`() {
        setContent(filterableCatalogViewModel())

        composeRule.onNodeWithText("Price: low to high").performClick()
        categoryChip("Produce").performClick()
        categoryChip("Meat").performClick()

        assertRenderedOrder("Bananas", "Steak")
        composeRule.onNodeWithText("Milk").assertDoesNotExist()
        composeRule.onNodeWithText("Price: low to high").assertIsSelected()
    }

    @Test
    fun `category chips render in the categories endpoint order`() {
        setContent(filterableCatalogViewModel())

        // Deliberately neither the items' order (Meat, Dairy, Produce) nor alphabetical,
        // so a rendering that re-sorts or reverses the endpoint's order fails.
        assertChipOrder("Produce", "Meat", "Dairy", "Frozen")
    }

    @Test
    fun `overflowing categories provide working explicit scroll controls`() {
        val categories = List(8) { index ->
            foodCategory(id = "category-$index", name = "Category ${index + 1}")
        }
        composeRule.setContent {
            CatalogScreen(
                state = CatalogUiState(
                    catalog = CatalogState.Content(
                        items = listOf(foodItem()),
                        categories = categories,
                    ),
                ),
                onRetry = {},
                onSortSelected = {},
                onCategoryToggled = {},
                onAddToCart = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithContentDescription("Show previous categories").assertIsNotEnabled()
        composeRule
            .onNodeWithContentDescription("Show more categories")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeRule.onNodeWithContentDescription("Show previous categories").assertIsEnabled()
    }

    @Test
    @Config(qualifiers = "w800dp-h320dp-land")
    fun `landscape starts with sort and categories collapsed and can expand them`() {
        setContent(filterableCatalogViewModel())

        composeRule.onNodeWithText("Sort & categories").assertIsDisplayed()
        composeRule.onNodeWithText("Default order, all categories").assertIsDisplayed()
        composeRule.onNodeWithText("Price: low to high").assertDoesNotExist()
        categoryChip("Produce").assertDoesNotExist()

        composeRule.onNodeWithText("Sort & categories").performClick()

        composeRule.onNodeWithText("Price: low to high").assertIsDisplayed()
        categoryChip("Produce").assertIsDisplayed()
    }

    // From the top of the list a filter change lands at the top regardless, because the
    // grid falls back to the first index when the anchor card leaves the list. The test
    // therefore scrolls to the bottom first, past every Produce item, so only the explicit
    // scroll-to-top can bring Bananas back into the short viewport.
    @Test
    @Config(qualifiers = "+h800dp")
    fun `changing the filter scrolls the list back to the top`() {
        val produce = foodCategory(id = "cat-produce", name = "Produce")
        val meat = foodCategory(id = "cat-meat", name = "Meat")
        val dairy = foodCategory(id = "cat-dairy", name = "Dairy")
        val viewModel = CatalogViewModel(
            FakeCatalogRepository(
                listOf(
                    Result.success(
                        catalog(
                            items = listOf(
                                foodItem(id = "bananas", name = "Bananas", category = produce),
                                foodItem(id = "apples", name = "Apples", category = produce),
                                foodItem(id = "cherries", name = "Cherries", category = produce),
                                foodItem(id = "steak", name = "Steak", category = meat),
                                foodItem(id = "milk", name = "Milk", category = dairy),
                            ),
                        ),
                    ),
                ),
            ),
            Cart(),
        )
        setContent(viewModel)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Milk"))
        composeRule.onNodeWithText("Bananas").assertDoesNotExist()

        categoryChip("Produce").performClick()

        composeRule.onNodeWithText("Bananas").assertIsDisplayed()
    }

    @Test
    fun `no badge shows while the cart is empty`() {
        setContent(filterableCatalogViewModel())

        composeRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun `tapping add on an item shows its count on the cart badge`() {
        setContent(filterableCatalogViewModel())

        addButton("Bananas").performClick()

        cartBadgeWithCount(1).assertIsDisplayed()
    }

    @Test
    fun `adding the same item three times shows a badge count of 3`() {
        setContent(filterableCatalogViewModel())

        repeat(3) { addButton("Bananas").performClick() }

        cartBadgeWithCount(3).assertIsDisplayed()
    }

    @Test
    fun `the badge counts every added item rather than distinct items`() {
        setContent(filterableCatalogViewModel())

        addButton("Bananas").performClick()
        addButton("Bananas").performClick()
        addButton("Milk").performClick()

        cartBadgeWithCount(3).assertIsDisplayed()
    }

    @Test
    fun `a card shows no quantity until its item is added`() {
        setContent(filterableCatalogViewModel())

        addButton("Milk").performClick()

        composeRule.onNodeWithContentDescription("1 of Milk in the cart").assertIsDisplayed()
        composeRule
            .onNode(hasContentDescription("of Bananas in the cart", substring = true))
            .assertDoesNotExist()
    }

    @Test
    fun `each card shows its own in-cart quantity beside the add control`() {
        setContent(filterableCatalogViewModel())

        repeat(2) { addButton("Bananas").performClick() }
        addButton("Milk").performClick()

        composeRule
            .onNode(hasText("2") and hasContentDescription("2 of Bananas in the cart"))
            .assertIsDisplayed()
        composeRule.onNodeWithText("×2").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("1 of Milk in the cart").assertIsDisplayed()
    }

    @Test
    fun `the badge stays visible while the catalog is in the error state`() {
        composeRule.setContent {
            CatalogScreen(
                state = CatalogUiState(catalog = CatalogState.Error, cartItemCount = 2),
                onRetry = {},
                onSortSelected = {},
                onCategoryToggled = {},
                onAddToCart = {},
                onViewCart = {},
            )
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        cartBadgeWithCount(2).assertIsDisplayed()
    }

    private fun cartBadgeWithCount(count: Int): SemanticsNodeInteraction {
        val itemLabel = if (count == 1) "item" else "items"
        return composeRule.onNode(
            hasText("View cart") and
                hasText(count.toString()) and
                hasStateDescription("$count $itemLabel in cart"),
        )
    }

    private fun addButton(itemName: String): SemanticsNodeInteraction =
        composeRule.onNodeWithContentDescription("Add $itemName to the cart")

    private fun setContent(viewModel: CatalogViewModel) {
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CatalogScreen(
                state = state,
                onRetry = viewModel::retry,
                onSortSelected = viewModel::onSortSelected,
                onCategoryToggled = viewModel::onCategoryToggled,
                onAddToCart = viewModel::onAddToCart,
                onViewCart = {},
            )
        }
    }

    /**
     * The API order is deliberately not the ascending price order, and Frozen deliberately
     * matches no item. Each category name also appears on its items' cards, so chip
     * assertions match on the selectable node rather than by text alone.
     */
    private fun filterableCatalogViewModel(): CatalogViewModel {
        val produce = foodCategory(id = "cat-produce", name = "Produce")
        val dairy = foodCategory(id = "cat-dairy", name = "Dairy")
        val meat = foodCategory(id = "cat-meat", name = "Meat")
        val frozen = foodCategory(id = "cat-frozen", name = "Frozen")
        return CatalogViewModel(
            FakeCatalogRepository(
                listOf(
                    Result.success(
                        catalog(
                            items = listOf(
                                foodItem(id = "steak", name = "Steak", price = BigDecimal("12.99"), category = meat),
                                foodItem(id = "milk", name = "Milk", price = BigDecimal("4.90"), category = dairy),
                                foodItem(
                                    id = "bananas",
                                    name = "Bananas",
                                    price = BigDecimal("1.49"),
                                    category = produce,
                                ),
                            ),
                            categories = listOf(produce, meat, dairy, frozen),
                        ),
                    ),
                ),
            ),
            Cart(),
        )
    }

    private fun catalogContent(vararg items: FoodItem) = CatalogUiState(catalog = CatalogState.Content(items.toList()))

    private fun categoryChip(name: String): SemanticsNodeInteraction =
        composeRule.onNode(hasText(name) and isSelectable())

    private fun assertRenderedOrder(vararg names: String) {
        val tops = names.map { composeRule.onNodeWithText(it).getBoundsInRoot().top }
        assertTrue(
            "Expected top-to-bottom order ${names.toList()}, but their tops were $tops",
            tops.zipWithNext().all { (above, below) -> above < below },
        )
    }

    private fun assertChipOrder(vararg names: String) {
        val lefts = names.map { categoryChip(it).getBoundsInRoot().left }
        assertTrue(
            "Expected left-to-right order ${names.toList()}, but their left edges were $lefts",
            lefts.zipWithNext().all { (left, right) -> left < right },
        )
    }
}
