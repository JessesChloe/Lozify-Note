package com.witte.lozify.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * ContributionHeatmap - 1:1 Flomo-styled activity punchcard grid.
 *
 * Stage 22:
 * - 7 rows (Monday to Sunday)
 * - 16 columns (~4 months)
 * - Neat 10.5dp blocks with 3.5dp spacing and 2.5dp corner radius
 * - Soft neutral empty cells (#F2F3F5)
 * - Gentle mint green active cells (#A5D6A7)
 * - Today cell marked with a 1.2dp emerald outline (#00C853)
 * - Month labels ("7月", "8月") aligned at the bottom
 */
@Composable
fun ContributionHeatmap(
    dailyCounts: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    numWeeks: Int = 16,
    onClick: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val blockSize = 10.5.dp
    val spacing = 3.5.dp
    val cornerRadius = 2.5.dp
    val numDaysInWeek = 7

    val blockSizePx = with(density) { blockSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    val totalGridWidth = (blockSize * numWeeks) + (spacing * (numWeeks - 1))
    val totalGridHeight = (blockSize * numDaysInWeek) + (spacing * (numDaysInWeek - 1))

    val today = remember { LocalDate.now() }

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

    // Auto-scroll to the rightmost edge to show today
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState)
        ) {
            Column {
                Canvas(
                    modifier = Modifier.size(width = totalGridWidth, height = totalGridHeight)
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
                        .size(width = totalGridWidth, height = 16.dp)
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
}
