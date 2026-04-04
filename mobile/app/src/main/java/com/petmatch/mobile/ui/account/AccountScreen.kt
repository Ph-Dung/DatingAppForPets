package com.petmatch.mobile.ui.account

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.ui.auth.AuthViewModel
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.petprofile.PetProfileViewModel
import com.petmatch.mobile.ui.petprofile.PetUiState
import com.petmatch.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    petVm: PetProfileViewModel,
    authVm: AuthViewModel,
    userVm: UserViewModel
) {
    val ctx = LocalContext.current
    val petState by petVm.myPet.collectAsState()
    val pet = (petState as? PetUiState.Success)?.pet
    val userState by userVm.userInfo.collectAsState()
    val user = (userState as? UserInfoState.Success)?.user

    LaunchedEffect(Unit) {
        petVm.loadMyProfile(ctx)
        userVm.loadMyInfo(ctx)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tài khoản",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = {
                        authVm.logout(ctx)
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }) {
                        Icon(Icons.Default.Logout, "Đăng xuất", tint = Color.White)
                    }
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
            // ── Hero header — hiển thị thông tin CHỦ SỞ HỮU ──────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(PrimaryPink, SecondaryOrange.copy(0.6f)))
                    )
                    .padding(top = 16.dp, bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar chủ
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.2f))
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user?.avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            // Ava default kiểu chuyên nghiệp
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE4E6EB)), // Màu xám nhạt FB
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = Color(0xFF8A8D91), // Màu xám đậm icon
                                    modifier = Modifier.size(70.dp).offset(y = 10.dp)
                                )
                            }
                        }
                    }

                    // Tên chủ sở hữu
                    Text(
                        user?.fullName ?: "Đang tải...",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    // Email
                    if (user?.email != null) {
                        Text(
                            user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.8f)
                        )
                    }

                    // Phone nếu có
                    if (!user?.phone.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Phone, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(14.dp))
                            Text(user!!.phone!!, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                        }
                    }
                }
            }

            // ── Thông tin cá nhân ─────────────────────────────────
            SectionHeader("Thông tin cá nhân")

            AccountMenuItem(
                icon = Icons.Default.Edit,
                label = "Sửa thông tin",
                subtitle = "Cập nhật họ tên, số điện thoại, địa chỉ",
                onClick = { navController.navigate(Routes.EDIT_USER_PROFILE) }
            )
            AccountMenuItem(
                icon = Icons.Default.Lock,
                label = "Đổi mật khẩu",
                subtitle = "Thay đổi mật khẩu tài khoản",
                onClick = { navController.navigate(Routes.CHANGE_PASSWORD) }
            )

            // ── Pet Profile Section ───────────────────────────────
            Spacer(Modifier.height(8.dp))
            SectionHeader("Hồ sơ thú cưng")

            if (pet == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPink.copy(0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🐾", fontSize = 36.sp)
                        Text("Chưa có hồ sơ thú cưng", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("Tạo hồ sơ để bắt đầu ghép đôi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { navController.navigate(Routes.PET_SETUP) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Tạo hồ sơ ngay")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { navController.navigate(Routes.MY_PET) },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = pet.avatarUrl ?: "https://placedog.net/60/60",
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pet.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                buildString {
                                    append(pet.species)
                                    if (!pet.breed.isNullOrBlank()) append(" · ${pet.breed}")
                                    if (pet.age != null) append(" · ${pet.age} tuổi")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionChip(icon = Icons.Default.Edit, label = "Sửa hồ sơ", onClick = { navController.navigate(Routes.PET_EDIT) }, modifier = Modifier.weight(1f))
                    QuickActionChip(icon = Icons.Default.PhotoLibrary, label = "Quản lý ảnh", onClick = { navController.navigate(Routes.PHOTO_MANAGE) }, modifier = Modifier.weight(1f))
                    QuickActionChip(icon = Icons.Default.Vaccines, label = "Vaccine", onClick = { navController.navigate(Routes.VAC_LIST) }, modifier = Modifier.weight(1f))
                }
            }

            // ── Hoạt động ─────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            SectionHeader("Hoạt động")

            AccountMenuItem(icon = Icons.Default.SmartToy, label = "AI Tìm kiếm thú cưng", subtitle = "Chat với AI để tìm bạn đôi phù hợp", onClick = { navController.navigate(Routes.AI_CHATBOT) }, iconTint = AccentPurple)
            AccountMenuItem(icon = Icons.Default.FavoriteBorder, label = "Ai đã thích tôi", subtitle = "Xem danh sách nhận được", onClick = { navController.navigate(Routes.WHO_LIKED_ME) })
            AccountMenuItem(icon = Icons.Default.Favorite, label = "Danh sách đã ghép đôi", subtitle = "Xem các cặp đôi của bạn", onClick = { navController.navigate(Routes.MATCHED_LIST) })

            // ── Cài đặt ───────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            SectionHeader("Cài đặt")

            AccountMenuItem(
                icon = Icons.Default.Block,
                label = "Danh sách đã chặn",
                subtitle = "Quản lý người dùng đã chặn",
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { navController.navigate(Routes.BLOCK_LIST) }
            )

            // ── Đăng xuất ─────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        authVm.logout(ctx)
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error)
                    Text("Đăng xuất", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun AccountMenuItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = PrimaryPink
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = iconTint.copy(0.1f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = PrimaryPink, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = PrimaryPink)
        }
    }
}
