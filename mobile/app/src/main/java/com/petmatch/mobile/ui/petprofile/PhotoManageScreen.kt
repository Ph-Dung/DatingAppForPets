package com.petmatch.mobile.ui.petprofile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.ui.common.*
import com.petmatch.mobile.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoManageScreen(navController: NavController, vm: PetProfileViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val petState by vm.myPet.collectAsState()
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.loadMyProfile(ctx) }

    val pet = (petState as? PetUiState.Success)?.pet

    // Image picker — setAsAvatar param truyền qua closure
    var pendingSetAsAvatar by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isUploading = true
        uploadError = null
        scope.launch {
            try {
                val stream = ctx.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = stream.readBytes()
                stream.close()

                val mimeType = ctx.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                val part = MultipartBody.Part.createFormData("file", "photo.jpg", requestBody)

                val res = RetrofitClient.petApi(ctx).addPhoto(part, pendingSetAsAvatar)
                if (res.isSuccessful) {
                    vm.loadMyProfile(ctx)   // refresh
                } else {
                    uploadError = "Upload thất bại: ${res.code()}"
                }
            } catch (e: Exception) {
                uploadError = "Lỗi: ${e.message}"
            } finally {
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = "Quản lý ảnh",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (pet == null) { PetMatchLoading(); return@Scaffold }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Avatar section ──────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Ảnh đại diện",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = pet.avatarUrl ?: "https://placedog.net/140/140",
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(3.dp, PrimaryPink, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            color = PrimaryPink,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    pendingSetAsAvatar = true
                                    launcher.launch("image/*")
                                }
                        ) {
                            Icon(
                                Icons.Default.Edit, null,
                                tint = Color.White,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                    Text(
                        "Nhấn vào biểu tượng ✏️ để thay đổi ảnh đại diện",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Upload status ────────────────────────────────
            if (isUploading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = PrimaryPink
                )
            }
            uploadError?.let { err ->
                Text(
                    err,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── Gallery header ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Thư viện (${pet.photos.size}/10)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                if (pet.photos.size < 10) {
                    TextButton(onClick = {
                        pendingSetAsAvatar = false
                        launcher.launch("image/*")
                    }) {
                        Icon(Icons.Default.Add, null, tint = PrimaryPink, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Thêm ảnh", color = PrimaryPink, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // ── Photo grid ───────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(pet.photos) { photo ->
                    PhotoGridItem(
                        url = photo.url,
                        isAvatar = photo.url == pet.avatarUrl,
                        onDelete = {
                            vm.deletePhoto(ctx, photo.id)
                        },
                        onSetAvatar = {
                            // re-upload as avatar is not possible; user should upload new avatar
                        }
                    )
                }
                // Add placeholder
                if (pet.photos.size < 10) {
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryPink.copy(0.08f))
                                .border(1.dp, PrimaryPink.copy(0.3f), RoundedCornerShape(12.dp))
                                .clickable {
                                    pendingSetAsAvatar = false
                                    launcher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = PrimaryPink, modifier = Modifier.size(30.dp))
                                Text("Thêm", style = MaterialTheme.typography.labelSmall, color = PrimaryPink)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PhotoGridItem(
    url: String,
    isAvatar: Boolean,
    onDelete: () -> Unit,
    onSetAvatar: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.15f))
        )
        if (isAvatar) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                color = PrimaryPink,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "Chính", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White
                )
            }
        }
        // Delete button
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Xóa ảnh") },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}
