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
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    
    // Member management state
    var showMembersDialog by remember { mutableStateOf(false) }
    var groupDetails by remember { mutableStateOf<com.petmatch.mobile.data.model.GroupChatResponse?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberIdInput by remember { mutableStateOf("") }
    var availableFriends by remember { mutableStateOf<List<com.petmatch.mobile.data.model.ConversationItem>>(emptyList()) }
    var friendPetProfiles by remember { mutableStateOf<Map<Long, com.petmatch.mobile.data.model.PetProfileResponse>>(emptyMap()) }
    
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

    // ── Load group details for member list ────────────────────
    LaunchedEffect(groupId) {
        try {
            val resp = RetrofitClient.groupChatApi(ctx).getUserGroups()
            if (resp.isSuccessful) {
                groupDetails = resp.body()?.find { it.id == groupId }
            }
        } catch (_: Exception) {}
    }

    // ── Load available friends for adding to group ────────────────
    LaunchedEffect(showAddMemberDialog, groupDetails) {
        if (showAddMemberDialog && groupDetails != null) {
            try {
                // Load conversations (friends list)
                val convResp = RetrofitClient.chatApi(ctx).getConversations()
                if (convResp.isSuccessful) {
                    val conversations = convResp.body() ?: emptyList()
                    val currentMemberIds = groupDetails!!.members.map { it.userId }.toSet()
                    
                    // Filter out members already in group
                    availableFriends = conversations.filter { !currentMemberIds.contains(it.userId) }
                    
                    // Load pet profiles for all available friends
                    val profileMap = mutableMapOf<Long, com.petmatch.mobile.data.model.PetProfileResponse>()
                    availableFriends.forEach { friend ->
                        try {
                            val petResp = RetrofitClient.petApi(ctx).getPetByUserId(friend.userId)
                            if (petResp.isSuccessful) {
                                petResp.body()?.let { profileMap[friend.userId] = it }
                            }
                        } catch (_: Exception) {}
                    }
                    friendPetProfiles = profileMap
                }
            } catch (_: Exception) {}
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
                    Column(modifier = Modifier.clickable { if (groupDetails != null) showMembersDialog = true }) {
                        Text(
                            groupName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            if (groupDetails != null) "${groupDetails!!.members.size} thành viên" else "Nhóm chat",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(0.8f))
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMembersDialog = true }) {
                        Icon(Icons.Default.People, null, tint = Color.White)
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

    // ── Members Dialog ────────────────────────────────────────────────────
    if (showMembersDialog && groupDetails != null) {
        AlertDialog(
            onDismissRequest = { showMembersDialog = false },
            title = { Text("Thành viên nhóm (${groupDetails!!.members.size})") },
            text = {
                LazyColumn {
                    items(groupDetails!!.members, key = { it.userId }) { member ->
                        MemberItem(member, ctx)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = true }) {
                    Text("Thêm thành viên")
                }
            },
            confirmButton = {
                TextButton(onClick = { showMembersDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // ── Add Member Dialog ────────────────────────────────────────────────────
    if (showAddMemberDialog && groupDetails != null) {
        // Filter suggestions by pet name, nickname, or user name
        val filteredFriends = if (newMemberIdInput.isNotBlank()) {
            availableFriends.filter { friend ->
                val petName = friendPetProfiles[friend.userId]?.name ?: ""
                val nickname = friend.nickname ?: ""
                val userName = friend.userName
                val query = newMemberIdInput.lowercase()
                petName.lowercase().contains(query) || 
                nickname.lowercase().contains(query) || 
                userName.lowercase().contains(query)
            }
        } else {
            availableFriends
        }
        
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Thêm thành viên vào nhóm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Tìm kiếm theo tên pet, biệt danh hoặc tên người dùng",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = newMemberIdInput,
                        onValueChange = { newMemberIdInput = it },
                        label = { Text("Tìm kiếm...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Nhập tên...") }
                    )
                    
                    // Show friend suggestions
                    if (availableFriends.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(filteredFriends, key = { it.userId }) { friend ->
                                FriendSuggestionItem(
                                    friend = friend,
                                    petProfile = friendPetProfiles[friend.userId],
                                    onSelect = {
                                        scope.launch {
                                            try {
                                                val resp = RetrofitClient.groupChatApi(ctx).addMember(groupId, friend.userId)
                                                if (resp.isSuccessful) {
                                                    android.widget.Toast.makeText(ctx, "Đã thêm ${friendPetProfiles[friend.userId]?.name ?: friend.userName}!", android.widget.Toast.LENGTH_SHORT).show()
                                                    showAddMemberDialog = false
                                                    newMemberIdInput = ""
                                                    // Reload group details
                                                    val groupResp = RetrofitClient.groupChatApi(ctx).getUserGroups()
                                                    if (groupResp.isSuccessful) {
                                                        groupDetails = groupResp.body()?.find { it.id == groupId }
                                                    }
                                                } else {
                                                    android.widget.Toast.makeText(ctx, "Không thể thêm thành viên", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(ctx, "Lỗi: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    } else if (newMemberIdInput.isBlank()) {
                        Text(
                            "Không có bạn bè khác trong danh sách hoặc tất cả đều đã là thành viên",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            "Không tìm thấy kết quả",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddMemberDialog = false
                    newMemberIdInput = ""
                }) {
                    Text("Hủy")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Đóng")
                }
            }
        )
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

@Composable
private fun MemberItem(member: com.petmatch.mobile.data.model.GroupMemberResponse, ctx: android.content.Context) {
    var petProfile by remember { mutableStateOf<com.petmatch.mobile.data.model.PetProfileResponse?>(null) }
    
    LaunchedEffect(member.userId) {
        try {
            val resp = RetrofitClient.petApi(ctx).getPetByUserId(member.userId)
            if (resp.isSuccessful) {
                petProfile = resp.body()
            }
        } catch (_: Exception) {}
    }
    
    val displayName = petProfile?.name ?: member.fullName
    val displayAvatar = petProfile?.avatarUrl ?: member.avatarUrl ?: "https://loremflickr.com/40/40/dog?lock=${member.userId}"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = displayAvatar,
            contentDescription = displayName,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
            if (member.role == "ADMIN") {
                Text(
                    "Quản trị viên",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryPink
                )
            }
        }
    }
}

@Composable
private fun FriendSuggestionItem(
    friend: com.petmatch.mobile.data.model.ConversationItem,
    petProfile: com.petmatch.mobile.data.model.PetProfileResponse?,
    onSelect: () -> Unit
) {
    val displayName = petProfile?.name ?: friend.userName
    val displayAvatar = petProfile?.avatarUrl ?: friend.userAvatar ?: "https://loremflickr.com/40/40/dog?lock=${friend.userId}"
    val subtitle = if (petProfile?.name != null && friend.nickname?.isNotBlank() == true) {
        "Biệt danh: ${friend.nickname}"
    } else if (petProfile?.name != null) {
        friend.userName
    } else {
        ""
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = displayAvatar,
            contentDescription = displayName,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
