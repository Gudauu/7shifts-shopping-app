package com.sevenshifts.shopping.domain

interface CatalogRepository {
    /**
     * Loads the full catalog: every food item joined to its category, plus the categories
     * themselves. Failures are returned, not thrown, so callers handle them as a state
     * rather than an exception.
     */
    suspend fun loadCatalog(): Result<Catalog>
}
