package com.sevenshifts.shopping.ui.catalog

import app.cash.turbine.test
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.domain.Catalog
import com.sevenshifts.shopping.domain.FoodItem
import com.sevenshifts.shopping.domain.PriceSortOrder
import com.sevenshifts.shopping.testing.FakeCatalogRepository
import com.sevenshifts.shopping.testing.MainDispatcherRule
import com.sevenshifts.shopping.testing.catalog
import com.sevenshifts.shopping.testing.foodCategory
import com.sevenshifts.shopping.testing.foodItem
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CatalogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(foodItem())
    private val loaded = catalog(items)

    private fun contentOf(
        catalog: Catalog,
        items: List<FoodItem>,
        sort: PriceSortOrder? = null,
        selectedCategoryIds: Set<String> = emptySet(),
        cartItemCount: Int = 0,
        cartQuantities: Map<String, Int> = emptyMap(),
    ) = CatalogUiState(
        catalog = CatalogState.Content(
            items = items,
            sort = sort,
            categories = catalog.categories,
            selectedCategoryIds = selectedCategoryIds,
        ),
        cartItemCount = cartItemCount,
        cartQuantities = cartQuantities,
    )

    @Test
    fun `loading is replaced by content when the fetch succeeds`() = runTest {
        val viewModel = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(loaded))), Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            assertEquals(contentOf(loaded, items), awaitItem())
        }
    }

    @Test
    fun `a failed fetch shows the error state and retry recovers`() = runTest {
        val viewModel = CatalogViewModel(
            FakeCatalogRepository(listOf(Result.failure(IOException("boom")), Result.success(loaded))),
            Cart(),
        )

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            assertEquals(CatalogUiState(catalog = CatalogState.Error), awaitItem())

            viewModel.retry()

            assertEquals(CatalogUiState(), awaitItem())
            assertEquals(contentOf(loaded, items), awaitItem())
        }
    }

    @Test
    fun `a new collector reuses the loaded state instead of refetching`() = runTest {
        val repository = FakeCatalogRepository(listOf(Result.success(loaded)))
        val viewModel = CatalogViewModel(repository, Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            assertEquals(contentOf(loaded, items), awaitItem())
        }

        // A configuration change recreates the UI, which collects the state again.
        viewModel.uiState.test {
            assertEquals(contentOf(loaded, items), awaitItem())
        }

        assertEquals(1, repository.loadCount)
    }

    private val cheap = foodItem(id = "cheap", price = BigDecimal("1.49"))
    private val dear = foodItem(id = "dear", price = BigDecimal("12.99"))
    private val sortable = catalog(listOf(dear, cheap))

    @Test
    fun `selecting a sort reorders the content and marks the sort active`() = runTest {
        val viewModel = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(sortable))), Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            assertEquals(contentOf(sortable, listOf(dear, cheap)), awaitItem())

            viewModel.onSortSelected(PriceSortOrder.ASCENDING)
            assertEquals(
                contentOf(sortable, listOf(cheap, dear), PriceSortOrder.ASCENDING),
                awaitItem(),
            )

            viewModel.onSortSelected(PriceSortOrder.DESCENDING)
            assertEquals(
                contentOf(sortable, listOf(dear, cheap), PriceSortOrder.DESCENDING),
                awaitItem(),
            )
        }
    }

    @Test
    fun `clearing the sort restores the API order`() = runTest {
        val viewModel = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(sortable))), Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            assertEquals(contentOf(sortable, listOf(dear, cheap)), awaitItem())

            viewModel.onSortSelected(PriceSortOrder.ASCENDING)
            assertEquals(
                contentOf(sortable, listOf(cheap, dear), PriceSortOrder.ASCENDING),
                awaitItem(),
            )

            viewModel.onSortSelected(null)
            assertEquals(contentOf(sortable, listOf(dear, cheap)), awaitItem())
        }
    }

    @Test
    fun `the sort survives a new collector without refetching`() = runTest {
        val repository = FakeCatalogRepository(listOf(Result.success(sortable)))
        val viewModel = CatalogViewModel(repository, Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            assertEquals(contentOf(sortable, listOf(dear, cheap)), awaitItem())

            viewModel.onSortSelected(PriceSortOrder.ASCENDING)
            awaitItem()
        }

        // A configuration change recreates the UI, which collects the state again.
        viewModel.uiState.test {
            assertEquals(
                contentOf(sortable, listOf(cheap, dear), PriceSortOrder.ASCENDING),
                awaitItem(),
            )
        }

        assertEquals(1, repository.loadCount)
    }

    private val produce = foodCategory(id = "cat-produce", name = "Produce")
    private val dairy = foodCategory(id = "cat-dairy", name = "Dairy")
    private val meat = foodCategory(id = "cat-meat", name = "Meat")

    private val steak = foodItem(id = "steak", name = "Steak", price = BigDecimal("12.99"), category = meat)
    private val milk = foodItem(id = "milk", name = "Milk", price = BigDecimal("4.90"), category = dairy)
    private val bananas = foodItem(id = "bananas", name = "Bananas", price = BigDecimal("1.49"), category = produce)
    private val filterable = catalog(listOf(steak, milk, bananas))

    @Test
    fun `selecting categories narrows the content to their union additively`() = runTest {
        val viewModel = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(filterable))), Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            assertEquals(contentOf(filterable, listOf(steak, milk, bananas)), awaitItem())

            viewModel.onCategoryToggled(produce.id)
            assertEquals(
                contentOf(filterable, listOf(bananas), selectedCategoryIds = setOf(produce.id)),
                awaitItem(),
            )

            viewModel.onCategoryToggled(dairy.id)
            assertEquals(
                contentOf(filterable, listOf(milk, bananas), selectedCategoryIds = setOf(produce.id, dairy.id)),
                awaitItem(),
            )
        }
    }

    @Test
    fun `deselecting every category restores the full list`() = runTest {
        val viewModel = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(filterable))), Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            awaitItem()

            viewModel.onCategoryToggled(produce.id)
            awaitItem()

            viewModel.onCategoryToggled(produce.id)
            assertEquals(contentOf(filterable, listOf(steak, milk, bananas)), awaitItem())
        }
    }

    @Test
    fun `the filter composes with the sort regardless of which is applied first`() = runTest {
        val expected = contentOf(
            filterable,
            listOf(bananas, steak),
            sort = PriceSortOrder.ASCENDING,
            selectedCategoryIds = setOf(produce.id, meat.id),
        )

        // Each view model is created just before collecting, so its load completes while
        // this collector is watching rather than during the other block.
        val sortThenFilter = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(filterable))), Cart())
        sortThenFilter.uiState.test {
            awaitItem()
            awaitItem()

            sortThenFilter.onSortSelected(PriceSortOrder.ASCENDING)
            awaitItem()
            sortThenFilter.onCategoryToggled(produce.id)
            awaitItem()
            sortThenFilter.onCategoryToggled(meat.id)

            assertEquals(expected, awaitItem())
        }

        val filterThenSort = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(filterable))), Cart())
        filterThenSort.uiState.test {
            awaitItem()
            awaitItem()

            filterThenSort.onCategoryToggled(produce.id)
            awaitItem()
            filterThenSort.onCategoryToggled(meat.id)
            awaitItem()
            filterThenSort.onSortSelected(PriceSortOrder.ASCENDING)

            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `the cart count counts every add, not distinct items`() = runTest {
        val viewModel = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(filterable))), Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            assertEquals(contentOf(filterable, listOf(steak, milk, bananas)), awaitItem())

            viewModel.onAddToCart(bananas)
            assertEquals(
                contentOf(
                    filterable,
                    listOf(steak, milk, bananas),
                    cartItemCount = 1,
                    cartQuantities = mapOf(bananas.id to 1),
                ),
                awaitItem(),
            )

            viewModel.onAddToCart(bananas)
            assertEquals(
                contentOf(
                    filterable,
                    listOf(steak, milk, bananas),
                    cartItemCount = 2,
                    cartQuantities = mapOf(bananas.id to 2),
                ),
                awaitItem(),
            )

            viewModel.onAddToCart(bananas)
            assertEquals(
                contentOf(
                    filterable,
                    listOf(steak, milk, bananas),
                    cartItemCount = 3,
                    cartQuantities = mapOf(bananas.id to 3),
                ),
                awaitItem(),
            )

            // A fourth add of a second distinct item shows the count is a total of
            // adds; a distinct-item count would read 2 here. The per-item quantities
            // keep each card's own number.
            viewModel.onAddToCart(milk)
            assertEquals(
                contentOf(
                    filterable,
                    listOf(steak, milk, bananas),
                    cartItemCount = 4,
                    cartQuantities = mapOf(bananas.id to 3, milk.id to 1),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `the cart count survives a new collector without refetching`() = runTest {
        val repository = FakeCatalogRepository(listOf(Result.success(filterable)))
        val viewModel = CatalogViewModel(repository, Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            awaitItem()

            viewModel.onAddToCart(bananas)
            awaitItem()
        }

        // A configuration change recreates the UI, which collects the state again.
        viewModel.uiState.test {
            assertEquals(
                contentOf(
                    filterable,
                    listOf(steak, milk, bananas),
                    cartItemCount = 1,
                    cartQuantities = mapOf(bananas.id to 1),
                ),
                awaitItem(),
            )
        }

        assertEquals(1, repository.loadCount)
    }

    // Production provides Cart as a @Singleton, which a JVM test cannot see; this pins
    // the sharing contract that scope must satisfy once a second screen injects the
    // cart in issue #6: state added through one view model is visible through another.
    @Test
    fun `two view models over the same cart agree on the count`() = runTest {
        val cart = Cart()
        val adder = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(filterable))), cart)
        val observer = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(filterable))), cart)

        adder.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            awaitItem()

            adder.onAddToCart(bananas)
            awaitItem()
        }

        observer.uiState.test {
            assertEquals(
                contentOf(
                    filterable,
                    listOf(steak, milk, bananas),
                    cartItemCount = 1,
                    cartQuantities = mapOf(bananas.id to 1),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `the selection survives a new collector without refetching`() = runTest {
        val repository = FakeCatalogRepository(listOf(Result.success(filterable)))
        val viewModel = CatalogViewModel(repository, Cart())

        viewModel.uiState.test {
            assertEquals(CatalogUiState(), awaitItem())
            awaitItem()

            viewModel.onCategoryToggled(dairy.id)
            awaitItem()
        }

        // A configuration change recreates the UI, which collects the state again.
        viewModel.uiState.test {
            assertEquals(
                contentOf(filterable, listOf(milk), selectedCategoryIds = setOf(dairy.id)),
                awaitItem(),
            )
        }

        assertEquals(1, repository.loadCount)
    }
}
