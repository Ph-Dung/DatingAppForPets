package com.petmatch.mobile.ui.match

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import com.petmatch.mobile.data.model.MatchRequestResponse
import com.petmatch.mobile.ui.common.*
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.petprofile.PetProfileViewModel
import com.petmatch.mobile.ui.chat.ChatViewModel
import com.petmatch.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoLikedMeScreen(
    navController: NavController,
    matchVm: MatchViewModel,
    petVm: PetProfileViewModel,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val whoLikedMe by matchVm.whoLikedMe.collectAsState()

    LaunchedEffect(Unit) { matchVm.loadWhoLikedMe(ctx) }

    val superLikes = whoLikedMe.filter { it.isSuperLike }
    val regular    = whoLikedMe.filter { !it.isSuperLike }

    Scaffold(
        topBar = {
            PetMatchTopBar(title = "Ai đã thích tôi")
        }
    ) { padding ->
        if (whoLikedMe.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("💝", fontSize = 72.sp)
                    Text("Chưa ai thích bạn", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Hãy kiên nhẫn – thú cưng hoàn hảo đang trên đường đến!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GradientButton(
                        text = "Đi khám phá",
                        onClick = { navController.navigate(Routes.MATCH_SWIPE) { popUpTo(Routes.WHO_LIKED_ME) { inclusive = true } } },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Super Like section header
            if (superLikes.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = SuperLikeGold, modifier = Modifier.size(20.dp))
                        Text(
                            "Siêu Thích (${superLikes.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SuperLikeAmber
                        )
                    }
                }
                items(superLikes) { req ->
                    WhoLikedMeCard(
                        req = req,
                        navController = navController,
                        matchVm = matchVm,
                        chatVm = chatVm,
                        isSuperLike = true
                    )
                }
            }

            // Regular likes section header
            if (regular.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Favorite, null, tint = PrimaryPink, modifier = Modifier.size(20.dp))
                        Text(
                            "Đã thích (${regular.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryPink
                        )
                    }
                }
                items(regular) { req ->
                    WhoLikedMeCard(
                        req = req,
                        navController = navController,
                        matchVm = matchVm,
                        chatVm = chatVm,
                        isSuperLike = false
                    )
                }
            }
        }
    }
}

@Composable
private fun WhoLikedMeCard(
    req: MatchRequestResponse,
    navController: NavController,
    matchVm: MatchViewModel,
    chatVm: ChatViewModel,
    isSuperLike: Boolean
) {
    val ctx = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = req.senderPetAvatarUrl ?: "https://placedog.net/200/280",
                contentDescription = req.senderPetName,
                modifier = Modifier.fillMaxSize().clickable { navController.navigate(Routes.petDetail(req.senderPetId)) },
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.65f)), startY = 200f)
                )
            )

            // Super Like badge
            if (isSuperLike) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = SuperLikeGold,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Star, null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp).size(18.dp)
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = PrimaryPink,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Favorite, null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp).size(18.dp)
                    )
                }
            }

            // Name
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    req.senderPetName ?: "?",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (isSuperLike) {
                    Text("⭐ Siêu thích bạn!", style = MaterialTheme.typography.labelSmall, color = SuperLikeGold)
                }
            }

            // Action Buttons (Accept/Reject)
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Reject button
                Surface(
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = DislikeRed.copy(0.8f),
                    onClick = { matchVm.respondToMatch(ctx, req.id, false) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // Accept button
                Surface(
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = LikeGreen.copy(0.9f),
                    onClick = {
                        matchVm.respondToMatch(ctx, req.id, true) {
                            chatVm.loadConversations(ctx)  // Refresh chat list
                        }
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Matched badge
            if (req.status == "ACCEPTED") {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    color = LikeGreen, shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Match!", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White)
                }
            }
        }
    }
}
