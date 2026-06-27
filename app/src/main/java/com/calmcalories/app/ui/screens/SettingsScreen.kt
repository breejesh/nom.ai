package com.calmcalories.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcalories.app.model.ActivityLevel
import com.calmcalories.app.ui.components.SCard
import com.calmcalories.app.ui.theme.*

@Composable
fun SettingsScreen(
    userName: String,
    onNameChange: (String) -> Unit,
    dailyGoal: Int,
    onGoalChange: (Int) -> Unit,
    weightKg: Float,
    onWeightChange: (Float) -> Unit,
    heightCm: Float,
    onHeightChange: (Float) -> Unit,
    activityLevel: ActivityLevel,
    onActivityChange: (ActivityLevel) -> Unit,
    age: Int,
    onAgeChange: (Int) -> Unit,
    suggestedCalories: Int?,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    var sliderVal by remember(dailyGoal) { mutableFloatStateOf(dailyGoal.toFloat()) }
    var userNameField by remember(userName) { mutableStateOf(userName) }
    var weightField by remember(weightKg) { mutableStateOf(if (weightKg > 0f) weightKg.toInt().toString() else "") }
    var heightField by remember(heightCm) { mutableStateOf(if (heightCm > 0f) heightCm.toInt().toString() else "") }
    var ageField by remember(age) { mutableStateOf(if (age > 0) age.toString() else "") }
    val ctx = LocalContext.current

    Column(
        Modifier.fillMaxSize().background(BrandSurface).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text("SETTINGS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = 1.sp)
            Text("YOUR PROFILE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp)
        }

        // ── 1. Daily Target (First) ──
        SCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("DAILY CALORIE TARGET", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("${sliderVal.toInt()} kcal", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = BrandDark, letterSpacing = (-0.5).sp)
                }
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    onValueChangeFinished = { onGoalChange(sliderVal.toInt()) },
                    valueRange = 1200f..4000f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = BrandDark,
                        inactiveTrackColor = DividerLight,
                        thumbColor = Amber,
                    ),
                )
            }
        }

        // ── 2. Name (Second) ──
        SCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("YOUR NAME", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = TextMuted, letterSpacing = 1.5.sp)
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrandSurface)
                        .border(0.5.dp, Divider, RoundedCornerShape(12.dp)).padding(12.dp),
                ) {
                    BasicTextField(
                        value = userNameField,
                        onValueChange = { userNameField = it; onNameChange(it) },
                        textStyle = TextStyle(fontSize = 15.sp, color = BrandDark, fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (userNameField.isEmpty()) Text("e.g. Alex", fontSize = 15.sp, color = TextFaint)
                            inner()
                        },
                    )
                }
            }
        }

        // ── 2.5 Appearance (Appearance Toggle Segment) ──
        SCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("APPEARANCE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = TextMuted, letterSpacing = 1.5.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandSurface)
                        .border(0.5.dp, Divider, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isDarkTheme) BrandCard else Color.Transparent)
                            .then(if (!isDarkTheme) Modifier.border(0.5.dp, Divider, RoundedCornerShape(10.dp)) else Modifier)
                            .clickable { onDarkThemeChange(false) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = if (!isDarkTheme) Amber else TextMuted, modifier = Modifier.size(14.dp))
                            Text("DAY", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = if (!isDarkTheme) BrandDark else TextMuted, letterSpacing = 0.5.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkTheme) BrandCard else Color.Transparent)
                            .then(if (isDarkTheme) Modifier.border(0.5.dp, Divider, RoundedCornerShape(10.dp)) else Modifier)
                            .clickable { onDarkThemeChange(true) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, tint = if (isDarkTheme) Amber else TextMuted, modifier = Modifier.size(14.dp))
                            Text("NIGHT", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) BrandDark else TextMuted, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }

        // ── 3. Body Metrics Advisor (Third) ──
        SCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("WEIGHT LOSS ADVISOR", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp)
                Text(
                    "Enter details to calculate weight-loss targets based on the scientific Mifflin-St Jeor equation.",
                    fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium, lineHeight = 16.sp,
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("WEIGHT (KG)", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.5.sp)
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrandSurface)
                                .border(0.5.dp, Divider, RoundedCornerShape(12.dp)).padding(10.dp),
                        ) {
                            BasicTextField(
                                value = weightField,
                                onValueChange = { v ->
                                    weightField = v.filter { it.isDigit() || it == '.' }
                                    weightField.toFloatOrNull()?.let { onWeightChange(it) }
                                },
                                textStyle = TextStyle(fontSize = 13.sp, color = BrandDark, fontWeight = FontWeight.Bold),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (weightField.isEmpty()) Text("75", fontSize = 13.sp, color = TextFaint)
                                    inner()
                                },
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("HEIGHT (CM)", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.5.sp)
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrandSurface)
                                .border(0.5.dp, Divider, RoundedCornerShape(12.dp)).padding(10.dp),
                        ) {
                            BasicTextField(
                                value = heightField,
                                onValueChange = { v ->
                                    heightField = v.filter { it.isDigit() || it == '.' }
                                    heightField.toFloatOrNull()?.let { onHeightChange(it) }
                                },
                                textStyle = TextStyle(fontSize = 13.sp, color = BrandDark, fontWeight = FontWeight.Bold),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (heightField.isEmpty()) Text("170", fontSize = 13.sp, color = TextFaint)
                                    inner()
                                },
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AGE (YRS)", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.5.sp)
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrandSurface)
                                .border(0.5.dp, Divider, RoundedCornerShape(12.dp)).padding(10.dp),
                        ) {
                            BasicTextField(
                                value = ageField,
                                onValueChange = { v ->
                                    ageField = v.filter { it.isDigit() }
                                    ageField.toIntOrNull()?.let { onAgeChange(it) }
                                },
                                textStyle = TextStyle(fontSize = 13.sp, color = BrandDark, fontWeight = FontWeight.Bold),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (ageField.isEmpty()) Text("28", fontSize = 13.sp, color = TextFaint)
                                    inner()
                                },
                            )
                        }
                    }
                }

                // Compact Tab activity level selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ACTIVITY LEVEL", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = TextMuted, letterSpacing = 1.5.sp)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrandSurface)
                            .border(0.5.dp, Divider, RoundedCornerShape(12.dp)).padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ActivityLevel.entries.forEach { level ->
                            val active = activityLevel == level
                            val shortLabel = when (level) {
                                ActivityLevel.Sedentary -> "SED"
                                ActivityLevel.Light -> "LGT"
                                ActivityLevel.Moderate -> "MOD"
                                ActivityLevel.Active -> "ACT"
                                ActivityLevel.VeryActive -> "V-ACT"
                            }
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (active) BrandCard else Color.Transparent)
                                    .then(if (active) Modifier.border(0.5.dp, Divider, RoundedCornerShape(10.dp)) else Modifier)
                                    .clickable { onActivityChange(level) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    shortLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (active) BrandDark else TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    val activeDesc = when (activityLevel) {
                        ActivityLevel.Sedentary -> "Desk job, little to no exercise (+150–300 kcal/day)"
                        ActivityLevel.Light -> "Light exercise 1–3 days/wk (+300–600 kcal/day)"
                        ActivityLevel.Moderate -> "Moderate exercise 3–5 days/wk (+600–900 kcal/day)"
                        ActivityLevel.Active -> "Heavy exercise 6–7 days/wk (+900–1300 kcal/day)"
                        ActivityLevel.VeryActive -> "Intense daily exercise or physical job (+1300–1800 kcal/day)"
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandSurface)
                            .border(0.5.dp, Divider, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(activityLevel.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                            Text(activeDesc, fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }

                // ── Suggestion result ──
                if (suggestedCalories != null) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Emerald.copy(alpha = 0.08f))
                            .border(0.5.dp, Emerald.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("SUGGESTED FOR WEIGHT LOSS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Emerald, letterSpacing = 1.5.sp)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("$suggestedCalories", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Emerald, letterSpacing = (-1).sp)
                            Text("kcal / day", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text("Suggested target based on Mifflin-St Jeor with a safe 500 kcal daily deficit.", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Emerald).clickable {
                                    onGoalChange(suggestedCalories)
                                    android.widget.Toast.makeText(ctx, "Goal updated to $suggestedCalories kcal", android.widget.Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("APPLY SUGGESTION", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp)
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Text("VERSION 1.0.0", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextFaint, letterSpacing = 2.sp)
        }
    }
}


