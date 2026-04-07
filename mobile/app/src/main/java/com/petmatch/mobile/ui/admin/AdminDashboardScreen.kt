package com.petmatch.mobile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class AdminCardItem(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun AdminDashboardScreen(
    vm: AdminViewModel,
    onOpenUsers: () -> Unit,
    onOpenPets: () -> Unit,
    onOpenReports: () -> Unit
) {
    val ctx = LocalContext.current
    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadDashboard(ctx)
    }

    val dashboard = state.dashboard

    val cards = listOf(
        AdminCardItem("Người dùng", (dashboard?.totalUsers ?: 0L).toString(), "Tổng tài khoản người dùng", Icons.Default.SupervisedUserCircle, Color(0xFF2A9D8F), onOpenUsers),
        AdminCardItem("Tài khoản khóa", (dashboard?.lockedUsers ?: 0L).toString(), "Đã bị ban/khóa", Icons.Default.Shield, Color(0xFFD64550), onOpenUsers),
        AdminCardItem("Hồ sơ thú cưng", (dashboard?.totalPets ?: 0L).toString(), "Đang tồn tại", Icons.Default.Pets, Color(0xFF3F72AF), onOpenPets),
        AdminCardItem("Báo cáo chờ xử lý", (dashboard?.pendingReports ?: 0L).toString(), "Cần quản trị viên xử lý", Icons.Default.Report, Color(0xFFF4A261), onOpenReports)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF6FBFF), Color(0xFFEEF5FF))))
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102542)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bảng điều khiển admin", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Trung tâm kiểm duyệt PetMatch", color = Color.White.copy(alpha = 0.82f))
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = Color(0x1AFFFFFF),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Báo cáo chờ xử lý: ${dashboard?.pendingReports ?: 0}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color(0xFFFFD166),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (state.loading && dashboard == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cards) { card ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable(onClick = card.onClick),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(card.icon, contentDescription = null, tint = card.color)
                        Column {
                            Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(card.value, style = MaterialTheme.typography.headlineSmall, color = card.color, fontWeight = FontWeight.ExtraBold)
                            Text(card.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onOpenUsers,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F), contentColor = Color.White)
            ) { Text("Người dùng") }
            Button(
                onClick = onOpenPets,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F72AF), contentColor = Color.White)
            ) { Text("Thú cưng") }
            Button(
                onClick = onOpenReports,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4A261), contentColor = Color.Black)
            ) { Text("Báo cáo") }
        }

        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
