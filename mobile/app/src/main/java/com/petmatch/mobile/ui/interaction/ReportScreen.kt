package com.petmatch.mobile.ui.interaction

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.petmatch.mobile.data.model.ReportRequest
import com.petmatch.mobile.ui.common.*
import com.petmatch.mobile.ui.petprofile.FlowChipGroup
import com.petmatch.mobile.ui.petprofile.SectionTitle
import com.petmatch.mobile.ui.theme.PrimaryPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    navController: NavController,
    petId: Long,
    ownerId: Long,
    vm: InteractionViewModel
) {
    val ctx = LocalContext.current
    val actionDone by vm.actionDone.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var selectedReason by remember { mutableStateOf("") }
    var customReason by remember { mutableStateOf("") }

    val predefinedReasons = listOf(
        "Nội dung không phù hợp",
        "Hình ảnh giả mạo",
        "Thông tin sai sự thật",
        "Hành vi quấy rối",
        "Ngược đãi động vật",
        "Lừa đảo",
        "Spam",
        "Khác"
    )

    LaunchedEffect(actionDone) {
        if (actionDone) {
            vm.resetState()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = "Báo cáo vi phạm",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Info banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.error)
                    Column {
                        Text(
                            "Báo cáo hồ sơ vi phạm",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Báo cáo của bạn sẽ được xem xét bởi đội ngũ kiểm duyệt. Xin cảm ơn vì đã giúp cộng đồng an toàn hơn.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionTitle("Lý do báo cáo *")
            FlowChipGroup(
                options = predefinedReasons,
                selected = listOf(selectedReason),
                onToggle = { selectedReason = if (selectedReason == it) "" else it }
            )

            if (selectedReason == "Khác" || selectedReason.isBlank()) {
                SectionTitle("Mô tả chi tiết *")
                OutlinedTextField(
                    value = customReason,
                    onValueChange = { customReason = it },
                    placeholder = { Text("Mô tả vấn đề bạn gặp phải...") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            val finalReason = if (selectedReason == "Khác" || selectedReason.isBlank()) customReason else selectedReason
            val canSubmit = finalReason.isNotBlank() && !loading

            Spacer(Modifier.height(8.dp))
            GradientButton(
                text = if (loading) "Đang gửi..." else "Gửi báo cáo",
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    vm.submitReport(
                        ctx = ctx,
                        req = ReportRequest(
                            targetId   = petId,
                            targetType = "PET_PROFILE",
                            reason     = finalReason
                        )
                    ) {}
                }
            )

            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Hủy")
            }
        }
    }
}
