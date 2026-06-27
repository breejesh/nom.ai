package com.calmcalories.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcalories.app.ui.theme.*

@Composable
fun DateNavBtn(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(if (enabled) BrandSurface else Color.Transparent).border(0.5.dp, if (enabled) Divider else Color.Transparent, CircleShape).clickable(enabled = enabled) { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, fontSize = 18.sp, color = if (enabled) TextMuted else Color.Transparent)
    }
}
