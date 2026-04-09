package com.petmatch.mobile.ui.match

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.Constants
import com.petmatch.mobile.data.model.MatchRequestResponse
import com.petmatch.mobile.data.model.PetProfileResponse
import com.petmatch.mobile.ui.common.*
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.petprofile.PetProfileViewModel
import com.petmatch.mobile.ui.petprofile.parsePersonalityTags
import com.petmatch.mobile.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSwipeScreen(
    navController: NavController,
    matchVm: MatchViewModel,
    petVm: PetProfileViewModel
) {
    val ctx = LocalContext.current
    val suggestions by matchVm.suggestions.collectAsState()
    val isLoading by matchVm.isLoadingSuggestions.collectAsState()
    val error by matchVm.error.collectAsState()
    val superLikeStatus by matchVm.superLikeStatus.collectAsState()
    val matchPopup by matchVm.matchPopup.collectAsState()
    val isSmartMode by matchVm.isSmartMode.collectAsState()

    LaunchedEffect(Unit) {
        matchVm.loadSuggestions(ctx, refresh = true)
        matchVm.loadSuperLikeStatus(ctx)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Pets, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Text("PetMatch", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
                        // AI mode badge
                        if (isSmartMode) {
                            Surface(
                                color = Color.White.copy(0.25f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "🤖 AI",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.MATCH_FILTER) }) {
                        Icon(Icons.Default.Tune, "Bộ lọc", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryPink)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading && suggestions.isEmpty() -> PetMatchLoading()
                suggestions.isEmpty() && !isLoading -> EmptyDeck(navController)
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Card deck area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Show background cards (next 2)
                            suggestions.take(3).reversed().forEachIndexed { index, pet ->
                                val reverseIndex = minOf(2, suggestions.size - 1) - index
                                if (reverseIndex > 0) {
                                    BackgroundCard(pet = pet, depth = reverseIndex)
                                }
                            }
                            // Top swipeable card
                            if (suggestions.isNotEmpty()) {
                                SwipeCard(
                                    pet = suggestions[0],
                                    superLikeStatus = superLikeStatus,
                                    onLike = { matchVm.sendLike(ctx, suggestions[0].id, false) },
                                    onDislike = { matchVm.sendDislike(ctx, suggestions[0].id) },
                                    onSuperLike = { matchVm.sendLike(ctx, suggestions[0].id, true) },
                                    onViewDetail = { navController.navigate(Routes.petDetail(suggestions[0].id)) }
                                )
                            }
                        }

                        // Action buttons row
                        ActionButtonsRow(
                            superLikeStatus = superLikeStatus,
                            onDislike = { if (suggestions.isNotEmpty()) matchVm.sendDislike(ctx, suggestions[0].id) },
                            onSuperLike = { if (suggestions.isNotEmpty()) matchVm.sendLike(ctx, suggestions[0].id, true) },
                            onLike = { if (suggestions.isNotEmpty()) matchVm.sendLike(ctx, suggestions[0].id, false) },
                            onLikedMe = { navController.navigate(Routes.WHO_LIKED_ME) },
                            onMatched  = { navController.navigate(Routes.MATCHED_LIST) }
                        )
                    }
                }
            }

            if (!error.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            error ?: "",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = {
                            matchVm.clearError()
                            if (suggestions.isEmpty()) {
                                matchVm.loadSuggestions(ctx, refresh = true)
                            }
                        }) {
                            Text("Đóng")
                        }
                    }
                }
            }
        }
    }

    // Match popup
    if (matchPopup != null) {
        MatchPopupDialog(
            matchResponse = matchPopup!!,
            onDismiss = { matchVm.dismissMatchPopup() },
            onKeepMatching = { matchVm.dismissMatchPopup() }
        )
    }
}

@Composable
private fun SwipeCard(
    pet: PetProfileResponse,
    superLikeStatus: com.petmatch.mobile.data.model.SuperLikeStatusResponse?,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onSuperLike: () -> Unit,
    onViewDetail: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val screenWidth = with(density) { 400.dp.toPx() }

    val rotation = (animOffsetX.value / screenWidth) * 18f
    val likeAlpha = (animOffsetX.value / (screenWidth * 0.4f)).coerceIn(0f, 1f)
    val nopeAlpha = (-animOffsetX.value / (screenWidth * 0.4f)).coerceIn(0f, 1f)
    val superAlpha = (-animOffsetY.value / (screenWidth * 0.3f)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .graphicsLayer {
                translationX = animOffsetX.value
                translationY = animOffsetY.value
                rotationZ = rotation
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, drag ->
                        change.consume()
                        scope.launch {
                            animOffsetX.snapTo(animOffsetX.value + drag.x)
                            animOffsetY.snapTo(animOffsetY.value + drag.y)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            when {
                                animOffsetX.value > screenWidth * 0.35f -> {
                                    animOffsetX.animateTo(screenWidth * 1.5f, tween(300))
                                    onLike()
                                    animOffsetX.snapTo(0f); animOffsetY.snapTo(0f)
                                }
                                animOffsetX.value < -screenWidth * 0.35f -> {
                                    animOffsetX.animateTo(-screenWidth * 1.5f, tween(300))
                                    onDislike()
                                    animOffsetX.snapTo(0f); animOffsetY.snapTo(0f)
                                }
                                animOffsetY.value < -screenWidth * 0.25f -> {
                                    if (superLikeStatus?.canSuperLike == true) {
                                        animOffsetY.animateTo(-screenWidth * 1.5f, tween(300))
                                        onSuperLike()
                                        animOffsetX.snapTo(0f); animOffsetY.snapTo(0f)
                                    } else {
                                        // Vuốt lên nhưng hếtượt -> bật ngược lại về giữa
                                        animOffsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                        animOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                    }
                                }
                                else -> {
                                    animOffsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                    animOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                }
                            }
                        }
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clickable { onViewDetail() },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background photo
                AsyncImage(
                    model = pet.avatarUrl ?: "https://i.pinimg.com/originals/f1/0f/f7/f10ff70a715515d1662550dccdd44832.png",
                    contentDescription = pet.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.8f)),
                                startY = 300f
                            )
                        )
                )

                // LIKE label
                if (likeAlpha > 0.1f) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(24.dp).rotate(-20f),
                        color = LikeGreen.copy(alpha = likeAlpha),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(3.dp, LikeGreen)
                    ) {
                        Text(
                            "THÍCH", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            fontSize = 28.sp
                        )
                    }
                }
                // NOPE label
                if (nopeAlpha > 0.1f) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).rotate(20f),
                        color = DislikeRed.copy(alpha = nopeAlpha),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(3.dp, DislikeRed)
                    ) {
                        Text(
                            "BỎ QUA", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            fontSize = 22.sp
                        )
                    }
                }
                // SUPER LIKE label
                if (superAlpha > 0.1f) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Star, null, tint = SuperLikeGold, modifier = Modifier.size(64.dp))
                        Surface(
                            color = SuperLikeGold.copy(alpha = superAlpha),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(3.dp, SuperLikeGold)
                        ) {
                            Text(
                                "SIÊU THÍCH", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                fontSize = 22.sp
                            )
                        }
                    }
                }

                // Pet info at bottom
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${pet.name}${if (pet.age != null) ", ${pet.age} tuổi" else ""}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (!pet.breed.isNullOrBlank())
                        Text(pet.breed, color = Color.White.copy(0.85f), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (pet.distanceKm != null)
                            PetInfoBadge("📍 ${pet.distanceKm} km", Color.White.copy(0.9f))
                        if (pet.isVaccinated == true)
                            PetInfoBadge("✓ Đã tiêm", Color(0xFF22C55E))
                        val lookingFor = Constants.LOOKING_FOR_LABELS[pet.lookingFor]
                        if (lookingFor != null) PetInfoBadge(lookingFor, PrimaryPink)
                        if (pet.weightKg != null) PetInfoBadge("${pet.weightKg}kg", Color.White.copy(0.7f))
                    }

                    // Personality tags (max 3)
                    val tags = parsePersonalityTags(pet.personalityTags).take(3)
                    if (tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            tags.forEach { tag ->
                                Surface(
                                    color = Color.White.copy(0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(tag, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }
                    }

                    // Swipe hint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onViewDetail) {
                            Icon(Icons.Default.Info, "Chi tiết", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundCard(pet: PetProfileResponse, depth: Int) {
    val scale = 1f - depth * 0.04f
    val offset = depth * 10f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                translationY = offset
                alpha = 1f - depth * 0.15f
            }
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            AsyncImage(
                model = pet.avatarUrl ?: "https://placedog.net/400/600",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun PetInfoBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.22f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (color == Color.White.copy(0.7f)) Color.White else color
        )
    }
}

@Composable
private fun ActionButtonsRow(
    superLikeStatus: com.petmatch.mobile.data.model.SuperLikeStatusResponse?,
    onDislike: () -> Unit,
    onSuperLike: () -> Unit,
    onLike: () -> Unit,
    onLikedMe: () -> Unit,
    onMatched: () -> Unit
) {
    val canSuperLike = superLikeStatus?.canSuperLike != false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Swipe hint text
        Text(
            "← Bỏ qua   ↑ Siêu thích   → Thích →",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Who liked me button
            SmallRoundButton(
                icon = Icons.Default.FavoriteBorder,
                tint = AccentPurple, size = 44,
                onClick = onLikedMe
            )
            // Dislike
            LargeRoundButton(
                icon = Icons.Default.Close,
                tint = DislikeRed, size = 60,
                onClick = onDislike
            )
            // Super Like
            Box {
                LargeRoundButton(
                    icon = Icons.Default.Star,
                    tint = if (canSuperLike) SuperLikeGold else Color.Gray,
                    size = 52,
                    onClick = { if (canSuperLike) onSuperLike() },
                    borderColor = if (canSuperLike) SuperLikeGold else Color.Gray
                )
                if (!canSuperLike) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = 10.dp),
                        color = Color.Gray, shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Hết hôm nay", modifier = Modifier.padding(3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = Color.White)
                    }
                }
            }
            // Like
            LargeRoundButton(
                icon = Icons.Default.Favorite,
                tint = LikeGreen, size = 60,
                onClick = onLike
            )
            // Matched list
            SmallRoundButton(
                icon = Icons.Default.ChatBubbleOutline,
                tint = PrimaryPink, size = 44,
                onClick = onMatched
            )
        }
    }
}

@Composable
private fun LargeRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    size: Int,
    onClick: () -> Unit,
    borderColor: Color = tint
) {
    val sizeDp = size.dp
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((sizeDp.value * 0.48f).dp))
    }
}

@Composable
private fun SmallRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    size: Int,
    onClick: () -> Unit
) {
    val sizeDp = size.dp
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((sizeDp.value * 0.5f).dp))
    }
}

@Composable
private fun EmptyDeck(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🐾", fontSize = 72.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Không còn hồ sơ nào!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Hãy thử mở rộng bộ lọc hoặc quay lại sau nhé",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        GradientButton(
            text = "Chỉnh bộ lọc",
            onClick = { navController.navigate(Routes.MATCH_FILTER) },
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}

@Composable
private fun MatchPopupDialog(
    matchResponse: MatchRequestResponse,
    onDismiss: () -> Unit,
    onKeepMatching: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                // Match title
                Text("🎉", fontSize = 56.sp)
                Text(
                    "Đã Ghép Đôi!",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Text(
                    "${matchResponse.senderPetName} và ${matchResponse.receiverPetName} thích nhau!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(0.85f)
                )

                // Avatars
                Row(horizontalArrangement = Arrangement.spacedBy((-20).dp)) {
                    AsyncImage(
                        model = matchResponse.senderPetAvatarUrl ?: "https://i.pinimg.com/originals/f1/0f/f7/f10ff70a715515d1662550dccdd44832.png",
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).clip(CircleShape).border(3.dp, PrimaryPink, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    AsyncImage(
                        model = matchResponse.receiverPetAvatarUrl ?: "https://i.pinimg.com/originals/f1/0f/f7/f10ff70a715515d1662550dccdd44832.png",
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).clip(CircleShape).border(3.dp, LikeGreen, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(8.dp))

                GradientButton(
                    text = "Gửi lời chào 👋",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onKeepMatching) {
                    Text("Tiếp tục ghép đôi", color = Color.White.copy(0.7f))
                }
            }
        }
    }
}
