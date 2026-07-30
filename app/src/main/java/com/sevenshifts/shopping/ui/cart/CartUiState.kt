package com.sevenshifts.shopping.ui.cart

import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.PurchaseFailure
import java.math.BigDecimal

/** The one immutable state rendered by the cart screen. */
data class CartUiState(
    /** Distinct items in first-added order, each with its quantity. */
    val lines: List<CartLine> = emptyList(),
    /** Exact sum of the exact line totals; formatted and rounded only at display. */
    val orderTotal: BigDecimal = BigDecimal.ZERO,
    /** One operation state covers submission and any repository-owned recovery. */
    val purchase: PurchaseUiState = PurchaseUiState.Idle,
)

sealed interface PurchaseUiState {
    data object Idle : PurchaseUiState

    data object InFlight : PurchaseUiState

    data class Succeeded(val purchaseId: String, val total: BigDecimal) : PurchaseUiState

    data class Failed(val failure: PurchaseFailure) : PurchaseUiState
}
