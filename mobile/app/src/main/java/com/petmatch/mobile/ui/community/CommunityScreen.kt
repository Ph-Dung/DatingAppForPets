package com.petmatch.mobile.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.data.model.CommunityPostResponse
import com.petmatch.mobile.ui.account.UserInfoState
import com.petmatch.mobile.ui.account.UserViewModel
import com.petmatch.mobile.ui.common.GradientButton
import com.petmatch.mobile.ui.common.PetMatchTopBar
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.petprofile.PetProfileViewModel
import com.petmatch.mobile.ui.theme.PrimaryPink
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    val userVm: UserViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val userInfo by userVm.userInfo.collectAsState()
    val currentUser = (userInfo as? UserInfoState.Success)?.user

    var commentPostId by remember { mutableStateOf<Long?>(null) }
    var replyToCommentId by remember { mutableStateOf<Long?>(null) }
    var replyToUserName by remember { mutableStateOf<String?>(null) }
    var showReportDialogForPostId by remember { mutableStateOf<Long?>(null) }
    var selectedReportReason by remember { mutableStateOf<String?>(null) }
    var customReportReason by remember { mutableStateOf("") }
    var showReportConfirmDialog by remember { mutableStateOf(false) }
    var commentInput by remember { mutableStateOf("") }
    val commentInputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(replyToCommentId) {
        if (replyToCommentId != null) {
            commentInputFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(Unit) {
        vm.loadFeed(ctx)
        petVm.loadMyProfile(ctx)
        userVm.loadMyInfo(ctx)
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
                    userAvatarUrl = currentUser?.avatarUrl,
                    userName = currentUser?.fullName,
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
                    isOwner = currentUser?.id == post.ownerId,
                    currentUserAvatarUrl = currentUser?.avatarUrl,
                    onToggleLike = { vm.toggleLike(ctx, post.id) },
                    onOpenComments = {
                        commentPostId = post.id
                        replyToCommentId = null
                        replyToUserName = null
                        commentInput = ""
                        vm.loadComments(ctx, post.id)
                    },
                    onEditPost = {
                        navController.navigate(Routes.postAdd(post.id))
                    },
                    onReportPost = {
                        showReportDialogForPostId = post.id
                        selectedReportReason = null
                        customReportReason = ""
                        showReportConfirmDialog = false
                    }
                )
            }
        }
    }

    if (commentPostId != null) {
        val commentsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = commentsSheetState,
            onDismissRequest = {
                commentPostId = null
                replyToCommentId = null
                replyToUserName = null
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Bình luận",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (commentsLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)
                ) {
                    if (!commentsLoading && comments.isEmpty()) {
                        item {
                            Text("Chưa có bình luận nào", color = Color.Gray)
                        }
                    }
                    items(comments, key = { it.id }) { c ->
                        CommentItem(
                            name = c.userName,
                            avatarUrl = c.userAvatar,
                            content = c.content,
                            createdAt = c.createdAt,
                            depth = 0,
                            canReply = true,
                            onReply = {
                                replyToCommentId = c.id
                                replyToUserName = c.userName
                                if (commentInput.isBlank()) {
                                    commentInput = "@${c.userName} "
                                }
                            }
                        )
                        c.replies.forEach { r ->
                            CommentItem(
                                name = r.userName,
                                avatarUrl = r.userAvatar,
                                content = r.content,
                                createdAt = r.createdAt,
                                depth = 1,
                                canReply = false,
                                onReply = null
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp)
                    ) {
                        if (replyToCommentId != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Đang trả lời @${replyToUserName ?: "user"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280)
                                )
                                TextButton(onClick = {
                                    replyToCommentId = null
                                    replyToUserName = null
                                }) {
                                    Text("Hủy")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(commentInputFocusRequester)
                                .heightIn(min = 90.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFFD1D5DB),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            ),
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
                                replyToUserName = null
                            }) {
                                Text("Đóng")
                            }
                            TextButton(
                                enabled = commentInput.isNotBlank() && !actionLoading,
                                onClick = {
                                    val postId = commentPostId
                                    if (postId != null) {
                                        val payload = commentInput.trim()
                                        val replyingId = replyToCommentId
                                        if (replyingId != null) {
                                            val replyTag = replyToUserName?.let { "@$it" }
                                            val replyContent = if (!replyTag.isNullOrBlank() && !payload.startsWith(replyTag)) {
                                                "$replyTag $payload"
                                            } else {
                                                payload
                                            }
                                            vm.replyComment(ctx, postId, replyingId, replyContent) {
                                                commentInput = ""
                                                replyToCommentId = null
                                                replyToUserName = null
                                            }
                                        } else {
                                            vm.addComment(ctx, postId, payload) {
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
        }
    }

    if (showReportDialogForPostId != null && !showReportConfirmDialog) {
        val reportReasons = listOf(
            "Không phải bài đăng liên quan đến thú cưng hoặc động vật",
            "Có hình ảnh nhạy cảm, bạo lực, gây hại đến thú cưng và động vật",
            "Liên quan đến các hành vi trái pháp luật và đạo đức",
            "Tôi không thích bài đăng này vì lý do xuất phát từ cá nhân"
        )
        val finalReason = selectedReportReason?.ifBlank { null }
            ?: customReportReason.trim().ifBlank { null }

        AlertDialog(
            onDismissRequest = { showReportDialogForPostId = null },
            title = {
                Text(
                    text = "Báo cáo bài viết",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reportReasons.forEach { reason ->
                        val selected = selectedReportReason == reason
                        Button(
                            onClick = {
                                selectedReportReason = reason
                                customReportReason = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Color(0xFFEE3F5A) else Color(0xFFE89AA8)
                            )
                        ) {
                            Text(reason, color = Color.White)
                        }
                    }

                    OutlinedTextField(
                        value = customReportReason,
                        onValueChange = {
                            customReportReason = it
                            if (it.isNotBlank()) selectedReportReason = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    selectedReportReason = null
                                }
                            },
                        label = { Text("Lý do khác") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = finalReason != null,
                    onClick = {
                        showReportConfirmDialog = true
                    }
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialogForPostId = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showReportDialogForPostId != null && showReportConfirmDialog) {
        val finalReason = selectedReportReason?.ifBlank { null }
            ?: customReportReason.trim().ifBlank { null }
        val postId = showReportDialogForPostId

        Dialog(onDismissRequest = { showReportConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Xác nhận báo cáo",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Lý do báo cáo: ${finalReason ?: "(trống)"}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showReportConfirmDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6))
                        ) {
                            Text("Hủy", color = Color(0xFF374151), maxLines = 1)
                        }

                        Button(
                            enabled = postId != null && finalReason != null && !actionLoading,
                            onClick = {
                                if (postId != null && finalReason != null) {
                                    vm.submitReport(ctx, postId, finalReason, hidePost = false) {
                                        showReportConfirmDialog = false
                                        showReportDialogForPostId = null
                                        selectedReportReason = null
                                        customReportReason = ""
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE3F5A))
                        ) {
                            Text(if (actionLoading) "Đang gửi..." else "Báo cáo", color = Color.White, maxLines = 1)
                        }

                        Button(
                            enabled = postId != null && finalReason != null && !actionLoading,
                            onClick = {
                                if (postId != null && finalReason != null) {
                                    vm.submitReport(ctx, postId, finalReason, hidePost = true) {
                                        showReportConfirmDialog = false
                                        showReportDialogForPostId = null
                                        selectedReportReason = null
                                        customReportReason = ""
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD12F49))
                        ) {
                            Text("Báo cáo và chặn", color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePostBar(
    userAvatarUrl: String?,
    userName: String?,
    onPostClick: () -> Unit,
    onManageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!userAvatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = userAvatarUrl,
                contentDescription = userName ?: "Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE4E6EB)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    Icons.Default.Person,
                    null,
                    tint = Color(0xFF8A8D91),
                    modifier = Modifier.size(30.dp).offset(y = 4.dp)
                )
            }
        }
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
            Text("Bạn đang nghĩ gì, ${userName ?: "người dùng"}?", color = Color.Gray, fontSize = 14.sp)
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
    isOwner: Boolean,
    currentUserAvatarUrl: String?,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onEditPost: () -> Unit,
    onReportPost: () -> Unit,
    onDeletePost: (() -> Unit)? = null
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val imageUrls = remember(post.imageUrl) { parsePostImageUrls(post.imageUrl) }
    val displayAvatar = post.ownerAvatar?.takeIf { it.isNotBlank() }
        ?: if (isOwner) currentUserAvatarUrl?.takeIf { it.isNotBlank() } else null

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!displayAvatar.isNullOrBlank()) {
                AsyncImage(
                    model = displayAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4E6EB)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = Color(0xFF8A8D91),
                        modifier = Modifier.size(30.dp).offset(y = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(post.ownerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!post.location.isNullOrBlank()) {
                        Text(post.location, color = Color.Gray, fontSize = 11.sp)
                        Text(", ", color = Color.Gray, fontSize = 11.sp)
                    }
                    Text(getRelativeTimeString(post.createdAt), color = Color.Gray, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Box {
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(Icons.Default.MoreVert, null)
            }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    if (isOwner) {
                        DropdownMenuItem(
                            text = { Text("Chỉnh sửa bài viết") },
                            onClick = {
                                showMoreMenu = false
                                onEditPost()
                            }
                        )
                        if (onDeletePost != null) {
                            DropdownMenuItem(
                                text = { Text("Xóa bài viết") },
                                onClick = {
                                    showMoreMenu = false
                                    onDeletePost()
                                }
                            )
                        }
                    } else {
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
        }

        Text(
            post.content,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            fontSize = 14.sp
        )

        if (imageUrls.isNotEmpty()) {
            PostImageCarousel(imageUrls = imageUrls)
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
private fun PostImageCarousel(imageUrls: List<String>) {
    var currentIndex by remember(imageUrls) { mutableIntStateOf(0) }
    var showFullScreen by remember(imageUrls) { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFECECEC), RoundedCornerShape(10.dp))
            .pointerInput(currentIndex, imageUrls.size) {
                detectHorizontalDragGestures(
                    onDragStart = { dragOffsetX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        dragOffsetX += dragAmount
                    },
                    onDragEnd = {
                        if (dragOffsetX < -80f && currentIndex < imageUrls.lastIndex) {
                            currentIndex += 1
                        } else if (dragOffsetX > 80f && currentIndex > 0) {
                            currentIndex -= 1
                        }
                        dragOffsetX = 0f
                    }
                )
            }
    ) {
        AsyncImage(
            model = imageUrls[currentIndex],
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showFullScreen = true },
            contentScale = ContentScale.Crop
        )

        if (currentIndex > 0) {
            IconButton(
                onClick = { currentIndex -= 1 },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Ảnh trước", tint = Color.White)
            }
        }

        if (currentIndex < imageUrls.lastIndex) {
            IconButton(
                onClick = { currentIndex += 1 },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Ảnh tiếp", tint = Color.White)
            }
        }

        if (imageUrls.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${currentIndex + 1}/${imageUrls.size}",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }

    if (showFullScreen) {
        FullScreenImageViewer(
            imageUrls = imageUrls,
            startIndex = currentIndex,
            onDismiss = { showFullScreen = false }
        )
    }
}

@Composable
private fun FullScreenImageViewer(
    imageUrls: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    var currentIndex by remember(startIndex, imageUrls) { mutableIntStateOf(startIndex.coerceIn(0, imageUrls.lastIndex)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrls[currentIndex],
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { },
                contentScale = ContentScale.Fit
            )

            if (currentIndex > 0) {
                IconButton(
                    onClick = { currentIndex -= 1 },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Ảnh trước", tint = Color.White)
                }
            }

            if (currentIndex < imageUrls.lastIndex) {
                IconButton(
                    onClick = { currentIndex += 1 },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Ảnh tiếp", tint = Color.White)
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
            }
        }
    }
}

@Composable
private fun CommentItem(
    name: String,
    avatarUrl: String?,
    content: String,
    createdAt: String?,
    depth: Int,
    canReply: Boolean,
    onReply: (() -> Unit)?
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
                    val timeTag = getRelativeTimeString(createdAt)
                    if (timeTag.isNotBlank() || canReply) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (timeTag.isNotBlank()) {
                                Text(timeTag, fontSize = 11.sp, color = Color.Gray)
                            }
                            if (canReply && onReply != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Trả lời",
                                    color = Color(0xFF2563EB),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { onReply() }
                                )
                            }
                        }
                    }
                }
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

private fun getRelativeTimeString(createdAtStr: String?): String {
    if (createdAtStr.isNullOrBlank()) return ""
    
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale("vi", "VN"))
        val createdTime = sdf.parse(createdAtStr) ?: return ""
        val now = Calendar.getInstance().time
        val diffMs = now.time - createdTime.time
        
        when {
            diffMs < 5 * 60 * 1000 -> "Mới"
            diffMs < 60 * 60 * 1000 -> {
                val minutes = (diffMs / (60 * 1000)).toInt()
                "$minutes phút trước"
            }
            diffMs < 24 * 60 * 60 * 1000 -> {
                val hours = (diffMs / (60 * 60 * 1000)).toInt()
                "$hours giờ trước"
            }
            else -> {
                val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
                dateSdf.format(createdTime)
            }
        }
    } catch (e: Exception) {
        ""
    }
}

private fun parsePostImageUrls(imageUrl: String?): List<String> {
    if (imageUrl.isNullOrBlank()) return emptyList()

    val normalized = imageUrl.trim()
    return if (normalized.startsWith("[") && normalized.endsWith("]")) {
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
}
