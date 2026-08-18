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
import java.time.temporal.ChronoUnit

/**
 * ContributionHeatmap - 1:1 Flomo-styled activity punchcard grid.
 *
 * Stage 22 Flomo Alignment:
 * - Displays a 16-week window ending at the end of the current month
 * - Strictly displays the two most recent month labels ("上月" 和 "本月"，如 "7月" 与 "8月")
 * - Completely eliminates distracting early/future month labels (no 5月/6月/9月 clutter)
 * - 100% mathematically precise calendar day mapping using LocalDate (accounts for 28/29/30/31 day months & leap years)
 * - Today (e.g. Aug 18) sits naturally in the middle of the current month with an emerald outline (#00C853)
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

    // 1:1 Flomo Month Labels: Strictly show previous month and current month (e.g. "7月", "8月")
    val monthTransitions = remember(startDate, today, numWeeks) {
        val list = mutableListOf<Pair<Int, String>>()

        val prevMonth = today.minusMonths(1)
        val firstDayOfPrevMonth = YearMonth.from(prevMonth).atDay(1)
        val prevMonthCol = (ChronoUnit.DAYS.between(startDate, firstDayOfPrevMonth) / 7).toInt()
        if (prevMonthCol in 0 until numWeeks) {
            list.add(Pair(prevMonthCol, "${prevMonth.monthValue}月"))
        }

        val currMonth = today
        val firstDayOfCurrMonth = YearMonth.from(currMonth).atDay(1)
        val currMonthCol = (ChronoUnit.DAYS.between(startDate, firstDayOfCurrMonth) / 7).toInt()
        if (currMonthCol in 0 until numWeeks) {
            list.add(Pair(currMonthCol, "${currMonth.monthValue}月"))
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

            // Month Labels Row: Cleanly renders only "7月" and "8月"
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
