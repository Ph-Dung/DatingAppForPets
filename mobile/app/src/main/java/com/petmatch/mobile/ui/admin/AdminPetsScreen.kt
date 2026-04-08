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
import com.petmatch.mobile.data.model.AdminPetItemResponse

private val petActionButtonColor = Color(0xFFD64550)

@Composable
fun AdminPetsScreen(vm: AdminViewModel) {
    val ctx = LocalContext.current
    val state by vm.uiState.collectAsState()

    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("all") } // "all", "hidden"
    var deleteTarget by remember { mutableStateOf<AdminPetItemResponse?>(null) }
    val listState = rememberLazyListState()

    val currentHiddenFilter = when (activeFilter) {
        "hidden" -> true
        else -> null
    }

    LaunchedEffect(Unit) {
        vm.loadPets(ctx)
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index, state.pets.size, state.petsHasMore) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (state.petsHasMore && lastVisible >= state.pets.size - 3) {
            vm.loadMorePets(ctx)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8FCFF), Color(0xFFF0F6FF))))
            .padding(16.dp)
    ) {
        Text("Quản lý hồ sơ thú cưng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Ẩn/hiện profile vi phạm mà không cần duyệt tạo mới.", color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                label = { Text("Tìm theo tên pet/chủng loại/chủ nuôi") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { vm.loadPets(ctx, query = query, hidden = currentHiddenFilter) },
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
                    vm.loadPets(ctx, query = query)
                },
                modifier = Modifier.weight(1f)
            )
            FilterButton(
                text = "Đang ẩn",
                isActive = activeFilter == "hidden",
                onClick = {
                    activeFilter = "hidden"
                    vm.loadPets(ctx, query = query, hidden = true)
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (state.loading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(10.dp))

        if (!state.loading && state.pets.isEmpty()) {
            val emptyMessage = if (activeFilter == "hidden") {
                "Hiện không có hồ sơ thú cưng nào đang bị ẩn."
            } else {
                "Không có hồ sơ thú cưng phù hợp."
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF5F8FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.pets) { pet ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("${pet.name} (${pet.species})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Chủ: ${pet.ownerName}", style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserActionButton(
                                text = "Chi tiết",
                                onClick = { vm.loadPetDetail(ctx, pet.id) },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { vm.setPetHidden(ctx, pet.id, !pet.hidden) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = petActionButtonColor, contentColor = Color.White)
                            ) {
                                Text(if (pet.hidden) "Hiện lại" else "Ẩn hồ sơ")
                            }
                            Button(
                                onClick = { deleteTarget = pet },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = petActionButtonColor, contentColor = Color.White)
                            ) {
                                Text("Xoá")
                            }
                        }
                    }
                }
            }

            if (state.petsHasMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
            }
        }
    }

    state.petDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { vm.clearPetDetail() },
            confirmButton = {
                TextButton(onClick = { vm.clearPetDetail() }) {
                    Text("Đóng")
                }
            },
            title = { Text("Chi tiết hồ sơ thú cưng") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val pet = detail.pet
                    Text("Tên: ${pet.name}")
                    Text("Chủ: ${pet.ownerName ?: "-"}")
                    Text("Loài / giống: ${pet.species} / ${pet.breed ?: "-"}")
                    Text("Giới tính: ${pet.gender ?: "-"}")
                    Text("Ngày sinh: ${pet.dateOfBirth ?: "-"}")
                    Text("Tuổi: ${pet.age ?: "-"}")
                    Text("Cân nặng: ${pet.weightKg ?: "-"}")
                    Text("Màu lông: ${pet.color ?: "-"}")
                    Text("Kích cỡ: ${pet.size ?: "-"}")
                    Text("Trạng thái sinh sản: ${pet.reproductiveStatus ?: "-"}")
                    Text("Tiêm vaccine: ${if (pet.isVaccinated) "Có" else "Chưa"}")
                    Text("Lần tiêm: ${pet.vaccinationCount}")
                    Text("Tình trạng sức khỏe: ${pet.healthStatus ?: "-"}")
                    Text("Mô tả: ${pet.notes ?: "-"}")
                    Text("Ẩn hiển thị: ${if (pet.isHidden) "Có" else "Không"}")

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

    deleteTarget?.let { pet ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xoá hồ sơ thú cưng") },
            text = { Text("Xoá hồ sơ của ${pet.name}? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deletePet(ctx, pet.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = petActionButtonColor, contentColor = Color.White)
                ) {
                    Text("Xoá")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Hủy") }
            }
        )
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

@Composable
private fun UserActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = petActionButtonColor, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text)
    }
}
