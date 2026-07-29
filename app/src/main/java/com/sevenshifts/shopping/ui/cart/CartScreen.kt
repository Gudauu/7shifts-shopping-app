package com.sevenshifts.shopping.ui.cart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sevenshifts.shopping.domain.CartLine
import com.sevenshifts.shopping.domain.lineTotal
import com.sevenshifts.shopping.ui.components.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(state: CartUiState, onBack: () -> Unit, onDecrease: (String) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Your cart") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        if (state.lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Your cart is empty")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                LazyColumn(
                    // The weight keeps the total row pinned below the list, so what the
                    // order costs stays visible however long the cart grows.
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.lines, key = { it.item.id }) { line ->
                        CartLineRow(
                            line = line,
                            onDecrease = { onDecrease(line.item.id) },
                        )
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
        }
    }
}

// The card gives each row its own surface block, so the rows read apart from each other
// and from the background; same visual language as the catalog's item cards.
@Composable
private fun CartLineRow(line: CartLine, onDecrease: () -> Unit, modifier: Modifier = Modifier) {
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
                    QuantityDecreaseControl(line = line, onDecrease = onDecrease)
                }
            }
        }
    }
}

@Composable
private fun QuantityDecreaseControl(line: CartLine, onDecrease: () -> Unit, modifier: Modifier = Modifier) {
    val colors = removalColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.background(
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
