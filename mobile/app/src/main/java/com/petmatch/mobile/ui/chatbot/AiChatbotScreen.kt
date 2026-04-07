package com.petmatch.mobile.ui.chatbot

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.Constants
import com.petmatch.mobile.data.model.ChatMessage
import com.petmatch.mobile.data.model.PetProfileResponse
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.petprofile.PetProfileViewModel
import com.petmatch.mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatbotScreen(
    navController: NavController,
    chatbotVm: ChatbotViewModel,
    petVm: PetProfileViewModel
) {
    val ctx = LocalContext.current
    val messages by chatbotVm.messages.collectAsState()
    val isLoading by chatbotVm.isLoading.collectAsState()
    val suggestions by chatbotVm.suggestions.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new message or suggestions arrive
    LaunchedEffect(messages.size, suggestions.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount)
        }
    }

    // Welcome message on first open
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            chatbotVm.sendMessage(ctx, "Xin chào! Tôi muốn tìm bạn đôi cho thú cưng của mình.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // AI avatar
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(AccentPurple, PrimaryPink))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                "PetMatch AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "Trợ lý tìm bạn đôi thú cưng",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { chatbotVm.resetConversation() }) {
                        Icon(Icons.Default.Refresh, "Bắt đầu lại", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AccentPurple
                )
            )
        },
        bottomBar = {
            // Input bar
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Nhập yêu cầu của bạn...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    // Send button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (!isLoading && inputText.isNotBlank())
                                    Brush.linearGradient(listOf(AccentPurple, PrimaryPink))
                                else
                                    Brush.linearGradient(listOf(Color.Gray, Color.Gray))
                            )
                            .clickable(enabled = !isLoading && inputText.isNotBlank()) {
                                val text = inputText.trim()
                                inputText = ""
                                chatbotVm.sendMessage(ctx, text)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }

            // Loading indicator
            if (isLoading) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(AccentPurple.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier.size(6.dp).clip(CircleShape)
                                            .background(AccentPurple.copy(0.5f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pet suggestions
            if (suggestions.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "🐾 Hồ sơ phù hợp với yêu cầu của bạn:",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = AccentPurple
                        )
                        suggestions.forEach { pet ->
                            SuggestedPetCard(
                                pet = pet,
                                onLike = {
                                    petVm.sendLikeFromChatbot(ctx, pet.id)
                                    navController.navigate(Routes.petDetail(pet.id))
                                },
                                onViewDetail = {
                                    navController.navigate(Routes.petDetail(pet.id))
                                }
                            )
                        }
                        TextButton(
                            onClick = { chatbotVm.clearSuggestions() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Tìm kiếm khác", color = AccentPurple)
                        }
                    }
                }
            }

            // Quick reply chips
            if (!isLoading && messages.isNotEmpty() && suggestions.isEmpty()) {
                item {
                    QuickReplies { reply ->
                        chatbotVm.sendMessage(ctx, reply)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(AccentPurple.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = AccentPurple, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(6.dp))
        }

        Surface(
            color = if (isUser) AccentPurple else MaterialTheme.colorScheme.surface,
            shape = if (isUser)
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            else
                RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            shadowElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }

        if (isUser) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(PrimaryPink.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = PrimaryPink, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SuggestedPetCard(
    pet: PetProfileResponse,
    onLike: () -> Unit,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Pet image header
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                AsyncImage(
                    model = if (pet.avatarUrl.isNullOrEmpty()) "https://placedog.net/400/200?id=${pet.id}" else pet.avatarUrl,
                    contentDescription = pet.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f))))
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                ) {
                    Text(
                        "${pet.name}${if (pet.age != null) ", ${pet.age} tuổi" else ""}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (!pet.breed.isNullOrBlank()) {
                        Text(pet.breed, color = Color.White.copy(0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Info row
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pet.gender?.let { g ->
                    AssistChip(
                        onClick = {},
                        label = { Text(Constants.GENDER_LABELS[g] ?: g, style = MaterialTheme.typography.labelSmall) }
                    )
                }
                pet.weightKg?.let { w ->
                    AssistChip(onClick = {}, label = { Text("${w}kg", style = MaterialTheme.typography.labelSmall) })
                }
                val healthLabel = Constants.HEALTH_STATUS_LABELS[pet.healthStatus]
                if (healthLabel != null) {
                    AssistChip(onClick = {}, label = { Text(healthLabel, style = MaterialTheme.typography.labelSmall) })
                }
            }

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetail,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.RemoveRedEye, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Xem chi tiết")
                }
                Button(
                    onClick = onLike,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LikeGreen)
                ) {
                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Thích ❤️")
                }
            }
        }
    }
}

@Composable
private fun QuickReplies(onSelect: (String) -> Unit) {
    val quickReplies = listOf(
        "Tôi muốn tìm bạn đôi cho chó",
        "Tôi muốn tìm bạn đôi cho mèo",
        "Tìm Poodle giới tính cái",
        "Thú cưng khỏe mạnh, 1-3 tuổi",
        "Mục đích phối giống"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Gợi ý nhanh:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickReplies) { reply ->
                SuggestionChip(
                    onClick = { onSelect(reply) },
                    label = { Text(reply, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
