package com.sevenshifts.shopping.data

import com.sevenshifts.shopping.data.network.FoodItemCategoryDto
import com.sevenshifts.shopping.data.network.FoodItemDto
import com.sevenshifts.shopping.data.network.ShoppingApi
import com.sevenshifts.shopping.data.network.decodeValidElements
import com.sevenshifts.shopping.domain.Catalog
import com.sevenshifts.shopping.domain.CatalogRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class CatalogRepositoryImpl @Inject constructor(private val api: ShoppingApi) : CatalogRepository {
    override suspend fun loadCatalog(): Result<Catalog> = try {
        // The endpoints are independent, so fetch them concurrently. If either fails,
        // coroutineScope cancels the other and the whole load is a failure.
        coroutineScope {
            val items = async { api.getFoodItems().decodeValidElements(FoodItemDto.serializer()) }
            val categories = async { api.getFoodItemCategories().decodeValidElements(FoodItemCategoryDto.serializer()) }
            Result.success(joinCatalog(items.await(), categories.await()))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
