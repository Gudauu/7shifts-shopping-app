package com.sevenshifts.shopping.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.PurchaseFailure
import com.sevenshifts.shopping.domain.PurchaseRepository
import com.sevenshifts.shopping.domain.PurchaseResult
import com.sevenshifts.shopping.domain.orderTotal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(private val cart: Cart, private val purchaseRepository: PurchaseRepository) :
    ViewModel() {
    private val purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    private var purchaseJob: Job? = null

    // The initial value reads the cart directly rather than defaulting to empty, so a
    // cart filled on the catalog screen never flashes the empty state on arrival.
    val uiState: StateFlow<CartUiState> =
        combine(cart.lines, purchaseState, ::uiStateOf)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                uiStateOf(cart.lines.value, purchaseState.value),
            )

    fun onDecrease(itemId: String) {
        if (purchaseState.value is PurchaseUiState.InFlight) return

        cart.decrease(itemId)
        // Item and field failures can become a new logical attempt after the shopper
        // changes the cart. An unresolved outcome must remain blocked even if the cart
        // changes, because another attempt could duplicate a completed purchase.
        val failure = (purchaseState.value as? PurchaseUiState.Failed)?.failure
        if (failure is PurchaseFailure.ItemsRequireAttention || failure is PurchaseFailure.InvalidRequest) {
            purchaseState.value = PurchaseUiState.Idle
        }
    }

    fun onPurchase() {
        val currentPurchase = purchaseState.value
        val canSubmit = currentPurchase is PurchaseUiState.Idle ||
            (currentPurchase is PurchaseUiState.Failed && currentPurchase.failure.retryable)
        val lines = cart.lines.value
        // These state-holder checks are the real submission guard. Button state alone
        // cannot stop two taps that arrive before Compose recomposes.
        if (!canSubmit || lines.isEmpty()) return

        purchaseState.value = PurchaseUiState.InFlight
        purchaseJob = viewModelScope.launch {
            val result = try {
                purchaseRepository.purchase(lines)
            } catch (_: CancellationException) {
                // Parent cancellation means the ViewModel is going away and must still
                // propagate. A timeout or child cancellation while this scope remains
                // active is an uncertain result, never permission for a new attempt.
                currentCoroutineContext().ensureActive()
                PurchaseResult.Failed(PurchaseFailure.UnresolvedOutcome)
            } catch (_: Exception) {
                // Only the repository knows whether a thrown transport failure happened
                // before or after acceptance. The conservative sink prevents a new key.
                PurchaseResult.Failed(PurchaseFailure.UnresolvedOutcome)
            }

            when (result) {
                is PurchaseResult.Completed -> {
                    // Publish confirmation before clearing so every rendered state after
                    // completion is a confirmation, never a transient empty-cart screen.
                    purchaseState.value = PurchaseUiState.Succeeded(
                        purchaseId = result.purchase.id,
                        total = result.purchase.total,
                    )
                    cart.clear()
                }

                is PurchaseResult.Failed -> {
                    purchaseState.value = PurchaseUiState.Failed(result.failure)
                }
            }
        }
    }

    /** Lets the shopper leave without turning an interrupted attempt into a safe retry. */
    fun onCartLeft() {
        when (purchaseState.value) {
            PurchaseUiState.InFlight -> {
                purchaseJob?.cancel()
                purchaseState.value = PurchaseUiState.Failed(PurchaseFailure.UnresolvedOutcome)
            }

            is PurchaseUiState.Succeeded -> purchaseState.value = PurchaseUiState.Idle

            PurchaseUiState.Idle,
            is PurchaseUiState.Failed,
            -> Unit
        }
    }
}

private fun uiStateOf(lines: List<CartLine>, purchase: PurchaseUiState) = CartUiState(
    lines = lines,
    orderTotal = lines.orderTotal,
    purchase = purchase,
)
