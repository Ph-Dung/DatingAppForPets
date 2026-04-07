package com.petmatch.mobile.ui.community

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.petmatch.mobile.ui.theme.PrimaryPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(navController: NavController, vm: CommunityViewModel) {
    val ctx = LocalContext.current
    val contentFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var content by rememberSaveable { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var location by rememberSaveable { mutableStateOf("Văn Quán, Hà Nội") }
    val actionLoading by vm.actionLoading.collectAsState()
    val actionDone by vm.actionDone.collectAsState()
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

    LaunchedEffect(actionDone) {
        if (actionDone) {
            vm.clearActionState()
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tạo bài viết", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            vm.createPostWithDeviceImage(
                                ctx = ctx,
                                content = content,
                                location = location,
                                imageUri = selectedImageUri,
                                onDone = { }
                            )
                        },
                        enabled = content.isNotBlank() && !actionLoading
                    ) {
                        Text(
                            if (actionLoading) "Đang đăng..." else "Đăng",
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // User Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e",
                    contentDescription = null,
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Bánh mì hoa cúc", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Text(location, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Content
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Bạn đang nghĩ gì về thú cưng của mình?", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .focusRequester(contentFocusRequester),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                minLines = 7,
                maxLines = 14,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = PrimaryPink)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chọn ảnh từ thư viện")
                }

                if (selectedImageUri != null) {
                    TextButton(onClick = { selectedImageUri = null }) {
                        Text("Xóa ảnh")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}
