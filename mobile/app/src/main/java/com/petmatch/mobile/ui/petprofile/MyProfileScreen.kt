package com.petmatch.mobile.ui.petprofile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.petmatch.mobile.ui.auth.AuthViewModel
import com.petmatch.mobile.ui.common.*
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    navController: NavController,
    vm: PetProfileViewModel,
    authVm: AuthViewModel
) {
    val ctx = LocalContext.current
    val petState by vm.myPet.collectAsState()

    LaunchedEffect(Unit) { vm.loadMyProfile(ctx) }

    val pet = (petState as? PetUiState.Success)?.pet

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = "Hồ sơ của tôi",
                actions = {
                    if (pet != null) {
                        IconButton(onClick = { navController.navigate(Routes.PET_EDIT) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color.White)
                        }
                        IconButton(onClick = { navController.navigate(Routes.PHOTO_MANAGE) }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Ảnh", tint = Color.White)
                        }
                    }
                    IconButton(onClick = {
                        authVm.logout(ctx)
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Đăng xuất", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            if (pet == null && petState !is PetUiState.Loading) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.PET_SETUP) },
                    containerColor = PrimaryPink,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Tạo hồ sơ") }
                )
            }
        }
    ) { padding ->
        when (petState) {
            is PetUiState.Loading -> PetMatchLoading()
            is PetUiState.Error   -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Pets, contentDescription = null, tint = PrimaryPink.copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Bạn chưa có hồ sơ thú cưng", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Tạo hồ sơ để bắt đầu ghép đôi!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is PetUiState.Success -> {
                MyProfileContent(
                    pet = pet!!,
                    padding = padding,
                    navController = navController,
                    onToggleHidden = { vm.toggleHidden(ctx) }
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun MyProfileContent(
    pet: PetProfileResponse,
    padding: PaddingValues,
    navController: NavController,
    onToggleHidden: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero section
        Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            AsyncImage(
                model = pet.avatarUrl ?: "https://i.pinimg.com/originals/f1/0f/f7/f10ff70a715515d1662550dccdd44832.png",
                contentDescription = pet.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            // Name + age
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
            ) {
                Text(
                    "${pet.name}${if (pet.age != null) ", ${pet.age} tuổi" else ""}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (!pet.breed.isNullOrBlank()) {
                    Text(pet.breed, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            // Hidden badge
            if (pet.isHidden == true) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Ẩn", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard("${pet.photoUrls.size}", "Ảnh", Icons.Default.PhotoLibrary)
            StatCard("${pet.vaccinationCount}", "Vaccine", Icons.Default.Vaccines)
            StatCard(
                Constants.HEALTH_STATUS_LABELS[pet.healthStatus] ?: pet.healthStatus ?: "-",
                "Sức khỏe", Icons.Default.Favorite
            )
        }

        Divider()

        // Info section
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Thông tin chi tiết", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            InfoRow("Loài", pet.species)
            InfoRow("Giới tính", Constants.GENDER_LABELS[pet.gender] ?: pet.gender)
            InfoRow("Cân nặng", if (pet.weightKg != null) "${pet.weightKg} kg" else null)
            InfoRow("Màu lông", pet.color)
            InfoRow("Kích thước", Constants.SIZE_LABELS[pet.size] ?: pet.size)
            InfoRow("Sinh sản", Constants.REPRODUCTIVE_STATUS_LABELS[pet.reproductiveStatus] ?: pet.reproductiveStatus)
            InfoRow("Mục đích", Constants.LOOKING_FOR_LABELS[pet.lookingFor] ?: pet.lookingFor)
            InfoRow("Ngày sinh", pet.dateOfBirth)
        }

        // Personality tags
        val tags = parsePersonalityTags(pet.personalityTags)
        if (tags.isNotEmpty()) {
            Divider()
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Cá tính", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(10.dp))
                FlowChipGroup(options = tags, selected = tags, onToggle = {})
            }
        }

        // Notes
        if (!pet.notes.isNullOrBlank()) {
            Divider()
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Giới thiệu", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                Text(pet.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Actions
        Divider()
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onToggleHidden,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(
                    if (pet.isHidden == true) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (pet.isHidden == true) "Hiện hồ sơ" else "Ẩn hồ sơ")
            }

            OutlinedButton(
                onClick = { navController.navigate(Routes.VAC_LIST) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(Icons.Default.Vaccines, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Quản lý vaccine (${pet.vaccinationCount})")
            }

            OutlinedButton(
                onClick = { navController.navigate(Routes.WHO_LIKED_ME) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ai đã thích tôi")
            }

            OutlinedButton(
                onClick = { navController.navigate(Routes.BLOCK_LIST) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Danh sách đã chặn", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Icon(icon, contentDescription = null, tint = PrimaryPink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
