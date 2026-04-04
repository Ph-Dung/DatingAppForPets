package com.petmatch.mobile.ui.match

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.petmatch.mobile.data.model.MatchRequestResponse
import com.petmatch.mobile.ui.common.*
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchedListScreen(navController: NavController, matchVm: MatchViewModel) {
    val ctx = LocalContext.current
    val matched by matchVm.matched.collectAsState()

    LaunchedEffect(Unit) { matchVm.loadMatched(ctx) }

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = "Đã ghép đôi 🎉",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (matched.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("💔", fontSize = 72.sp)
                    Text("Chưa có cặp đôi nào", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Bắt đầu quẹt để tìm người bạn đồng hành cho thú cưng nhé!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GradientButton(
                        text = "Khám phá ngay",
                        onClick = { navController.navigate(Routes.MATCH_SWIPE) { popUpTo(Routes.MATCHED_LIST) { inclusive = true } } },
                        modifier = Modifier.fillMaxWidth(0.65f)
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "${matched.size} cặp đôi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(matched, key = { it.id }) { req ->
                MatchedCard(req = req, navController = navController)
            }
        }
    }
}

@Composable
private fun MatchedCard(req: MatchRequestResponse, navController: NavController) {
    // Determine "the other pet" based on context
    val otherName   = req.receiverPetName ?: req.senderPetName ?: "?"
    val otherAvatar = req.receiverPetAvatarUrl ?: req.senderPetAvatarUrl
    val otherPetId  = req.receiverPetId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Routes.petDetail(otherPetId)) },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with match ring
            Box {
                AsyncImage(
                    model = otherAvatar ?: "https://placedog.net/80/80",
                    contentDescription = otherName,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(3.dp, Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                            CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
                // Match icon overlay
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    color = PrimaryPink, shape = CircleShape
                ) {
                    Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.padding(4.dp).size(12.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(otherName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Surface(
                    color = LikeGreen.copy(0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "✓ Đã ghép đôi",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LikeGreen
                    )
                }
                if (req.isSuperLike) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, null, tint = SuperLikeGold, modifier = Modifier.size(14.dp))
                        Text("Siêu thích", style = MaterialTheme.typography.labelSmall, color = SuperLikeAmber)
                    }
                }
            }

            // Chat button (if canOpenConversation)
            if (req.canOpenConversation) {
                IconButton(
                    onClick = { /* TODO: navigate to chat */ },
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryPink.copy(0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Chat, "Nhắn tin", tint = PrimaryPink)
                }
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
