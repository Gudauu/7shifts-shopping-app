package com.sevenshifts.shopping.ui.components

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class PriceFormatTest {
    @Test
    fun `amounts always show two decimals`() {
        assertEquals("$1.49", formatPrice(BigDecimal("1.49")))
        assertEquals("$4.90", formatPrice(BigDecimal("4.9")))
        assertEquals("$12.00", formatPrice(BigDecimal("12")))
    }

    @Test
    fun `zero formats as zero dollars`() {
        assertEquals("$0.00", formatPrice(BigDecimal.ZERO))
    }

    // Exact sub-cent amounts can arrive from summing unrounded line totals; display is
    // the single rounding step and it rounds half up.
    @Test
    fun `sub-cent amounts round half up at display`() {
        assertEquals("$2.01", formatPrice(BigDecimal("2.005")))
        assertEquals("$2.00", formatPrice(BigDecimal("2.004")))
    }
}
