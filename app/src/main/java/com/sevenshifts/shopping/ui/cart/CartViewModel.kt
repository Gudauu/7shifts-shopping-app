package com.sevenshifts.shopping.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sevenshifts.shopping.domain.Cart
import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.orderTotal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CartViewModel @Inject constructor(private val cart: Cart) : ViewModel() {
    // The initial value reads the cart directly rather than defaulting to empty, so a
    // cart filled on the catalog screen never flashes the empty state on arrival.
    val uiState: StateFlow<CartUiState> =
        cart.lines
            .map(::uiStateOf)
            .stateIn(viewModelScope, SharingStarted.Eagerly, uiStateOf(cart.lines.value))

    fun onDecrease(itemId: String) {
        cart.decrease(itemId)
    }
}

private fun uiStateOf(lines: List<CartLine>) = CartUiState(lines = lines, orderTotal = lines.orderTotal)
