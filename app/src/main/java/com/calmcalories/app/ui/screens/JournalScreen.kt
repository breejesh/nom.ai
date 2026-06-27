package com.calmcalories.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcalories.app.model.MealEntry
import com.calmcalories.app.ui.components.MealRow
import com.calmcalories.app.ui.dialogs.MealDetailDialog
import com.calmcalories.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(
    meals: List<MealEntry>,
    onDeleteMeal: (Long) -> Unit,
    onUpdateMeal: (Long, String, Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var detailMeal by remember { mutableStateOf<MealEntry?>(null) }
    val filtered = remember(meals, query) {
        val q = query.lowercase()
        if (q.isBlank()) meals else meals.filter { m ->
            m.name.lowercase().contains(q) || m.foodItems.any { it.food.lowercase().contains(q) } ||
                SimpleDateFormat("MMMM d yyyy", Locale.getDefault()).format(Date(m.createdAt)).lowercase().contains(q)
        }
    }
    val grouped = remember(filtered) {
        val map = linkedMapOf<String, MutableList<MealEntry>>()
        filtered.forEach { m -> map.getOrPut(dayKey(m.createdAt)) { mutableListOf() }.add(m) }
        map
    }
    val sortedDates = grouped.keys.sortedDescending()

    Column(Modifier.fillMaxSize().background(BrandSurface)) {
        Column(Modifier.fillMaxWidth().background(BrandSurface).padding(horizontal = 20.dp).padding(top = 28.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("JOURNAL", fontSize = 15.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = 1.sp)
                Text("NOURISHMENT HISTORY", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp)
            }
            Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(16.dp)).background(DividerLight).border(0.5.dp, Divider, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextFaint, modifier = Modifier.size(18.dp))
                BasicTextField(value = query, onValueChange = { query = it }, textStyle = TextStyle(fontSize = 13.sp, color = BrandDark, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f),
                    decorationBox = { inner -> if (query.isEmpty()) Text("Search entries...", fontSize = 13.sp, color = TextFaint, fontWeight = FontWeight.Bold); inner() })
            }
        }

        if (sortedDates.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(DividerLight).border(0.5.dp, Divider, RoundedCornerShape(28.dp)).padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextFaint, modifier = Modifier.size(32.dp))
                    Text("NO RESULTS", fontSize = 14.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = 1.sp)
                    Text("Try different keywords.", fontSize = 11.sp, color = TextMuted)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                sortedDates.forEach { dateKey ->
                    val dayMeals = grouped[dateKey] ?: emptyList()
                    val dayTotal = dayMeals.sumOf { it.calories }
                    val epoch = parseEpochFromDayKey(dateKey)
                    item(key = "hdr_$dateKey") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                                Text(SimpleDateFormat("EEE", Locale.getDefault()).format(Date(epoch)).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Emerald, letterSpacing = 1.sp)
                                Text(SimpleDateFormat("d", Locale.getDefault()).format(Date(epoch)), fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = (-0.5).sp)
                            }
                            Box(Modifier.weight(1f).height(1.dp).background(Divider))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$dayTotal kcal", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = (-0.3).sp)
                                Text(SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(epoch)).uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.sp)
                            }
                        }
                    }
                    items(dayMeals, key = { "meal_${it.id}" }) { meal ->
                        MealRow(meal) { detailMeal = meal }
                        HorizontalDivider(color = DividerLight)
                    }
                }
            }
        }
    }
    detailMeal?.let { snapshot ->
        val meal = meals.find { it.id == snapshot.id } ?: snapshot
        MealDetailDialog(
            meal = meal,
            onDismiss = { detailMeal = null },
            onDelete = { onDeleteMeal(meal.id); detailMeal = null },
            onUpdate = { n, c -> onUpdateMeal(meal.id, n, c) }
        )
    }
}

private fun dayKey(e: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(e))
private fun parseEpochFromDayKey(key: String) = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(key)?.time ?: 0L
