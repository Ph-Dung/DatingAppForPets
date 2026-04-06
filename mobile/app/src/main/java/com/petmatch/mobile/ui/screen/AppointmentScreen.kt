package com.petmatch.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmatch.mobile.ui.theme.*

data class Appointment(
    val id: Int,
    val petOwnerName: String,
    val petName: String,
    val date: String,
    val time: String,
    val location: String,
    val status: AppointmentStatus,
    val note: String = ""
)

enum class AppointmentStatus { PENDING, CONFIRMED, CANCELLED, COMPLETED }

val sampleAppointments = listOf(
    Appointment(
        1, "Trần Thị Lan", "Bella",
        "Thứ 7, 12/04/2026", "09:00 - 10:30",
        "Công viên Thống Nhất, Hà Nội",
        AppointmentStatus.CONFIRMED,
        "Mang theo đồ chơi cho bé"
    ),
    Appointment(
        2, "Nguyễn Văn An", "Max",
        "Chủ nhật, 13/04/2026", "14:00 - 15:30",
        "Hồ Tây, Hà Nội",
        AppointmentStatus.PENDING
    ),
    Appointment(
        3, "Lê Thị Hoa", "Rocky",
        "Thứ 2, 07/04/2026", "08:00 - 09:00",
        "Vườn Bách Thảo, Hà Nội",
        AppointmentStatus.COMPLETED
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(
    onBackClick: () -> Unit = {},
    onCreateAppointment: () -> Unit = {},
    onAppointmentClick: (Int) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Sắp tới", "Chờ xác nhận", "Đã xong")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top bar
        Surface(color = White, shadowElevation = 2.dp) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBackIos, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "Lịch hẹn",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCreateAppointment) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = White,
                    contentColor = PrimaryPink,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryPink,
                            height = 2.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedTab == index) PrimaryPink else TextSecondary
                                )
                            }
                        )
                    }
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Upcoming appointment banner
            if (selectedTab == 0) {
                UpcomingBanner()
            }

            val filtered = when (selectedTab) {
                0 -> sampleAppointments.filter { it.status == AppointmentStatus.CONFIRMED }
                1 -> sampleAppointments.filter { it.status == AppointmentStatus.PENDING }
                else -> sampleAppointments.filter { it.status == AppointmentStatus.COMPLETED }
            }

            if (filtered.isEmpty()) {
                EmptyAppointmentState()
            } else {
                filtered.forEach { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        onClick = { onAppointmentClick(appointment.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun UpcomingBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PrimaryPink, Color(0xFFFF80AB))
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🗓 Lịch hẹn sắp tới",
                    fontSize = 13.sp,
                    color = White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Thứ 7, 12/04 lúc 09:00",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gặp Bella & Trần Thị Lan",
                    fontSize = 14.sp,
                    color = White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "📍 Công viên Thống Nhất",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            color = White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🐕", fontSize = 36.sp)
            }
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit
) {
    val statusColor = when (appointment.status) {
        AppointmentStatus.CONFIRMED -> ActionGreen
        AppointmentStatus.PENDING -> ActionYellow
        AppointmentStatus.CANCELLED -> ActionRed
        AppointmentStatus.COMPLETED -> TextSecondary
    }
    val statusText = when (appointment.status) {
        AppointmentStatus.CONFIRMED -> "Đã xác nhận"
        AppointmentStatus.PENDING -> "Chờ xác nhận"
        AppointmentStatus.CANCELLED -> "Đã hủy"
        AppointmentStatus.COMPLETED -> "Hoàn thành"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(LightPink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐕", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = appointment.petOwnerName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Bé ${appointment.petName}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = DividerColor)

            // Time & location
            AppointmentDetailRow(Icons.Default.CalendarToday, appointment.date)
            Spacer(modifier = Modifier.height(6.dp))
            AppointmentDetailRow(Icons.Default.Schedule, appointment.time)
            Spacer(modifier = Modifier.height(6.dp))
            AppointmentDetailRow(Icons.Default.LocationOn, appointment.location)

            if (appointment.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                AppointmentDetailRow(Icons.Default.Notes, appointment.note)
            }

            // Action buttons for pending
            if (appointment.status == AppointmentStatus.PENDING) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ActionRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ActionRed)
                    ) {
                        Text("Từ chối", fontSize = 13.sp)
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                    ) {
                        Text("Xác nhận", fontSize = 13.sp, color = White)
                    }
                }
            }

            // Review button for completed
            if (appointment.status == AppointmentStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightPink,
                        contentColor = PrimaryPink
                    )
                ) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đánh giá buổi gặp mặt", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AppointmentDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = PrimaryPink, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
fun EmptyAppointmentState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📅", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Chưa có lịch hẹn",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Hãy tạo lịch hẹn để 2 bé gặp nhau nhé!",
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}


// ─── Create Appointment Bottom Sheet ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAppointmentSheet(
    contactName: String = "Trần Thị Lan",
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val dateOptions = listOf("Thứ 7, 12/04", "Chủ nhật, 13/04", "Thứ 2, 14/04", "Thứ 3, 15/04")
    val timeOptions = listOf("08:00", "09:00", "10:00", "14:00", "15:00", "16:00")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DividerColor)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Tạo lịch hẹn",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            "Gặp $contactName và bé cưng",
            fontSize = 14.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Date selection
        Text("📅 Chọn ngày", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dateOptions.take(4).forEach { date ->
                DateTimeChip(
                    text = date,
                    isSelected = selectedDate == date,
                    onClick = { selectedDate = date },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time selection
        Text("🕐 Chọn giờ", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeOptions.take(3).forEach { time ->
                DateTimeChip(
                    text = time,
                    isSelected = selectedTime == time,
                    onClick = { selectedTime = time },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeOptions.drop(3).forEach { time ->
                DateTimeChip(
                    text = time,
                    isSelected = selectedTime == time,
                    onClick = { selectedTime = time },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location
        Text("📍 Địa điểm", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            placeholder = { Text("Nhập địa điểm gặp mặt...", color = TextHint, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPink,
                unfocusedBorderColor = DividerColor
            ),
            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = PrimaryPink) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Note
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("Ghi chú thêm (tùy chọn)...", color = TextHint, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPink,
                unfocusedBorderColor = DividerColor
            ),
            minLines = 2,
            maxLines = 3,
            leadingIcon = { Icon(Icons.Default.Notes, null, tint = PrimaryPink) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
            enabled = selectedDate.isNotEmpty() && selectedTime.isNotEmpty() && location.isNotEmpty()
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Gửi yêu cầu hẹn",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DateTimeChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryPink else BackgroundLight)
            .border(
                1.dp,
                if (isSelected) PrimaryPink else DividerColor,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) White else TextPrimary
        )
    }
}
