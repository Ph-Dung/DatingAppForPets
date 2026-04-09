package com.petmatch.mobile.ui.petprofile

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.petmatch.mobile.Constants
import com.petmatch.mobile.data.model.PetProfileRequest
import com.petmatch.mobile.ui.common.*
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.theme.PrimaryPink

/** Shared form state để dùng cho cả Setup và Edit */
data class PetFormState(
    val name: String = "",
    val species: String = "Chó",
    val breed: String = "",
    val gender: String = "MALE",
    val dateOfBirth: String = "",
    val weightKg: String = "",
    val color: String = "",
    val size: String = "medium",
    val reproductiveStatus: String = "INTACT",
    val healthStatus: String = "HEALTHY",
    val healthNotes: String = "",
    val lookingFor: String = "FRIENDSHIP",
    val notes: String = "",
    val personalityTags: List<String> = emptyList()
)

fun PetFormState.toRequest() = PetProfileRequest(
    name = name,
    species = species,
    breed = breed.ifBlank { null },
    gender = gender,
    dateOfBirth = dateOfBirth.ifBlank { null },
    weightKg = weightKg.toDoubleOrNull(),
    color = color.ifBlank { null },
    size = size,
    reproductiveStatus = reproductiveStatus,
    isVaccinated = false,
    lastVaccineDate = null,
    healthStatus = healthStatus,
    healthNotes = healthNotes.ifBlank { null },
    personalityTags = if (personalityTags.isEmpty()) null
                      else "[${personalityTags.joinToString(",") { "\"$it\""}}]",
    lookingFor = lookingFor,
    notes = notes.ifBlank { null }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetProfileSetupScreen(navController: NavController, vm: PetProfileViewModel) {
    val ctx = LocalContext.current
    var form by remember { mutableStateOf(PetFormState()) }
    val actionState by vm.actionState.collectAsState()
    var step by remember { mutableIntStateOf(0) }  // wizard: 0=basic, 1=health, 2=personality
    val steps = listOf("Thông tin cơ bản", "Sức khỏe", "Cá tính & Mục đích")

    LaunchedEffect(actionState) {
        if (actionState is ActionState.Success) {
            vm.resetAction()
            navController.navigate(Routes.MATCH_SWIPE) {
                popUpTo(Routes.PET_SETUP) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            PetMatchTopBar(title = "Tạo hồ sơ thú cưng")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Step indicator
            StepIndicator(currentStep = step, totalSteps = steps.size, labels = steps)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedContent(targetState = step, label = "step") { s ->
                    when (s) {
                        0 -> BasicInfoStep(form = form, onFormChange = { form = it })
                        1 -> HealthStep(form = form, onFormChange = { form = it })
                        2 -> PersonalityStep(form = form, onFormChange = { form = it })
                    }
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) { Text("Quay lại") }
                }

                GradientButton(
                    text = if (step < 2) "Tiếp theo" else "Tạo hồ sơ",
                    enabled = actionState !is ActionState.Loading && form.name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (step < 2) step++
                        else vm.createProfile(ctx, form.toRequest()) {}
                    }
                )
            }

            if (actionState is ActionState.Error) {
                Text(
                    text = (actionState as ActionState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int, labels: List<String>) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(totalSteps) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (i <= currentStep) PrimaryPink else PrimaryPink.copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = labels[currentStep],
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = PrimaryPink
        )
    }
}

@Composable
fun BasicInfoStep(form: PetFormState, onFormChange: (PetFormState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Tên thú cưng *")
        OutlinedTextField(
            value = form.name,
            onValueChange = { onFormChange(form.copy(name = it)) },
            placeholder = { Text("Ví dụ: Mochi, Bông, ...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        SectionTitle("Loài *")
        ChipGroup(
            options = Constants.SPECIES_LIST,
            selected = form.species,
            onSelect = { onFormChange(form.copy(species = it)) }
        )

        SectionTitle("Giống (tùy chọn)")
        OutlinedTextField(
            value = form.breed,
            onValueChange = { onFormChange(form.copy(breed = it)) },
            placeholder = { Text("Poodle, Mèo Anh lông ngắn, ...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        SectionTitle("Giới tính *")
        ChipGroup(
            options = listOf("MALE", "FEMALE", "OTHER"),
            selected = form.gender,
            onSelect = { onFormChange(form.copy(gender = it)) },
            labelMap = Constants.GENDER_LABELS
        )

        SectionTitle("Ngày sinh")
        DatePickerField(
            label = "Ngày sinh (tùy chọn)",
            value = form.dateOfBirth,
            onValueChange = { onFormChange(form.copy(dateOfBirth = it)) },
            optional = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitle("Cân nặng (kg)")
                OutlinedTextField(
                    value = form.weightKg,
                    onValueChange = { onFormChange(form.copy(weightKg = it)) },
                    placeholder = { Text("3.5") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                SectionTitle("Màu lông")
                OutlinedTextField(
                    value = form.color,
                    onValueChange = { onFormChange(form.copy(color = it)) },
                    placeholder = { Text("Trắng, Đen, ...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        SectionTitle("Kích thước")
        ChipGroup(
            options = listOf("small", "medium", "large"),
            selected = form.size,
            onSelect = { onFormChange(form.copy(size = it)) },
            labelMap = Constants.SIZE_LABELS
        )
    }
}

@Composable
fun HealthStep(form: PetFormState, onFormChange: (PetFormState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Tình trạng sức khỏe *")
        ChipGroup(
            options = listOf("HEALTHY", "SICK", "RECOVERING", "CHRONIC"),
            selected = form.healthStatus,
            onSelect = { onFormChange(form.copy(healthStatus = it)) },
            labelMap = Constants.HEALTH_STATUS_LABELS
        )

        SectionTitle("Ghi chú sức khỏe")
        OutlinedTextField(
            value = form.healthNotes,
            onValueChange = { onFormChange(form.copy(healthNotes = it)) },
            placeholder = { Text("Dị ứng, bệnh nền, ghi chú khác...") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4
        )

        SectionTitle("Tình trạng sinh sản *")
        ChipGroup(
            options = listOf("INTACT", "NEUTERED", "SPAYED"),
            selected = form.reproductiveStatus,
            onSelect = { onFormChange(form.copy(reproductiveStatus = it)) },
            labelMap = Constants.REPRODUCTIVE_STATUS_LABELS
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = PrimaryPink.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Vaccines, contentDescription = null, tint = PrimaryPink)
                Column {
                    Text(
                        "Thêm lịch sử vaccine sau khi tạo profile",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        "Vào mục Vaccine trong hồ sơ của bạn để thêm chi tiết.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalityStep(form: PetFormState, onFormChange: (PetFormState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Mục đích ghép đôi *")
        ChipGroup(
            options = listOf("BREEDING", "FRIENDSHIP", "PLAY"),
            selected = form.lookingFor,
            onSelect = { onFormChange(form.copy(lookingFor = it)) },
            labelMap = Constants.LOOKING_FOR_LABELS
        )

        SectionTitle("Tính cách (chọn nhiều)")
        FlowChipGroup(
            options = Constants.PERSONALITY_TAGS,
            selected = form.personalityTags,
            onToggle = { tag ->
                val updated = if (tag in form.personalityTags)
                    form.personalityTags - tag
                else form.personalityTags + tag
                onFormChange(form.copy(personalityTags = updated))
            }
        )

        SectionTitle("Giới thiệu thêm")
        OutlinedTextField(
            value = form.notes,
            onValueChange = { onFormChange(form.copy(notes = it)) },
            placeholder = { Text("Vài điều thú vị về thú cưng của bạn...") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 5
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun ChipGroup(
    options: List<String>, selected: String, onSelect: (String) -> Unit,
    labelMap: Map<String, String> = emptyMap()
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        options.forEach { opt ->
            SelectableChip(
                label = labelMap[opt] ?: opt,
                selected = selected == opt,
                onClick = { onSelect(opt) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowChipGroup(
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { tag ->
            SelectableChip(
                label = tag,
                selected = tag in selected,
                onClick = { onToggle(tag) }
            )
        }
    }
}
