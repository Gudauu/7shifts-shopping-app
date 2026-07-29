package com.sevenshifts.shopping.domain

import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** One distinct item in the cart and how many of it the shopper has added. */
data class CartLine(val item: FoodItem, val quantity: Int)

/** Every add counts, so three of one item is 3 rather than 1. */
val List<CartLine>.totalQuantity: Int
    get() = sumOf { it.quantity }

/**
 * Unit price times quantity, exact and unrounded. Rounding to cents happens once, at
 * display time, because rounding each line before summing would let the total drift.
 */
val CartLine.lineTotal: BigDecimal
    get() = item.price * quantity.toBigDecimal()

/** The sum of the exact line totals; `0` for an empty cart. Rounded only at display. */
val List<CartLine>.orderTotal: BigDecimal
    get() = fold(BigDecimal.ZERO) { total, line -> total + line.lineTotal }

/**
 * The shopper's cart. In memory only, by design: the requirements describe no
 * persistence, and a cart that survived a cold start would need an invalidation policy
 * against a catalog that can change. Screens share the single instance provided by the
 * dependency graph, which is what lets the badge and the cart screen agree.
 */
class Cart {
    private val _lines = MutableStateFlow<List<CartLine>>(emptyList())

    /** Distinct items in the order they were first added; re-adding raises a quantity. */
    val lines: StateFlow<List<CartLine>> = _lines.asStateFlow()

    /**
     * Adds one of [item], identified by its uuid. There is no upper bound because the
     * API exposes no stock or availability.
     */
    fun add(item: FoodItem) {
        _lines.update { lines ->
            val existing = lines.indexOfFirst { it.item.id == item.id }
            if (existing == -1) {
                lines + CartLine(item = item, quantity = 1)
            } else {
                lines.mapIndexed { index, line ->
                    if (index == existing) line.copy(quantity = line.quantity + 1) else line
                }
            }
        }
    }

    /**
     * Removes one unit of the item identified by [itemId]. A quantity of one removes the
     * line; an id no longer in the cart is a no-op, so a repeated tap cannot remove a
     * different item while the UI catches up.
     */
    fun decrease(itemId: String) {
        _lines.update { lines ->
            lines.mapNotNull { line ->
                if (line.item.id != itemId) {
                    line
                } else {
                    line.copy(quantity = line.quantity - 1).takeIf { it.quantity > 0 }
                }
            }
        }
    }
}
