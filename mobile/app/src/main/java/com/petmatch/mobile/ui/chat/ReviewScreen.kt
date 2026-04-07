package com.petmatch.mobile.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.ui.common.GradientButton
import com.petmatch.mobile.ui.common.petMatchGradient
import com.petmatch.mobile.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// ReviewScreen – Đánh giá sau cuộc gặp
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    revieweeId: Long,
    revieweeName: String,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val loading by chatVm.reviewLoading.collectAsState()
    val error by chatVm.reviewError.collectAsState()
    val success by chatVm.reviewSuccess.collectAsState()

    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(success) {
        if (success) {
            showSuccessDialog = true
            chatVm.resetReviewSuccess()
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
                    Text(
                        "Đánh giá sau gặp",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
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
                .verticalScroll(rememberScrollState())
        ) {
            // ── User card ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(PrimaryPink.copy(0.1f), Color.Transparent)
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = "https://placedog.net/96/96?r=$revieweeId",
                            contentDescription = revieweeName,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(
                                    3.dp,
                                    Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                    CircleShape
                                ),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            color = SuperLikeGold,
                            shape = CircleShape
                        ) {
                            Text("⭐", fontSize = 14.sp, modifier = Modifier.padding(4.dp))
                        }
                    }
                    Text(
                        revieweeName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Bạn vừa gặp người này! Hãy chia sẻ cảm nhận của bạn 🐾",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── Rating stars ─────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Trải nghiệm như thế nào?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )

                        // Star rating
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(5) { index ->
                                val starIndex = index + 1
                                val isSelected = starIndex <= rating
                                IconButton(
                                    onClick = { rating = starIndex },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Sao $starIndex",
                                        tint = if (isSelected) SuperLikeGold else TextHint,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        // Rating label
                        AnimatedVisibility(visible = rating > 0) {
                            val ratingLabel = when (rating) {
                                1 -> "😞 Không hài lòng"
                                2 -> "😐 Bình thường"
                                3 -> "🙂 Ổn"
                                4 -> "😊 Hài lòng"
                                5 -> "🤩 Tuyệt vời!"
                                else -> ""
                            }
                            Surface(
                                color = getRatingColor(rating).copy(0.1f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    ratingLabel,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = getRatingColor(rating)
                                    )
                                )
                            }
                        }
                    }
                }

                // ── Quick tags ───────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Thêm nhận xét nhanh",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    val tags = listOf(
                        "Thân thiện 😊", "Thú cưng dễ thương 🐾", "Đúng giờ ⏰",
                        "Địa điểm đẹp 📍", "Muốn gặp lại 💫", "Rất chuyên nghiệp 👍"
                    )
                    val selectedTags = remember { mutableStateListOf<String>() }

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags.size) { i ->
                            val tag = tags[i]
                            val selected = tag in selectedTags
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) selectedTags.remove(tag)
                                    else {
                                        selectedTags.add(tag)
                                        if (comment.isNotEmpty() && !comment.endsWith(" ")) comment += " "
                                        comment += tag
                                    }
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPink,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }
                }

                // ── Comment field ─────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Chia sẻ cảm nhận của bạn",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        placeholder = { Text("Hãy chia sẻ những gì bạn nghĩ về cuộc gặp này...") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPink,
                            unfocusedBorderColor = Divider
                        ),
                        maxLines = 6,
                        supportingText = {
                            Text(
                                "${comment.length}/500",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                                color = TextSecondary
                            )
                        }
                    )
                }

                // ── Error ─────────────────────────────────────────────────────
                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = DislikeRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Submit ────────────────────────────────────────────────────
                if (loading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryPink)
                    }
                } else {
                    GradientButton(
                        text = "Gửi đánh giá ⭐",
                        onClick = {
                            if (rating > 0) {
                                chatVm.submitReview(
                                    ctx = ctx,
                                    revieweeId = revieweeId,
                                    rating = rating,
                                    comment = comment.trim().ifBlank { null }
                                ) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = rating > 0
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.popBackStack()
            },
            icon = { Text("⭐", fontSize = 48.sp) },
            title = {
                Text(
                    "Cảm ơn bạn!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(rating) {
                            Text("⭐", fontSize = 24.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Đánh giá của bạn đã được gửi tới $revieweeName.\nNhận xét giúp cộng đồng PetMatch ngày càng tốt hơn! 🐾",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                ) { Text("Hoàn tất!") }
            }
        )
    }
}

private fun getRatingColor(rating: Int): Color = when (rating) {
    1 -> DislikeRed
    2 -> SecondaryOrange
    3 -> Color(0xFFE9C46A)
    4 -> LikeGreen
    5 -> SuperLikeGold
    else -> TextSecondary
}
