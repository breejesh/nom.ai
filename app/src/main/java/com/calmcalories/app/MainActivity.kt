package com.calmcalories.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calmcalories.app.ui.dialogs.AiProcessingDialog
import com.calmcalories.app.ui.screens.*
import com.calmcalories.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val isDark by vm.isDarkTheme.collectAsState()
            CalmCaloriesTheme(isDark) {
                CalmCaloriesApp(vm)
            }
        }
    }
}

@Composable
private fun CalmCaloriesApp(vm: AppViewModel) {
    var tab by remember { mutableStateOf(Tab.Home) }
    val meals by vm.meals.collectAsState()
    val dailyGoal by vm.dailyGoal.collectAsState()
    val isBusy by vm.isBusy.collectAsState()
    val lastError by vm.lastError.collectAsState()
    val userName by vm.userName.collectAsState()
    val weightKg by vm.weightKg.collectAsState()
    val heightCm by vm.heightCm.collectAsState()
    val activityLevel by vm.activityLevel.collectAsState()
    val suggestedCalories by vm.suggestedCalories.collectAsState()
    val age by vm.age.collectAsState()
    val isDarkTheme by vm.isDarkTheme.collectAsState()

    val isModelPresent by vm.isModelPresent.collectAsState()
    val downloadState by vm.downloadState.collectAsState()
    val downloadError by vm.downloadError.collectAsState()

    val processingPrompt by vm.processingPrompt.collectAsState()
    val processingImageBytes by vm.processingImageBytes.collectAsState()

    val ctx = LocalContext.current

    val showRestoreConfirm by vm.showRestoreConfirm.collectAsState()

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.handleImportFileSelected(uri)
        }
    }

    LaunchedEffect(isDarkTheme) {
        val activity = ctx as? ComponentActivity ?: return@LaunchedEffect
        activity.enableEdgeToEdge(
            statusBarStyle = if (isDarkTheme) {
                androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                androidx.activity.SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        vm.mealAddedEvent.collect { msg ->
            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BrandSurface).statusBarsPadding()) {
        if (!isModelPresent) {
            ModelDownloaderScreen(
                state = downloadState,
                error = downloadError,
                onDownload = vm::downloadModel,
            )
        } else {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                modifier = Modifier.fillMaxSize().padding(bottom = 72.dp),
                label = "tab",
            ) { t ->
                when (t) {
                    Tab.Home -> HomeScreen(
                        meals = meals,
                        dailyGoal = dailyGoal,
                        isBusy = isBusy,
                        lastError = lastError,
                        userName = userName,
                        onAddManual = vm::addManualMeal,
                        onAddPrompt = vm::addByPrompt,
                        onAddImage = vm::addByImage,
                        onDeleteMeal = vm::deleteMeal,
                        onUpdateMeal = vm::updateMeal
                    )
                    Tab.Stats -> StatsScreen(meals, dailyGoal)
                    Tab.Coach -> {
                        val chatMsgs by vm.chatMessages.collectAsState()
                        val isCoachBusy by vm.isCoachBusy.collectAsState()
                        CoachScreen(
                            messages = chatMsgs,
                            isBusy = isCoachBusy,
                            onSendMessage = vm::sendMessageToCoach,
                            onClearHistory = vm::clearChatHistory
                        )
                    }
                    Tab.Journal -> JournalScreen(meals, onDeleteMeal = vm::deleteMeal, onUpdateMeal = vm::updateMeal)
                    Tab.Settings -> SettingsScreen(
                        userName = userName,
                        onNameChange = vm::updateUserName,
                        dailyGoal = dailyGoal,
                        onGoalChange = vm::updateGoal,
                        weightKg = weightKg,
                        onWeightChange = vm::updateWeight,
                        heightCm = heightCm,
                        onHeightChange = vm::updateHeight,
                        activityLevel = activityLevel,
                        onActivityChange = vm::updateActivityLevel,
                        age = age,
                        onAgeChange = vm::updateAge,
                        suggestedCalories = suggestedCalories,
                        isDarkTheme = isDarkTheme,
                        onDarkThemeChange = vm::updateDarkTheme,
                        onExportBackup = { vm.handleExportClick(ctx) },
                        onImportBackup = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
                    )
                }
            }
            BottomNav(activeTab = tab, onSelect = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
        }

        if (isBusy) {
            AiProcessingDialog(processingPrompt, processingImageBytes)
        }

        if (showRestoreConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = vm::dismissBackupDialogs,
                title = { Text("Restore Backup?", fontWeight = FontWeight.Bold, color = BrandDark) },
                text = { Text("This will overwrite your current settings and meal history with the selected backup ZIP file. The app will restart automatically. Proceed?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            vm.performImport(ctx)
                        }
                    ) {
                        Text("RESTORE", color = BrandRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = vm::dismissBackupDialogs) {
                        Text("CANCEL", color = TextMuted)
                    }
                },
                containerColor = BrandCard
            )
        }
    }
}

@Composable
private fun BottomNav(activeTab: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple(Tab.Home, Icons.Default.Home, "Today"),
        Triple(Tab.Stats, Icons.Default.Star, "Stats"),
        Triple(Tab.Journal, Icons.Default.Search, "Journal"),
        Triple(Tab.Settings, Icons.Default.Settings, "Settings"),
    )
    Row(
        modifier = modifier.fillMaxWidth().height(72.dp).background(BrandCard)
            .border(width = 1.dp, color = Divider, shape = RoundedCornerShape(0.dp))
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (t, icon, label) ->
            val active = activeTab == t
            Column(
                modifier = Modifier
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSelect(t) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(icon, contentDescription = label, tint = if (active) BrandDark else TextFaint, modifier = Modifier.size(20.dp))
                Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (active) BrandDark else TextFaint, letterSpacing = 0.5.sp)
            }
        }
    }
}
