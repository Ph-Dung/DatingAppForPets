package com.petmatch.mobile.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.data.model.ConversationItem
import com.petmatch.mobile.data.model.GroupChatResponse
import com.petmatch.mobile.ui.common.GradientButton
import com.petmatch.mobile.ui.common.PetMatchLoading
import com.petmatch.mobile.ui.common.petMatchGradient
import com.petmatch.mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    val conversations by chatVm.conversations.collectAsState()
    val conversationsLoading by chatVm.conversationsLoading.collectAsState()
    val groups by chatVm.groups.collectAsState()
    val groupLoading by chatVm.groupLoading.collectAsState()

    // ── Reload khi vào lại chat tab (selectedTab == 0) ───────────
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            chatVm.loadConversations(ctx)
            chatVm.loadUserGroups(ctx)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tin nhắn",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("chat/group/create") }) {
                        Icon(Icons.Default.GroupAdd, "Tạo nhóm", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryPink)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Search Bar ──────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("Tìm kiếm cuộc trò chuyện...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = TextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPink,
                    unfocusedBorderColor = Divider,
                    focusedContainerColor = SurfaceLight,
                    unfocusedContainerColor = SurfaceVariant
                ),
                singleLine = true
            )

            // ── Tab Row ─────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryPink,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryPink
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Trực tiếp",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Nhóm chat",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // ── Content ─────────────────────────────────────────────────────
            if (selectedTab == 0) {
                if (conversationsLoading) {
                    PetMatchLoading()
                } else {
                    val filtered = conversations.filter {
                        searchQuery.isEmpty() || it.userName.contains(searchQuery, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        EmptyConversationsPlaceholder()
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(filtered, key = { it.userId }) { conv ->
                                SwipeableConversationItem(
                                    conv = conv,
                                    onClick = { navController.navigate("chat/direct/${conv.userId}/${conv.userName}") },
                                    onDelete = { chatVm.deleteConversation(ctx, conv.userId) },
                                    onMute = { chatVm.muteConversation(conv.userId) }
                                )
                            }
                        }
                    }
                }
            } else {
                if (groupLoading) {
                    PetMatchLoading()
                } else if (groups.isEmpty()) {
                    EmptyGroupsPlaceholder(onCreateGroup = { navController.navigate("chat/group/create") })
                } else {
                    val filteredGroups = groups.filter {
                        searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(filteredGroups, key = { it.id }) { group ->
                            GroupConversationListItem(
                                group = group,
                                onClick = { navController.navigate("chat/group/${group.id}/${group.name}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Swipeable Conversation Item ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationItem(
    conv: ConversationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMute: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    showDeleteDialog = true
                    false  // Chưa dismiss thật — chờ dialog confirm
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onMute()
                    false  // Reset sau mute
                }
                else -> false
            }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null, tint = DislikeRed) },
            title = { Text("Xóa cuộc trò chuyện?") },
            text = {
                Text(
                    "Các tin nhắn sẽ bị xóa khỏi thiết bị của bạn. Nếu nhắn tin lại, cuộc trò chuyện sẽ xuất hiện trở lại.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = DislikeRed)
                ) { Text("Xóa", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Huỷ") }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            // Vuốt phải → Tắt thông báo (xanh)
            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (conv.isMuted) AccentPurple else LikeGreen)
                        .padding(start = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (conv.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            null, tint = Color.White, modifier = Modifier.size(24.dp)
                        )
                        Text(
                            if (conv.isMuted) "Bật TB" else "Tắt TB",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            // Vuốt trái → Xóa (đỏ)
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(DislikeRed)
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text("Xóa", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        },
        content = {
            ConversationListItem(conv = conv, onClick = onClick)
        }
    )
}

// ── Conversation Row ──────────────────────────────────────────────────────────
@Composable
private fun ConversationListItem(conv: ConversationItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = conv.userAvatar ?: "https://placedog.net/60/60?r=${conv.userId}",
                contentDescription = conv.userName,
                modifier = Modifier.size(58.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (conv.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .background(LikeGreen, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conv.userName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (conv.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                    if (conv.isMuted) {
                        Icon(Icons.Default.VolumeOff, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
                Text(
                    text = formatConvTime(conv.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (conv.unreadCount > 0) PrimaryPink else TextSecondary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conv.lastMessage ?: "Hãy bắt đầu trò chuyện!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (conv.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                    ),
                    color = if (conv.unreadCount > 0) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (conv.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            .background(PrimaryPink, CircleShape)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (conv.unreadCount > 99) "99+" else conv.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold, fontSize = 10.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── Group Conversation Row ────────────────────────────────────────────────────
@Composable
private fun GroupConversationListItem(group: GroupChatResponse, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(petMatchGradient),
            contentAlignment = Alignment.Center
        ) {
            if (group.avatarUrl != null) {
                AsyncImage(
                    model = group.avatarUrl,
                    contentDescription = group.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = group.name.firstOrNull()?.toString() ?: "G",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Surface(color = AccentPurple.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        "${group.memberIds.size} TV",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPurple
                    )
                }
            }
            Text(
                text = group.lastMessage ?: "Hãy bắt đầu trò chuyện!",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(Icons.Default.ChevronRight, null, tint = Divider)
    }
}

// ── Empty States ──────────────────────────────────────────────────────────────
@Composable
private fun EmptyConversationsPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💬", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Chưa có cuộc trò chuyện nào",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Match với ai đó để bắt đầu nhắn tin!",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun EmptyGroupsPlaceholder(onCreateGroup: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👥", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Chưa có nhóm chat nào",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tạo nhóm để kết nối với nhiều người yêu thú cưng cùng lúc!",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        GradientButton(text = "Tạo nhóm ngay", onClick = onCreateGroup, modifier = Modifier.fillMaxWidth(0.65f))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun formatConvTime(isoTime: String?): String {
    if (isoTime == null) return ""
    return try {
        val dt = java.time.LocalDateTime.parse(isoTime)
        val now = java.time.LocalDateTime.now()
        when {
            dt.toLocalDate() == now.toLocalDate() -> java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(dt)
            dt.toLocalDate() == now.toLocalDate().minusDays(1) -> "Hôm qua"
            else -> java.time.format.DateTimeFormatter.ofPattern("dd/MM").format(dt)
        }
    } catch (_: Exception) { isoTime }
}
