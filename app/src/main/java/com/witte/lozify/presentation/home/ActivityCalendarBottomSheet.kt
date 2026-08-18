package com.witte.lozify.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.domain.model.Note
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * View modes for the activity calendar.
 */
enum class CalendarViewMode {
    MONTH,
    YEAR
}

/**
 * ActivityCalendarBottomSheet - 1:1 Flomo styled activity & monthly punchcard calendar sheet.
 *
 * Stage 22:
 * - Top navigation bar with close arrow (▾), segmented control ([ 月 ] [ 年 ]), and share button
 * - Weekday header: 一  二  三  四  五  六  日
 * - Grouped monthly cards with total notes, word count, active recorded days, and 7-column calendar matrix
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCalendarBottomSheet(
    allNotes: List<Note>,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }

    // Group notes by LocalDate
    val notesByDate = remember(allNotes) {
        allNotes.groupBy { it.createdAt.atZone(zoneId).toLocalDate() }
    }

    // Determine months to display (from current month back to oldest note or at least last 6 months)
    val monthsToDisplay = remember(allNotes, today) {
        val currentMonth = YearMonth.from(today)
        val oldestMonth = if (allNotes.isNotEmpty()) {
            val earliest = allNotes.minOf { it.createdAt }.atZone(zoneId).toLocalDate()
            YearMonth.from(earliest)
        } else {
            currentMonth.minusMonths(5)
        }

        val list = mutableListOf<YearMonth>()
        var cur = currentMonth
        while (!cur.isBefore(oldestMonth) || list.size < 2) {
            list.add(cur)
            cur = cur.minusMonths(1)
        }
        list
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header Bar: Close (▾) + Segmented Control [ 月 ] [ 年 ] + Share icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dismiss Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "关闭",
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Capsule Segmented Control
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF2F3F5))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedMode == CalendarViewMode.MONTH) Color.White else Color.Transparent)
                            .clickable { selectedMode = CalendarViewMode.MONTH }
                            .padding(horizontal = 22.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "月",
                            fontSize = 14.sp,
                            fontWeight = if (selectedMode == CalendarViewMode.MONTH) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedMode == CalendarViewMode.MONTH) Color(0xFF222222) else Color(0xFF777777)
                        )
                    }

                    // Year Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selectedMode == CalendarViewMode.YEAR) Color.White else Color.Transparent)
                            .clickable { selectedMode = CalendarViewMode.YEAR }
                            .padding(horizontal = 22.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "年",
                            fontSize = 14.sp,
                            fontWeight = if (selectedMode == CalendarViewMode.YEAR) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedMode == CalendarViewMode.YEAR) Color(0xFF222222) else Color(0xFF777777)
                        )
                    }
                }

                // Share Button / Actions
                IconButton(
                    onClick = { /* Share or Export punch card */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "分享",
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Weekday Header: 一  二  三  四  五  六  日 (Only in Month view)
            if (selectedMode == CalendarViewMode.MONTH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
                    weekDays.forEach { day ->
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
            }

            // Body Content
            when (selectedMode) {
                CalendarViewMode.MONTH -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        items(monthsToDisplay, key = { it.toString() }) { yearMonth ->
                            MonthlyCalendarSection(
                                yearMonth = yearMonth,
                                notesByDate = notesByDate,
                                today = today
                            )
                        }
                    }
                }
                CalendarViewMode.YEAR -> {
                    YearlyOverviewSection(
                        allNotes = allNotes,
                        notesByDate = notesByDate,
                        today = today,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * MonthlyCalendarSection - Renders a single month's header, statistics summary, and 7-column calendar matrix.
 */
@Composable
private fun MonthlyCalendarSection(
    yearMonth: YearMonth,
    notesByDate: Map<LocalDate, List<Note>>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val monthNotes = remember(yearMonth, notesByDate) {
        notesByDate.filterKeys { YearMonth.from(it) == yearMonth }.values.flatten()
    }
    val totalWords = remember(monthNotes) {
        monthNotes.sumOf { it.content.length }
    }
    val activeDays = remember(yearMonth, notesByDate) {
        notesByDate.keys.count { YearMonth.from(it) == yearMonth }
    }

    val firstDayOfMonth = remember(yearMonth) { yearMonth.atDay(1) }
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }

    // Monday (1) to Sunday (7) -> leading empty cell count = dayOfWeek.value - 1
    val leadingOffset = remember(firstDayOfMonth) {
        firstDayOfMonth.dayOfWeek.value - 1
    }

    val totalCells = leadingOffset + daysInMonth
    val numRows = (totalCells + 6) / 7

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Title & Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = yearMonth.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "${monthNotes.size} 条笔记 · $totalWords 字 · 坚持记录 $activeDays 天",
                    fontSize = 13.sp,
                    color = Color(0xFF777777)
                )
            }

            IconButton(
                onClick = { /* Share this month's stats card */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "分享月度打卡",
                    tint = Color(0xFF777777),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Calendar Grid Matrix (7 columns)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (row in 0 until numRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadingOffset + 1

                        if (dayNumber in 1..daysInMonth) {
                            val currentDate = yearMonth.atDay(dayNumber)
                            val dayNotes = notesByDate[currentDate] ?: emptyList()
                            val isToday = currentDate.isEqual(today)
                            val hasNotes = dayNotes.isNotEmpty()

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Day Number
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 12.sp,
                                    color = if (isToday) Color(0xFF00C853) else Color(0xFF8E8E93),
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )

                                // Rounded Day Block
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (hasNotes) Color(0xFFA5D6A7) else Color(0xFFF2F3F5)
                                        )
                                        .then(
                                            if (isToday) {
                                                Modifier.border(
                                                    width = 1.3.dp,
                                                    color = Color(0xFF00C853),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            } else Modifier
                                        )
                                )
                            }
                        } else {
                            // Empty placeholder cell for alignment
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * YearlyOverviewSection - Annual statistics overview and heat breakdown.
 */
@Composable
private fun YearlyOverviewSection(
    allNotes: List<Note>,
    notesByDate: Map<LocalDate, List<Note>>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val currentYear = today.year
    val yearNotes = remember(allNotes, currentYear) {
        allNotes.filter { it.createdAt.atZone(ZoneId.systemDefault()).year == currentYear }
    }
    val totalWords = remember(yearNotes) {
        yearNotes.sumOf { it.content.length }
    }
    val activeDays = remember(notesByDate, currentYear) {
        notesByDate.keys.count { it.year == currentYear }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$currentYear 年度打卡总览",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                    Text(
                        text = "共记录 ${yearNotes.size} 条笔记 · 累计 $totalWords 字 · 坚持打卡 $activeDays 天",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}
