package com.petmatch.mobile.ui.chat

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.petmatch.mobile.data.model.AppointmentRequest
import com.petmatch.mobile.data.model.AppointmentResponse
import com.petmatch.mobile.ui.common.GradientButton
import com.petmatch.mobile.ui.common.PetMatchLoading
import com.petmatch.mobile.ui.common.petMatchGradient
import com.petmatch.mobile.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
// AppointmentScreen – Đặt lịch hẹn
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(
    navController: NavController,
    recipientId: Long,
    recipientName: String,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val loading by chatVm.appointmentLoading.collectAsState()
    val error by chatVm.appointmentError.collectAsState()
    val success by chatVm.appointmentSuccess.collectAsState()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Respond to success
    LaunchedEffect(success) {
        if (success) {
            showSuccessDialog = true
            chatVm.resetAppointmentSuccess()
        }
    }

    val dateDisplayStr = selectedDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Chọn ngày"
    val timeDisplayStr = selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Chọn giờ"

    // Date Picker
    val cal = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        ctx,
        { _, y, m, d -> selectedDate = LocalDate.of(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
    ).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }

    // Time Picker
    val timePickerDialog = TimePickerDialog(
        ctx,
        { _, h, min -> selectedTime = LocalTime.of(h, min) },
        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
    )

    val isFormValid = selectedDate != null && selectedTime != null && location.isNotBlank()

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
                        "Đặt lịch hẹn",
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
            // ── Hero header ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(PrimaryPink.copy(0.08f), Color.Transparent))
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📅", fontSize = 48.sp)
                    Text(
                        "Gặp mặt cùng $recipientName",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Lên kế hoạch cho cuộc gặp gỡ đáng nhớ!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Date + Time row ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date
                    AppointmentPickerCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        label = "Ngày",
                        value = dateDisplayStr,
                        isSelected = selectedDate != null,
                        onClick = { datePickerDialog.show() }
                    )
                    // Time
                    AppointmentPickerCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccessTime,
                        label = "Giờ",
                        value = timeDisplayStr,
                        isSelected = selectedTime != null,
                        onClick = { timePickerDialog.show() }
                    )
                }

                // ── Location ─────────────────────────────────────────────────
                SectionLabel("📍 Địa điểm")
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Vd: Công viên Thảo Cầm Viên, Q1") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, null, tint = PrimaryPink)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = Divider
                    ),
                    singleLine = true
                )

                // ── Quick location chips ─────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Công viên", "Café thú cưng", "Bãi biển", "Nhà riêng").forEach { suggestion ->
                        SuggestionChip(
                            onClick = { location = suggestion },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // ── Notes ────────────────────────────────────────────────────
                SectionLabel("📝 Ghi chú (tuỳ chọn)")
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    placeholder = { Text("Thêm ghi chú cho cuộc hẹn...") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = Divider
                    ),
                    maxLines = 4
                )

                // ── Summary card (if all filled) ─────────────────────────────
                AnimatedVisibility(
                    visible = isFormValid,
                    enter = fadeIn() + expandVertically()
                ) {
                    AppointmentSummaryCard(
                        recipientName = recipientName,
                        date = dateDisplayStr,
                        time = timeDisplayStr,
                        location = location
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

                Spacer(Modifier.height(8.dp))

                // ── Submit button ─────────────────────────────────────────────
                if (loading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryPink)
                    }
                } else {
                    GradientButton(
                        text = "Xác nhận lịch hẹn 📅",
                        onClick = {
                            if (isFormValid) {
                                val dateTime = LocalDateTime.of(selectedDate!!, selectedTime!!)
                                chatVm.createAppointment(
                                    ctx,
                                    AppointmentRequest(
                                        recipientId = recipientId,
                                        meetingTime = dateTime.toString(),
                                        location = location,
                                        notes = notes.ifBlank { null }
                                    )
                                ) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isFormValid
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
            icon = { Text("🎉", fontSize = 48.sp) },
            title = {
                Text(
                    "Đặt lịch thành công!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Lịch hẹn của bạn đã được gửi tới $recipientName.\nChúc bạn có một buổi gặp gỡ vui vẻ! 🐾",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                ) { Text("Tuyệt vời!") }
            }
        )
    }
}

// ── Appointment Picker Card ───────────────────────────────────────────────────
@Composable
private fun AppointmentPickerCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isSelected) PrimaryPink else Divider
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryPink.copy(0.05f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon, null,
                tint = if (isSelected) PrimaryPink else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) PrimaryPink else TextPrimary
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// ── Appointment Summary Card ──────────────────────────────────────────────────
@Composable
private fun AppointmentSummaryCard(
    recipientName: String,
    date: String,
    time: String,
    location: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(GradientStart, GradientEnd)))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(GradientStart.copy(0.06f), GradientEnd.copy(0.06f))
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(petMatchGradient, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📅", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Tóm tắt lịch hẹn",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                HorizontalDivider(color = Divider)
                SummaryRow(Icons.Default.Person, "Gặp với", recipientName)
                SummaryRow(Icons.Default.CalendarMonth, "Ngày", date)
                SummaryRow(Icons.Default.AccessTime, "Giờ", "$time")
                SummaryRow(Icons.Default.LocationOn, "Địa điểm", location)
            }
        }
    }
}

@Composable
private fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = PrimaryPink, modifier = Modifier.size(18.dp))
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.width(70.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// AppointmentListScreen – Danh sách lịch hẹn
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListScreen(
    navController: NavController,
    userId: Long,
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val appointments by chatVm.appointments.collectAsState()
    val loading by chatVm.appointmentLoading.collectAsState()

    LaunchedEffect(Unit) { chatVm.loadUserAppointments(ctx, userId) }

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
                        "Lịch hẹn của tôi",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold, color = Color.White
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryPink)
            )
        }
    ) { padding ->
        if (loading) {
            PetMatchLoading(modifier = Modifier.padding(padding))
        } else if (appointments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📅", fontSize = 72.sp)
                    Text("Chưa có lịch hẹn nào", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Tạo lịch hẹn trong cuộc trò chuyện!", color = TextSecondary)
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                appointments.groupBy { it.status }.forEach { (status, items) ->
                    item {
                        AppointmentStatusHeader(status)
                    }
                    androidx.compose.foundation.lazy.items(items, key = { it.id }) { appt ->
                        AppointmentCard(
                            appt = appt,
                            onConfirm = { chatVm.updateAppointmentStatus(ctx, appt.id, "CONFIRMED") },
                            onCancel = { chatVm.updateAppointmentStatus(ctx, appt.id, "CANCELLED") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentStatusHeader(status: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        val (emoji, label, color) = when (status) {
            "PENDING" -> Triple("⏳", "Đang chờ xác nhận", SecondaryOrange)
            "CONFIRMED" -> Triple("✅", "Đã xác nhận", LikeGreen)
            "COMPLETED" -> Triple("🎉", "Đã hoàn thành", AccentPurple)
            "CANCELLED" -> Triple("❌", "Đã hủy", DislikeRed)
            else -> Triple("📅", status, TextSecondary)
        }
        Text(emoji, fontSize = 18.sp)
        Text(
            label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = color)
        )
    }
}

@Composable
private fun AppointmentCard(
    appt: AppointmentResponse,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (appt.status) {
        "PENDING" -> SecondaryOrange
        "CONFIRMED" -> LikeGreen
        "COMPLETED" -> AccentPurple
        "CANCELLED" -> DislikeRed
        else -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Hẹn gặp ${appt.recipientName ?: "người dùng khác"}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Surface(color = statusColor.copy(0.12f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        appt.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    )
                }
            }

            HorizontalDivider(color = Divider)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = PrimaryPink, modifier = Modifier.size(16.dp))
                        Text(
                            try {
                                LocalDateTime.parse(appt.meetingTime).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            } catch (_: Exception) { appt.meetingTime },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = PrimaryPink, modifier = Modifier.size(16.dp))
                        Text(appt.location, style = MaterialTheme.typography.bodySmall)
                    }
                    appt.notes?.let {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notes, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }

            // Action buttons
            if (appt.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DislikeRed),
                        border = BorderStroke(1.dp, DislikeRed.copy(0.5f))
                    ) { Text("Hủy") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LikeGreen)
                    ) { Text("Xác nhận", color = Color.White) }
                }
            }
        }
    }
}
