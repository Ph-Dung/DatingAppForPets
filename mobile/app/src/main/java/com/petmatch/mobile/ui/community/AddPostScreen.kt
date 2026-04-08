package com.petmatch.mobile.ui.community

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.ui.account.UserInfoState
import com.petmatch.mobile.ui.account.UserViewModel
import com.petmatch.mobile.ui.common.LocationUpdateButton
import com.petmatch.mobile.ui.theme.PrimaryPink
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    navController: NavController,
    vm: CommunityViewModel,
    editPostId: Long? = null
) {
    val ctx = LocalContext.current
    val userVm: UserViewModel = viewModel()
    val userInfo by userVm.userInfo.collectAsState()
    val currentUser = (userInfo as? UserInfoState.Success)?.user

    var content by rememberSaveable { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var location by rememberSaveable { mutableStateOf("") }
    val actionLoading by vm.actionLoading.collectAsState()
    val actionDone by vm.actionDone.collectAsState()
    val myPosts by vm.myPosts.collectAsState()
    val isEditMode = editPostId != null
    val editingPost = remember(editPostId, myPosts) { myPosts.firstOrNull { it.id == editPostId } }
    var prefilled by remember(editPostId) { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { 
            val uri = saveBitmapToTempUri(ctx, it)
            if (uri != null && selectedImageUris.size < 4) {
                selectedImageUris = selectedImageUris + uri
            }
        }
    }

    val libraryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                if (selectedImageUris.size < 4) {
                    selectedImageUris = selectedImageUris + uri
                }
            }
        }
    }

    LaunchedEffect(actionDone) {
        if (actionDone) {
            vm.clearActionState()
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        userVm.loadMyInfo(ctx)
        if (isEditMode) {
            vm.loadMyPosts(ctx)
        }
    }

    LaunchedEffect(editingPost?.id) {
        if (isEditMode && !prefilled && editingPost != null) {
            content = editingPost.content
            location = editingPost.location.orEmpty()
            selectedImageUris = parseImageUris(editingPost.imageUrl)
            prefilled = true
        }
    }

    LaunchedEffect(currentUser?.address) {
        if (location.isBlank()) {
            location = currentUser?.address.orEmpty()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditMode) "Chỉnh sửa bài viết" else "Tạo bài viết", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (isEditMode && editPostId != null) {
                                vm.updatePostWithDeviceImages(
                                    ctx = ctx,
                                    id = editPostId,
                                    content = content,
                                    location = location,
                                    imageUris = selectedImageUris,
                                    onDone = { navController.popBackStack() }
                                )
                            } else {
                                vm.createPostWithDeviceImages(
                                    ctx = ctx,
                                    content = content,
                                    location = location,
                                    imageUris = selectedImageUris,
                                    onDone = { }
                                )
                            }
                        },
                        enabled = content.isNotBlank() && !actionLoading
                    ) {
                        Text(
                            if (actionLoading) {
                                if (isEditMode) "Đang cập nhật..." else "Đang đăng..."
                            } else {
                                if (isEditMode) "Cập nhật" else "Đăng"
                            },
                            color = if (content.isNotBlank() && !actionLoading) PrimaryPink else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            // User Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!currentUser?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = currentUser?.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE4E6EB)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Icon(
                            Icons.Default.Person,
                            null,
                            tint = Color(0xFF8A8D91),
                            modifier = Modifier.size(34.dp).offset(y = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(currentUser?.fullName ?: "Người dùng", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Text(
                            if (location.isBlank()) "Chưa cập nhật vị trí" else location,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isEditMode) {
                LocationUpdateButton(
                    onLocationObtained = { lat, lon ->
                        userVm.updateLocation(ctx, lat, lon)
                        val cityName = getReverseGeocodedAddress(ctx, lat, lon)
                        location = cityName ?: "Vị trí: %.4f, %.4f".format(lat, lon)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Input Content
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Bạn đang nghĩ gì về thú cưng của mình?", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                minLines = 6,
                maxLines = 12,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Image Gallery (LazyRow with horizontal scroll, max 4 images)
            if (selectedImageUris.isNotEmpty()) {
                Text("Ảnh đã chọn (${selectedImageUris.size}/4)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(selectedImageUris, key = { it.toString() }) { uri ->
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEEEEEE))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    selectedImageUris = selectedImageUris.filterNot { it == uri }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            }

            // Button area fixed at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedImageUris.size < 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera", fontSize = 14.sp)
                        }

                        FilledTonalButton(
                            onClick = { libraryLauncher.launch(createDownloadsPickerIntent()) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thư viện", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun createDownloadsPickerIntent(): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadsUri = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:Download"
            )
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloadsUri)
        }
    }
}

private fun saveBitmapToTempUri(ctx: android.content.Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(ctx.cacheDir, "camera_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

private fun getReverseGeocodedAddress(ctx: android.content.Context, lat: Double, lon: Double): String? {
    return try {
        val geocoder = Geocoder(ctx, Locale("vi", "VN"))
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
        }
        
        addresses?.let {
            when {
                !it.subAdminArea.isNullOrBlank() -> it.subAdminArea
                !it.adminArea.isNullOrBlank() -> it.adminArea
                !it.countryName.isNullOrBlank() -> it.countryName
                else -> null
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun parseImageUris(imageUrlRaw: String?): List<Uri> {
    if (imageUrlRaw.isNullOrBlank()) return emptyList()

    val normalized = imageUrlRaw.trim()
    val urls = if (normalized.startsWith("[") && normalized.endsWith("]")) {
        normalized.removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim().trim('"') }
            .filter { it.isNotBlank() }
    } else if (normalized.contains(",") || normalized.contains(";")) {
        normalized.split(',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    } else {
        listOf(normalized)
    }

    return urls.map { Uri.parse(it) }
}
