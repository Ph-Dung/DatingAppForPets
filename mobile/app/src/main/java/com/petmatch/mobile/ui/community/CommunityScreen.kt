package com.petmatch.mobile.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.data.model.CommunityPostResponse
import com.petmatch.mobile.ui.common.GradientButton
import com.petmatch.mobile.ui.common.PetMatchTopBar
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.theme.PrimaryPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: NavController, vm: CommunityViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val posts by vm.feed.collectAsState()
    val loading by vm.loading.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadFeed(ctx)
    }

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = "Cộng đồng",
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Search, null, tint = Color.Black)
                    }
                    IconButton(onClick = { /* TODO: Notification */ }) {
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
                        onExplore = { navController.navigate(Routes.MATCH_SWIPE) { launchSingleTop = true } },
                        onCreatePost = { navController.navigate(Routes.POST_ADD) },
                        onRetry = { vm.loadFeed(ctx) }
                    )
                }
            }

            items(posts) { post ->
                CommunityPostItem(post = post, onToggleLike = { vm.toggleLike(ctx, post.id) })
            }
        }
    }
}

@Composable
fun CreatePostBar(onPostClick: () -> Unit, onManageClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e",
            contentDescription = "Avatar",
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
            Text("Bạn đang nghĩ gì?", color = Color.Gray, fontSize = 14.sp)
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
fun CommunityPostItem(post: CommunityPostResponse, onToggleLike: () -> Unit) {
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
                Text(post.location ?: "", color = Color.Gray, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { /* Options */ }) {
                Icon(Icons.Default.MoreVert, null)
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
                    tint = if (post.isLiked) PrimaryPink else Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("${post.likesCount}", fontSize = 13.sp)
            
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("${post.commentsCount}", fontSize = 13.sp)

            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(24.dp))
        }
        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
    }
}

@Composable
private fun CommunityEmptyState(
    onExplore: () -> Unit,
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
            text = "Chia sẻ khoảnh khắc thú cưng của bạn hoặc đi vào khám phá ngay để kết nối với mọi người.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        GradientButton(
            text = "Tạo bài viết",
            onClick = onCreatePost,
            modifier = Modifier.fillMaxWidth(0.78f)
        )
        OutlinedButton(
            onClick = onExplore,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            Text("Đi vào khám phá ngay")
        }
        TextButton(onClick = onRetry) {
            Text("Làm mới")
        }
    }
}
