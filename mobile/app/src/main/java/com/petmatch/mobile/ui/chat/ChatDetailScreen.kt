package com.petmatch.mobile.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.data.model.MessageResponse
import com.petmatch.mobile.ui.common.petMatchGradient
import com.petmatch.mobile.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// ChatDetailScreen
// navArgs: otherUserId, otherUserName
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: NavController,
    otherUserId: Long,
    otherUserName: String,
    currentUserId: Long = 1L,  // thay bằng userId từ DataStore trong thực tế
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val messages by chatVm.messages.collectAsState()
    val loading by chatVm.chatLoading.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        chatVm.loadChatHistory(ctx, currentUserId, otherUserId)
        chatVm.markAsRead(ctx, otherUserId, currentUserId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    var showAppointmentDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            AsyncImage(
                                model = "https://placedog.net/40/40?r=$otherUserId",
                                contentDescription = otherUserName,
                                modifier = Modifier.size(38.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(11.dp)
                                    .background(LikeGreen, CircleShape)
                                    .border(2.dp, PrimaryPink, CircleShape)
                            )
                        }
                        Column {
                            Text(
                                otherUserName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                "Đang hoạt động",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(0.8f))
                            )
                        }
                    }
                },
                actions = {
                    // Audio call
                    IconButton(onClick = {
                        navController.navigate("chat/call/$otherUserId/$otherUserName/AUDIO")
                    }) {
                        Icon(Icons.Default.Phone, "Gọi điện", tint = Color.White)
                    }
                    // Video call
                    IconButton(onClick = {
                        navController.navigate("chat/call/$otherUserId/$otherUserName/VIDEO")
                    }) {
                        Icon(Icons.Default.VideoCall, "Gọi video", tint = Color.White)
                    }
                    // More options
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Đặt lịch hẹn 📅") },
                                onClick = {
                                    showOptionsMenu = false
                                    showAppointmentDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Đánh giá sau gặp ⭐") },
                                onClick = {
                                    showOptionsMenu = false
                                    navController.navigate("chat/review/$otherUserId/$otherUserName")
                                },
                                leadingIcon = { Icon(Icons.Default.Star, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryPink)
            )
        },
        bottomBar = {
            ChatInputBar(
                text = messageText,
                onTextChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        // TODO: gửi qua WebSocket STOMP
                        val localMsg = MessageResponse(
                            id = System.currentTimeMillis(),
                            senderId = currentUserId,
                            receiverId = otherUserId,
                            content = messageText.trim(),
                            sentAt = LocalDateTime.now().toString(),
                            isRead = false
                        )
                        chatVm.addLocalMessage(localMsg)
                        messageText = ""
                    }
                }
            )
        }
    ) { padding ->
        if (loading && messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPink)
            }
        } else if (messages.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🐾", fontSize = 56.sp)
                    Text(
                        "Hãy gửi lời chào đầu tiên!",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == currentUserId
                    MessageBubble(msg = msg, isMe = isMe)
                }
            }
        }
    }

    // Appointment Dialog
    if (showAppointmentDialog) {
        AppointmentQuickDialog(
            otherUserId = otherUserId,
            otherUserName = otherUserName,
            chatVm = chatVm,
            onDismiss = { showAppointmentDialog = false },
            onNavigateToFull = {
                showAppointmentDialog = false
                navController.navigate("chat/appointment/$otherUserId/$otherUserName")
            }
        )
    }
}

// ── Message Bubble ────────────────────────────────────────────────────────────
@Composable
private fun MessageBubble(msg: MessageResponse, isMe: Boolean) {
    val timeStr = try {
        val dt = LocalDateTime.parse(msg.sentAt)
        DateTimeFormatter.ofPattern("HH:mm").format(dt)
    } catch (_: Exception) { "" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            AsyncImage(
                model = "https://placedog.net/32/32?r=${msg.senderId}",
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .align(Alignment.Bottom),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isMe) 20.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 20.dp
                        )
                    )
                    .background(
                        if (isMe)
                            Brush.linearGradient(
                                listOf(GradientStart, GradientEnd),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                            )
                        else
                            Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) Color.White else TextPrimary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
                if (isMe) {
                    Icon(
                        imageVector = if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = null,
                        tint = if (msg.isRead) PrimaryPink else TextHint,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── Chat Input Bar ────────────────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attachment
            IconButton(
                onClick = {},
                modifier = Modifier.size(40.dp).background(SurfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Add, null, tint = PrimaryPink, modifier = Modifier.size(20.dp))
            }

            // Text field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...", style = MaterialTheme.typography.bodyMedium) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPink,
                    unfocusedBorderColor = Divider,
                    focusedContainerColor = SurfaceLight,
                    unfocusedContainerColor = SurfaceVariant
                ),
                singleLine = false,
                maxLines = 4,
                trailingIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.EmojiEmotions, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            )

            // Send / Mic
            if (text.isBlank()) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(44.dp).background(PrimaryPink.copy(0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Mic, "Voice", tint = PrimaryPink)
                }
            } else {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier.size(44.dp).background(petMatchGradient, CircleShape),
                    colors = IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ── Quick Appointment Dialog (inline trong chat) ──────────────────────────────
@Composable
private fun AppointmentQuickDialog(
    otherUserId: Long,
    otherUserName: String,
    chatVm: ChatViewModel,
    onDismiss: () -> Unit,
    onNavigateToFull: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("📅", fontSize = 32.sp) },
        title = {
            Text(
                "Đặt lịch hẹn với $otherUserName",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Bạn muốn tạo lịch hẹn để gặp mặt trực tiếp cùng thú cưng?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onNavigateToFull,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
            ) {
                Text("Đặt lịch ngay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Để sau") }
        }
    )
}

// ── Group Chat Detail Screen ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatDetailScreen(
    navController: NavController,
    groupId: Long,
    groupName: String,
    currentUserId: Long = 1L,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val messages by chatVm.groupMessages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { chatVm.loadGroupHistory(ctx, groupId) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(petMatchGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                groupName.firstOrNull()?.toString() ?: "G",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        Column {
                            Text(
                                groupName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                "Nhóm chat",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(0.8f))
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.GroupAdd, null, tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryPink)
            )
        },
        bottomBar = {
            ChatInputBar(
                text = messageText,
                onTextChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        val localMsg = com.petmatch.mobile.data.model.GroupMessageResponse(
                            id = System.currentTimeMillis(),
                            groupId = groupId,
                            senderId = currentUserId,
                            senderName = "Tôi",
                            senderAvatarUrl = null,
                            content = messageText.trim(),
                            sentAt = LocalDateTime.now().toString()
                        )
                        chatVm.addLocalGroupMessage(localMsg)
                        messageText = ""
                    }
                }
            )
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("👥", fontSize = 56.sp)
                    Text("Hãy bắt đầu cuộc trò chuyện!", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderId == currentUserId
                    GroupMessageBubble(msg = msg, isMe = isMe)
                }
            }
        }
    }
}

@Composable
private fun GroupMessageBubble(msg: com.petmatch.mobile.data.model.GroupMessageResponse, isMe: Boolean) {
    val timeStr = try {
        val dt = LocalDateTime.parse(msg.sentAt)
        DateTimeFormatter.ofPattern("HH:mm").format(dt)
    } catch (_: Exception) { "" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            AsyncImage(
                model = msg.senderAvatarUrl ?: "https://placedog.net/32/32?r=${msg.senderId}",
                contentDescription = msg.senderName,
                modifier = Modifier.size(32.dp).clip(CircleShape).align(Alignment.Bottom),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMe) {
                Text(
                    msg.senderName ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryPink
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp, topEnd = 20.dp,
                            bottomStart = if (isMe) 20.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 20.dp
                        )
                    )
                    .background(
                        if (isMe) Brush.linearGradient(listOf(GradientStart, GradientEnd), Offset.Zero, Offset(Float.POSITIVE_INFINITY, 0f))
                        else Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(msg.content, style = MaterialTheme.typography.bodyMedium, color = if (isMe) Color.White else TextPrimary)
            }
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextHint, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
