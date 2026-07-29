package com.sevenshifts.shopping.data

import com.sevenshifts.shopping.data.network.FoodItemCategoryDto
import com.sevenshifts.shopping.data.network.FoodItemDto
import com.sevenshifts.shopping.data.network.ShoppingApi
import com.sevenshifts.shopping.domain.FoodItem
import java.io.IOException
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryImplTest {
    private class FakeShoppingApi(
        private val items: () -> List<FoodItemDto>,
        private val categories: () -> List<FoodItemCategoryDto>,
    ) : ShoppingApi {
        override suspend fun getFoodItems(): List<FoodItemDto> = items()

        override suspend fun getFoodItemCategories(): List<FoodItemCategoryDto> = categories()
    }

    @Test
    fun `a successful load returns items joined to their categories`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeShoppingApi(
                items = {
                    listOf(
                        FoodItemDto(
                            uuid = "item-1",
                            name = "Bananas",
                            price = BigDecimal("1.49"),
                            categoryUuid = "cat-produce",
                            imageUrl = "https://example.test/bananas.png",
                        ),
                    )
                },
                categories = { listOf(FoodItemCategoryDto(uuid = "cat-produce", name = "Produce")) },
            ),
        )

        val result = repository.loadCatalog()

        assertEquals(
            listOf(
                FoodItem(
                    id = "item-1",
                    name = "Bananas",
                    price = BigDecimal("1.49"),
                    categoryName = "Produce",
                    imageUrl = "https://example.test/bananas.png",
                ),
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `a failing endpoint becomes a failure result instead of a thrown exception`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeShoppingApi(items = { throw IOException("boom") }, categories = { emptyList() }),
        )

        val result = repository.loadCatalog()

        // Coroutines rethrow a copy of the exception across scope boundaries, so assert
        // on type and message rather than instance identity.
        val thrown = result.exceptionOrNull()
        assertTrue(thrown is IOException)
        assertEquals("boom", thrown?.message)
    }

    @Test
    fun `cancellation propagates instead of becoming a failure result`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeShoppingApi(
                items = { throw CancellationException("cancelled") },
                categories = { emptyList() },
            ),
        )

        val thrown = runCatching { repository.loadCatalog() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }
}
