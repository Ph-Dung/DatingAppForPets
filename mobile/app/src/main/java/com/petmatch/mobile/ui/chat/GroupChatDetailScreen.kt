package com.petmatch.mobile.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.data.model.GroupChatResponse
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.ui.common.petMatchGradient
import com.petmatch.mobile.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatDetailScreen(
    navController: NavController,
    groupId: Long,
    groupName: String,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val currentUserId by chatVm.currentUserId.collectAsState()
    val groupMessages by chatVm.groupMessages.collectAsState()
    val loading by chatVm.groupLoading.collectAsState()
    val mediaUploading by chatVm.mediaUploadLoading.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Image picker for group chat
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            chatVm.uploadAndSendGroupMedia(ctx, it, "IMAGE", groupId)
        }
    }

    LaunchedEffect(Unit) {
        chatVm.loadCurrentUserId(ctx)
    }

    LaunchedEffect(currentUserId, groupId) {
        if (currentUserId > 0) {
            chatVm.loadGroupHistory(ctx, groupId)
        }
    }

    LaunchedEffect(groupMessages.size) {
        if (groupMessages.isNotEmpty()) listState.animateScrollToItem(groupMessages.lastIndex)
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryPink)
            )
        },
        bottomBar = {
            Column {
                GroupChatInputBar(
                    text = messageText,
                    onTextChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            chatVm.sendGroupMessage(ctx, groupId, currentUserId, messageText.trim())
                            messageText = ""
                        }
                    },
                    onAttachImage = { imagePicker.launch("image/*") },
                    isUploading = mediaUploading
                )
            }
        }
    ) { padding ->
        if (loading && groupMessages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPink)
            }
        } else if (groupMessages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💬", fontSize = 56.sp)
                    Text(
                        "Chưa có tin nhắn nào!",
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
                items(groupMessages, key = { it.id }) { msg ->
                    GroupMessageBubble(msg, isMe = msg.senderId == currentUserId)
                }
            }
        }
    }
}

@Composable
private fun GroupMessageBubble(msg: com.petmatch.mobile.data.model.GroupMessageResponse, isMe: Boolean) {
    val timeStr = try {
        DateTimeFormatter.ofPattern("HH:mm").format(java.time.LocalDateTime.parse(msg.sentAt))
    } catch (_: Exception) { "" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMe) {
                Text(
                    msg.senderName ?: "Thành viên",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            
            val isImage = msg.content.startsWith("http") && (msg.content.endsWith(".jpg") || msg.content.endsWith(".png") || msg.content.endsWith(".gif") || msg.content.contains("cloudinary"))
            when {
                isImage -> {
                    AsyncImage(
                        model = msg.content,
                        contentDescription = "Ảnh trong nhóm",
                        modifier = Modifier
                            .widthIn(max = 200.dp)
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isMe) Brush.linearGradient(listOf(GradientStart, GradientEnd))
                                else Brush.linearGradient(listOf(SurfaceVariant, SurfaceVariant))
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            msg.content ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isMe) Color.White else TextPrimary
                        )
                    }
                }
            }
            
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextHint)
        }
    }
}

@Composable
private fun GroupChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    isUploading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAttachImage,
            modifier = Modifier.size(40.dp),
            enabled = !isUploading
        ) {
            Icon(Icons.Default.AttachFile, "Gửi ảnh", tint = PrimaryPink, modifier = Modifier.size(24.dp))
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp, max = 120.dp),
            placeholder = { Text("Nhắn tin...") },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPink,
                unfocusedBorderColor = Divider
            ),
            enabled = !isUploading
        )

        IconButton(
            onClick = onSend,
            modifier = Modifier.size(40.dp),
            enabled = text.isNotBlank() && !isUploading
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = if (text.isNotBlank() && !isUploading) PrimaryPink else TextSecondary)
        }
    }
}
