package com.mehmetdem.dil.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val DilOuterPadding = 16.dp
val DilCardRadius = 16.dp
val DilControlHeight = 52.dp
val DilMaxContentWidth = 520.dp

@Composable
fun DilResponsiveContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = DilMaxContentWidth),
        ) {
            content()
        }
    }
}

@Composable
fun DilSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        action?.invoke(this)
    }
}

@Composable
fun DilElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DilBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
fun DilStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * A density-independent progress rail. Text glyphs such as "●━━○" shift between
 * Android fonts; real layout primitives keep every step aligned on every device.
 */
@Composable
fun DilProgressDots(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    val safeTotal = totalSteps.coerceAtLeast(1)
    val safeCurrent = currentStep.coerceIn(0, safeTotal)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(safeTotal) { index ->
            val completed = index < safeCurrent
            Box(
                Modifier
                    .size(if (completed) 10.dp else 9.dp)
                    .background(
                        color = if (completed) DilTeal else DilBorder,
                        shape = CircleShape,
                    ),
            )
            if (index < safeTotal - 1) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (index < safeCurrent - 1) DilTeal else DilBorder),
                )
            }
        }
    }
}

@Composable
fun DilVoiceBars(
    modifier: Modifier = Modifier,
    color: Color = DilTeal,
) {
    val heights = listOf(10.dp, 18.dp, 27.dp, 16.dp, 32.dp, 22.dp, 14.dp, 25.dp, 12.dp)
    Row(
        modifier = modifier.height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        heights.forEach { barHeight ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .background(color, CircleShape),
            )
        }
    }
}
