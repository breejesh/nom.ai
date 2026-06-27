package com.calmcalories.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcalories.app.model.MealEntry
import com.calmcalories.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MealRow(meal: MealEntry, onClick: () -> Unit) {
    val t = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(meal.createdAt))
    val a = SimpleDateFormat("a", Locale.getDefault()).format(Date(meal.createdAt)).uppercase()
    val totalProtein = remember(meal) { meal.foodItems.sumOf { it.proteinGrams } }
    val totalCarbs = remember(meal) { meal.foodItems.sumOf { it.carbsGrams } }
    val totalFat = remember(meal) { meal.foodItems.sumOf { it.fatGrams } }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(52.dp)) {
            Text(t, fontSize = 13.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = (-0.5).sp)
            Text(a, fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextFaint, letterSpacing = 1.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(meal.name.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meal.foodItems.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("P: ${totalProtein}g", fontSize = 9.5.sp, color = Emerald, fontWeight = FontWeight.Bold)
                    Text("•", fontSize = 9.5.sp, color = TextFaint)
                    Text("C: ${totalCarbs}g", fontSize = 9.5.sp, color = Amber, fontWeight = FontWeight.Bold)
                    Text("•", fontSize = 9.5.sp, color = TextFaint)
                    Text("F: ${totalFat}g", fontSize = 9.5.sp, color = BrandRed, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
            Text("${meal.calories}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = (-0.5).sp)
            Text("KCAL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextFaint, letterSpacing = 1.sp)
        }
    }
}
