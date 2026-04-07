package com.petmatch.mobile.ui.petprofile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.Constants
import com.petmatch.mobile.data.model.PetProfileResponse
import com.petmatch.mobile.ui.common.*
import com.petmatch.mobile.ui.interaction.InteractionViewModel
import com.petmatch.mobile.ui.match.MatchViewModel
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PetProfileDetailScreen(
    navController: NavController,
    petId: Long,
    vm: PetProfileViewModel,
    matchVm: MatchViewModel,
    interVm: InteractionViewModel
) {
    val ctx = LocalContext.current
    val petState by vm.viewedPet.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(petId) { vm.loadPetById(ctx, petId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn", tint = Color.White)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.error) },
                                text = { Text("Báo cáo vi phạm") },
                                onClick = {
                                    showMenu = false
                                    val pet = (petState as? PetUiState.Success)?.pet
                                    if (pet != null) navController.navigate(Routes.report(petId, pet.ownerId))
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) },
                                text = { Text("Chặn người dùng này") },
                                onClick = { showMenu = false; showBlockDialog = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { _ ->
        when (val state = petState) {
            is PetUiState.Loading -> PetMatchLoading()
            is PetUiState.Error   -> ErrorMessage(state.message) { vm.loadPetById(ctx, petId) }
            is PetUiState.Success -> {
                PetDetailContent(pet = state.pet, navController = navController, matchVm = matchVm, ctx = ctx)
            }
            else -> {}
        }
    }

    // Block confirmation dialog
    if (showBlockDialog) {
        val pet = (petState as? PetUiState.Success)?.pet
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Chặn người dùng?") },
            text = { Text("Họ sẽ không thể xem hồ sơ của bạn và ngược lại. Bạn có thể bỏ chặn sau.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockDialog = false
                        if (pet != null) {
                            interVm.blockUser(ctx, pet.ownerId) { navController.popBackStack() }
                        }
                    }
                ) { Text("Chặn", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PetDetailContent(
    pet: PetProfileResponse,
    navController: NavController,
    matchVm: MatchViewModel,
    ctx: android.content.Context
) {
    val photos = if (pet.avatarUrl != null)
        listOf(pet.avatarUrl) + pet.photoUrls.filter { it != pet.avatarUrl }
    else pet.photoUrls.ifEmpty { listOf("https://placedog.net/400/600") }

    val pagerState = rememberPagerState { photos.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Photo pager
        Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = photos[page],
                    contentDescription = pet.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(0.75f)),
                        startY = 200f
                    ))
            )
            // Page indicators
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                photos.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pagerState.currentPage) 24.dp else 6.dp, 4.dp)
                            .background(
                                if (i == pagerState.currentPage) Color.White else Color.White.copy(0.5f),
                                CircleShape
                            )
                    )
                }
            }
            // Info overlay at bottom
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "${pet.name}${if (pet.age != null) ", ${pet.age} tuổi" else ""}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (!pet.breed.isNullOrBlank())
                    Text(pet.breed, color = Color.White.copy(0.88f))
                // Health + vaccine row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pet.isVaccinated == true) {
                        PillBadge("✓ Đã tiêm vaccine", Color(0xFF22C55E))
                    }
                    val healthLabel = Constants.HEALTH_STATUS_LABELS[pet.healthStatus]
                    if (healthLabel != null) PillBadge(healthLabel, PrimaryPink)
                }
            }
        }

        // Detail cards
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Mục đích
            val lookingForLabel = Constants.LOOKING_FOR_LABELS[pet.lookingFor] ?: pet.lookingFor
            if (lookingForLabel != null) {
                InfoCard(title = "Mục đích") {
                    Text(lookingForLabel, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = PrimaryPink)
                }
            }

            // Thông tin
            InfoCard(title = "Thông tin") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoRow("Loài", pet.species)
                    InfoRow("Giống", pet.breed)
                    InfoRow("Giới tính", Constants.GENDER_LABELS[pet.gender] ?: pet.gender)
                    InfoRow("Cân nặng", if (pet.weightKg != null) "${pet.weightKg} kg" else null)
                    InfoRow("Màu lông", pet.color)
                    InfoRow("Sized", Constants.SIZE_LABELS[pet.size] ?: pet.size)
                    InfoRow("Sinh sản", Constants.REPRODUCTIVE_STATUS_LABELS[pet.reproductiveStatus] ?: pet.reproductiveStatus)
                    InfoRow("Vaccine", if (pet.isVaccinated == true) "${pet.vaccinationCount} lần tiêm" else "Chưa tiêm")
                }
            }

            // Sức khỏe
            if (!pet.healthNotes.isNullOrBlank()) {
                InfoCard(title = "Sức khỏe") {
                    Text(pet.healthNotes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Tính cách
            val tags = parsePersonalityTags(pet.personalityTags)
            if (tags.isNotEmpty()) {
                InfoCard(title = "Cá tính") {
                    FlowChipGroup(options = tags, selected = tags, onToggle = {})
                }
            }

            // Giới thiệu
            if (!pet.notes.isNullOrBlank()) {
                InfoCard(title = "Về ${pet.name}") {
                    Text(pet.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun PillBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            content()
        }
    }
}
