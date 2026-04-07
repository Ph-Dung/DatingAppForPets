package com.petmatch.mobile.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

// ─────────────────────────────────────────────────────────────────────────────
// ChatListScreen – Danh sách hội thoại chính
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }  // 0=Direct, 1=Groups

    val groups by chatVm.groups.collectAsState()
    val groupLoading by chatVm.groupLoading.collectAsState()

    // Dữ liệu mẫu conversations (trong app thật sẽ load từ WebSocket/API)
    val conversations = remember {
        listOf(
            ConversationItem(1L, "Mèo Bông", null, "Chào bạn! Thú cưng của bạn dễ thương quá 🐱", "10:30", 3L, true),
            ConversationItem(2L, "Chó Đốm", null, "Bao giờ chúng ta gặp nhau nhỉ?", "Hôm qua", 1L, false),
            ConversationItem(3L, "Thỏ Trắng", null, "Ok mình sẽ liên lạc sau nhé", "T2", 0L, true),
            ConversationItem(4L, "Hamster Gold", null, "Cảm ơn bạn đã match! 🐹", "T6", 0L, false),
            ConversationItem(5L, "Chó Phốc", null, "Lịch hẹn lúc 3h chiều nha 📅", "T5", 2L, true),
        )
    }

    LaunchedEffect(Unit) {
        chatVm.loadUserGroups(ctx)
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
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.FilterList, "Lọc", tint = Color.White)
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

            // ── Active Story Row ────────────────────────────────────────────
            if (selectedTab == 0) {
                Column {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(conversations.filter { it.isOnline }.take(5)) { conv ->
                            ActiveStoryItem(conv)
                        }
                    }
                    HorizontalDivider(color = Divider)
                }
            }

            // ── Content ─────────────────────────────────────────────────────
            if (selectedTab == 0) {
                // Direct messages
                val filtered = conversations.filter {
                    searchQuery.isEmpty() || it.userName.contains(searchQuery, ignoreCase = true)
                }
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(filtered, key = { it.userId }) { conv ->
                        ConversationListItem(
                            conv = conv,
                            onClick = { navController.navigate("chat/direct/${conv.userId}/${conv.userName}") }
                        )
                    }
                }
            } else {
                // Group chats
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

// ── Active Story Avatar ───────────────────────────────────────────────────────
@Composable
private fun ActiveStoryItem(conv: ConversationItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            AsyncImage(
                model = conv.userAvatar ?: "https://placedog.net/56/56?r=${conv.userId}",
                contentDescription = conv.userName,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(2.5.dp, Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                        CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
            // Online indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .background(LikeGreen, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        Text(
            text = conv.userName.split(" ").firstOrNull() ?: conv.userName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Conversation Row ──────────────────────────────────────────────────────────
@Composable
private fun ConversationListItem(conv: ConversationItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
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

        // Content
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conv.userName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (conv.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                Text(
                    text = conv.lastMessageTime ?: "",
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
                    text = conv.lastMessage,
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
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
        // Group avatar
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
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                Surface(
                    color = AccentPurple.copy(0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "${group.memberIds.size} thành viên",
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

// ── Empty Groups Placeholder ──────────────────────────────────────────────────
@Composable
private fun EmptyGroupsPlaceholder(onCreateGroup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
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
        GradientButton(
            text = "Tạo nhóm ngay",
            onClick = onCreateGroup,
            modifier = Modifier.fillMaxWidth(0.65f)
        )
    }
}
