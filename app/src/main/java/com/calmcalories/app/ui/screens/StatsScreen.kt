package com.calmcalories.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcalories.app.model.MealEntry
import com.calmcalories.app.ui.components.DateNavBtn
import com.calmcalories.app.ui.components.StatCard
import com.calmcalories.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

private enum class ViewMode { Day, Week, Month }

@Composable
fun StatsScreen(meals: List<MealEntry>, dailyGoal: Int) {
    var viewMode by remember { mutableStateOf(ViewMode.Month) }
    var currentEpoch by remember { mutableStateOf(System.currentTimeMillis()) }

    val rangeStart = remember(viewMode, currentEpoch) { when (viewMode) { ViewMode.Day -> startOfDay(currentEpoch); ViewMode.Week -> startOfWeek(currentEpoch); ViewMode.Month -> startOfMonth(currentEpoch) } }
    val rangeEnd = remember(viewMode, currentEpoch) { when (viewMode) { ViewMode.Day -> endOfDay(currentEpoch); ViewMode.Week -> endOfWeek(currentEpoch); ViewMode.Month -> endOfMonth(currentEpoch) } }

    val mealsInRange = remember(meals, rangeStart, rangeEnd) { meals.filter { it.createdAt in rangeStart..rangeEnd } }
    val daySummaries = remember(mealsInRange) {
        val m = mutableMapOf<String, Int>()
        mealsInRange.forEach { meal -> val k = dayKey(meal.createdAt); m[k] = (m[k] ?: 0) + meal.calories }
        m
    }
    val avgCal = remember(daySummaries) { daySummaries.values.filter { it > 0 }.let { if (it.isEmpty()) 0 else it.sum() / it.size } }
    val ratio = avgCal.toFloat() / dailyGoal
    val statusColor = if (ratio > 1f) BrandRed else if (ratio >= 0.8f) Emerald else Amber
    val statusLabel = if (ratio > 1f) "Cap Reached" else if (ratio >= 0.8f) "Optimal" else "Replenishing"

    Column(Modifier.fillMaxSize().background(BrandSurface).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text("STATS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = 1.sp)
            Text("INTELLIGENCE DASHBOARD", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp)
        }

        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DividerLight).border(0.5.dp, Divider, RoundedCornerShape(16.dp)).padding(4.dp)) {
            ViewMode.entries.forEach { mode ->
                val active = viewMode == mode
                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (active) BrandCard else Color.Transparent)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewMode = mode }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(mode.name.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (active) BrandDark else TextFaint, letterSpacing = 1.sp)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            DateNavBtn("‹") {
                currentEpoch = when (viewMode) { ViewMode.Day -> currentEpoch - 86400000L; ViewMode.Week -> currentEpoch - 7 * 86400000L; ViewMode.Month -> shiftMonth(currentEpoch, -1) }
            }
            Text(when (viewMode) {
                ViewMode.Month -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(currentEpoch))
                ViewMode.Day -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(currentEpoch))
                ViewMode.Week -> "${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(rangeStart))} – ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(rangeEnd))}"
            }, fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandDark)
            val canFwd = rangeEnd < System.currentTimeMillis()
            DateNavBtn("›", enabled = canFwd) {
                currentEpoch = when (viewMode) { ViewMode.Day -> currentEpoch + 86400000L; ViewMode.Week -> currentEpoch + 7 * 86400000L; ViewMode.Month -> shiftMonth(currentEpoch, 1) }
            }
        }

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = if (viewMode == ViewMode.Day) "TOTAL KCAL" else "AVERAGE KCAL",
                value = "$avgCal kcal",
                valueColor = statusColor
            )
            StatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = "CYCLE ZONE",
                value = statusLabel,
                valueColor = statusColor
            )
        }

        when (viewMode) {
            ViewMode.Month -> MonthCalendar(currentEpoch, daySummaries, dailyGoal) { e -> currentEpoch = e; viewMode = ViewMode.Day }
            ViewMode.Week -> WeekChart(rangeStart, daySummaries, dailyGoal) { e -> currentEpoch = e; viewMode = ViewMode.Day }
            ViewMode.Day -> DayChart(mealsInRange, dailyGoal, statusColor)
        }
    }
}

@Composable
private fun MonthCalendar(currentEpoch: Long, daySummaries: Map<String, Int>, dailyGoal: Int, onDaySelect: (Long) -> Unit) {
    val daysInMonth = Calendar.getInstance().apply { timeInMillis = currentEpoch }.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = Calendar.getInstance().apply { timeInMillis = currentEpoch; set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
    val rows = (firstDow + daysInMonth + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach { d -> Text(d, fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center) }
        }
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (col in 0..6) {
                    val dayNum = row * 7 + col - firstDow + 1
                    if (dayNum < 1 || dayNum > daysInMonth) { Box(Modifier.size(36.dp)) } else {
                        val epoch = Calendar.getInstance().apply { timeInMillis = currentEpoch; set(Calendar.DAY_OF_MONTH, dayNum) }.timeInMillis
                        val kcal = daySummaries[dayKey(epoch)] ?: 0
                        val r = kcal.toFloat() / dailyGoal
                        val color = if (kcal == 0) null else if (r > 1f) BrandRed else if (r >= 0.8f) Emerald else Amber
                        Box(Modifier.size(36.dp).clip(CircleShape).background(color ?: BrandSurface).border(0.5.dp, if (color == null) Divider else Color.Transparent, CircleShape).clickable { onDaySelect(epoch) }, contentAlignment = Alignment.Center) {
                            Text("$dayNum", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (color != null) Color.White else TextFaint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekChart(rangeStart: Long, daySummaries: Map<String, Int>, dailyGoal: Int, onDaySelect: (Long) -> Unit) {
    val maxVal = (daySummaries.values.maxOrNull() ?: dailyGoal).coerceAtLeast(dailyGoal).toFloat()
    Row(Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 0..6) {
            val epoch = rangeStart + i * 86400000L
            val kcal = daySummaries[dayKey(epoch)] ?: 0
            val frac = if (kcal == 0) 0.02f else min(kcal.toFloat() / maxVal, 1f)
            val r = kcal.toFloat() / dailyGoal
            val barColor = if (kcal == 0) BrandSurface else if (r > 1f) BrandRed else if (r >= 0.8f) Emerald else Amber
            val lbl = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(epoch)).take(1)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth(0.8f).fillMaxHeight(frac).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(barColor)
                        .border(if (kcal == 0) 1.dp else 0.dp, Divider, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).clickable { onDaySelect(epoch) })
                }
                Spacer(Modifier.height(4.dp))
                Text(lbl, fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted)
            }
        }
    }
}

@Composable
private fun DayChart(mealsInRange: List<MealEntry>, dailyGoal: Int, statusColor: Color) {
    val hourMap = remember(mealsInRange) {
        val m = mutableMapOf<Int, Int>()
        mealsInRange.forEach { meal -> val h = Calendar.getInstance().apply { timeInMillis = meal.createdAt }.get(Calendar.HOUR_OF_DAY); m[h] = (m[h] ?: 0) + meal.calories }
        m
    }
    val maxH = (hourMap.values.maxOrNull() ?: (dailyGoal / 4)).coerceAtLeast(200).toFloat()
    Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
        for (h in 0..23) {
            val kcal = hourMap[h] ?: 0
            val frac = if (kcal == 0) 0.02f else min(kcal.toFloat() / maxH, 1f)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(frac).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(if (kcal == 0) BrandSurface else statusColor)
                        .border(if (kcal == 0) 1.dp else 0.dp, Divider, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                }
                Text(if (h % 4 == 0) "$h" else "", fontSize = 6.sp, fontWeight = FontWeight.Black, color = TextFaint)
            }
        }
    }
}

private fun startOfDay(e: Long) = Calendar.getInstance().apply { timeInMillis = e; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis
private fun endOfDay(e: Long) = Calendar.getInstance().apply { timeInMillis = e; set(Calendar.HOUR_OF_DAY,23); set(Calendar.MINUTE,59); set(Calendar.SECOND,59); set(Calendar.MILLISECOND,999) }.timeInMillis
private fun startOfWeek(e: Long) = Calendar.getInstance().apply { timeInMillis = e; set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY); set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis
private fun endOfWeek(e: Long) = Calendar.getInstance().apply { timeInMillis = e; set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY); set(Calendar.HOUR_OF_DAY,23); set(Calendar.MINUTE,59); set(Calendar.SECOND,59); set(Calendar.MILLISECOND,999) }.timeInMillis
private fun startOfMonth(e: Long) = Calendar.getInstance().apply { timeInMillis = e; set(Calendar.DAY_OF_MONTH,1); set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis
private fun endOfMonth(e: Long) = Calendar.getInstance().apply { timeInMillis = e; set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)); set(Calendar.HOUR_OF_DAY,23); set(Calendar.MINUTE,59); set(Calendar.SECOND,59); set(Calendar.MILLISECOND,999) }.timeInMillis
private fun shiftMonth(e: Long, delta: Int) = Calendar.getInstance().apply { timeInMillis = e; add(Calendar.MONTH, delta) }.timeInMillis
private fun dayKey(e: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(e))
