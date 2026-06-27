package com.calmcalories.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.calmcalories.app.model.MealEntry
import com.calmcalories.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MealDetailDialog(
    meal: MealEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (String, Int) -> Unit
) {
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(meal.createdAt))
    var isEditing by remember(meal) { mutableStateOf(false) }
    var editName by remember(meal) { mutableStateOf(meal.name) }
    var editCalories by remember(meal) { mutableStateOf(meal.calories.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(BrandCard).border(0.5.dp, Divider, RoundedCornerShape(28.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isEditing) {
                // EDIT MODE
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("EDIT MEAL ENTRY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = 1.sp)
                    Text("Update the meal name and calorie values.", fontSize = 11.sp, color = TextMuted)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MEAL NAME", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrandSurface).border(0.5.dp, Divider, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        BasicTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            textStyle = TextStyle(fontSize = 14.sp, color = BrandDark, fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CALORIES (KCAL)", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrandSurface).border(0.5.dp, Divider, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        BasicTextField(
                            value = editCalories,
                            onValueChange = { editCalories = it.filter { c -> c.isDigit() } },
                            textStyle = TextStyle(fontSize = 14.sp, color = BrandDark, fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(BrandSurface).border(0.5.dp, Divider, RoundedCornerShape(12.dp)).clickable { isEditing = false }, contentAlignment = Alignment.Center) {
                        Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSecondary, letterSpacing = 0.5.sp)
                    }
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(Emerald).clickable {
                        onUpdate(editName, editCalories.toIntOrNull() ?: meal.calories)
                        isEditing = false
                        onDismiss()
                    }, contentAlignment = Alignment.Center) {
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                    }
                }
            } else {
                // VIEW MODE
                val totalProtein = meal.foodItems.sumOf { it.proteinGrams }
                val totalCarbs = meal.foodItems.sumOf { it.carbsGrams }
                val totalFat = meal.foodItems.sumOf { it.fatGrams }
                val totalSugar = meal.foodItems.sumOf { it.sugarGrams }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(meal.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("P: ${totalProtein}g", fontSize = 11.sp, color = Emerald, fontWeight = FontWeight.Bold)
                            Text("•", fontSize = 11.sp, color = TextFaint)
                            Text("C: ${totalCarbs}g (${totalSugar}g sug)", fontSize = 11.sp, color = Amber, fontWeight = FontWeight.Bold)
                            Text("•", fontSize = 11.sp, color = TextFaint)
                            Text("F: ${totalFat}g", fontSize = 11.sp, color = BrandRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(timeStr, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${meal.calories}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = (-0.5).sp)
                        Text("KCAL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextFaint, letterSpacing = 1.sp)
                    }
                }
                if (meal.foodItems.isNotEmpty()) {
                    HorizontalDivider(color = Divider)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("NOURISHMENT BREAKDOWN", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(4.dp))
                        meal.foodItems.forEach { item ->
                            val itemMacroStr = "P: ${item.proteinGrams}g  •  C: ${item.carbsGrams}g (${item.sugarGrams}g sugar)  •  F: ${item.fatGrams}g"
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(item.food.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                                    Text(
                                        if (item.quantity.isNotBlank()) "${item.quantity}  •  $itemMacroStr" else itemMacroStr,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                                Text("${item.calories} kcal", fontSize = 12.sp, fontWeight = FontWeight.Black, color = BrandSecondary)
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(BrandSurface).border(0.5.dp, Divider, RoundedCornerShape(12.dp)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                        Text("Close", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondary, letterSpacing = 0.5.sp)
                    }
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(BrandSurface).border(0.5.dp, Divider, RoundedCornerShape(12.dp)).clickable { isEditing = true }, contentAlignment = Alignment.Center) {
                        Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandSecondary, letterSpacing = 0.5.sp)
                    }
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(BrandRed.copy(alpha = 0.08f)).border(0.5.dp, BrandRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                        Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandRed, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}
