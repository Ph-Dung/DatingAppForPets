package com.petmatch.mobile.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.petmatch.mobile.data.model.CommunityPostResponse
import com.petmatch.mobile.ui.common.GradientButton
import com.petmatch.mobile.ui.common.PetMatchTopBar
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.petprofile.PetProfileViewModel
import com.petmatch.mobile.ui.petprofile.PetUiState
import com.petmatch.mobile.ui.theme.PrimaryPink
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: NavController, vm: CommunityViewModel) {
    val ctx = LocalContext.current
    val posts by vm.feed.collectAsState()
    val loading by vm.loading.collectAsState()
    val actionLoading by vm.actionLoading.collectAsState()
    val error by vm.error.collectAsState()
    val comments by vm.comments.collectAsState()
    val commentsLoading by vm.commentsLoading.collectAsState()
    val petVm: PetProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val myPet by petVm.myPet.collectAsState()
    val currentPet = (myPet as? PetUiState.Success)?.pet

    var commentPostId by remember { mutableStateOf<Long?>(null) }
    var replyToCommentId by remember { mutableStateOf<Long?>(null) }
    var showReportDialogForPostId by remember { mutableStateOf<Long?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var commentInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.loadFeed(ctx)
        petVm.loadMyProfile(ctx)
    }

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = "Cộng đồng",
                actions = {
                    IconButton(onClick = { Log.d("CommunityScreen", "Search tapped - TODO") }) {
                        Icon(Icons.Default.Search, null, tint = Color.Black)
                    }
                    IconButton(onClick = { Log.d("CommunityScreen", "Notification tapped - TODO") }) {
                        Icon(Icons.Default.NotificationsNone, null, tint = Color.Black)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                CreatePostBar(
                    petAvatarUrl = currentPet?.avatarUrl,
                    petName = currentPet?.name,
                    onPostClick = { navController.navigate(Routes.POST_ADD) },
                    onManageClick = { navController.navigate(Routes.POST_MANAGEMENT) }
                )
            }
            if (loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!loading && posts.isEmpty()) {
                item {
                    CommunityEmptyState(
                        onCreatePost = { navController.navigate(Routes.POST_ADD) },
                        onRetry = { vm.loadFeed(ctx) }
                    )
                }
            }

            if (!error.isNullOrBlank()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error ?: "",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { vm.clearError() }) {
                                Text("Đóng")
                            }
                        }
                    }
                }
            }

            items(posts, key = { it.id }) { post ->
                CommunityPostItem(
                    post = post,
                    onToggleLike = { vm.toggleLike(ctx, post.id) },
                    onOpenComments = {
                        commentPostId = post.id
                        replyToCommentId = null
                        commentInput = ""
                        vm.loadComments(ctx, post.id)
                    },
                    onReportPost = {
                        showReportDialogForPostId = post.id
                        reportReason = ""
                    }
                )
            }
        }
    }

    if (commentPostId != null) {
        ModalBottomSheet(
            onDismissRequest = {
                commentPostId = null
                replyToCommentId = null
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Bình luận",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (commentsLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (comments.isEmpty()) {
                    Text("Chưa có bình luận nào", color = Color.Gray)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(comments, key = { it.id }) { c ->
                            CommentItem(
                                name = c.userName,
                                avatarUrl = c.userAvatar,
                                content = c.content,
                                depth = 0,
                                onReply = { replyToCommentId = c.id }
                            )
                            c.replies.forEach { r ->
                                CommentItem(
                                    name = r.userName,
                                    avatarUrl = r.userAvatar,
                                    content = r.content,
                                    depth = 1,
                                    onReply = { replyToCommentId = r.id }
                                )
                            }
                        }
                    }
                }

                if (replyToCommentId != null) {
                    Text(
                        text = "Đang trả lời bình luận #$replyToCommentId",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryPink
                    )
                    TextButton(onClick = { replyToCommentId = null }) {
                        Text("Hủy trả lời")
                    }
                }

                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp),
                    label = { Text(if (replyToCommentId != null) "Viết phản hồi" else "Viết bình luận") },
                    minLines = 2,
                    maxLines = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        commentPostId = null
                        replyToCommentId = null
                    }) {
                        Text("Đóng")
                    }
                    TextButton(
                        enabled = commentInput.isNotBlank() && !actionLoading,
                        onClick = {
                            val postId = commentPostId
                            if (postId != null) {
                                val replyingId = replyToCommentId
                                if (replyingId != null) {
                                    vm.replyComment(ctx, postId, replyingId, commentInput) {
                                        commentInput = ""
                                        replyToCommentId = null
                                    }
                                } else {
                                    vm.addComment(ctx, postId, commentInput) {
                                        commentInput = ""
                                    }
                                }
                            }
                        }
                    ) {
                        Text(if (actionLoading) "Đang gửi..." else "Gửi")
                    }
                }
            }
        }
    }

    if (showReportDialogForPostId != null) {
        AlertDialog(
            onDismissRequest = { showReportDialogForPostId = null },
            title = { Text("Báo cáo bài viết") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lý do") },
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(
                    enabled = reportReason.isNotBlank() && !actionLoading,
                    onClick = {
                        val postId = showReportDialogForPostId
                        if (postId != null) {
                            vm.submitReport(ctx, postId, reportReason) {
                                showReportDialogForPostId = null
                                reportReason = ""
                            }
                        }
                    }
                ) {
                    Text(if (actionLoading) "Đang gửi..." else "Gửi báo cáo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialogForPostId = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun CreatePostBar(
    petAvatarUrl: String?,
    petName: String?,
    onPostClick: () -> Unit,
    onManageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = petAvatarUrl ?: "https://placedog.net/120/120",
            contentDescription = petName ?: "Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF5F5F5))
                .clickable { onPostClick() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Bạn đang nghĩ gì, ${petName ?: "thú cưng của bạn"}?", color = Color.Gray, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onManageClick() }
        ) {
            Icon(Icons.Outlined.Settings, null, tint = PrimaryPink)
            Text("Quản lý", color = PrimaryPink, fontSize = 10.sp)
        }
    }
}

@Composable
fun CommunityPostItem(
    post: CommunityPostResponse,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onReportPost: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.ownerAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(post.ownerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (!post.location.isNullOrBlank()) {
                    Text(post.location, color = Color.Gray, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Box {
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(Icons.Default.MoreVert, null)
            }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Báo cáo bài viết") },
                        onClick = {
                            showMoreMenu = false
                            onReportPost()
                        }
                    )
                }
            }
        }

        Text(
            post.content,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            fontSize = 14.sp
        )

        if (!post.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleLike) {
                Icon(
                    if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    null,
                    tint = if (post.isLiked) Color(0xFFE53935) else Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("${post.likesCount}", fontSize = 13.sp)
            
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onOpenComments() }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("${post.commentsCount}", fontSize = 13.sp)
        }
        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
    }
}

@Composable
private fun CommentItem(
    name: String,
    avatarUrl: String?,
    content: String,
    depth: Int,
    onReply: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 18).dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = avatarUrl ?: "https://placedog.net/80/80",
            contentDescription = name,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(content, fontSize = 13.sp)
                }
            }
            TextButton(onClick = onReply, modifier = Modifier.height(28.dp)) {
                Text("Trả lời", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CommunityEmptyState(
    onCreatePost: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🐾", fontSize = 64.sp)
        Text(
            text = "Hãy tạo bài đăng đầu tiên",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Chia sẻ khoảnh khắc thú cưng của bạn để kết nối với mọi người.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        GradientButton(
            text = "Tạo bài viết",
            onClick = onCreatePost,
            modifier = Modifier.fillMaxWidth(0.78f)
        )
        TextButton(onClick = onRetry) {
            Text("Làm mới")
        }
    }
}
