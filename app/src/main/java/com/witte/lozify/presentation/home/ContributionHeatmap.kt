package com.witte.lozify.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * ContributionHeatmap - GitHub-style activity punchcard / contribution graph.
 *
 * Stage 14: Visual Enhancement
 *
 * Features:
 * - 52 weeks (1 year) horizontally scrollable grid
 * - 7 rows (Sunday to Saturday)
 * - 12dp rounded blocks with 4dp spacing and 2dp corner radius
 * - 5-level color gradation based on note count
 * - Auto-scrolls to the rightmost edge to show recent activity
 *
 * @param dailyCounts Map of date to note creation count
 * @param modifier Composable modifier
 * @param numWeeks Number of weeks to display (default 52 weeks)
 */
@Composable
fun ContributionHeatmap(
    dailyCounts: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    numWeeks: Int = 52
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val blockSize = 12.dp
    val spacing = 4.dp
    val cornerRadius = 2.dp
    val numDaysInWeek = 7

    val blockSizePx = with(density) { blockSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    val totalWidth = (blockSize * numWeeks) + (spacing * (numWeeks - 1))
    val totalHeight = (blockSize * numDaysInWeek) + (spacing * (numDaysInWeek - 1))

    val today = remember { LocalDate.now() }
    val dayOfWeekOffset = remember(today) {
        when (today.dayOfWeek) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
            null -> 0
        }
    }

    val startDate = remember(today, dayOfWeekOffset, numWeeks) {
        today.minusWeeks((numWeeks - 1).toLong()).minusDays(dayOfWeekOffset.toLong())
    }

    // Auto-scroll to the rightmost edge to show current date on initial render
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier.horizontalScroll(scrollState)
    ) {
        Box {
            Canvas(
                modifier = Modifier.size(width = totalWidth, height = totalHeight)
            ) {
                for (col in 0 until numWeeks) {
                    for (row in 0 until numDaysInWeek) {
                        val daysToAdd = (col * 7 + row).toLong()
                        val currentDate = startDate.plusDays(daysToAdd)

                        // Do not draw future days beyond today
                        if (currentDate.isAfter(today)) {
                            continue
                        }

                        val count = dailyCounts[currentDate] ?: 0
                        val color = getHeatmapColor(count)

                        val x = col * (blockSizePx + spacingPx)
                        val y = row * (blockSizePx + spacingPx)

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(blockSizePx, blockSizePx),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Maps note count to GitHub-style green gradation colors.
 *
 * - 0: Light gray (#EBEDF0)
 * - 1-2: Light green (#9BE9A8)
 * - 3-5: Medium green (#40C463)
 * - 6-8: Dark green (#30A14E)
 * - 9+: Deep green (#216E39)
 */
fun getHeatmapColor(count: Int): Color {
    return when {
        count <= 0 -> Color(0xFFEBEDF0)
        count in 1..2 -> Color(0xFF9BE9A8)
        count in 3..5 -> Color(0xFF40C463)
        count in 6..8 -> Color(0xFF30A14E)
        else -> Color(0xFF216E39)
    }
}
