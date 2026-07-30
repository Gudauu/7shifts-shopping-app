package com.sevenshifts.shopping.data

import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.CompletedPurchase
import com.sevenshifts.shopping.domain.PurchaseCurrency
import com.sevenshifts.shopping.domain.PurchaseFailure
import com.sevenshifts.shopping.domain.PurchaseFieldFailure
import com.sevenshifts.shopping.domain.PurchaseRepository
import com.sevenshifts.shopping.domain.PurchaseResult
import com.sevenshifts.shopping.domain.PurchasedLine
import com.sevenshifts.shopping.domain.lineTotal
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay

/**
 * Simulates the unimplemented Purchases API with a short successful request. A real
 * replacement keeps this same domain boundary and owns idempotency and recovery.
 */
class StubPurchaseRepository @Inject constructor() : PurchaseRepository {
    override suspend fun purchase(lines: List<CartLine>): PurchaseResult {
        delay(SIMULATED_REQUEST_MILLIS)
        if (lines.isEmpty()) {
            return PurchaseResult.Failed(
                PurchaseFailure.InvalidRequest(
                    fields = listOf(PurchaseFieldFailure(path = "items", code = "must_not_be_empty")),
                ),
            )
        }

        val createdAt = Instant.now()
        val purchasedLines = lines.map { line ->
            PurchasedLine(
                foodItemId = line.item.id,
                name = line.item.name,
                quantity = line.quantity,
                // With no server to reprice against, the stub echoes the quote as the
                // authoritative response. A network implementation replaces both fields.
                unitPrice = line.item.price,
                lineTotal = line.lineTotal,
            )
        }
        delay(SIMULATED_PROCESSING_MILLIS)
        return PurchaseResult.Completed(
            CompletedPurchase(
                id = UUID.randomUUID().toString(),
                createdAt = createdAt,
                purchasedAt = Instant.now(),
                currency = PurchaseCurrency.CAD,
                lines = purchasedLines,
                total = purchasedLines.fold(BigDecimal.ZERO) { total, line -> total + line.lineTotal },
            ),
        )
    }

    private companion object {
        const val SIMULATED_REQUEST_MILLIS = 350L
        const val SIMULATED_PROCESSING_MILLIS = 450L
    }
}
