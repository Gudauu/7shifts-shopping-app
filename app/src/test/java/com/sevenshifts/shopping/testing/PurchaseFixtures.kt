package com.sevenshifts.shopping.testing

import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.CompletedPurchase
import com.sevenshifts.shopping.domain.PurchaseCurrency
import com.sevenshifts.shopping.domain.PurchaseResult
import com.sevenshifts.shopping.domain.PurchasedLine
import com.sevenshifts.shopping.domain.lineTotal
import java.math.BigDecimal
import java.time.Instant

fun completedPurchaseResult(lines: List<CartLine>, id: String = "purchase-1"): PurchaseResult.Completed {
    val purchasedLines = lines.map { line ->
        PurchasedLine(
            foodItemId = line.item.id,
            name = line.item.name,
            quantity = line.quantity,
            unitPrice = line.item.price,
            lineTotal = line.lineTotal,
        )
    }
    return PurchaseResult.Completed(
        CompletedPurchase(
            id = id,
            createdAt = Instant.parse("2026-07-29T22:42:16Z"),
            purchasedAt = Instant.parse("2026-07-29T22:42:17Z"),
            currency = PurchaseCurrency.CAD,
            lines = purchasedLines,
            total = purchasedLines.fold(BigDecimal.ZERO) { total, line -> total + line.lineTotal },
        ),
    )
}
