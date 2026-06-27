package com.calmcalories.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcalories.app.ui.theme.*

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(BrandCard).border(0.5.dp, Divider, RoundedCornerShape(20.dp)).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 2.sp)
            Text(
                text = value.uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}
