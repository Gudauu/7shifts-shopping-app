package com.sevenshifts.shopping.ui.catalog

import app.cash.turbine.test
import com.sevenshifts.shopping.testing.FakeCatalogRepository
import com.sevenshifts.shopping.testing.MainDispatcherRule
import com.sevenshifts.shopping.testing.foodItem
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CatalogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(foodItem())

    @Test
    fun `loading is replaced by content when the fetch succeeds`() = runTest {
        val viewModel = CatalogViewModel(FakeCatalogRepository(listOf(Result.success(items))))

        viewModel.uiState.test {
            assertEquals(CatalogUiState.Loading, awaitItem())
            assertEquals(CatalogUiState.Content(items), awaitItem())
        }
    }

    @Test
    fun `a failed fetch shows the error state and retry recovers`() = runTest {
        val viewModel = CatalogViewModel(
            FakeCatalogRepository(listOf(Result.failure(IOException("boom")), Result.success(items))),
        )

        viewModel.uiState.test {
            assertEquals(CatalogUiState.Loading, awaitItem())
            assertEquals(CatalogUiState.Error, awaitItem())

            viewModel.retry()

            assertEquals(CatalogUiState.Loading, awaitItem())
            assertEquals(CatalogUiState.Content(items), awaitItem())
        }
    }

    @Test
    fun `a new collector reuses the loaded state instead of refetching`() = runTest {
        val repository = FakeCatalogRepository(listOf(Result.success(items)))
        val viewModel = CatalogViewModel(repository)

        viewModel.uiState.test {
            assertEquals(CatalogUiState.Loading, awaitItem())
            assertEquals(CatalogUiState.Content(items), awaitItem())
        }

        // A configuration change recreates the UI, which collects the state again.
        viewModel.uiState.test {
            assertEquals(CatalogUiState.Content(items), awaitItem())
        }

        assertEquals(1, repository.loadCount)
    }
}
