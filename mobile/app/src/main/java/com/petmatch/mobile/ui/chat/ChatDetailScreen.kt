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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.petmatch.mobile.ui.navigation.Routes
import coil.compose.AsyncImage
import com.petmatch.mobile.data.model.BlockStatus
import com.petmatch.mobile.data.model.MessageResponse
import com.petmatch.mobile.data.model.PetProfileResponse
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.ui.common.petMatchGradient
import com.petmatch.mobile.ui.theme.*
import kotlinx.coroutines.launch
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
    val convs by chatVm.conversations.collectAsState()
    
    // Load pet profile of other user
    var otherPetProfile by remember { mutableStateOf<PetProfileResponse?>(null) }
    
    // Get nickname from conversations
    val otherConversation = convs.find { it.userId == otherUserId }
    val otherNickname = otherConversation?.nickname?.takeIf { it?.isNotBlank() == true }
    
    val otherAvatarUrl = otherPetProfile?.avatarUrl ?: "https://loremflickr.com/40/40/dog?lock=$otherUserId"
    val displayName = otherNickname ?: otherPetProfile?.name ?: otherUserName

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var showAppointmentDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val activateSearchState = navController.currentBackStackEntry?.savedStateHandle?.getStateFlow("activateSearch", false)?.collectAsState(initial = false)
    val activateSearch = activateSearchState?.value ?: false
    LaunchedEffect(activateSearch) {
        if (activateSearch) {
            isSearching = true
            navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("activateSearch")
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Filter messages for search
    val displayedMessages = remember(messages, searchQuery, isSearching) {
        if (isSearching && searchQuery.isNotBlank()) {
            messages.filter { it.content?.contains(searchQuery, ignoreCase = true) == true }
        } else {
            messages
        }
    }

    LaunchedEffect(Unit) {
        chatVm.loadCurrentUserId(ctx)
        chatVm.loadConversations(ctx)  // Load conversations to get nickname
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId > 0) {
            chatVm.loadConversations(ctx)
            chatVm.loadChatHistory(ctx, currentUserId, otherUserId)
            chatVm.markAsRead(ctx, otherUserId, currentUserId)
            chatVm.loadBlockStatus(ctx, otherUserId)
            chatVm.loadUserAppointments(ctx, currentUserId)
        }
    }
    
    // Load pet profile of other user
    LaunchedEffect(otherUserId) {
        try {
            val petResp = RetrofitClient.petApi(ctx).getPetByUserId(otherUserId)
            if (petResp.isSuccessful) {
                otherPetProfile = petResp.body()
            }
        } catch (_: Exception) {}
        // Reload conversations to ensure we have latest nickname
        chatVm.loadConversations(ctx)
    }

    LaunchedEffect(displayedMessages.size) {
        if (displayedMessages.isNotEmpty()) listState.animateScrollToItem(displayedMessages.lastIndex)
    }

    // ── Image Picker ─────────────────────────────────────────────────────────
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            chatVm.uploadAndSendMedia(ctx, it, "IMAGE", currentUserId, otherUserId)
        }
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm kiếm tin nhắn...", color = Color.White.copy(0.7f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .focusRequester(searchFocusRequester),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSearching = false 
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryPink)
                )
            } else {
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
                            model = otherAvatarUrl ?: "https://loremflickr.com/40/40/dog?lock=$otherUserId",
                                contentDescription = displayName,
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
                                displayName,
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
            }
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
                        isUploading = mediaUploading,
                        onVoiceRecord = { voiceUri ->
                            voiceUri?.let {
                                chatVm.uploadAndSendMedia(ctx, it, "VOICE", currentUserId, otherUserId)
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (isSearching) {
            // ── Search Mode: Show search results ──────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (searchQuery.isBlank()) {
                    // Empty state - user hasn't typed anything yet
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🔍", fontSize = 48.sp)
                            Text(
                                "Tìm kiếm tin nhắn",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            Text(
                                "Gõ từ khóa để tìm",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextHint
                            )
                        }
                    }
                } else if (displayedMessages.isEmpty()) {
                    // No results found
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📭", fontSize = 48.sp)
                            Text(
                                "Không tìm thấy tin nhắn",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    // Show search results
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedMessages, key = { it.id }) { msg ->
                            SearchResultItem(
                                msg = msg,
                                searchQuery = searchQuery,
                                isMe = msg.senderId == currentUserId,
                                onClick = {
                                    val index = messages.indexOfFirst { it.id == msg.id }
                                    if (index >= 0) {
                                        scope.launch {
                                            listState.animateScrollToItem(index)
                                            isSearching = false
                                            searchQuery = ""
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else if (loading && messages.isEmpty()) {
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
                    if (msg.type == "APPOINTMENT") {
                        AppointmentBubble(msg, isMe = msg.senderId == currentUserId)
                    } else {
                        MessageBubble(msg = msg, isMe = msg.senderId == currentUserId, avatarUrl = if (msg.senderId == currentUserId) null else otherAvatarUrl)
                    }
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
private fun MessageBubble(msg: MessageResponse, isMe: Boolean, avatarUrl: String? = null) {
    val timeStr = try {
        DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.parse(msg.sentAt))
    } catch (_: Exception) { "" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            AsyncImage(
                model = avatarUrl ?: "https://loremflickr.com/32/32/dog?lock=${msg.senderId}",
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
                "APPOINTMENT" -> AppointmentBubble(msg, isMe)
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
private fun AppointmentBubble(msg: MessageResponse, isMe: Boolean) {
    val ctx = LocalContext.current
    val chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val liveAppointments by chatVm.appointments.collectAsState()

    var id: Long = 0
    var location = ""
    var time = ""
    var status = "PENDING"
    try {
        val json = org.json.JSONObject(msg.content ?: "{}")
        id = json.optLong("id", 0)
        location = json.optString("location", "")
        time = json.optString("time", "")
        status = json.optString("status", "PENDING")
    } catch (_: Exception) {}

    // Ghi đè trạng thái realtime nếu được cập nhật (trong phiên này hoặc đã load)
    val liveAptt = liveAppointments.find { it.id == id }
    if (liveAptt != null) {
        status = liveAptt.status
    }

    val statusColor = when (status) {
        "PENDING" -> SecondaryOrange
        "CONFIRMED" -> LikeGreen
        "CANCELLED" -> DislikeRed
        else -> TextSecondary
    }

    Box(
        modifier = Modifier
            .clip(bubbleShape(isMe))
            .background(
                if (isMe)
                    Brush.linearGradient(listOf(GradientStart, GradientEnd), Offset.Zero, Offset(Float.POSITIVE_INFINITY, 0f))
                else Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
            )
            .padding(14.dp)
            .widthIn(min = 200.dp, max = 240.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📅 Cuộc hẹn", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (isMe) Color.White else PrimaryPink))
            HorizontalDivider(color = if (isMe) Color.White.copy(0.3f) else Divider)
            Text("📍 $location", style = MaterialTheme.typography.bodySmall, color = if (isMe) Color.White else TextPrimary)
            Text("⏰ $time", style = MaterialTheme.typography.bodySmall, color = if (isMe) Color.White else TextPrimary)
            
            if (status == "PENDING" && !isMe) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { chatVm.updateAppointmentStatus(ctx, id, "CONFIRMED") },
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LikeGreen)
                    ) { Text("Chấp nhận", fontSize = 12.sp, color = Color.White) }
                    Button(
                        onClick = { chatVm.updateAppointmentStatus(ctx, id, "CANCELLED") },
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DislikeRed)
                    ) { Text("Từ chối", fontSize = 12.sp, color = Color.White) }
                }
            } else if (status != "PENDING") {
                val statusText = if (status == "CONFIRMED") "Đã chấp nhận ✔️" else "Đã từ chối ✖️"
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = if (status == "PENDING") "Đang đợi phản hồi..." else if (status == "CONFIRMED") "Đã chấp nhận" else if (status == "CANCELLED") "Đã từ chối" else status,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = if (isMe) Color.White.copy(0.8f) else statusColor
                )
            }
        }
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
    var showFullImage by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(200.dp, 150.dp)
            .clip(bubbleShape(isMe))
            .background(Color.LightGray.copy(alpha = 0.3f))
            .clickable { showFullImage = true }
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current)
                .data(msg.mediaUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Hình ảnh",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }

    if (showFullImage) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullImage = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { showFullImage = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(msg.mediaUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Hình ảnh Full",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
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
    isUploading: Boolean,
    onVoiceRecord: (Uri?) -> Unit = {},
    isGroupChat: Boolean = false
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
            } else if (!isGroupChat) {
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
                if (!isGroupChat) {
                    VoiceRecordButton(onVoiceReady = onVoiceRecord)
                }
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

// ── Search Result Item ────────────────────────────────────────────────────────
@Composable
private fun SearchResultItem(
    msg: MessageResponse,
    searchQuery: String,
    isMe: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) PrimaryPink else SurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Sender info
            Text(
                text = if (isMe) "Bạn" else "Họ",
                style = MaterialTheme.typography.labelSmall,
                color = if (isMe) Color.White else TextSecondary
            )

            // Message content with highlight
            val content = msg.content ?: ""
            val regex = Regex(searchQuery, RegexOption.IGNORE_CASE)
            val annotatedString = buildAnnotatedString {
                var lastIndex = 0
                regex.findAll(content).forEach { match ->
                    append(content.substring(lastIndex, match.range.first))
                    withStyle(
                        style = SpanStyle(
                            background = if (isMe) Color.White.copy(0.3f) else LikeGreen.copy(0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(match.value)
                    }
                    lastIndex = match.range.last + 1
                }
                append(content.substring(lastIndex))
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall,
                color = if (isMe) Color.White else TextPrimary
            )

            // Timestamp
            Text(
                text = try {
                    DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.parse(msg.sentAt))
                } catch (_: Exception) { msg.sentAt ?: "" },
                style = MaterialTheme.typography.labelSmall,
                color = if (isMe) Color.White.copy(0.7f) else TextSecondary
            )
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
    val groups by chatVm.groups.collectAsState()
    val currentGroup = groups.find { it.id == groupId }
    val conversations by chatVm.conversations.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    var showMembersSheet by remember { mutableStateOf(false) }
    var showAddMemberSheet by remember { mutableStateOf(false) }
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
                        modifier = Modifier.clickable { showMembersSheet = true },
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
                            Text("${currentGroup?.members?.size ?: 0} thành viên", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(0.8f)))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMemberSheet = true }) { 
                        Icon(Icons.Default.GroupAdd, null, tint = Color.White) 
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
                            id = System.currentTimeMillis(), groupId = groupId, senderId = currentUserId,
                            senderName = "Tôi", senderAvatarUrl = null, content = messageText.trim(),
                            sentAt = LocalDateTime.now().toString()
                        )
                        chatVm.addLocalGroupMessage(localMsg)
                        chatVm.sendGroupMessage(ctx, groupId, currentUserId, messageText.trim())
                        messageText = ""
                    }
                },
                onAttach = {},
                isUploading = false,
                isGroupChat = true
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

    if (showMembersSheet && currentGroup != null) {
        ModalBottomSheet(onDismissRequest = { showMembersSheet = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Thành viên nhóm (${currentGroup.members.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(currentGroup.members, key = { it.userId }) { member ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = member.avatarUrl ?: "https://loremflickr.com/40/40/dog?lock=${member.userId}",
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(member.fullName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text("Vai trò: ${member.role}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showAddMemberSheet && currentGroup != null) {
        val existingMemberIds = currentGroup.memberIds
        val friendsToAdd = conversations.filter { it.userId !in existingMemberIds }
        
        ModalBottomSheet(onDismissRequest = { showAddMemberSheet = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Thêm thành viên", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                if (friendsToAdd.isEmpty()) {
                    Text("Không có bạn bè nào để thêm.", color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(friendsToAdd, key = { it.userId }) { friend ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    chatVm.addGroupMember(ctx, groupId, friend.userId) { showAddMemberSheet = false }
                                }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = friend.userAvatar ?: "https://loremflickr.com/40/40/dog?lock=${friend.userId}",
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(friend.userName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.AddCircleOutline, null, tint = PrimaryPink)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
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
                model = msg.senderAvatarUrl ?: "https://loremflickr.com/32/32/dog?lock=${msg.senderId}",
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
