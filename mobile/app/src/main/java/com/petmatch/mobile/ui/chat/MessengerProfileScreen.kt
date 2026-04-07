package com.petmatch.mobile.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerProfileScreen(
    navController: NavController,
    userId: Long,
    userName: String
) {
    val scrollState = rememberScrollState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentNickname by remember { mutableStateOf<String?>(null) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf("") }
    
    val displayTitle = currentNickname?.takeIf { it.isNotBlank() } ?: userName

    // Load nickname
    LaunchedEffect(userId) {
        try {
            val resp = RetrofitClient.chatApi(ctx).getNickname(userId)
            if (resp.isSuccessful) {
                currentNickname = resp.body()?.get("nickname")
            }
        } catch (_: Exception) {}
    }

    // URL ảnh tạm để UI demo
    val avatarUrl = "https://placedog.net/400/400?r=$userId"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, "Quay lại", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Avatar
            AsyncImage(
                model = avatarUrl,
                contentDescription = displayTitle,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(16.dp))

            // Name
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            
            if (currentNickname?.isNotBlank() == true) {
                Text(
                    text = "($userName)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(24.dp))

            // 4 Circular Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileActionButton(icon = Icons.Default.Call, label = "Audio") {
                    navController.navigate(Routes.call(peerId = userId, peerName = userName, callType = "AUDIO"))
                }
                ProfileActionButton(icon = Icons.Default.Videocam, label = "Video") {
                    navController.navigate(Routes.call(peerId = userId, peerName = userName, callType = "VIDEO"))
                }
                ProfileActionButton(icon = Icons.Default.Person, label = "Profile") {
                    // Navigate to user profile
                }
                ProfileActionButton(icon = Icons.Default.NotificationsOff, label = "Mute") {
                    // Mute logic
                }
            }

            Spacer(Modifier.height(32.dp))

            // Options List
            ProfileOptionRow(label = "Color", isColorPicker = true, onClick = { /* TODO: Đổi màu chat (Local state) */ })
            ProfileOptionRow(label = "Nicknames", onClick = { 
                nicknameInput = currentNickname ?: ""
                showNicknameDialog = true 
            })

            Spacer(Modifier.height(24.dp))
            SectionHeader("MORE ACTIONS")
            ProfileOptionRow(label = "Search in Conversation", icon = Icons.Default.Search, onClick = { })
            ProfileOptionRow(label = "Create group", icon = Icons.Default.GroupAdd, onClick = { navController.navigate(Routes.GROUP_CHAT_CREATE) })
            ProfileOptionRow(label = "Create an appointment", icon = Icons.Default.Event, onClick = {
                navController.navigate(Routes.appointment(userId, userName))
            })

            Spacer(Modifier.height(24.dp))
            SectionHeader("PRIVACY")
            ProfileOptionRow(label = "Review", onClick = { navController.navigate(Routes.review(userId, userName)) })
            ProfileOptionRow(label = "Block", onClick = { /* TODO: Block user via modal */ })
            ProfileOptionRow(label = "Something isn't working", onClick = { /* Report */ })

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Đổi biệt danh", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    label = { Text("Biệt danh mới") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val req = mapOf("nickname" to nicknameInput.trim())
                            val resp = RetrofitClient.chatApi(ctx).setNickname(userId, req)
                            if (resp.isSuccessful) {
                                currentNickname = resp.body()?.get("nickname")
                            }
                        } catch (_: Exception) {}
                        showNicknameDialog = false
                    }
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun ProfileActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(SurfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = TextPrimary, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextSecondary
        )
    }
}

@Composable
private fun ProfileOptionRow(
    label: String,
    icon: ImageVector? = null,
    isColorPicker: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )

        if (isColorPicker) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(PrimaryPink, CircleShape) // Hiện tại hardcode màu Theme
                    .padding(4.dp)
                    .background(Color.White, CircleShape)
            )
        } else if (icon != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Divider)
        }
    }
}
