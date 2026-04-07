package com.petmatch.mobile.ui.match

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
import com.petmatch.mobile.Constants
import com.petmatch.mobile.ui.common.*
import com.petmatch.mobile.ui.petprofile.FlowChipGroup
import com.petmatch.mobile.ui.petprofile.SectionTitle
import com.petmatch.mobile.ui.theme.PrimaryPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchFilterScreen(navController: NavController, matchVm: MatchViewModel) {
    val ctx = LocalContext.current

    // Local state mirrors VM filter state
    var species     by remember { mutableStateOf(matchVm.filterSpecies.value) }
    var gender      by remember { mutableStateOf(matchVm.filterGender.value) }
    var lookingFor  by remember { mutableStateOf(matchVm.filterLookingFor.value) }
    var healthStatus by remember { mutableStateOf(matchVm.filterHealthStatus.value) }
    var minAge      by remember { mutableFloatStateOf((matchVm.filterMinAge.value ?: 0).toFloat()) }
    var maxAge      by remember { mutableFloatStateOf((matchVm.filterMaxAge.value ?: 15).toFloat()) }

    val hasFilter = species != null || gender != null || lookingFor != null ||
            healthStatus != null || minAge > 0f || maxAge < 15f

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = "Bộ lọc ghép đôi",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (hasFilter) {
                        TextButton(
                            onClick = {
                                species = null; gender = null; lookingFor = null
                                healthStatus = null; minAge = 0f; maxAge = 15f
                                matchVm.clearFilters(ctx)
                                navController.popBackStack()
                            }
                        ) { Text("Xóa lọc", color = Color.White) }
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                GradientButton(
                    text = "Áp dụng bộ lọc",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        matchVm.applyFilters(
                            species      = species,
                            gender       = gender,
                            lookingFor   = lookingFor,
                            minAge       = minAge.toInt().takeIf { it > 0 },
                            maxAge       = maxAge.toInt().takeIf { it < 15 },
                            healthStatus = healthStatus,
                            ctx          = ctx
                        )
                        navController.popBackStack()
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Loài
            FilterSection(title = "Loài thú cưng") {
                MultiSelectChips(
                    options = Constants.SPECIES_LIST,
                    selected = listOfNotNull(species),
                    onToggle = { species = if (species == it) null else it }
                )
            }

            // Giới tính
            FilterSection(title = "Giới tính") {
                MultiSelectChips(
                    options = listOf("MALE", "FEMALE", "OTHER"),
                    selected = listOfNotNull(gender),
                    labelMap = Constants.GENDER_LABELS,
                    onToggle = { gender = if (gender == it) null else it }
                )
            }

            // Mục đích
            FilterSection(title = "Mục đích ghép đôi") {
                MultiSelectChips(
                    options = listOf("BREEDING", "FRIENDSHIP", "PLAY"),
                    selected = listOfNotNull(lookingFor),
                    labelMap = Constants.LOOKING_FOR_LABELS,
                    onToggle = { lookingFor = if (lookingFor == it) null else it }
                )
            }

            // Sức khỏe
            FilterSection(title = "Tình trạng sức khỏe") {
                MultiSelectChips(
                    options = listOf("HEALTHY", "SICK", "RECOVERING", "CHRONIC"),
                    selected = listOfNotNull(healthStatus),
                    labelMap = Constants.HEALTH_STATUS_LABELS,
                    onToggle = { healthStatus = if (healthStatus == it) null else it }
                )
            }

            // Độ tuổi
            FilterSection(title = "Độ tuổi: ${minAge.toInt()} – ${maxAge.toInt()} tuổi") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Tuổi tối thiểu: ${minAge.toInt()} tuổi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = minAge,
                        onValueChange = { minAge = it.coerceAtMost(maxAge - 1) },
                        valueRange = 0f..15f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = PrimaryPink, activeTrackColor = PrimaryPink)
                    )
                    Text(
                        "Tuổi tối đa: ${maxAge.toInt()} tuổi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = maxAge,
                        onValueChange = { maxAge = it.coerceAtLeast(minAge + 1) },
                        valueRange = 1f..15f,
                        steps = 13,
                        colors = SliderDefaults.colors(thumbColor = PrimaryPink, activeTrackColor = PrimaryPink)
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = PrimaryPink
        )
        content()
        Divider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiSelectChips(
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    labelMap: Map<String, String> = emptyMap()
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            SelectableChip(
                label = labelMap[opt] ?: opt,
                selected = opt in selected,
                onClick = { onToggle(opt) }
            )
        }
    }
}
