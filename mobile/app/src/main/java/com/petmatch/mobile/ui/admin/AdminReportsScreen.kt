package com.petmatch.mobile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val actionButtonColor = Color(0xFF1D3557)

private enum class ReportReasonType {
    WARN,
    BAN
}

private val warnReasons = listOf(
    "Ngôn từ xúc phạm",
    "Quấy rối người dùng khác",
    "Spam hoặc tái phạm",
    "Thông tin sai sự thật"
)

private val banReasons = listOf(
    "Tái phạm nhiều lần",
    "Nội dung nghiêm trọng",
    "Lừa đảo hoặc giả mạo",
    "Vi phạm quy định cộng đồng"
)

@Composable
fun AdminReportsScreen(vm: AdminViewModel) {
    val ctx = LocalContext.current
    val state by vm.uiState.collectAsState()

    var selectedStatus by remember { mutableStateOf("PENDING") }
    var selectedReport by remember { mutableStateOf<com.petmatch.mobile.data.model.AdminReportItemResponse?>(null) }
    var reasonDialogType by remember { mutableStateOf<ReportReasonType?>(null) }
    var actionReportId by remember { mutableStateOf<Long?>(null) }
    var selectedReason by remember { mutableStateOf("") }

    LaunchedEffect(selectedStatus) {
        vm.loadReports(ctx, selectedStatus)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8FCFF), Color(0xFFEFF5FF))))
            .padding(16.dp)
    ) {
        Text("Danh sách báo cáo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Xử lý nhanh: cảnh báo, ẩn hồ sơ thú cưng, ban user hoặc bỏ qua.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = selectedStatus == "PENDING", onClick = { selectedStatus = "PENDING" }, label = { Text("Chờ xử lý") })
            FilterChip(selected = selectedStatus == "RESOLVED", onClick = { selectedStatus = "RESOLVED" }, label = { Text("Đã xử lý") })
            FilterChip(selected = selectedStatus == "DISMISSED", onClick = { selectedStatus = "DISMISSED" }, label = { Text("Bỏ qua") })
        }

        if (state.loading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.reports) { report ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("#${report.id} • ${report.targetType}(${report.targetId})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Reporter: ${report.reporterName}")
                        Text("Lý do: ${report.reason}")
                        val statusColor = when (report.status) {
                            "PENDING" -> Color(0xFFF4A261)
                            "RESOLVED" -> Color(0xFF2A9D8F)
                            else -> Color(0xFF8D99AE)
                        }
                        val statusLabel = when (report.status) {
                            "PENDING" -> "Chờ xử lý"
                            "RESOLVED" -> "Đã xử lý"
                            else -> "Bỏ qua"
                        }
                        Surface(color = statusColor.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                            Text(
                                "Trạng thái: $statusLabel",
                                color = statusColor,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { selectedReport = report },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Xem chi tiết báo cáo")
                        }

                        Spacer(Modifier.height(8.dp))

                        if (report.status == "PENDING") {
                            ReportActionButtons(
                                report = report,
                                onWarn = {
                                    actionReportId = report.id
                                    reasonDialogType = ReportReasonType.WARN
                                    selectedReason = warnReasons.first()
                                },
                                onHideProfile = {
                                    vm.handleReport(ctx, report.id, "AUTO_HIDE_PROFILE", "Ẩn hồ sơ thú cưng do vi phạm")
                                },
                                onBan = {
                                    actionReportId = report.id
                                    reasonDialogType = ReportReasonType.BAN
                                    selectedReason = banReasons.first()
                                },
                                onDismiss = {
                                    vm.handleReport(ctx, report.id, "DISMISS", "Bỏ qua báo cáo")
                                }
                            )
                        } else {
                            Text("Đã xử lý bởi: ${report.handledByName ?: "-"}")
                            if (!report.adminNote.isNullOrBlank()) {
                                Text("Ghi chú: ${report.adminNote}")
                            }
                        }
                    }
                }
            }
        }

        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }

    selectedReport?.let { report ->
        AlertDialog(
            onDismissRequest = { selectedReport = null },
            confirmButton = {
                TextButton(onClick = { selectedReport = null }) {
                    Text("Đóng")
                }
            },
            title = { Text("Chi tiết báo cáo #${report.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Người báo cáo: ${report.reporterName}")
                    Text("Đối tượng: ${report.targetType} (${report.targetId})")
                    Text("Lý do: ${report.reason}")
                    Text("Trạng thái: ${if (report.status == "PENDING") "Chờ xử lý" else if (report.status == "RESOLVED") "Đã xử lý" else "Bỏ qua"}")
                    Text("Thời gian tạo: ${report.createdAt ?: "-"}")
                    if (!report.adminNote.isNullOrBlank()) {
                        Text("Ghi chú xử lý: ${report.adminNote}")
                    }

                    if (report.status == "PENDING") {
                        Spacer(Modifier.height(4.dp))
                        ReportActionButtons(
                            report = report,
                            onWarn = {
                                actionReportId = report.id
                                reasonDialogType = ReportReasonType.WARN
                                selectedReason = warnReasons.first()
                            },
                            onHideProfile = {
                                vm.handleReport(ctx, report.id, "AUTO_HIDE_PROFILE", "Ẩn hồ sơ thú cưng do vi phạm")
                                selectedReport = null
                            },
                            onBan = {
                                actionReportId = report.id
                                reasonDialogType = ReportReasonType.BAN
                                selectedReason = banReasons.first()
                            },
                            onDismiss = {
                                vm.handleReport(ctx, report.id, "DISMISS", "Bỏ qua báo cáo")
                                selectedReport = null
                            }
                        )
                    }
                }
            }
        )
    }

    if (reasonDialogType != null && actionReportId != null) {
        val reasons = if (reasonDialogType == ReportReasonType.WARN) warnReasons else banReasons
        AlertDialog(
            onDismissRequest = {
                reasonDialogType = null
                actionReportId = null
            },
            title = {
                Text(if (reasonDialogType == ReportReasonType.WARN) "Chọn lý do cảnh báo" else "Chọn lý do ban user")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Text(reason)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reportId = actionReportId ?: return@Button
                        if (reasonDialogType == ReportReasonType.WARN) {
                            vm.handleReport(ctx, reportId, "WARN_USER", selectedReason)
                        } else {
                            vm.handleReport(ctx, reportId, "BAN_USER", selectedReason)
                        }
                        reasonDialogType = null
                        actionReportId = null
                        selectedReport = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = actionButtonColor, contentColor = Color.White)
                ) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = {
                    reasonDialogType = null
                    actionReportId = null
                }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun ReportActionButtons(
    report: com.petmatch.mobile.data.model.AdminReportItemResponse,
    onWarn: () -> Unit,
    onHideProfile: () -> Unit,
    onBan: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnifiedActionButton(text = "Cảnh báo", onClick = onWarn, modifier = Modifier.weight(1f))
            UnifiedActionButton(
                text = "Ẩn hồ sơ thú cưng",
                onClick = onHideProfile,
                enabled = report.targetType == "PET_PROFILE",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnifiedActionButton(text = "Ban user", onClick = onBan, modifier = Modifier.weight(1f))
            UnifiedActionButton(text = "Bỏ qua", onClick = onDismiss, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun UnifiedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = actionButtonColor, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text)
    }
}
