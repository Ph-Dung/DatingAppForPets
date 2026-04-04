package com.petmatch.mobile.ui.petprofile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.petmatch.mobile.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetProfileEditScreen(navController: NavController, vm: PetProfileViewModel) {
    val ctx = LocalContext.current
    val petState by vm.myPet.collectAsState()
    val actionState by vm.actionState.collectAsState()

    // Pre-populate form from existing profile
    var form by remember { mutableStateOf(PetFormState()) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadMyProfile(ctx) }

    LaunchedEffect(petState) {
        if (petState is PetUiState.Success && !initialized) {
            val p = (petState as PetUiState.Success).pet
            form = PetFormState(
                name = p.name,
                species = p.species,
                breed = p.breed ?: "",
                gender = p.gender ?: "MALE",
                dateOfBirth = p.dateOfBirth ?: "",
                weightKg = p.weightKg?.toString() ?: "",
                color = p.color ?: "",
                size = p.size ?: "medium",
                reproductiveStatus = p.reproductiveStatus ?: "INTACT",
                healthStatus = p.healthStatus ?: "HEALTHY",
                healthNotes = p.healthNotes ?: "",
                lookingFor = p.lookingFor ?: "FRIENDSHIP",
                notes = p.notes ?: "",
                personalityTags = parsePersonalityTags(p.personalityTags)
            )
            initialized = true
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
                title = "Chỉnh sửa hồ sơ",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            )
        }
    ) { padding ->
        when (petState) {
            is PetUiState.Loading -> PetMatchLoading()
            is PetUiState.Error -> ErrorMessage((petState as PetUiState.Error).message) { vm.loadMyProfile(ctx) }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Render all steps at once for edit view
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Thông tin cơ bản")
                            Spacer(Modifier.height(12.dp))
                            BasicInfoStep(form = form, onFormChange = { form = it })
                        }
                    }
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Sức khỏe")
                            Spacer(Modifier.height(12.dp))
                            HealthStep(form = form, onFormChange = { form = it })
                        }
                    }
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Cá tính & Mục đích")
                            Spacer(Modifier.height(12.dp))
                            PersonalityStep(form = form, onFormChange = { form = it })
                        }
                    }
                }

                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (actionState is ActionState.Error) {
                        Text(
                            text = (actionState as ActionState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    GradientButton(
                        text = if (actionState is ActionState.Loading) "Đang lưu..." else "Lưu thay đổi",
                        enabled = actionState !is ActionState.Loading && form.name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { vm.updateProfile(ctx, form.toRequest()) {} }
                    )
                }
            }
        }
    }
}

fun parsePersonalityTags(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        json.trim('[', ']').split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }
    } catch (_: Exception) { emptyList() }
}
