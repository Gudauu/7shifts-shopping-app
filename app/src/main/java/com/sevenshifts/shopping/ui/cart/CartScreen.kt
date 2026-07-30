package com.sevenshifts.shopping.ui.cart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.PurchaseFailure
import com.sevenshifts.shopping.domain.PurchaseItemFailure
import com.sevenshifts.shopping.domain.PurchaseItemFailureReason
import com.sevenshifts.shopping.domain.lineTotal
import com.sevenshifts.shopping.ui.components.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    state: CartUiState,
    onBack: () -> Unit,
    onDecrease: (String) -> Unit,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Your cart") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" },
                    ) {
                        BackArrowSign(color = MaterialTheme.colorScheme.onSurface)
                    }
                },
            )
        },
    ) { contentPadding ->
        val success = state.purchase as? PurchaseUiState.Succeeded
        if (success != null) {
            PurchaseSuccessContent(
                success = success,
                onContinueShopping = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        } else {
            CartContents(
                state = state,
                onDecrease = onDecrease,
                onPurchase = onPurchase,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

@Composable
private fun CartContents(
    state: CartUiState,
    onDecrease: (String) -> Unit,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val purchaseInFlight = state.purchase is PurchaseUiState.InFlight
    Column(modifier = modifier) {
        if (state.lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Your cart is empty")
            }
        } else {
            LazyColumn(
                // The weight keeps the total and purchase action pinned below the list.
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = state.lines, key = { it.item.id }) { line ->
                    CartLineRow(
                        line = line,
                        decreaseEnabled = !purchaseInFlight,
                        onDecrease = { onDecrease(line.item.id) },
                    )
                }
            }
        }

        HorizontalDivider()
        if (state.lines.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatPrice(state.orderTotal),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        val failedState = state.purchase as? PurchaseUiState.Failed
        if (failedState != null) {
            PurchaseFailureContent(
                failure = failedState.failure,
                lines = state.lines,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        val purchaseEnabled = state.lines.isNotEmpty() && when (val purchase = state.purchase) {
            PurchaseUiState.Idle -> true

            is PurchaseUiState.Failed -> purchase.failure.retryable

            PurchaseUiState.InFlight,
            is PurchaseUiState.Succeeded,
            -> false
        }
        Button(
            onClick = onPurchase,
            enabled = purchaseEnabled,
            colors = if (purchaseInFlight) {
                ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            when (state.purchase) {
                PurchaseUiState.InFlight -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Purchasing...")
                }

                is PurchaseUiState.Failed -> Text(purchaseFailureAction(state.purchase.failure))

                PurchaseUiState.Idle,
                is PurchaseUiState.Succeeded,
                -> Text("Purchase")
            }
        }
    }
}

@Composable
private fun PurchaseFailureContent(failure: PurchaseFailure, lines: List<CartLine>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Purchase failed", style = MaterialTheme.typography.titleSmall)
            Text(purchaseFailureMessage(failure), style = MaterialTheme.typography.bodyMedium)
            if (failure is PurchaseFailure.ItemsRequireAttention) {
                failure.items.forEach { itemFailure ->
                    Text(
                        text = purchaseItemFailureMessage(itemFailure, lines),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun purchaseFailureMessage(failure: PurchaseFailure): String = when (failure) {
    is PurchaseFailure.ItemsRequireAttention ->
        "Some items need attention before another purchase. Your cart is unchanged."

    is PurchaseFailure.InvalidRequest ->
        "This cart needs to be changed before another purchase."

    PurchaseFailure.TemporarilyUnavailable ->
        "Purchases are temporarily unavailable. Your cart is unchanged."

    is PurchaseFailure.PurchaseNotCompleted -> if (failure.retryable) {
        "The purchase was not completed. Your cart is unchanged."
    } else {
        "The purchase was not completed and cannot be retried. Your cart is unchanged."
    }

    PurchaseFailure.UnresolvedOutcome ->
        "We could not confirm the purchase outcome. Your cart is unchanged."

    PurchaseFailure.ClientStateConflict ->
        "We could not safely retry this purchase. Your cart is unchanged."
}

private fun purchaseFailureAction(failure: PurchaseFailure): String = if (failure.retryable) {
    "Retry"
} else {
    when (failure) {
        is PurchaseFailure.ItemsRequireAttention,
        is PurchaseFailure.InvalidRequest,
        -> "Update cart to continue"

        PurchaseFailure.UnresolvedOutcome -> "Outcome unresolved"

        is PurchaseFailure.PurchaseNotCompleted,
        PurchaseFailure.ClientStateConflict,
        PurchaseFailure.TemporarilyUnavailable,
        -> "Purchase unavailable"
    }
}

private fun purchaseItemFailureMessage(failure: PurchaseItemFailure, lines: List<CartLine>): String {
    val itemName = lines.firstOrNull { it.item.id == failure.foodItemId }?.item?.name ?: failure.foodItemId
    return when (failure.reason) {
        PurchaseItemFailureReason.NOT_FOUND -> "$itemName is no longer in the catalog."

        PurchaseItemFailureReason.UNAVAILABLE -> "$itemName is no longer available."

        PurchaseItemFailureReason.QUANTITY_UNAVAILABLE -> failure.availableQuantity?.let { available ->
            "$itemName has only $available available."
        } ?: "$itemName does not have the requested quantity available."

        PurchaseItemFailureReason.PRICE_CHANGED -> when {
            failure.expectedUnitPrice != null && failure.currentUnitPrice != null ->
                "$itemName changed from ${formatPrice(failure.expectedUnitPrice)} to " +
                    "${formatPrice(failure.currentUnitPrice)}."

            failure.currentUnitPrice != null ->
                "$itemName now costs ${formatPrice(failure.currentUnitPrice)}."

            else -> "$itemName has a new price."
        }
    }
}

@Composable
private fun PurchaseSuccessContent(
    success: PurchaseUiState.Succeeded,
    onContinueShopping: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PurchaseCompleteSign()
        Text(
            text = "Purchase complete",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = "Your ${formatPrice(success.total)} purchase is confirmed.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onContinueShopping,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Text("Continue shopping")
        }
    }
}

@Composable
private fun PurchaseCompleteSign(modifier: Modifier = Modifier) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val check = MaterialTheme.colorScheme.onPrimaryContainer
    Canvas(modifier = modifier.size(64.dp)) {
        drawCircle(color = container)
        val path = Path().apply {
            moveTo(size.width * 0.27f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.68f)
            lineTo(size.width * 0.75f, size.height * 0.34f)
        }
        drawPath(
            path = path,
            color = check,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

// The card gives each row its own surface block, so the rows read apart from each other
// and from the background; same visual language as the catalog's item cards.
@Composable
private fun CartLineRow(
    line: CartLine,
    decreaseEnabled: Boolean,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val imagePlaceholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            AsyncImage(
                model = line.item.imageUrl,
                // The name is announced by the Text beside it; describing the image too
                // would make TalkBack read every row twice.
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                placeholder = imagePlaceholder,
                error = imagePlaceholder,
                // Shown when imageUrl is null; a missing image must not hide the row.
                fallback = imagePlaceholder,
            )
            Column(
                // The image and this column are the row's only top-level children, so
                // the name, prices, and quantity can use the remaining width without
                // squeezing one another at large font scales.
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = line.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${formatPrice(line.item.price)} / ${formatPrice(line.lineTotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription =
                                    "${formatPrice(line.item.price)} each, ${formatPrice(line.lineTotal)} line total"
                            },
                    )
                    QuantityDecreaseControl(
                        line = line,
                        enabled = decreaseEnabled,
                        onDecrease = onDecrease,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityDecreaseControl(
    line: CartLine,
    enabled: Boolean,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = removalColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .background(
                color = colors.container,
                shape = RoundedCornerShape(percent = 50),
            ),
    ) {
        Text(
            text = line.quantity.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = colors.content,
            modifier = Modifier
                .padding(start = 12.dp)
                .semantics {
                    contentDescription = "${line.quantity} of ${line.item.name} in the cart"
                },
        )
        IconButton(
            onClick = onDecrease,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = if (line.quantity == 1) {
                    "Remove ${line.item.name} from the cart"
                } else {
                    "Decrease ${line.item.name} quantity"
                }
            },
        ) {
            if (line.quantity == 1) {
                TrashSign(color = colors.content)
            } else {
                DecreaseSign(color = colors.content)
            }
        }
    }
}

private data class RemovalColors(val container: Color, val content: Color)

/** Soft red tones that stay clear without reading as a high-severity alert. */
@Composable
private fun removalColors(): RemovalColors = if (isSystemInDarkTheme()) {
    RemovalColors(container = Color(0xFF3D2B2A), content = Color(0xFFFFB4AD))
} else {
    RemovalColors(container = Color(0xFFF9ECEA), content = Color(0xFFB6453D))
}

/** A navigation arrow drawn in-app so a full icon dependency is not needed. */
@Composable
private fun BackArrowSign(color: Color, modifier: Modifier = Modifier) {
    val pointsLeft = LocalLayoutDirection.current == LayoutDirection.Ltr
    Canvas(modifier = modifier.size(24.dp)) {
        val arrowPointX = if (pointsLeft) size.width * 0.2f else size.width * 0.8f
        val bendX = if (pointsLeft) size.width * 0.55f else size.width * 0.45f
        val tailX = if (pointsLeft) size.width * 0.88f else size.width * 0.12f
        val arrowPoint = Offset(arrowPointX, size.height * 0.5f)
        drawLine(
            color = color,
            start = Offset(bendX, size.height * 0.18f),
            end = arrowPoint,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = arrowPoint,
            end = Offset(bendX, size.height * 0.82f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = arrowPoint,
            end = Offset(tailX, size.height * 0.5f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/** A minus sign drawn in-app so a full icon dependency is not needed for one control. */
@Composable
private fun DecreaseSign(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/** A bin sign marks that decreasing a quantity of one removes the entire cart row. */
@Composable
private fun TrashSign(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val path = Path().apply {
            // Lid and handle.
            moveTo(size.width * 0.2f, size.height * 0.28f)
            lineTo(size.width * 0.8f, size.height * 0.28f)
            moveTo(size.width * 0.36f, size.height * 0.28f)
            lineTo(size.width * 0.4f, size.height * 0.14f)
            lineTo(size.width * 0.6f, size.height * 0.14f)
            lineTo(size.width * 0.64f, size.height * 0.28f)
            // Empty body: no inset lines keeps the small sign legible.
            moveTo(size.width * 0.26f, size.height * 0.38f)
            lineTo(size.width * 0.32f, size.height * 0.9f)
            lineTo(size.width * 0.68f, size.height * 0.9f)
            lineTo(size.width * 0.74f, size.height * 0.38f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
