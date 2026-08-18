package com.witte.lozify.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

/**
 * ContributionHeatmap - 1:1 Flomo-styled activity punchcard grid.
 *
 * Stage 22 Flomo Calendar Alignment:
 * - Displays a 16-week window ending at the END of the CURRENT MONTH (complete month view)
 * - 7 rows (Monday to Sunday, row 0 = Mon ... row 6 = Sun)
 * - 16 columns spanning ~4 months (e.g. May, June, July, August)
 * - Today (e.g. Aug 18) is highlighted in the current month's week with an emerald outline
 * - Future days in the current month are shown as clean light gray blocks
 * - Month labels ("7月", "8月") precisely aligned under the column where each month begins
 */
@Composable
fun ContributionHeatmap(
    dailyCounts: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    numWeeks: Int = 16,
    onClick: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val today = remember { LocalDate.now() }
    val numDaysInWeek = 7

    // End date: Sunday of the week containing the end of the current month
    val endOfGridDate = remember(today) {
        val endOfMonth = YearMonth.from(today).atEndOfMonth()
        val daysUntilSunday = (7 - endOfMonth.dayOfWeek.value) % 7
        endOfMonth.plusDays(daysUntilSunday.toLong())
    }

    // Start date: Monday of the week (numWeeks - 1) weeks before endOfGridDate
    val startDate = remember(endOfGridDate, numWeeks) {
        endOfGridDate.minusWeeks((numWeeks - 1).toLong()).minusDays(6)
    }

    // Identify month label positions
    val monthTransitions = remember(startDate, numWeeks) {
        val rawList = mutableListOf<Pair<Int, String>>()
        var lastMonth = -1
        for (col in 0 until numWeeks) {
            val colMonday = startDate.plusDays((col * 7).toLong())
            val colSunday = colMonday.plusDays(6)
            
            // Check if month changes in this week or if week contains day 1 of a month
            val month = if (colMonday.monthValue != colSunday.monthValue) {
                colSunday.monthValue
            } else {
                colMonday.monthValue
            }

            if (month != lastMonth) {
                rawList.add(Pair(col, "${month}月"))
                lastMonth = month
            }
        }
        // Keep prominent spaced month labels (e.g. at least 3 weeks apart)
        val result = mutableListOf<Pair<Int, String>>()
        for (item in rawList) {
            if (result.isEmpty()) {
                if (item.first >= 1) {
                    result.add(item)
                }
            } else {
                if (item.first - result.last().first >= 3) {
                    result.add(item)
                }
            }
        }
        result
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        val availableWidth = maxWidth
        val spacing = 3.5.dp
        val totalSpacing = spacing * (numWeeks - 1)
        val blockSize = ((availableWidth - totalSpacing) / numWeeks).coerceAtLeast(10.dp)
        val cornerRadius = 3.dp

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

                        // Today emerald outline stroke
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

            Spacer(modifier = Modifier.height(6.dp))

            // Month Labels Row (precisely aligned without clipping)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            ) {
                monthTransitions.forEach { (colIndex, label) ->
                    val colCenterX = (blockSize + spacing) * colIndex + (blockSize / 2)
                    val labelWidth = 32.dp
                    val labelOffset = (colCenterX - (labelWidth / 2))
                        .coerceIn(0.dp, (availableWidth - labelWidth).coerceAtLeast(0.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .offset(x = labelOffset)
                            .width(labelWidth)
                    )
                }
            }
        }
    }
}
