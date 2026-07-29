package com.sevenshifts.shopping.data.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

internal const val SHOPPING_API_BASE_URL = "https://7shifts.github.io/mobile-takehome/"

/**
 * The one Json configuration production parses with. Tests reference it instead of
 * building their own, so parsing behaviour cannot drift between the two.
 */
internal val shoppingJson = Json { ignoreUnknownKeys = true }

/**
 * Takes the client as a parameter so a JVM test can serve canned responses through an
 * OkHttp interceptor while still exercising the real base URL, paths, converter, and
 * Json configuration.
 */
internal fun createShoppingApi(client: OkHttpClient): ShoppingApi = Retrofit.Builder()
    .baseUrl(SHOPPING_API_BASE_URL)
    .client(client)
    .addConverterFactory(shoppingJson.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(ShoppingApi::class.java)
