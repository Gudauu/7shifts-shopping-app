package com.sevenshifts.shopping.data

import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.PurchaseCurrency
import com.sevenshifts.shopping.domain.PurchaseFailure
import com.sevenshifts.shopping.domain.PurchaseFieldFailure
import com.sevenshifts.shopping.domain.PurchaseResult
import com.sevenshifts.shopping.domain.PurchasedLine
import com.sevenshifts.shopping.testing.foodItem
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StubPurchaseRepositoryTest {
    private val bananas = foodItem(id = "bananas", name = "Bananas", price = BigDecimal("1.49"))

    @Test
    fun `the app stub simulates a completed purchase response`() = runTest {
        val result = StubPurchaseRepository().purchase(
            listOf(CartLine(item = bananas, quantity = 2)),
        )

        assertTrue(result is PurchaseResult.Completed)
        val purchase = (result as PurchaseResult.Completed).purchase
        assertEquals(PurchaseCurrency.CAD, purchase.currency)
        assertEquals(
            listOf(
                PurchasedLine(
                    foodItemId = "bananas",
                    name = "Bananas",
                    quantity = 2,
                    unitPrice = BigDecimal("1.49"),
                    lineTotal = BigDecimal("2.98"),
                ),
            ),
            purchase.lines,
        )
        assertEquals(BigDecimal("2.98"), purchase.total)
        assertTrue(purchase.purchasedAt >= purchase.createdAt)
    }

    @Test
    fun `the stub models the contract failure for an empty item array`() = runTest {
        val result = StubPurchaseRepository().purchase(emptyList())

        assertEquals(
            PurchaseResult.Failed(
                PurchaseFailure.InvalidRequest(
                    fields = listOf(
                        PurchaseFieldFailure(
                            path = "items",
                            code = "must_not_be_empty",
                        ),
                    ),
                ),
            ),
            result,
        )
    }
}
