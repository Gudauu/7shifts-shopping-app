package com.sevenshifts.shopping.domain

import java.math.BigDecimal
import java.time.Instant

/**
 * Purchase boundary described by purchase_api_contract.md. Implementations own
 * idempotency keys, safe transport retries, and uncertain-outcome recovery.
 */
interface PurchaseRepository {
    /**
     * Purchases one snapshot of the cart. Each line supplies an item id, quantity, and
     * the last unit price shown to the shopper. A completed result contains prices and
     * totals recalculated by the server implementation.
     *
     * A repeated call after a retryable failure remains repository-owned: the
     * implementation resumes the existing idempotency key when required, or creates a
     * new key only after a terminal failure that permits a new logical attempt.
     */
    suspend fun purchase(lines: List<CartLine>): PurchaseResult
}

sealed interface PurchaseResult {
    data class Completed(val purchase: CompletedPurchase) : PurchaseResult

    /** No partial purchase was made. */
    data class Failed(val failure: PurchaseFailure) : PurchaseResult
}

data class CompletedPurchase(
    val id: String,
    val createdAt: Instant,
    val purchasedAt: Instant,
    val currency: PurchaseCurrency,
    /** Server-authoritative purchased lines, not the client cart lines. */
    val lines: List<PurchasedLine>,
    /** Server-authoritative sum of [PurchasedLine.lineTotal]. */
    val total: BigDecimal,
)

enum class PurchaseCurrency(val code: String) {
    CAD("CAD"),
}

data class PurchasedLine(
    val foodItemId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
)

/**
 * Actionable failures that can remain after a repository has handled HTTP retries and
 * purchase-status polling. Transport and recovery details do not escape this boundary.
 */
sealed interface PurchaseFailure {
    /** Whether retrying the unchanged cart as a new UI action is safe. */
    val retryable: Boolean

    /** The server rejected one or more cart lines and created no purchase. */
    data class ItemsRequireAttention(val items: List<PurchaseItemFailure>) : PurchaseFailure {
        override val retryable = false
    }

    /** The request itself was invalid and must be corrected before another attempt. */
    data class InvalidRequest(val fields: List<PurchaseFieldFailure> = emptyList()) : PurchaseFailure {
        override val retryable = false
    }

    /** Retry policy was exhausted before the service accepted a purchase. */
    data object TemporarilyUnavailable : PurchaseFailure {
        override val retryable = true
    }

    /** An accepted attempt reached a failed terminal state with server-defined retryability. */
    data class PurchaseNotCompleted(override val retryable: Boolean) : PurchaseFailure

    /** Recovery could not prove completion or failure, so a new attempt is not safe. */
    data object UnresolvedOutcome : PurchaseFailure {
        override val retryable = false
    }

    /** The idempotency key was reused inconsistently, indicating invalid client state. */
    data object ClientStateConflict : PurchaseFailure {
        override val retryable = false
    }
}

data class PurchaseFieldFailure(val path: String, val code: String)

data class PurchaseItemFailure(
    val foodItemId: String,
    val reason: PurchaseItemFailureReason,
    val expectedUnitPrice: BigDecimal? = null,
    val currentUnitPrice: BigDecimal? = null,
    val availableQuantity: Int? = null,
)

enum class PurchaseItemFailureReason {
    NOT_FOUND,
    UNAVAILABLE,
    QUANTITY_UNAVAILABLE,
    PRICE_CHANGED,
}
