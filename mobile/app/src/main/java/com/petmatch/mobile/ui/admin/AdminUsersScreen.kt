package com.petmatch.mobile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private val userActionButtonColor = Color(0xFFD64550)

@Composable
fun AdminUsersScreen(vm: AdminViewModel) {
    val ctx = LocalContext.current
    val state by vm.uiState.collectAsState()

    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("all") } // "all", "locked", "warned"
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        vm.loadUsers(ctx)
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index, state.users.size, state.usersHasMore) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (state.usersHasMore && lastVisible >= state.users.size - 3) {
            vm.loadMoreUsers(ctx)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF6FBFF), Color(0xFFEDF4FF))))
            .padding(16.dp)
    ) {
        Text("Quản lý người dùng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Tìm kiếm, khóa hoặc mở khóa tài khoản nhanh.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(10.dp))

        // Search bar with search button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Tìm theo tên/email") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { vm.loadUsers(ctx, query = query) },
                modifier = Modifier.height(56.dp)
            ) {
                Text("Tìm")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Filter buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterButton(
                text = "Tất cả",
                isActive = activeFilter == "all",
                onClick = {
                    activeFilter = "all"
                    vm.loadUsers(ctx)
                },
                modifier = Modifier.weight(1f)
            )
            FilterButton(
                text = "Đang khóa",
                isActive = activeFilter == "locked",
                onClick = {
                    activeFilter = "locked"
                    vm.loadUsers(ctx, locked = true)
                },
                modifier = Modifier.weight(1f)
            )
            FilterButton(
                text = "Bị cảnh cáo",
                isActive = activeFilter == "warned",
                onClick = {
                    activeFilter = "warned"
                    vm.loadUsers(ctx, warned = true)
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (state.loading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.users) { user ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(user.email, style = MaterialTheme.typography.bodyMedium)
                        if (!user.phone.isNullOrBlank()) {
                            Text("SĐT: ${user.phone}", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeColor = if (user.locked) Color(0xFFD64550) else Color(0xFF2A9D8F)
                            Surface(color = badgeColor.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    if (user.locked) "Đang bị khóa" else "Đang hoạt động",
                                    color = badgeColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        if (user.warned) {
                            Text(
                                "Cảnh cáo: ${user.warningCount}",
                                color = Color(0xFFC77D00),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserActionButton(
                                text = "Chi tiết",
                                onClick = { vm.loadUserDetail(ctx, user.id) },
                                modifier = Modifier.weight(1f)
                            )

                            UserActionButton(
                                text = "Cảnh cáo",
                                onClick = { vm.warnUser(ctx, user.id, "Cảnh cáo mức nhẹ bởi admin") },
                                modifier = Modifier.weight(1f)
                            )

                            UserActionButton(
                                text = if (user.locked) "Mở khóa" else "Khóa",
                                onClick = { vm.setUserLocked(ctx, user.id, !user.locked) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (state.usersHasMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
            }
        }
    }

    state.userDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { vm.clearUserDetail() },
            confirmButton = {
                TextButton(onClick = { vm.clearUserDetail() }) {
                    Text("Đóng")
                }
            },
            title = { Text("Chi tiết user") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val user = detail.user
                    Text("Họ tên: ${user.fullName}")
                    Text("Email: ${user.email}")
                    Text("SĐT: ${user.phone ?: "-"}")
                    Text("Trạng thái: ${if (user.locked) "Đang bị khóa" else "Đang hoạt động"}")
                    Text("Cảnh cáo: ${user.warningCount}")
                    Text("Lần cảnh cáo gần nhất: ${user.lastWarnedAt ?: "-"}")
                    Text("Ngày tạo: ${user.createdAt ?: "-"}")

                    Spacer(Modifier.height(6.dp))
                    Text("Lịch sử vi phạm", fontWeight = FontWeight.Bold)
                    if (detail.violations.isEmpty()) {
                        Text("Chưa có vi phạm nào.")
                    } else {
                        detail.violations.forEach { violation ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF5F8FF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("#${violation.id} • ${violation.targetType}(${violation.targetId})", fontWeight = FontWeight.SemiBold)
                                    Text(violation.reason)
                                    Text("Trạng thái: ${violation.status}")
                                    Text("Thời gian: ${violation.createdAt ?: "-"}")
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun UserActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = userActionButtonColor, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun FilterButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) Color(0xFF1D3557) else Color(0xFF1D3557).copy(alpha = 0.2f)
    val contentColor = if (isActive) Color.White else Color(0xFF1D3557)
    
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text)
    }
}
