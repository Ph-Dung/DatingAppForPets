package com.petmatch.mobile.ui.community

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.petmatch.mobile.data.model.CommunityPostResponse
import com.petmatch.mobile.ui.account.UserInfoState
import com.petmatch.mobile.ui.account.UserViewModel
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.theme.PrimaryPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostManagementScreen(
    navController: NavController,
    vm: CommunityViewModel,
    editPostId: Long? = null
) {
    val ctx = LocalContext.current
    val userVm: UserViewModel = viewModel()
    val userInfo by userVm.userInfo.collectAsState()
    val currentUser = (userInfo as? UserInfoState.Success)?.user

    val myPosts by vm.myPosts.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var deleteTarget by remember { mutableStateOf<CommunityPostResponse?>(null) }

    LaunchedEffect(Unit) {
        userVm.loadMyInfo(ctx)
        vm.loadMyPosts(ctx)
    }

    LaunchedEffect(editPostId) {
        if (editPostId != null) {
            navController.navigate(Routes.postAdd(editPostId))
        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!error.isNullOrBlank()) {
                item {
                    Text(
                        text = error ?: "",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = androidx.compose.ui.graphics.Color.Red
                    )
                }
            }

            items(myPosts, key = { it.id }) { post ->
                CommunityPostItem(
                    post = post,
                    isOwner = true,
                    currentUserAvatarUrl = currentUser?.avatarUrl,
                    onToggleLike = { vm.toggleLike(ctx, post.id) },
                    onOpenComments = { },
                    onEditPost = { navController.navigate(Routes.postAdd(post.id)) },
                    onReportPost = { },
                    onDeletePost = { deleteTarget = post }
                )
            }
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xóa bài viết") },
            text = { Text("Bạn có chắc muốn xóa bài viết này không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val postId = deleteTarget?.id
                        if (postId != null) {
                            vm.deletePost(ctx, postId)
                        }
                        deleteTarget = null
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}
