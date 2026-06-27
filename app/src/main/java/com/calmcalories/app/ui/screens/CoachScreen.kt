package com.calmcalories.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmcalories.app.model.ChatMessage
import com.calmcalories.app.ui.theme.*

@Composable
fun CoachScreen(
    messages: List<ChatMessage>,
    isBusy: Boolean,
    onSendMessage: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Automatically scroll to bottom when new messages arrive or typing indicator pops up
    LaunchedEffect(messages.size, isBusy) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestions = listOf(
        "How can I eat cleaner today?",
        "Suggest a high protein dinner",
        "Give me a mindful habit tips"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandSurface)
    ) {
        // ── Title Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("COACH", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BrandDark, letterSpacing = 1.sp)
                Text("MINDFUL HEALTH ADVISOR", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 1.5.sp)
            }
            if (messages.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandRed.copy(alpha = 0.08f))
                        .border(0.5.dp, BrandRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { onClearHistory() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Clear History",
                        tint = BrandRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Message dialogue area ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (messages.isEmpty()) {
                // Empty state greeting
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Emerald.copy(alpha = 0.12f))
                            .border(0.5.dp, Emerald.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Eco, contentDescription = null, tint = Emerald, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("HI, I'M EUCALYPTUS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = BrandDark, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your offline AI advisor. Ask me questions about nutrition, exercise, or mindful eating habits.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        lineHeight = 16.sp
                    )

                    Spacer(Modifier.height(32.dp))

                    // Suggestions pills
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        suggestions.forEach { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandCard)
                                    .border(0.5.dp, Divider, RoundedCornerShape(12.dp))
                                    .clickable { onSendMessage(prompt) }
                                    .padding(vertical = 10.dp, horizontal = 16.dp)
                            ) {
                                Text(
                                    prompt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                // Chat conversation logs list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(message)
                    }

                    if (isBusy) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }

        // ── Input area ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandCard)
                .border(0.5.dp, Divider, RoundedCornerShape(0.dp))
                .padding(14.dp)
                .padding(bottom = 24.dp), // extra padding for bottom system bar spacing
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandSurface)
                    .border(0.5.dp, Divider, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    textStyle = TextStyle(fontSize = 14.sp, color = BrandDark, fontWeight = FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (textInput.isEmpty()) Text("Ask Eucalyptus...", fontSize = 14.sp, color = TextFaint)
                        inner()
                    }
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (textInput.isNotBlank() && !isBusy) BrandDark else TextFaint)
                    .clickable(enabled = textInput.isNotBlank() && !isBusy) {
                        onSendMessage(textInput.trim())
                        textInput = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val bubbleBg = if (message.isUser) {
        BrandCard // Crema / Card background
    } else {
        BrandSurface // Alabaster background
    }

    val borderCol = if (message.isUser) Divider else Emerald.copy(alpha = 0.15f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp, top = 4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Emerald.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = Emerald, modifier = Modifier.size(12.dp))
            }
        }

        Box(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(0.5.dp, borderCol, bubbleShape)
                .padding(12.dp)
        ) {
            Text(
                message.text,
                fontSize = 13.sp,
                color = BrandDark,
                lineHeight = 18.sp,
                fontWeight = if (message.isUser) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Emerald.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Eco, contentDescription = null, tint = Emerald, modifier = Modifier.size(12.dp))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                .background(BrandSurface)
                .border(0.5.dp, Emerald.copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = Emerald,
                    strokeWidth = 2.dp
                )
                Text(
                    "Eucalyptus is composing advice...",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
