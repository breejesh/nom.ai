package com.calmcalories.app.ui.dialogs

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.calmcalories.app.ui.theme.*

@Composable
fun PromptDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = BrandCard, shape = RoundedCornerShape(24.dp),
        title = { Text("Describe your meal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("e.g. 2 eggs and toast with coffee", fontSize = 12.sp, color = TextMuted)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrandSurface).border(0.5.dp, Divider, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    BasicTextField(value = text, onValueChange = { text = it }, textStyle = TextStyle(fontSize = 14.sp, color = BrandDark), modifier = Modifier.fillMaxWidth().height(80.dp),
                        decorationBox = { inner -> if (text.isEmpty()) Text("What did you eat?", fontSize = 14.sp, color = TextFaint); inner() })
                }
            }
        },
        confirmButton = { Button(onClick = { if (text.isNotBlank()) onConfirm(text) }, colors = ButtonDefaults.buttonColors(containerColor = BrandDark), shape = RoundedCornerShape(12.dp)) { Text("Analyze with AI", fontWeight = FontWeight.Black, fontSize = 12.sp) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted, fontSize = 12.sp) } }
    )
}

@Composable
fun ManualEntryDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }; var cals by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = BrandCard, shape = RoundedCornerShape(24.dp),
        title = { Text("Add manually", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabelInput("MEAL NAME", name, "e.g. Chicken salad") { name = it }
                LabelInput("CALORIES (KCAL)", cals, "e.g. 450") { cals = it }
            }
        },
        confirmButton = { Button(onClick = { val c = cals.toIntOrNull() ?: 0; if (name.isNotBlank() && c > 0) onConfirm(name, c) }, colors = ButtonDefaults.buttonColors(containerColor = BrandDark), shape = RoundedCornerShape(12.dp)) { Text("Add", fontWeight = FontWeight.Black, fontSize = 12.sp) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted, fontSize = 12.sp) } },
    )
}

@Composable
fun LabelInput(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.sp)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrandSurface).border(0.5.dp, Divider, RoundedCornerShape(12.dp)).padding(12.dp)) {
            BasicTextField(value = value, onValueChange = onValueChange, textStyle = TextStyle(fontSize = 14.sp, color = BrandDark), modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, fontSize = 14.sp, color = TextFaint); inner() })
        }
    }
}

@Composable
fun AiProcessingDialog(prompt: String?, imageBytes: ByteArray?) {
    val phrases = listOf(
        "Gemma is examining the ingredients...",
        "Scanning portion sizes...",
        "Estimating nutritional densities...",
        "Parsing food categories...",
        "Calculating total calories...",
        "Formulating nutritional breakdown..."
    )
    var phraseIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    val bitmap = remember(imageBytes) {
        if (imageBytes != null) {
            runCatching { BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) }.getOrNull()
        } else null
    }

    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(BrandCard)
                .border(0.5.dp, Divider, RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Emerald,
                    strokeWidth = 3.dp,
                    trackColor = Divider
                )
                Text(
                    "ANALYSING PALETTE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandSecondary,
                    letterSpacing = 2.sp
                )

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Processing meal photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(0.5.dp, Divider, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (!prompt.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandSurface)
                            .border(0.5.dp, Divider, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "\"$prompt\"",
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = BrandSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text(
                    phrases[phraseIndex],
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
