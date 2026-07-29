package com.sevenshifts.shopping.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sevenshifts.shopping.domain.FoodItem
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(state: CatalogUiState, onRetry: () -> Unit, onViewCart: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Food items") },
                actions = {
                    TextButton(onClick = onViewCart) {
                        Text("View cart")
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (state) {
                CatalogUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                CatalogUiState.Error ->
                    ErrorContent(onRetry = onRetry, modifier = Modifier.align(Alignment.Center))

                is CatalogUiState.Content ->
                    if (state.items.isEmpty()) {
                        Text(
                            text = "No food items to show",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        FoodItemGrid(items = state.items, modifier = Modifier.fillMaxSize())
                    }
            }
        }
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Couldn't load the food items")
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun FoodItemGrid(items: List<FoodItem>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = items, key = { it.id }) { item ->
            FoodItemCard(item = item)
        }
    }
}

@Composable
private fun FoodItemCard(item: FoodItem, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        val imagePlaceholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
        AsyncImage(
            model = item.imageUrl,
            // The name is announced by the Text below; describing the image too would
            // make TalkBack read every card twice.
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Crop,
            placeholder = imagePlaceholder,
            error = imagePlaceholder,
            // Shown when imageUrl is null; a missing image must not hide the item.
            fallback = imagePlaceholder,
        )
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.categoryName?.let { categoryName ->
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatPrice(item.price),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/** Money stays [BigDecimal] everywhere else; it becomes `$0.00` text only here. */
private fun formatPrice(price: BigDecimal): String = "$" + price.setScale(2, RoundingMode.HALF_UP).toPlainString()
