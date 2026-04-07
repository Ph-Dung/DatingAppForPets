package com.petmatch.mobile.ui.chat

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.petmatch.mobile.ui.navigation.Routes
import coil.compose.AsyncImage
import com.petmatch.mobile.data.model.BlockStatus
import com.petmatch.mobile.data.model.MessageResponse
import com.petmatch.mobile.ui.common.petMatchGradient
import com.petmatch.mobile.ui.theme.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// ChatDetailScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: NavController,
    otherUserId: Long,
    otherUserName: String,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val currentUserId by chatVm.currentUserId.collectAsState()
    val messages by chatVm.messages.collectAsState()
    val loading by chatVm.chatLoading.collectAsState()
    val blockStatus by chatVm.blockStatus.collectAsState()
    val mediaUploading by chatVm.mediaUploadLoading.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var showAppointmentDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        chatVm.loadCurrentUserId(ctx)
        chatVm.loadChatHistory(ctx, currentUserId, otherUserId)
        chatVm.markAsRead(ctx, otherUserId, currentUserId)
        chatVm.loadBlockStatus(ctx, otherUserId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    // ── Image Picker ─────────────────────────────────────────────────────────
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            chatVm.uploadAndSendMedia(ctx, it, "IMAGE", currentUserId, otherUserId)
        }
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
                        modifier = Modifier.clickable {
                            navController.navigate(Routes.messengerProfile(otherUserId, otherUserName))
                        },
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
                                    fontWeight = FontWeight.Bold, color = Color.White
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
                    // Chỉ hiện call nếu không bị block call/All
                    val callBlocked = blockStatus?.theyBlockedMe == true &&
                            (blockStatus?.myBlockLevel == "CALL" || blockStatus?.myBlockLevel == "ALL")
                    if (!callBlocked) {
                        IconButton(onClick = {
                            navController.navigate(Routes.call(peerId = otherUserId, peerName = otherUserName, callType = "AUDIO"))
                        }) {
                            Icon(Icons.Default.Phone, "Gọi điện", tint = Color.White)
                        }
                        IconButton(onClick = {
                            navController.navigate(Routes.call(peerId = otherUserId, peerName = otherUserName, callType = "VIDEO"))
                        }) {
                            Icon(Icons.Default.VideoCall, "Gọi video", tint = Color.White)
                        }
                    }
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
                                onClick = { showOptionsMenu = false; showAppointmentDialog = true },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                                contentPadding = MenuDefaults.DropdownMenuItemContentPadding
                            )
                            DropdownMenuItem(
                                text = { Text("Đánh giá sau gặp ⭐") },
                                onClick = {
                                    showOptionsMenu = false
                                    navController.navigate("chat/review/$otherUserId/$otherUserName")
                                },
                                leadingIcon = { Icon(Icons.Default.Star, null) },
                                contentPadding = MenuDefaults.DropdownMenuItemContentPadding
                            )
                            HorizontalDivider()
                            // Block / Unblock
                            if (blockStatus?.iBlockedThem == true) {
                                DropdownMenuItem(
                                    text = { Text("Mở chặn 🔓", color = LikeGreen) },
                                    onClick = {
                                        showOptionsMenu = false
                                        chatVm.unblockUser(ctx, otherUserId)
                                    },
                                    leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = LikeGreen) },
                                    contentPadding = MenuDefaults.DropdownMenuItemContentPadding
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Chặn người dùng 🚫", color = DislikeRed) },
                                    onClick = { showOptionsMenu = false; showBlockDialog = true },
                                    leadingIcon = { Icon(Icons.Default.Block, null, tint = DislikeRed) },
                                    contentPadding = MenuDefaults.DropdownMenuItemContentPadding
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryPink)
            )
        },
        bottomBar = {
            Column {
                // Banner bị chặn (người kia đã chặn mình)
                if (blockStatus?.theyBlockedMe == true) {
                    Surface(color = DislikeRed.copy(0.1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Block, null, tint = DislikeRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Bạn đã bị chặn, không thể liên lạc với người dùng này",
                                style = MaterialTheme.typography.bodySmall,
                                color = DislikeRed
                            )
                        }
                    }
                } else if (blockStatus?.iBlockedThem == true) {
                    Surface(color = AccentPurple.copy(0.08f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Block, null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Bạn đang chặn người này. ",
                                style = MaterialTheme.typography.bodySmall, color = AccentPurple
                            )
                            Text(
                                "Mở chặn?",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentPurple,
                                modifier = Modifier.clickable { chatVm.unblockUser(ctx, otherUserId) }
                            )
                        }
                    }
                } else {
                    ChatInputBar(
                        text = messageText,
                        onTextChange = { messageText = it },
                        onSend = {
                            if (messageText.isNotBlank()) {
                                chatVm.sendTextMessage(ctx, currentUserId, otherUserId, messageText.trim())
                                messageText = ""
                            }
                        },
                        onAttach = { showAttachSheet = true },
                        isUploading = mediaUploading
                    )
                }
            }
        }
    ) { padding ->
        if (loading && messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPink)
            }
        } else if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    MessageBubble(msg = msg, isMe = msg.senderId == currentUserId)
                }
            }
        }
    }

    // ── Attach Bottom Sheet ───────────────────────────────────────────────────
    if (showAttachSheet) {
        AttachBottomSheet(
            onDismiss = { showAttachSheet = false },
            onPickImage = {
                showAttachSheet = false
                imagePicker.launch("image/*")
            },
            onVoiceRecord = { voiceUri ->
                showAttachSheet = false
                voiceUri?.let {
                    chatVm.uploadAndSendMedia(ctx, it, "VOICE", currentUserId, otherUserId)
                }
            }
        )
    }

    // ── Block Dialog ──────────────────────────────────────────────────────────
    if (showBlockDialog) {
        BlockUserDialog(
            userName = otherUserName,
            onDismiss = { showBlockDialog = false },
            onBlock = { level ->
                showBlockDialog = false
                chatVm.blockUser(ctx, otherUserId, level)
            }
        )
    }

    // ── Appointment Dialog ────────────────────────────────────────────────────
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
        DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.parse(msg.sentAt))
    } catch (_: Exception) { "" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            AsyncImage(
                model = "https://placedog.net/32/32?r=${msg.senderId}",
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape).align(Alignment.Bottom),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            when (msg.type) {
                "IMAGE" -> ImageBubble(msg, isMe)
                "VOICE" -> VoiceBubble(msg, isMe)
                "CALL" -> CallBubble(msg, isMe)
                else -> TextBubble(msg, isMe)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextHint)
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

@Composable
private fun TextBubble(msg: MessageResponse, isMe: Boolean) {
    Box(
        modifier = Modifier
            .clip(bubbleShape(isMe))
            .background(
                if (isMe)
                    Brush.linearGradient(listOf(GradientStart, GradientEnd), Offset.Zero, Offset(Float.POSITIVE_INFINITY, 0f))
                else Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(msg.content ?: "", style = MaterialTheme.typography.bodyMedium, color = if (isMe) Color.White else TextPrimary)
    }
}

@Composable
private fun CallBubble(msg: MessageResponse, isMe: Boolean) {
    Box(
        modifier = Modifier
            .clip(bubbleShape(isMe))
            .background(
                if (isMe)
                    Brush.linearGradient(listOf(GradientStart, GradientEnd), Offset.Zero, Offset(Float.POSITIVE_INFINITY, 0f))
                else Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable { /* Tương lai có thể bấm gọi lại rảnh tay */ }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isMissed = msg.content?.contains("nhỡ") == true
            val icon = if (msg.content?.contains("video", ignoreCase = true) == true) Icons.Default.VideoCall else Icons.Default.PhoneCallback
            val tint = if (isMissed) DislikeRed else if (isMe) Color.White else PrimaryPink

            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Text(
                msg.content ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isMe) Color.White else TextPrimary
            )
        }
    }
}

@Composable
private fun ImageBubble(msg: MessageResponse, isMe: Boolean) {
    Box(
        modifier = Modifier
            .size(200.dp, 150.dp)
            .clip(bubbleShape(isMe))
    ) {
        AsyncImage(
            model = msg.mediaUrl,
            contentDescription = "Hình ảnh",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun VoiceBubble(msg: MessageResponse, isMe: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose { player.value?.release() }
    }

    Box(
        modifier = Modifier
            .clip(bubbleShape(isMe))
            .background(
                if (isMe) Brush.linearGradient(listOf(GradientStart, GradientEnd), Offset.Zero, Offset(Float.POSITIVE_INFINITY, 0f))
                else Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(min = 120.dp)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        player.value?.pause()
                        isPlaying = false
                    } else {
                        try {
                            val mp = MediaPlayer().apply {
                                setDataSource(msg.mediaUrl ?: return@apply)
                                prepare()
                                setOnCompletionListener { isPlaying = false }
                                start()
                            }
                            player.value?.release()
                            player.value = mp
                            isPlaying = true
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = if (isMe) Color.White else PrimaryPink,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Waveform visual (simplified)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bars = listOf(0.4f, 0.7f, 0.5f, 1f, 0.6f, 0.8f, 0.4f, 0.9f, 0.6f)
                bars.forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height((20 * h).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isMe) Color.White.copy(0.7f) else PrimaryPink.copy(0.7f))
                    )
                }
            }
            Icon(Icons.Default.Mic, null, tint = if (isMe) Color.White.copy(0.7f) else TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

private fun bubbleShape(isMe: Boolean) = RoundedCornerShape(
    topStart = 20.dp, topEnd = 20.dp,
    bottomStart = if (isMe) 20.dp else 4.dp,
    bottomEnd = if (isMe) 4.dp else 20.dp
)

// ── Chat Input Bar ────────────────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    isUploading: Boolean
) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp), color = PrimaryPink, strokeWidth = 3.dp)
            } else {
                IconButton(
                    onClick = onAttach,
                    modifier = Modifier.size(40.dp).background(SurfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = PrimaryPink, modifier = Modifier.size(20.dp))
                }
            }

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

            if (text.isBlank()) {
                VoiceRecordButton(onVoiceReady = { uri ->
                    // gửi voice message
                })
            } else {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier.size(44.dp).background(petMatchGradient, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ── Voice Record Button (nhấn giữ để ghi, thả để gửi) ───────────────────────
@Composable
private fun VoiceRecordButton(onVoiceReady: (Uri?) -> Unit) {
    val ctx = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val scale by animateFloatAsState(if (isRecording) 1.3f else 1f, label = "mic_scale")

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .background(
                if (isRecording) DislikeRed else PrimaryPink.copy(0.1f),
                CircleShape
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when {
                            event.type == androidx.compose.ui.input.pointer.PointerEventType.Press -> {
                                // Start recording
                                val file = File(ctx.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                                outputFile = file
                                try {
                                    val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        MediaRecorder(ctx)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        MediaRecorder()
                                    }
                                    mr.apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                        setOutputFile(file.absolutePath)
                                        prepare()
                                        start()
                                    }
                                    recorder = mr
                                    isRecording = true
                                } catch (_: Exception) {
                                    permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            event.type == androidx.compose.ui.input.pointer.PointerEventType.Release -> {
                                // Stop recording & send
                                try {
                                    recorder?.stop()
                                    recorder?.release()
                                    recorder = null
                                    isRecording = false
                                    outputFile?.let { onVoiceReady(Uri.fromFile(it)) }
                                    outputFile = null
                                } catch (_: Exception) { isRecording = false }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Mic,
            "Ghi âm",
            tint = if (isRecording) Color.White else PrimaryPink,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ── Attach Bottom Sheet ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachBottomSheet(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onVoiceRecord: (Uri?) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Đính kèm",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AttachOption(icon = Icons.Default.Image, label = "Hình ảnh", bgColor = AccentPurple, onClick = onPickImage)
                AttachOption(icon = Icons.Default.CameraAlt, label = "Camera", bgColor = LikeGreen, onClick = onPickImage)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RowScope.AttachOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(bgColor.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = bgColor, modifier = Modifier.size(32.dp))
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

// ── Block Dialog ──────────────────────────────────────────────────────────────
@Composable
private fun BlockUserDialog(
    userName: String,
    onDismiss: () -> Unit,
    onBlock: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Block, null, tint = DislikeRed, modifier = Modifier.size(32.dp)) },
        title = {
            Text(
                "Chặn $userName",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Chọn mức độ chặn:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                BlockOption(
                    icon = "💬",
                    title = "Chặn tin nhắn",
                    description = "Không nhận được tin nhắn từ người này",
                    onClick = { onBlock("MESSAGE") }
                )
                BlockOption(
                    icon = "📞",
                    title = "Chặn cuộc gọi",
                    description = "Không nhận được cuộc gọi từ người này",
                    onClick = { onBlock("CALL") }
                )
                BlockOption(
                    icon = "🚫",
                    title = "Chặn tất cả",
                    description = "Chặn mọi liên lạc và ẩn khỏi tìm kiếm",
                    onClick = { onBlock("ALL") },
                    highlight = true
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        }
    )
}

@Composable
private fun BlockOption(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) DislikeRed.copy(0.08f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (highlight) BorderStroke(1.dp, DislikeRed.copy(0.3f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (highlight) DislikeRed else TextPrimary
                    )
                )
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

// ── Appointment Quick Dialog ──────────────────────────────────────────────────
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
            ) { Text("Đặt lịch ngay") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Để sau") } }
    )
}

// ── Group Chat Detail (giữ nguyên từ file cũ) ────────────────────────────────
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
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(petMatchGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                groupName.firstOrNull()?.toString() ?: "G",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                        Column {
                            Text(groupName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            Text("Nhóm chat", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(0.8f)))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.GroupAdd, null, tint = Color.White) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
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
                            id = System.currentTimeMillis(), groupId = groupId, senderId = currentUserId,
                            senderName = "Tôi", senderAvatarUrl = null, content = messageText.trim(),
                            sentAt = LocalDateTime.now().toString()
                        )
                        chatVm.addLocalGroupMessage(localMsg)
                        messageText = ""
                    }
                },
                onAttach = {},
                isUploading = false
            )
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    GroupMessageBubble(msg = msg, isMe = msg.senderId == currentUserId)
                }
            }
        }
    }
}

@Composable
private fun GroupMessageBubble(msg: com.petmatch.mobile.data.model.GroupMessageResponse, isMe: Boolean) {
    val timeStr = try { DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.parse(msg.sentAt)) } catch (_: Exception) { "" }
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
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 280.dp)) {
            if (!isMe) {
                Text(
                    msg.senderName ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = PrimaryPink),
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(bubbleShape(isMe))
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
