package com.sevenshifts.shopping.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sevenshifts.shopping.ui.cart.CartScreen
import com.sevenshifts.shopping.ui.catalog.CatalogScreen
import com.sevenshifts.shopping.ui.catalog.CatalogViewModel
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations. Adding an argument means adding a property here,
 * which the compiler then enforces at every call site.
 */
@Serializable
object Catalog

@Serializable
object Cart

/**
 * The catalog view model is a parameter rather than a `hiltViewModel()` call inside the
 * destination: tests construct one directly with a fake repository, and in production the
 * default scopes it to the activity, so catalog state survives both configuration changes
 * and navigating to the cart and back without refetching.
 */
@Composable
fun ShoppingNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    catalogViewModel: CatalogViewModel = hiltViewModel(),
) {
    NavHost(
        navController = navController,
        startDestination = Catalog,
        modifier = modifier,
    ) {
        composable<Catalog> {
            val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
            CatalogScreen(
                state = catalogState,
                onRetry = catalogViewModel::retry,
                onSortSelected = catalogViewModel::onSortSelected,
                onCategoryToggled = catalogViewModel::onCategoryToggled,
                onAddToCart = catalogViewModel::onAddToCart,
                onViewCart = { navController.navigate(Cart) },
            )
        }
        composable<Cart> {
            CartScreen(onBack = { navController.popBackStack() })
        }
    }
}
