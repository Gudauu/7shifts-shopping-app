package com.sevenshifts.shopping.ui.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sevenshifts.shopping.domain.FoodCategory
import com.sevenshifts.shopping.domain.FoodItem
import com.sevenshifts.shopping.domain.PriceSortOrder
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onRetry: () -> Unit,
    onSortSelected: (PriceSortOrder?) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onViewCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    if (state.items.isEmpty() && state.selectedCategoryIds.isEmpty()) {
                        Text(
                            text = "No food items to show",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PriceSortRow(sort = state.sort, onSortSelected = onSortSelected)
                            CategoryFilterRow(
                                categories = state.categories,
                                selectedCategoryIds = state.selectedCategoryIds,
                                onCategoryToggled = onCategoryToggled,
                            )
                            if (state.items.isEmpty()) {
                                // Only an active filter can empty the list here, so the
                                // chips stay visible for the user to widen the selection.
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    Text(
                                        text = "No items in the selected categories",
                                        modifier = Modifier.align(Alignment.Center),
                                    )
                                }
                            } else {
                                FoodItemGrid(
                                    items = state.items,
                                    sort = state.sort,
                                    selectedCategoryIds = state.selectedCategoryIds,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
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
private fun PriceSortRow(
    sort: PriceSortOrder?,
    onSortSelected: (PriceSortOrder?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PriceSortChip(
            label = "Price: low to high",
            order = PriceSortOrder.ASCENDING,
            activeSort = sort,
            onSortSelected = onSortSelected,
        )
        PriceSortChip(
            label = "Price: high to low",
            order = PriceSortOrder.DESCENDING,
            activeSort = sort,
            onSortSelected = onSortSelected,
        )
    }
}

@Composable
private fun PriceSortChip(
    label: String,
    order: PriceSortOrder,
    activeSort: PriceSortOrder?,
    onSortSelected: (PriceSortOrder?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = activeSort == order
    FilterChip(
        selected = selected,
        // Tapping the active chip clears the sort and restores the API's order.
        onClick = { onSortSelected(if (selected) null else order) },
        label = { Text(label) },
        modifier = modifier,
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<FoodCategory>,
    selectedCategoryIds: Set<String>,
    onCategoryToggled: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Chips keep the categories endpoint's order. The row scrolls sideways rather than
    // wrapping so the grid keeps its vertical space on narrow screens.
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id in selectedCategoryIds,
                onClick = { onCategoryToggled(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}

@Composable
private fun FoodItemGrid(
    items: List<FoodItem>,
    sort: PriceSortOrder?,
    selectedCategoryIds: Set<String>,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val activeControls by rememberUpdatedState(sort to selectedCategoryIds)
    // The keyed items make the grid track the first visible card through a list change,
    // which reads as jumping to wherever that card lands. A sort or filter change should
    // present the top of the new list instead. Dropping the initial value keeps the scroll
    // position when the composition is restored after a configuration change or navigation.
    LaunchedEffect(gridState) {
        snapshotFlow { activeControls }
            .drop(1)
            .collect { gridState.scrollToItem(0) }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = gridState,
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
            item.category?.let { category ->
                Text(
                    text = category.name,
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
