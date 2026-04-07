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

@Composable
fun AdminPetsScreen(vm: AdminViewModel) {
    val ctx = LocalContext.current
    val state by vm.uiState.collectAsState()

    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

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

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Tìm theo tên pet/chủng loại/chủ nuôi") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { vm.loadPets(ctx, query = query) }) { Text("Tìm") }
            OutlinedButton(onClick = { vm.loadPets(ctx, hidden = true) }) { Text("Đang ẩn") }
            OutlinedButton(onClick = { vm.loadPets(ctx) }) { Text("Tất cả") }
        }

        if (state.loading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(10.dp))

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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val text = if (pet.hidden) "Đã ẩn khỏi gợi ý" else "Đang hiển thị"
                            val color = if (pet.hidden) Color(0xFFD64550) else Color(0xFF2A9D8F)
                            Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text,
                                    color = color,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Button(onClick = {
                                vm.setPetHidden(ctx, pet.id, !pet.hidden)
                            }) {
                                Text(if (pet.hidden) "Hiện lại" else "Ẩn hồ sơ")
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
}
