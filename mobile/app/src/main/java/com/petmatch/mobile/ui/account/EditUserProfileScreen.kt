package com.petmatch.mobile.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.petmatch.mobile.data.model.UserResponse
import com.petmatch.mobile.ui.common.PetMatchTopBar
import com.petmatch.mobile.ui.theme.PrimaryPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserProfileScreen(
    navController: NavController,
    userVm: UserViewModel
) {
    val ctx = LocalContext.current
    val state by userVm.userInfo.collectAsState()
    val updateState by userVm.updateState.collectAsState()
    val user = (state as? UserInfoState.Success)?.user

    var fullName by remember(user) { mutableStateOf(user?.fullName ?: "") }
    var phone    by remember(user) { mutableStateOf(user?.phone ?: "") }
    var address  by remember(user) { mutableStateOf(user?.address ?: "") }

    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackHostState = remember { SnackbarHostState() }

    LaunchedEffect(updateState) {
        when (updateState) {
            "success" -> {
                snackMsg = "✅ Cập nhật thành công!"
                userVm.resetUpdateState()
            }
            null -> {}
            else -> {
                snackMsg = updateState
                userVm.resetUpdateState()
            }
        }
    }

    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackHostState.showSnackbar(it)
            snackMsg = null
            if (it.startsWith("✅")) navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = "Sửa thông tin cá nhân",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Thông tin tài khoản",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PrimaryPink
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ tên") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Số điện thoại") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Địa chỉ") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                minLines = 2
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        snackMsg = "Vui lòng nhập họ tên"
                        return@Button
                    }
                    userVm.updateMyInfo(ctx, fullName.trim(), phone, address)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Lưu thay đổi", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}
