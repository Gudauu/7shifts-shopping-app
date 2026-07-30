package com.sevenshifts.shopping.ui.catalog

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sevenshifts.shopping.domain.FoodCategory
import com.sevenshifts.shopping.domain.FoodItem
import com.sevenshifts.shopping.domain.PriceSortOrder
import com.sevenshifts.shopping.ui.components.formatPrice
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onRetry: () -> Unit,
    onSortSelected: (PriceSortOrder?) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    onViewCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Shopping") },
                actions = {
                    TextButton(onClick = onViewCart) {
                        Text("View cart")
                        // An empty cart shows no badge at all; a permanent "0" would
                        // read as something needing attention. The count lives beside
                        // the catalog section, so it stays through loading and errors.
                        if (state.cartItemCount > 0) {
                            Badge(
                                // The badge and checkout action use the same theme accent,
                                // making the cart journey read as one consistent action.
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .semantics {
                                        // The button merges this state with "View cart",
                                        // keeping the total distinct from bare per-card counts.
                                        stateDescription = cartCountDescription(state.cartItemCount)
                                    },
                            ) {
                                Text(state.cartItemCount.toString())
                            }
                        }
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
            when (val catalog = state.catalog) {
                CatalogState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                CatalogState.Error ->
                    ErrorContent(onRetry = onRetry, modifier = Modifier.align(Alignment.Center))

                is CatalogState.Content ->
                    if (catalog.items.isEmpty() && catalog.selectedCategoryIds.isEmpty()) {
                        Text(
                            text = "No food items to show",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            CatalogControls(
                                sort = catalog.sort,
                                categories = catalog.categories,
                                selectedCategoryIds = catalog.selectedCategoryIds,
                                onSortSelected = onSortSelected,
                                onCategoryToggled = onCategoryToggled,
                            )
                            if (catalog.items.isEmpty()) {
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
                                    items = catalog.items,
                                    sort = catalog.sort,
                                    selectedCategoryIds = catalog.selectedCategoryIds,
                                    cartQuantities = state.cartQuantities,
                                    onAddToCart = onAddToCart,
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
private fun CatalogControls(
    sort: PriceSortOrder?,
    categories: List<FoodCategory>,
    selectedCategoryIds: Set<String>,
    onSortSelected: (PriceSortOrder?) -> Unit,
    onCategoryToggled: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var landscapeControlsExpanded by remember(isLandscape) { mutableStateOf(false) }
    val controlsExpanded = !isLandscape || landscapeControlsExpanded

    Column(modifier = modifier.fillMaxWidth()) {
        if (isLandscape) {
            val stateLabel = if (controlsExpanded) "Expanded" else "Collapsed"
            TextButton(
                onClick = { landscapeControlsExpanded = !landscapeControlsExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .semantics {
                        stateDescription = "$stateLabel. ${catalogControlsSummary(sort, selectedCategoryIds.size)}"
                    },
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text("Sort & categories")
                    Text(
                        text = catalogControlsSummary(sort, selectedCategoryIds.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ExpandCollapseSign(expanded = controlsExpanded)
            }
        }

        if (controlsExpanded) {
            PriceSortRow(sort = sort, onSortSelected = onSortSelected)
            CategoryFilterRow(
                categories = categories,
                selectedCategoryIds = selectedCategoryIds,
                onCategoryToggled = onCategoryToggled,
            )
        }
    }
}

private fun catalogControlsSummary(sort: PriceSortOrder?, selectedCategoryCount: Int): String {
    val sortLabel = when (sort) {
        PriceSortOrder.ASCENDING -> "Low to high"
        PriceSortOrder.DESCENDING -> "High to low"
        null -> "Default order"
    }
    val categoryLabel = when (selectedCategoryCount) {
        0 -> "all categories"
        1 -> "1 category"
        else -> "$selectedCategoryCount categories"
    }
    return "$sortLabel, $categoryLabel"
}

@Composable
private fun ExpandCollapseSign(expanded: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.size(20.dp)) {
        val left = if (expanded) {
            Offset(size.width * 0.2f, size.height * 0.65f)
        } else {
            Offset(size.width * 0.2f, size.height * 0.35f)
        }
        val middle = if (expanded) {
            Offset(size.width * 0.5f, size.height * 0.35f)
        } else {
            Offset(size.width * 0.5f, size.height * 0.65f)
        }
        val right = Offset(size.width * 0.8f, left.y)
        drawLine(color, left, middle, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, middle, right, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
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
    // Chips keep the endpoint order and stay in one row to preserve grid space. The
    // fades retain the overflow cue, while the functional arrows make horizontal
    // scrolling explicit for shoppers who do not discover the swipe gesture.
    val scrollState = rememberScrollState()
    val background = MaterialTheme.colorScheme.background
    val coroutineScope = rememberCoroutineScope()
    val hasOverflow = scrollState.maxValue > 0
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasOverflow) {
            CategoryScrollButton(
                showMore = false,
                enabled = scrollState.canScrollBackward,
                onClick = {
                    coroutineScope.launch {
                        scrollState.animateToCategoryPage(scrollState.previousCategoryPage())
                    }
                },
            )
        }

        Row(
            modifier = (if (hasOverflow) Modifier.weight(1f) else Modifier.fillMaxWidth())
                .fadedOverflowEdges(scrollState, background)
                .horizontalScroll(scrollState)
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

        if (hasOverflow) {
            CategoryScrollButton(
                showMore = true,
                enabled = scrollState.canScrollForward,
                onClick = {
                    coroutineScope.launch {
                        scrollState.animateToCategoryPage(scrollState.nextCategoryPage())
                    }
                },
            )
        }
    }
}

private fun ScrollState.previousCategoryPage(): Int =
    (value - viewportSize * CATEGORY_PAGE_FRACTION).toInt().coerceAtLeast(0)

private fun ScrollState.nextCategoryPage(): Int =
    (value + viewportSize * CATEGORY_PAGE_FRACTION).toInt().coerceAtMost(maxValue)

private suspend fun ScrollState.animateToCategoryPage(target: Int) {
    animateScrollTo(
        value = target,
        animationSpec = tween(
            durationMillis = CATEGORY_SCROLL_ANIMATION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
    )
}

@Composable
private fun CategoryScrollButton(
    showMore: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .padding(horizontal = 2.dp)
            .size(48.dp)
            .semantics {
                contentDescription = if (showMore) "Show more categories" else "Show previous categories"
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(CategoryScrollIndicatorWidth)
                .height(CategoryScrollIndicatorHeight)
                .alpha(if (enabled) 1f else 0.2f)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp),
                ),
        ) {
            HorizontalChevronSign(
                pointsRight = showMore == (LocalLayoutDirection.current == LayoutDirection.Ltr),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun HorizontalChevronSign(pointsRight: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val outerX = if (pointsRight) size.width * 0.3f else size.width * 0.7f
        val middleX = if (pointsRight) size.width * 0.7f else size.width * 0.3f
        val top = Offset(outerX, size.height * 0.2f)
        val middle = Offset(middleX, size.height * 0.5f)
        val bottom = Offset(outerX, size.height * 0.8f)
        drawLine(color, top, middle, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, middle, bottom, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

private val OverflowFadeWidth = 24.dp
private val CategoryScrollIndicatorWidth = 20.dp
private val CategoryScrollIndicatorHeight = 44.dp
private const val CATEGORY_PAGE_FRACTION = 0.75f
private const val CATEGORY_SCROLL_ANIMATION_MILLIS = 280

/**
 * Fades an edge into [background] while more content lies beyond it, so a scrollable row
 * looks cut off rather than complete. Placed before `horizontalScroll` in the chain, it
 * draws in the fixed viewport instead of moving with the content, and reading the
 * snapshot-backed [ScrollState.canScrollForward] inside the draw phase redraws the fades
 * as the user scrolls, with none once the corresponding end is reached.
 */
private fun Modifier.fadedOverflowEdges(scrollState: ScrollState, background: Color): Modifier = drawWithContent {
    drawContent()
    val fadeWidth = OverflowFadeWidth.toPx()
    if (scrollState.canScrollBackward) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(background, Color.Transparent),
                startX = 0f,
                endX = fadeWidth,
            ),
            size = Size(fadeWidth, size.height),
        )
    }
    if (scrollState.canScrollForward) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, background),
                startX = size.width - fadeWidth,
                endX = size.width,
            ),
            topLeft = Offset(size.width - fadeWidth, 0f),
            size = Size(fadeWidth, size.height),
        )
    }
}

@Composable
private fun FoodItemGrid(
    items: List<FoodItem>,
    sort: PriceSortOrder?,
    selectedCategoryIds: Set<String>,
    cartQuantities: Map<String, Int>,
    onAddToCart: (FoodItem) -> Unit,
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
            FoodItemCard(
                item = item,
                quantityInCart = cartQuantities[item.id] ?: 0,
                onAddToCart = onAddToCart,
            )
        }
    }
}

@Composable
private fun FoodItemCard(
    item: FoodItem,
    quantityInCart: Int,
    onAddToCart: (FoodItem) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatPrice(item.price),
                    style = MaterialTheme.typography.titleSmall,
                    // The weight lets a large font scale wrap the price instead of
                    // squeezing the add control toward zero width. Robolectric cannot
                    // exercise this (its text measures ~1px per character), so the
                    // large-font-scale layout is part of the manual emulator check.
                    modifier = Modifier.weight(1f),
                )
                // The pill groups the plain quantity with the control that changes it,
                // matching the cart's count-and-action pattern.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(
                        color = AddSignGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(percent = 50),
                    ),
                ) {
                    if (quantityInCart > 0) {
                        Text(
                            text = quantityInCart.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // Every card shows a number in this position, so TalkBack
                            // gets the item name as well as the visible count.
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .semantics {
                                    contentDescription = "$quantityInCart of ${item.name} in the cart"
                                },
                        )
                    }
                    IconButton(
                        onClick = { onAddToCart(item) },
                        // An icon-only control needs a description, and it names the
                        // item so TalkBack does not announce thirty identical buttons.
                        modifier = Modifier.semantics {
                            contentDescription = "Add ${item.name} to the cart"
                        },
                    ) {
                        AddSign()
                    }
                }
            }
        }
    }
}

private fun cartCountDescription(count: Int): String {
    val itemLabel = if (count == 1) "item" else "items"
    return "$count $itemLabel in cart"
}

/** Green 600: reads as "add" on both the light and the dark card surface. */
private val AddSignGreen = Color(0xFF43A047)

/**
 * A green plus sign, drawn by hand because the pinned material3 no longer brings
 * material-icons along and the icon set is not worth a dependency of its own.
 */
@Composable
private fun AddSign(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.5.dp.toPx()
        drawLine(
            color = AddSignGreen,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = AddSignGreen,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
