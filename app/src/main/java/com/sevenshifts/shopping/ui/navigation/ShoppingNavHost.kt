package com.sevenshifts.shopping.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sevenshifts.shopping.ui.cart.CartScreen
import com.sevenshifts.shopping.ui.catalog.CatalogScreen
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations. Adding an argument means adding a property here,
 * which the compiler then enforces at every call site.
 */
@Serializable
object Catalog

@Serializable
object Cart

@Composable
fun ShoppingNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Catalog,
        modifier = modifier,
    ) {
        composable<Catalog> {
            CatalogScreen(onViewCart = { navController.navigate(Cart) })
        }
        composable<Cart> {
            CartScreen(onBack = { navController.popBackStack() })
        }
    }
}
