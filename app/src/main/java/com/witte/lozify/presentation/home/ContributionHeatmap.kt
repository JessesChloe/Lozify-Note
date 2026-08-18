package com.witte.lozify.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * ContributionHeatmap - 1:1 Flomo-styled full-width activity punchcard grid.
 *
 * Stage 22 Polish:
 * - Automatically fills 100% of the drawer width using BoxWithConstraints
 * - 7 rows (Monday to Sunday)
 * - 18 responsive columns (~4.5 months)
 * - Dynamic block sizing to span the complete drawer width with 3dp spacing
 * - Soft neutral empty cells (#F2F3F5)
 * - Gentle mint green active cells (#A5D6A7)
 * - Today cell marked with emerald stroke outline (#00C853)
 * - Month labels ("7月", "8月") aligned at the bottom
 */
@Composable
fun ContributionHeatmap(
    dailyCounts: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    numWeeks: Int = 18,
    onClick: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val today = remember { LocalDate.now() }
    val numDaysInWeek = 7

    // Offset for Monday (0) to Sunday (6)
    val dayOfWeekOffset = remember(today) {
        when (today.dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
            null -> 0
        }
    }

    // Start on Monday of the first week
    val startDate = remember(today, dayOfWeekOffset, numWeeks) {
        today.minusWeeks((numWeeks - 1).toLong()).minusDays(dayOfWeekOffset.toLong())
    }

    // Identify month label positions
    val monthLabels = remember(startDate, numWeeks) {
        val list = mutableListOf<Pair<Int, String>>() // (columnIndex, "X月")
        var lastMonth = -1
        for (col in 0 until numWeeks) {
            val colMonday = startDate.plusDays((col * 7).toLong())
            val month = colMonday.monthValue
            if (month != lastMonth && col < numWeeks - 1) {
                list.add(Pair(col, "${month}月"))
                lastMonth = month
            }
        }
        list
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        val availableWidth = maxWidth
        val spacing = 3.dp
        val totalSpacing = spacing * (numWeeks - 1)
        val blockSize = ((availableWidth - totalSpacing) / numWeeks).coerceAtLeast(8.dp)
        val cornerRadius = (blockSize * 0.22f).coerceAtLeast(2.dp)

        val blockSizePx = with(density) { blockSize.toPx() }
        val spacingPx = with(density) { spacing.toPx() }
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }

        val totalGridHeight = (blockSize * numDaysInWeek) + (spacing * (numDaysInWeek - 1))

        Column(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalGridHeight)
            ) {
                for (col in 0 until numWeeks) {
                    for (row in 0 until numDaysInWeek) {
                        val daysToAdd = (col * 7 + row).toLong()
                        val currentDate = startDate.plusDays(daysToAdd)

                        val x = col * (blockSizePx + spacingPx)
                        val y = row * (blockSizePx + spacingPx)

                        val isFuture = currentDate.isAfter(today)
                        val isToday = currentDate.isEqual(today)
                        val count = if (isFuture) 0 else (dailyCounts[currentDate] ?: 0)

                        // Background color
                        val cellColor = if (count > 0) {
                            Color(0xFFA5D6A7) // Flomo Soft Mint Green
                        } else {
                            Color(0xFFF2F3F5) // Flomo Clean Light Gray
                        }

                        drawRoundRect(
                            color = cellColor,
                            topLeft = Offset(x, y),
                            size = Size(blockSizePx, blockSizePx),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )

                        // Today special emerald stroke outline
                        if (isToday) {
                            drawRoundRect(
                                color = Color(0xFF00C853),
                                topLeft = Offset(x, y),
                                size = Size(blockSizePx, blockSizePx),
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                style = Stroke(width = 1.3f * density.density)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Month Labels Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            ) {
                monthLabels.forEach { (colIndex, label) ->
                    val leftOffset = (blockSize + spacing) * colIndex
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = Color(0xFFB0B0B0),
                        modifier = Modifier.padding(start = leftOffset)
                    )
                }
            }
        }
    }
}
