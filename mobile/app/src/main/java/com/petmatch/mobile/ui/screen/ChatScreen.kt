package com.petmatch.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmatch.mobile.ui.theme.*

data class ChatMessage(
    val id: Int,
    val content: String,
    val isSent: Boolean,
    val time: String,
    val type: MessageType = MessageType.TEXT
)

enum class MessageType { TEXT, IMAGE, APPOINTMENT }

val sampleMessages = listOf(
    ChatMessage(1, "Chào bạn! Bé Golden nhà bạn trông đáng yêu quá 🐾", false, "09:10"),
    ChatMessage(2, "Cảm ơn! Bé tên Claus, 3 tuổi. Bé nhà bạn tên gì vậy?", true, "09:12"),
    ChatMessage(3, "Bé mình là Bella, 2 tuổi, cũng thích đi dạo lắm 🐕", false, "09:13"),
    ChatMessage(4, "Hay đấy! Bạn muốn cho 2 bé gặp nhau không?", true, "09:15"),
    ChatMessage(5, "Muốn chứ! Cuối tuần này bạn rảnh không?", false, "09:16"),
    ChatMessage(6, "Thứ 7 mình rảnh buổi sáng. Mình đặt lịch hẹn nhé!", true, "09:18"),
    ChatMessage(7, "ok bạn, hẹn gặp nhé! 🤝", false, "09:19"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactName: String = "Trần Thị Lan",
    onBackClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onAppointmentClick: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages = remember { sampleMessages.toMutableStateList() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ─── Top Bar ───────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = White,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBackIos,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryPink, AccentPink)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD4845A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐕", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name & status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contactName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ActionGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đang hoạt động",
                            fontSize = 12.sp,
                            color = ActionGreen
                        )
                    }
                }

                // Action icons
                IconButton(onClick = onCallClick) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = PrimaryPink,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onVideoCallClick) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Video call",
                        tint = PrimaryPink,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onAppointmentClick) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Appointment",
                        tint = PrimaryPink,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ─── Messages List ─────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                // Date divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = DividerColor)
                    Text(
                        text = "  Hôm nay  ",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Divider(modifier = Modifier.weight(1f), color = DividerColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(messages) { message ->
                ChatBubble(message = message)
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Appointment suggestion card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AppointmentSuggestionCard(onClick = onAppointmentClick)
            }
        }

        // ─── Input Area ────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Emoji & Image
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Outlined.EmojiEmotions,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Text field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = {
                        Text("Nhắn tin...", color = TextHint, fontSize = 14.sp)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = BackgroundLight,
                        unfocusedContainerColor = BackgroundLight
                    ),
                    maxLines = 4,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            messages.add(
                                ChatMessage(
                                    id = messages.size + 1,
                                    content = messageText,
                                    isSent = true,
                                    time = "Vừa xong"
                                )
                            )
                            messageText = ""
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    containerColor = if (messageText.isNotBlank()) PrimaryPink else DividerColor,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank()) White else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSent) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isSent) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD4845A)),
                contentAlignment = Alignment.Center
            ) {
                Text("🐕", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (message.isSent) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(
                            topStart = if (message.isSent) 18.dp else 4.dp,
                            topEnd = if (message.isSent) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .background(
                        color = if (message.isSent) PrimaryPink else White,
                        shape = RoundedCornerShape(
                            topStart = if (message.isSent) 18.dp else 4.dp,
                            topEnd = if (message.isSent) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = if (message.isSent) White else TextPrimary,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message.time,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun AppointmentSuggestionCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightPink),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Đặt lịch hẹn gặp mặt",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryPink
                )
                Text(
                    text = "Cho 2 bé gặp nhau và làm quen 🐾",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PrimaryPink
            )
        }
    }
}
