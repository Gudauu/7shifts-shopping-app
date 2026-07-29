package com.sevenshifts.shopping.testing

import com.sevenshifts.shopping.domain.CatalogRepository
import com.sevenshifts.shopping.domain.FoodItem

/**
 * Returns the given results in order, repeating the last one once the queue runs out, so
 * a test can script "fail, then succeed on retry" with two entries. Not a vararg because
 * Kotlin prohibits vararg parameters of type [Result].
 */
class FakeCatalogRepository(results: List<Result<List<FoodItem>>>) : CatalogRepository {
    init {
        require(results.isNotEmpty()) { "Provide at least one result" }
    }

    private val results = ArrayDeque(results)

    var loadCount = 0
        private set

    override suspend fun loadCatalog(): Result<List<FoodItem>> {
        loadCount++
        return if (results.size > 1) results.removeFirst() else results.first()
    }
}
