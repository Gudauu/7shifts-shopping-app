package com.sevenshifts.shopping.testing

import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.PurchaseRepository
import com.sevenshifts.shopping.domain.PurchaseResult

/** Scriptable purchase boundary that records each submitted cart snapshot. */
class FakePurchaseRepository(results: List<PurchaseResult>, private val beforeResult: suspend () -> Unit = {}) :
    PurchaseRepository {
    init {
        require(results.isNotEmpty()) { "Provide at least one result" }
    }

    private val results = ArrayDeque(results)

    val requests = mutableListOf<List<CartLine>>()

    override suspend fun purchase(lines: List<CartLine>): PurchaseResult {
        requests += lines
        beforeResult()
        return if (results.size > 1) results.removeFirst() else results.first()
    }
}
