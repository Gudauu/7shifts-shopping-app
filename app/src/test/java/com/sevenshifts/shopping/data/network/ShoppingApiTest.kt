package com.sevenshifts.shopping.data.network

import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Exercises the production Retrofit and Json wiring end to end on the JVM. An OkHttp
 * interceptor stands in for the server, so no mock-server dependency is needed, while
 * the real base URL, paths, converter, and Json configuration are all in play.
 */
class ShoppingApiTest {
    private val requestedUrls = mutableListOf<String>()

    private fun apiServing(body: String): ShoppingApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedUrls += chain.request().url.toString()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return createShoppingApi(client)
    }

    @Test
    fun `food items come from the documented endpoint and parse with the production Json`() = runTest {
        // The extra key pins ignoreUnknownKeys in the production configuration.
        val api = apiServing(
            """
            [
              {
                "uuid": "u1",
                "name": "Bananas",
                "price": 1.49,
                "category_uuid": "c1",
                "image_url": "https://example.test/bananas.png",
                "not_in_the_dto": true
              }
            ]
            """.trimIndent(),
        )

        val items = api.getFoodItems()

        assertEquals(listOf("https://7shifts.github.io/mobile-takehome/api/food_items.json"), requestedUrls)
        assertEquals("Bananas", items.single().name)
        assertEquals(BigDecimal("1.49"), items.single().price)
    }

    @Test
    fun `categories come from the documented endpoint and parse with the production Json`() = runTest {
        val api = apiServing("""[{"uuid": "c1", "name": "Produce"}]""")

        val categories = api.getFoodItemCategories()

        assertEquals(listOf("https://7shifts.github.io/mobile-takehome/api/food_item_categories.json"), requestedUrls)
        assertEquals("Produce", categories.single().name)
    }
}
