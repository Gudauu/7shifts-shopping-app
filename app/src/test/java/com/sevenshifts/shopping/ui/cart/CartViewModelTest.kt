package com.sevenshifts.shopping.ui.cart

import app.cash.turbine.test
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.PurchaseFailure
import com.sevenshifts.shopping.domain.PurchaseItemFailure
import com.sevenshifts.shopping.domain.PurchaseItemFailureReason
import com.sevenshifts.shopping.domain.PurchaseRepository
import com.sevenshifts.shopping.domain.PurchaseResult
import com.sevenshifts.shopping.testing.FakePurchaseRepository
import com.sevenshifts.shopping.testing.MainDispatcherRule
import com.sevenshifts.shopping.testing.completedPurchaseResult
import com.sevenshifts.shopping.testing.foodItem
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bananas = foodItem(id = "bananas", name = "Bananas", price = BigDecimal("1.49"))
    private val milk = foodItem(id = "milk", name = "Milk", price = BigDecimal("4.90"))

    @Test
    fun `an empty cart renders as empty lines and a zero total`() = runTest {
        val viewModel = viewModel(Cart())

        viewModel.uiState.test {
            assertEquals(CartUiState(), awaitItem())
        }
    }

    @Test
    fun `the state mirrors the cart's lines and total as items are added`() = runTest {
        val cart = Cart()
        val viewModel = viewModel(cart)

        viewModel.uiState.test {
            assertEquals(CartUiState(), awaitItem())

            cart.add(bananas)
            assertEquals(
                CartUiState(
                    lines = listOf(CartLine(bananas, quantity = 1)),
                    orderTotal = BigDecimal("1.49"),
                ),
                awaitItem(),
            )

            cart.add(milk)
            cart.add(bananas)
            assertEquals(
                CartUiState(
                    lines = listOf(CartLine(bananas, quantity = 2), CartLine(milk, quantity = 1)),
                    orderTotal = BigDecimal("7.88"),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `decreasing through the view model updates the line quantity and order total`() = runTest {
        val cart = Cart()
        repeat(2) { cart.add(bananas) }
        val viewModel = viewModel(cart)

        viewModel.uiState.test {
            assertEquals(
                CartUiState(
                    lines = listOf(CartLine(bananas, quantity = 2)),
                    orderTotal = BigDecimal("2.98"),
                ),
                awaitItem(),
            )

            viewModel.onDecrease(bananas.id)

            assertEquals(
                CartUiState(
                    lines = listOf(CartLine(bananas, quantity = 1)),
                    orderTotal = BigDecimal("1.49"),
                ),
                awaitItem(),
            )
        }
    }

    // Arriving on the cart screen constructs the view model after the shopper has been
    // adding items on the catalog; the first emission must already hold them.
    @Test
    fun `a cart filled before construction is visible in the first state`() = runTest {
        val cart = Cart()
        repeat(2) { cart.add(bananas) }

        val viewModel = viewModel(cart)

        viewModel.uiState.test {
            assertEquals(
                CartUiState(
                    lines = listOf(CartLine(bananas, quantity = 2)),
                    orderTotal = BigDecimal("2.98"),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `a completed purchase clears the cart only after completion`() = runTest {
        val cart = Cart().apply { repeat(2) { add(bananas) } }
        val submittedLines = cart.lines.value
        val releaseResult = CompletableDeferred<Unit>()
        val repository = FakePurchaseRepository(
            results = listOf(completedPurchaseResult(submittedLines)),
            beforeResult = { releaseResult.await() },
        )
        val viewModel = CartViewModel(cart, repository)

        viewModel.onPurchase()
        runCurrent()

        assertEquals(PurchaseUiState.InFlight, viewModel.uiState.value.purchase)
        assertEquals(submittedLines, cart.lines.value)

        releaseResult.complete(Unit)
        advanceUntilIdle()

        assertEquals(emptyList<CartLine>(), cart.lines.value)
        assertEquals(
            PurchaseUiState.Succeeded(purchaseId = "purchase-1", total = BigDecimal("2.98")),
            viewModel.uiState.value.purchase,
        )
        assertEquals(submittedLines, repository.requests.single())
    }

    @Test
    fun `a failed purchase preserves the cart and exposes a safe retry`() = runTest {
        val cart = Cart().apply { add(bananas) }
        val originalLines = cart.lines.value
        val repository = FakePurchaseRepository(
            listOf(PurchaseResult.Failed(PurchaseFailure.TemporarilyUnavailable)),
        )
        val viewModel = CartViewModel(cart, repository)

        viewModel.onPurchase()
        advanceUntilIdle()

        assertEquals(originalLines, cart.lines.value)
        assertEquals(BigDecimal("1.49"), viewModel.uiState.value.orderTotal)
        assertEquals(
            PurchaseUiState.Failed(PurchaseFailure.TemporarilyUnavailable),
            viewModel.uiState.value.purchase,
        )

        viewModel.onPurchase()
        advanceUntilIdle()
        assertEquals(2, repository.requests.size)
        assertEquals(originalLines, cart.lines.value)
    }

    @Test
    fun `correcting an item failure permits a new purchase attempt`() = runTest {
        val cart = Cart().apply { repeat(2) { add(bananas) } }
        val repository = FakePurchaseRepository(
            listOf(
                PurchaseResult.Failed(
                    PurchaseFailure.ItemsRequireAttention(
                        items = listOf(
                            PurchaseItemFailure(
                                foodItemId = bananas.id,
                                reason = PurchaseItemFailureReason.QUANTITY_UNAVAILABLE,
                                availableQuantity = 1,
                            ),
                        ),
                    ),
                ),
                completedPurchaseResult(listOf(CartLine(bananas, quantity = 1))),
            ),
        )
        val viewModel = CartViewModel(cart, repository)

        viewModel.onPurchase()
        advanceUntilIdle()
        viewModel.onDecrease(bananas.id)
        runCurrent()

        assertEquals(PurchaseUiState.Idle, viewModel.uiState.value.purchase)
        assertEquals(listOf(CartLine(bananas, quantity = 1)), cart.lines.value)

        viewModel.onPurchase()
        advanceUntilIdle()
        assertTrue(cart.lines.value.isEmpty())
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `a second purchase while one is in flight is ignored`() = runTest {
        val cart = Cart().apply { add(bananas) }
        val releaseResult = CompletableDeferred<Unit>()
        val repository = FakePurchaseRepository(
            results = listOf(completedPurchaseResult(cart.lines.value)),
            beforeResult = { releaseResult.await() },
        )
        val viewModel = CartViewModel(cart, repository)

        viewModel.onPurchase()
        viewModel.onPurchase()
        runCurrent()

        assertEquals(1, repository.requests.size)
        assertEquals(PurchaseUiState.InFlight, viewModel.uiState.value.purchase)

        releaseResult.complete(Unit)
        advanceUntilIdle()
        assertTrue(cart.lines.value.isEmpty())
    }

    @Test
    fun `an unexpected repository exception becomes an unresolved outcome`() = runTest {
        val cart = Cart().apply { repeat(2) { add(bananas) } }
        var purchaseCount = 0
        val repository = object : PurchaseRepository {
            override suspend fun purchase(lines: List<CartLine>): PurchaseResult {
                purchaseCount++
                error("Unexpected transport failure")
            }
        }
        val viewModel = CartViewModel(cart, repository)

        viewModel.onPurchase()
        advanceUntilIdle()

        assertEquals(
            PurchaseUiState.Failed(PurchaseFailure.UnresolvedOutcome),
            viewModel.uiState.value.purchase,
        )
        assertEquals(listOf(CartLine(bananas, quantity = 2)), cart.lines.value)

        viewModel.onDecrease(bananas.id)
        assertEquals(listOf(CartLine(bananas, quantity = 1)), cart.lines.value)
        assertEquals(
            PurchaseUiState.Failed(PurchaseFailure.UnresolvedOutcome),
            viewModel.uiState.value.purchase,
        )
        viewModel.onPurchase()
        advanceUntilIdle()
        assertEquals(1, purchaseCount)
    }

    @Test
    fun `a repository timeout becomes an unresolved outcome instead of staying in flight`() = runTest {
        val cart = Cart().apply { add(bananas) }
        val repository = object : PurchaseRepository {
            override suspend fun purchase(lines: List<CartLine>): PurchaseResult = withTimeout(1) {
                awaitCancellation()
            }
        }
        val viewModel = CartViewModel(cart, repository)

        viewModel.onPurchase()
        advanceUntilIdle()

        assertEquals(
            PurchaseUiState.Failed(PurchaseFailure.UnresolvedOutcome),
            viewModel.uiState.value.purchase,
        )
        assertEquals(listOf(CartLine(bananas, quantity = 1)), cart.lines.value)
    }

    @Test
    fun `leaving a hanging purchase preserves the cart as unresolved`() = runTest {
        val cart = Cart().apply { add(bananas) }
        val repository = FakePurchaseRepository(
            results = listOf(completedPurchaseResult(cart.lines.value)),
            beforeResult = { awaitCancellation() },
        )
        val viewModel = CartViewModel(cart, repository)

        viewModel.onPurchase()
        runCurrent()
        viewModel.onCartLeft()
        runCurrent()

        assertEquals(
            PurchaseUiState.Failed(PurchaseFailure.UnresolvedOutcome),
            viewModel.uiState.value.purchase,
        )
        assertEquals(listOf(CartLine(bananas, quantity = 1)), cart.lines.value)
    }

    @Test
    fun `purchasing an empty cart never reaches the repository`() = runTest {
        val repository = FakePurchaseRepository(
            listOf(PurchaseResult.Failed(PurchaseFailure.TemporarilyUnavailable)),
        )
        val viewModel = CartViewModel(Cart(), repository)

        viewModel.onPurchase()
        advanceUntilIdle()

        assertTrue(repository.requests.isEmpty())
        assertEquals(PurchaseUiState.Idle, viewModel.uiState.value.purchase)
    }

    private fun viewModel(cart: Cart): CartViewModel = CartViewModel(
        cart,
        FakePurchaseRepository(
            listOf(PurchaseResult.Failed(PurchaseFailure.TemporarilyUnavailable)),
        ),
    )
}
