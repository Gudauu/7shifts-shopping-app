package com.sevenshifts.shopping.ui.cart

import com.sevenshifts.shopping.domain.CartLine
import java.math.BigDecimal

/**
 * The one state the cart screen renders. Empty [lines] mean the empty-cart state; the
 * screen never sees a loading or error state because the cart lives in memory.
 */
data class CartUiState(
    /** Distinct items in first-added order, each with its quantity. */
    val lines: List<CartLine> = emptyList(),
    /** Exact sum of the exact line totals; formatted and rounded only at display. */
    val orderTotal: BigDecimal = BigDecimal.ZERO,
)
