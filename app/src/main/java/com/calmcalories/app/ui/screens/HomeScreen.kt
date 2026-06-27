package com.calmcalories.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.calmcalories.app.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcalories.app.model.MealEntry
import com.calmcalories.app.ui.components.MealRow
import com.calmcalories.app.ui.components.ProgressRing
import com.calmcalories.app.ui.dialogs.ManualEntryDialog
import com.calmcalories.app.ui.dialogs.MealDetailDialog
import com.calmcalories.app.ui.dialogs.PromptDialog
import com.calmcalories.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    meals: List<MealEntry>,
    dailyGoal: Int,
    isBusy: Boolean,
    lastError: String?,
    userName: String,
    onAddManual: (String, Int) -> Unit,
    onAddPrompt: (String) -> Unit,
    onAddImage: (ByteArray, String?) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onUpdateMeal: (Long, String, Int) -> Unit,
) {
    val todayMeals = remember(meals) {
        val d = startOfDay(System.currentTimeMillis())
        meals.filter { it.createdAt >= d }
    }
    val total = todayMeals.sumOf { it.calories }
    val remaining = (dailyGoal - total).coerceAtLeast(0)
    val ratio = total.toFloat() / dailyGoal.toFloat()
    val fraction by animateFloatAsState(targetValue = ratio, animationSpec = tween(1500), label = "ring")
    val isExceeded = ratio > 1f; val isOptimal = ratio >= 0.8f && !isExceeded
    val statusColor = if (isExceeded) BrandRed else if (isOptimal) Emerald else Amber
    val statusText = if (isExceeded) "Cap Reached" else if (isOptimal) "Optimal" else "Replenishing"

    val totalProtein = remember(todayMeals) { todayMeals.sumOf { it.foodItems.sumOf { item -> item.proteinGrams } } }
    val totalCarbs = remember(todayMeals) { todayMeals.sumOf { it.foodItems.sumOf { item -> item.carbsGrams } } }
    val totalFat = remember(todayMeals) { todayMeals.sumOf { it.foodItems.sumOf { item -> item.fatGrams } } }

    var showOptions by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }
    var detailMeal by remember { mutableStateOf<MealEntry?>(null) }

    val ctx = LocalContext.current; val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val bytes = downscaleImageBytes(ctx, uri) ?: return@runCatching
                onAddImage(bytes, ctx.contentResolver.getType(uri))
            }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        if (bmp == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
            onAddImage(bytes, "image/jpeg")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrandSurface),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Emerald.copy(alpha = 0.08f))
                            .border(0.5.dp, Emerald.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logo_nomai),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            if (userName.isNotBlank()) "Hey, $userName" else "NomAI",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandDark
                        )
                        Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp)
                    }
                }
                StatusBadge(statusText, statusColor, isExceeded)
            }
        }

        if (!lastError.isNullOrBlank()) {
            item {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x15A6453A)).padding(12.dp)) { Text(lastError, fontSize = 11.sp, color = BrandRed, fontWeight = FontWeight.Bold) }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(BrandCard).border(0.5.dp, Divider, RoundedCornerShape(24.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    ProgressRing(fraction, Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$total", fontSize = 42.sp, fontWeight = FontWeight.Light, color = if (isExceeded) BrandRed else BrandDark, letterSpacing = (-1.5).sp)
                        Text("CONSUMED", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = TextMuted, letterSpacing = 2.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Column(Modifier.fillMaxWidth(0.85f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth().height(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(Amber))
                        Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(Emerald))
                        Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(BrandRed))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val limit80 = (dailyGoal * 0.8f).toInt()
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("REPLENISHING", fontSize = 7.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.5.sp)
                            Text("< $limit80 kcal", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("OPTIMAL", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Emerald, letterSpacing = 0.5.sp)
                            Text("$limit80 - $dailyGoal kcal", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Emerald)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("EXCEEDED", fontSize = 7.sp, fontWeight = FontWeight.Black, color = BrandRed, letterSpacing = 0.5.sp)
                            Text("> $dailyGoal kcal", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = BrandRed)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(0.5.dp, Divider, RoundedCornerShape(12.dp)), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                        Text("$dailyGoal", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = BrandDark, letterSpacing = (-0.5).sp)
                        Text("DAILY GOAL", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = TextFaint, letterSpacing = 1.sp)
                    }
                    Box(Modifier.width(0.5.dp).height(32.dp).background(Divider))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                        Text(if (isExceeded) "+${total - dailyGoal}" else "$remaining", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = if (isExceeded) BrandRed else if (ratio < 0.8f) Amber else Emerald, letterSpacing = (-0.5).sp)
                        Text(if (isExceeded) "EXCEEDED" else "REMAINING", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = TextMuted, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroSummaryChip(label = "Protein", value = "${totalProtein}g", color = Emerald, modifier = Modifier.weight(1f))
                    MacroSummaryChip(label = "Carbs", value = "${totalCarbs}g", color = Amber, modifier = Modifier.weight(1f))
                    MacroSummaryChip(label = "Fats", value = "${totalFat}g", color = BrandRed, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(BrandDark).clickable { showOptions = true }, contentAlignment = Alignment.Center) {
                    Text("ADD ENTRY", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandCard, letterSpacing = 1.5.sp)
                }
            }
        }

        item { Text("TIMELINE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = BrandDark, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 4.dp)) }

        if (todayMeals.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(DividerLight).border(0.5.dp, Divider, RoundedCornerShape(20.dp)).padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DinnerDining, contentDescription = null, tint = TextFaint, modifier = Modifier.size(24.dp))
                        Text("LOG YOUR PALETTE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TextFaint, letterSpacing = 1.5.sp)
                    }
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    todayMeals.forEachIndexed { index, meal ->
                        MealRow(meal) { detailMeal = meal }
                        if (index < todayMeals.lastIndex) {
                            HorizontalDivider(color = DividerLight)
                        }
                    }
                }
            }
        }
    }

    if (showOptions) EntryOptionsSheet(
        onDismiss = { showOptions = false },
        onCamera = { showOptions = false; camera.launch(null) },
        onGallery = { showOptions = false; picker.launch("image/*") },
        onPrompt = { showOptions = false; showPrompt = true },
        onManual = { showOptions = false; showManual = true },
    )
    if (showPrompt) PromptDialog(onDismiss = { showPrompt = false }) { text -> showPrompt = false; onAddPrompt(text) }
    if (showManual) ManualEntryDialog(onDismiss = { showManual = false }) { n, c -> showManual = false; onAddManual(n, c) }
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

@Composable
private fun StatusBadge(text: String, color: Color, isExceeded: Boolean) {
    Row(
        modifier = Modifier.clip(CircleShape).border(0.5.dp, color.copy(alpha = if (isExceeded) 1f else 0.3f), CircleShape)
            .background(if (isExceeded) Color.Transparent else color.copy(0.15f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.sp)
    }
}

@Composable
private fun MacroSummaryChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.5.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = (-0.5).sp)
        Text(label.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = TextMuted, letterSpacing = 1.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryOptionsSheet(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit, onPrompt: () -> Unit, onManual: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = BrandCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.padding(20.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ADD ENTRY", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TextMuted, letterSpacing = 2.sp)
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(BrandSurface).border(0.5.dp, Divider, RoundedCornerShape(8.dp)).clickable { onDismiss() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(16.dp)) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandSurface)
                        .border(0.5.dp, Divider, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Amber.copy(alpha = 0.08f))
                                .border(0.5.dp, Amber.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("AI Photo Scan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                            Text("Gemma AI will estimate calories from a photo", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandCard)
                                .border(0.5.dp, Divider, RoundedCornerShape(8.dp))
                                .clickable { onCamera() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CAMERA", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = BrandDark, letterSpacing = 0.5.sp)
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandCard)
                                .border(0.5.dp, Divider, RoundedCornerShape(8.dp))
                                .clickable { onGallery() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("GALLERY", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = BrandDark, letterSpacing = 0.5.sp)
                        }
                    }
                }

                OptionRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Describe with AI",
                    subtitle = "Gemma AI will translate free text details",
                    tint = Emerald,
                    onClick = onPrompt
                )
                OptionRow(
                    icon = Icons.Default.Edit,
                    title = "Log Manually",
                    subtitle = "Directly record meal details and calories",
                    tint = BrandSecondary,
                    onClick = onManual
                )
            }
        }
    }
}

@Composable
private fun OptionRow(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface)
            .border(0.5.dp, Divider, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.08f))
                .border(0.5.dp, tint.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandDark)
            Text(subtitle, fontSize = 10.sp, color = TextMuted, lineHeight = 13.sp)
        }
    }
}

private fun startOfDay(e: Long) = Calendar.getInstance().apply { timeInMillis = e; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis

private fun downscaleImageBytes(ctx: android.content.Context, uri: Uri, maxDim: Int = 512): ByteArray? {
    return try {
        ctx.contentResolver.openInputStream(uri)?.use { stream ->
            val src = BitmapFactory.decodeStream(stream) ?: return null
            val ratio = src.width.toFloat() / src.height.toFloat()
            val (w, h) = if (src.width > maxDim || src.height > maxDim) {
                if (ratio > 1f) {
                    maxDim to (maxDim / ratio).toInt()
                } else {
                    (maxDim * ratio).toInt() to maxDim
                }
            } else {
                src.width to src.height
            }
            val scaled = Bitmap.createScaledBitmap(src, w, h, true)
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                out.toByteArray()
            }
        }
    } catch (e: Exception) {
        null
    }
}
