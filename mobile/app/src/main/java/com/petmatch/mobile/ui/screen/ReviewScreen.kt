package com.petmatch.mobile.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmatch.mobile.ui.theme.*

@Composable
fun ReviewScreen(
    contactName: String = "Trần Thị Lan",
    petName: String = "Bella",
    meetingDate: String = "Thứ 7, 12/04/2026",
    meetingPlace: String = "Công viên Thống Nhất",
    onBackClick: () -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    var overallRating by remember { mutableStateOf(0) }
    var behaviorRating by remember { mutableStateOf(0) }
    var punctualityRating by remember { mutableStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var wouldMeetAgain by remember { mutableStateOf<Boolean?>(null) }

    val positiveTags = listOf(
        "Đúng giờ 🕐", "Thú cưng thân thiện 🐾",
        "Địa điểm đẹp 📍", "Giao tiếp tốt 💬",
        "Sạch sẽ ✨", "An toàn 🛡️"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top bar
        Surface(color = White, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.Close, null, tint = TextPrimary)
                }
                Text(
                    "Đánh giá buổi gặp mặt",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meeting summary card
            MeetingSummaryCard(
                contactName = contactName,
                petName = petName,
                meetingDate = meetingDate,
                meetingPlace = meetingPlace
            )

            // Overall rating
            ReviewCard(title = "⭐ Đánh giá tổng thể") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (overallRating) {
                            1 -> "Không tốt 😞"
                            2 -> "Tạm được 😐"
                            3 -> "Ổn 🙂"
                            4 -> "Tốt 😊"
                            5 -> "Tuyệt vời! 🤩"
                            else -> "Chạm vào sao để đánh giá"
                        },
                        fontSize = 15.sp,
                        color = if (overallRating > 0) PrimaryPink else TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StarRatingBar(
                        rating = overallRating,
                        onRatingChanged = { overallRating = it },
                        starSize = 44
                    )
                }
            }

            // Detailed ratings
            ReviewCard(title = "📊 Đánh giá chi tiết") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DetailedRatingRow(
                        label = "Thái độ & thân thiện",
                        rating = behaviorRating,
                        onRatingChanged = { behaviorRating = it }
                    )
                    DetailedRatingRow(
                        label = "Đúng giờ",
                        rating = punctualityRating,
                        onRatingChanged = { punctualityRating = it }
                    )
                }
            }

            // Tags
            ReviewCard(title = "🏷️ Nhận xét nhanh") {
                Column {
                    Text(
                        "Chọn những điểm nổi bật",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Tag grid (2 columns)
                    val rows = positiveTags.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowTags ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTags.forEach { tag ->
                                    val isSelected = tag in selectedTags
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) PrimaryPink else White,
                                        modifier = Modifier
                                            .weight(1f),
                                        onClick = {
                                            selectedTags = if (isSelected) {
                                                selectedTags - tag
                                            } else {
                                                selectedTags + tag
                                            }
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) PrimaryPink else DividerColor
                                        )
                                    ) {
                                        Text(
                                            text = tag,
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .fillMaxWidth(),
                                            fontSize = 13.sp,
                                            color = if (isSelected) White else TextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Text review
            ReviewCard(title = "💬 Chia sẻ trải nghiệm") {
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    placeholder = {
                        Text(
                            "Chia sẻ cảm nhận về buổi gặp mặt...",
                            color = TextHint,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = DividerColor
                    )
                )
            }

            // Would meet again?
            ReviewCard(title = "🤝 Bạn có muốn gặp lại không?") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MeetAgainButton(
                        text = "Có, muốn gặp lại! 🐾",
                        isSelected = wouldMeetAgain == true,
                        backgroundColor = ActionGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { wouldMeetAgain = true }
                    )
                    MeetAgainButton(
                        text = "Không gặp thêm 😐",
                        isSelected = wouldMeetAgain == false,
                        backgroundColor = TextSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { wouldMeetAgain = false }
                    )
                }
            }

            // Report / Block option
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ActionRed.copy(alpha = 0.06f),
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Flag,
                        null,
                        tint = ActionRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Báo cáo hoặc chặn người dùng này",
                        fontSize = 13.sp,
                        color = ActionRed
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = ActionRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Submit button
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                enabled = overallRating > 0
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Gửi đánh giá",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun MeetingSummaryCard(
    contactName: String,
    petName: String,
    meetingDate: String,
    meetingPlace: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PrimaryPink.copy(alpha = 0.08f), LightPink.copy(alpha = 0.5f))
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Two pet avatars overlapping
                Box(modifier = Modifier.size(64.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD4845A))
                            .align(Alignment.CenterStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐕", fontSize = 22.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(LightPink)
                            .align(Alignment.CenterEnd)
                            .offset(x = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐩", fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Bé Claus × $petName",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "với $contactName",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$meetingDate · $meetingPlace",
                        fontSize = 12.sp,
                        color = PrimaryPink,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    starSize: Int = 32
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingChanged(i) },
                modifier = Modifier.size(starSize.dp)
            ) {
                Icon(
                    if (i <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = "Star $i",
                    tint = if (i <= rating) StarGold else TextHint,
                    modifier = Modifier.size(starSize.dp)
                )
            }
        }
    }
}

@Composable
fun DetailedRatingRow(
    label: String,
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Row {
            for (i in 1..5) {
                IconButton(
                    onClick = { onRatingChanged(i) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (i <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = null,
                        tint = if (i <= rating) StarGold else TextHint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MeetAgainButton(
    text: String,
    isSelected: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) backgroundColor else backgroundColor.copy(alpha = 0.1f),
            contentColor = if (isSelected) White else backgroundColor
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}
