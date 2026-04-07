package com.petmatch.mobile.ui.petprofile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.petmatch.mobile.data.model.VaccinationRequest
import com.petmatch.mobile.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationFormScreen(
    navController: NavController,
    vm: PetProfileViewModel,
    editVacId: Long?
) {
    val ctx = LocalContext.current
    val vaccinations by vm.vaccinations.collectAsState()
    val actionState by vm.actionState.collectAsState()

    // Form state
    var vaccineName by remember { mutableStateOf("") }
    var vaccinatedDate by remember { mutableStateOf("") }
    var nextDueDate by remember { mutableStateOf("") }
    var clinicName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val isEdit = editVacId != null
    val title = if (isEdit) "Sửa bản ghi vaccine" else "Thêm vaccine"

    // Pre-fill if editing
    LaunchedEffect(editVacId, vaccinations) {
        if (isEdit) {
            vm.loadVaccinations(ctx)
            val existing = vaccinations.find { it.id == editVacId }
            if (existing != null) {
                vaccineName    = existing.vaccineName
                vaccinatedDate = existing.vaccinatedDate
                nextDueDate    = existing.nextDueDate ?: ""
                clinicName     = existing.clinicName ?: ""
                notes          = existing.notes ?: ""
            }
        }
    }

    LaunchedEffect(actionState) {
        if (actionState is ActionState.Success) {
            vm.resetAction()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = title,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Common vaccine chips
            SectionTitle("Tên vaccine *")
            val commonVaccines = listOf("Dại", "5in1", "7in1", "Lepto", "Bordetella", "Kháng sinh", "FPV", "FHV")
            FlowChipGroup(
                options = commonVaccines,
                selected = listOfNotNull(vaccineName.takeIf { it in commonVaccines }),
                onToggle = { vaccineName = it }
            )
            OutlinedTextField(
                value = vaccineName,
                onValueChange = { vaccineName = it },
                placeholder = { Text("Hoặc nhập tên vaccine khác") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Vaccines, null) }
            )

            SectionTitle("Ngày tiêm *")
            DatePickerField(
                label = "Chọn ngày tiêm",
                value = vaccinatedDate,
                onValueChange = { vaccinatedDate = it }
            )

            SectionTitle("Ngày tiêm tiếp theo (tùy chọn)")
            DatePickerField(
                label = "Chọn ngày hẹn tiêm lại",
                value = nextDueDate,
                onValueChange = { nextDueDate = it },
                optional = true
            )

            SectionTitle("Phòng khám / cơ sở thú y")
            OutlinedTextField(
                value = clinicName,
                onValueChange = { clinicName = it },
                placeholder = { Text("Thú y Hoa Mai, Bệnh viện thú y HCMC, ...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.LocalHospital, null) }
            )

            SectionTitle("Ghi chú")
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Phản ứng sau tiêm, liều lượng, ...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            if (actionState is ActionState.Error) {
                Text(
                    (actionState as ActionState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))
            GradientButton(
                text = if (actionState is ActionState.Loading) "Đang lưu..." else if (isEdit) "Cập nhật" else "Thêm vaccine",
                enabled = vaccineName.isNotBlank() && vaccinatedDate.isNotBlank() && actionState !is ActionState.Loading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val req = VaccinationRequest(
                        vaccineName    = vaccineName.trim(),
                        vaccinatedDate = vaccinatedDate.trim(),
                        nextDueDate    = nextDueDate.trim().ifBlank { null },
                        clinicName     = clinicName.trim().ifBlank { null },
                        notes          = notes.trim().ifBlank { null }
                    )
                    if (isEdit) vm.updateVaccination(ctx, editVacId!!, req) {}
                    else vm.addVaccination(ctx, req) {}
                }
            )
        }
    }
}
