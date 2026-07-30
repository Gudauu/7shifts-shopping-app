package com.sevenshifts.shopping.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseTest {
    @Test
    fun `an accepted terminal failure preserves server retryability`() {
        assertTrue(PurchaseFailure.PurchaseNotCompleted(retryable = true).retryable)
        assertFalse(PurchaseFailure.PurchaseNotCompleted(retryable = false).retryable)
    }
}
