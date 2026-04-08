package com.petmatch.mobile.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.data.model.CommunityPostResponse
import com.petmatch.mobile.ui.theme.PrimaryPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostManagementScreen(
    navController: NavController,
    vm: CommunityViewModel,
    editPostId: Long? = null
) {
    val ctx = LocalContext.current
    val myPosts by vm.myPosts.collectAsState()
    val loading by vm.loading.collectAsState()
    val actionLoading by vm.actionLoading.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadMyPosts(ctx)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quản lý bài đăng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryPink)
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
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(myPosts) { post ->
                    ManagePostItem(
                        post = post,
                        startInEditMode = editPostId == post.id,
                        actionLoading = actionLoading,
                        onDelete = { vm.deletePost(ctx, post.id) },
                        onSaveEdit = { content, imageUrl, location ->
                            vm.updatePost(ctx, post.id, content, imageUrl, location)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ManagePostItem(
    post: CommunityPostResponse,
    startInEditMode: Boolean,
    actionLoading: Boolean,
    onDelete: () -> Unit,
    onSaveEdit: (String, String?, String?) -> Unit
) {
    var isEditing by remember(post.id, startInEditMode) { mutableStateOf(startInEditMode) }
    var editContent by remember(post.id) { mutableStateOf(post.content) }
    var editImageUrl by remember(post.id) { mutableStateOf(post.imageUrl ?: "") }
    var editLocation by remember(post.id) { mutableStateOf(post.location ?: "") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.ownerAvatar,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier) {
                Text(post.ownerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(post.location ?: "", color = Color.Gray, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isEditing) {
                Button(
                    onClick = { isEditing = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF1F1)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Sửa", color = PrimaryPink, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    enabled = !actionLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Xóa", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        if (isEditing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nội dung") }
                )
                OutlinedTextField(
                    value = editLocation,
                    onValueChange = { editLocation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Địa điểm") }
                )
                OutlinedTextField(
                    value = editImageUrl,
                    onValueChange = { editImageUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL ảnh") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSaveEdit(
                                editContent,
                                editImageUrl.ifBlank { null },
                                editLocation.ifBlank { null }
                            )
                            isEditing = false
                        },
                        enabled = editContent.isNotBlank() && !actionLoading
                    ) {
                        Text("Lưu")
                    }
                    TextButton(onClick = {
                        editContent = post.content
                        editImageUrl = post.imageUrl ?: ""
                        editLocation = post.location ?: ""
                        isEditing = false
                    }) {
                        Text("Hủy")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Text(
                "${post.ownerName} ${post.content}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 13.sp
            )
            Text(
                "${post.likesCount} lượt thích, ${post.commentsCount} bình luận",
                modifier = Modifier.padding(horizontal = 12.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        if (!post.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = "Post Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PostManagementPreview() {
    MaterialTheme {
        val context = androidx.compose.ui.platform.LocalContext.current
        PostManagementScreen(
            navController = androidx.navigation.NavController(context),
            vm = CommunityViewModel()
        )
    }
}
