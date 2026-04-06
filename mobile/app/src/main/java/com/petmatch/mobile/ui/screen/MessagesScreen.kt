package com.petmatch.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmatch.mobile.ui.theme.*

data class MatchStory(
    val id: Int,
    val name: String,
    val petImageRes: Int,
    val isNew: Boolean = false
)

data class Conversation(
    val id: Int,
    val name: String,
    val petImageRes: Int,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

val sampleMatches = listOf(
    MatchStory(1, "Claus", 0, true),
    MatchStory(2, "Bella", 0, true),
    MatchStory(3, "Max", 0),
    MatchStory(4, "Luna", 0),
    MatchStory(5, "Rocky", 0),
)

val sampleConversations = listOf(
    Conversation(1, "Trần Thị Lan", 0, "Bé nhà mình rất thích đi dạo 🐾", "10:30", 2, true),
    Conversation(2, "Nguyễn Văn An", 0, "Hẹn gặp bé Golden vào thứ 7 nhé!", "09:15", 0, false),
    Conversation(3, "Lê Thị Hoa", 0, "Bé Bella của bạn đáng yêu quá 😍", "Hôm qua", 1, true),
    Conversation(4, "Phạm Minh Tuấn", 0, "Cảm ơn bạn đã xác nhận lịch hẹn", "Hôm qua", 0, false),
    Conversation(5, "Hoàng Thu Thảo", 0, "Bé nhà mình bị dị ứng phấn hoa nè...", "T2", 0, false),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onConversationClick: (Int) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        // Top bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Tin nhắn",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Tìm kiếm...", color = TextHint, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = BackgroundLight,
                        unfocusedContainerColor = BackgroundLight
                    ),
                    singleLine = true
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // New matches section
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Kết đôi mới ✨",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sampleMatches) { match ->
                            NewMatchItem(match = match)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = DividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tin nhắn",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            // Conversation list
            items(sampleConversations) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    onClick = { onConversationClick(conversation.id) }
                )
            }
        }
    }
}

@Composable
fun NewMatchItem(match: MatchStory) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Pet avatar with pink gradient ring
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryPink, AccentPink, LightPink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD4845A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐕", fontSize = 28.sp)
                }
            }
            // New badge
            if (match.isNew) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ActionGreen)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = match.name,
            fontSize = 12.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD4845A)),
                contentAlignment = Alignment.Center
            ) {
                Text("🐕", fontSize = 24.sp)
            }
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(White)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(ActionGreen)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Message info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.name,
                    fontSize = 15.sp,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = conversation.time,
                    fontSize = 12.sp,
                    color = if (conversation.unreadCount > 0) PrimaryPink else TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage,
                    fontSize = 13.sp,
                    color = if (conversation.unreadCount > 0) TextPrimary else TextSecondary,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(PrimaryPink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            fontSize = 11.sp,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
