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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcalories.app.ui.components.SCard
import com.calmcalories.app.ui.theme.*
import java.util.Locale

@Composable
fun ModelDownloaderScreen(
    state: com.calmcalories.app.model.DownloadState?,
    error: String?,
    onDownload: (String) -> Unit,
) {
    val defaultUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true"
    val isDownloading = state != null

    LaunchedEffect(Unit) {
        if (!isDownloading && error == null) {
            onDownload(defaultUrl)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandSurface)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Emerald.copy(alpha = 0.12f))
                .border(0.5.dp, Emerald.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Eco, contentDescription = null, tint = Emerald, modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "CalmCalories",
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            color = BrandDark,
            letterSpacing = (-0.5).sp,
        )
        Text(
            "PREPARING LOCAL AI",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = TextMuted,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(20.dp))

        SCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state != null) {
                    val p = state.progress
                    val currentMb = state.bytesRead / (1024.0 * 1024.0)
                    val totalMb = state.totalBytes / (1024.0 * 1024.0)
                    val speed = state.speedBytesPerSecond
                    val speedText = if (speed > 1024.0 * 1024.0) {
                        String.format(Locale.US, "%.1f MB/s", speed / (1024.0 * 1024.0))
                    } else {
                        String.format(Locale.US, "%.1f KB/s", speed / 1024.0)
                    }

                    val timeRemaining = state.timeRemainingSeconds
                    val timeText = if (timeRemaining != null) {
                        val mins = timeRemaining / 60
                        val secs = timeRemaining % 60
                        if (mins > 0) "${mins}m ${secs}s remaining" else "${secs}s remaining"
                    } else {
                        "Estimating time remaining..."
                    }

                    Text(
                        "DOWNLOADING GEMMA MODEL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        letterSpacing = 1.5.sp,
                    )

                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Emerald,
                        trackColor = Divider,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Completed", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text("${(p * 100).toInt()}%", fontSize = 12.sp, color = Emerald, fontWeight = FontWeight.Bold)
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("File Size", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text(
                                String.format(Locale.US, "%.0f / %.0f MB", currentMb, totalMb),
                                fontSize = 11.sp, color = BrandDark, fontWeight = FontWeight.Bold
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Speed", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text(speedText, fontSize = 11.sp, color = BrandDark, fontWeight = FontWeight.Bold)
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Time left", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text(timeText, fontSize = 11.sp, color = BrandSecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Divider)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Background safe: You can safely minimize the app. The download will continue in the background.",
                            fontSize = 10.sp,
                            color = TextMuted,
                            lineHeight = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "CONNECTING TO HUGGINGFACE...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 1.5.sp,
                        )
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Emerald,
                            trackColor = Divider,
                        )
                    }
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x12A6453A))
                    .border(0.5.dp, BrandRed.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("DOWNLOAD FAILURE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = BrandRed, letterSpacing = 1.sp)
                    Text(error, fontSize = 11.sp, color = BrandSecondary, lineHeight = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandDark)
                            .clickable { onDownload(defaultUrl) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("RETRY DOWNLOAD", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
