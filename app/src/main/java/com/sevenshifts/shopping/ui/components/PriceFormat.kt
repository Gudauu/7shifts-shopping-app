package com.sevenshifts.shopping.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money stays [BigDecimal] from the DTO boundary through UI state; it becomes `$0.00`
 * text only here, at the Compose boundary. This is also the single rounding step: exact
 * amounts are rounded to cents with HALF_UP when they are shown, never before.
 */
fun formatPrice(price: BigDecimal): String = "$" + price.setScale(2, RoundingMode.HALF_UP).toPlainString()
