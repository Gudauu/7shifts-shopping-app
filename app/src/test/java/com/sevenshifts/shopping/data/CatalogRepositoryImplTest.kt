package com.sevenshifts.shopping.data

import com.sevenshifts.shopping.data.network.ShoppingApi
import com.sevenshifts.shopping.data.network.shoppingJson
import com.sevenshifts.shopping.domain.Catalog
import com.sevenshifts.shopping.domain.FoodCategory
import com.sevenshifts.shopping.domain.FoodItem
import java.io.IOException
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryImplTest {
    private class FakeShoppingApi(private val items: () -> JsonArray, private val categories: () -> JsonArray) :
        ShoppingApi {
        override suspend fun getFoodItems(): JsonArray = items()

        override suspend fun getFoodItemCategories(): JsonArray = categories()
    }

    private fun jsonArrayOf(raw: String): JsonArray = shoppingJson.parseToJsonElement(raw).jsonArray

    @Test
    fun `a successful load returns items joined to their categories`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeShoppingApi(
                items = {
                    jsonArrayOf(
                        """
                        [
                          {
                            "uuid": "item-1",
                            "name": "Bananas",
                            "price": 1.49,
                            "category_uuid": "cat-produce",
                            "image_url": "https://example.test/bananas.png"
                          }
                        ]
                        """.trimIndent(),
                    )
                },
                categories = { jsonArrayOf("""[{"uuid": "cat-produce", "name": "Produce"}]""") },
            ),
        )

        val result = repository.loadCatalog()

        val produce = FoodCategory(id = "cat-produce", name = "Produce")
        assertEquals(
            Catalog(
                items = listOf(
                    FoodItem(
                        id = "item-1",
                        name = "Bananas",
                        price = BigDecimal("1.49"),
                        category = produce,
                        imageUrl = "https://example.test/bananas.png",
                    ),
                ),
                categories = listOf(produce),
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun `a malformed element costs only itself never the catalog`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeShoppingApi(
                items = { jsonArrayOf("""[{"uuid": "broken"}, {"uuid": "item-1", "name": "Milk", "price": 4.99}]""") },
                categories = { JsonArray(emptyList()) },
            ),
        )

        val result = repository.loadCatalog()

        assertEquals(listOf("item-1"), result.getOrThrow().items.map { it.id })
    }

    @Test
    fun `a failing endpoint becomes a failure result instead of a thrown exception`() = runTest {
        val repository = CatalogRepositoryImpl(
            FakeShoppingApi(items = { throw IOException("boom") }, categories = { JsonArray(emptyList()) }),
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
                categories = { JsonArray(emptyList()) },
            ),
        )

        val thrown = runCatching { repository.loadCatalog() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }
}
