package com.petmatch.mobile.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.petmatch.mobile.ui.common.PetMatchTopBar
import com.petmatch.mobile.ui.theme.PrimaryPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    userVm: UserViewModel
) {
    val ctx = LocalContext.current

    var oldPassword  by remember { mutableStateOf("") }
    var newPassword  by remember { mutableStateOf("") }
    var confirmPass  by remember { mutableStateOf("") }

    var showOld     by remember { mutableStateOf(false) }
    var showNew     by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    val snackHostState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackHostState.showSnackbar(it)
            if (it.startsWith("✅")) navController.popBackStack()
            snackMsg = null
        }
    }

    Scaffold(
        topBar = {
            PetMatchTopBar(
                title = "Đổi mật khẩu",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = androidx.compose.ui.graphics.Color.White)
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
                "Bảo mật tài khoản",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PrimaryPink
            )

            // Old password
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("Mật khẩu hiện tại") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showOld = !showOld }) {
                        Icon(if (showOld) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (showOld) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // New password
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Mật khẩu mới (tối thiểu 6 ký tự)") },
                leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                trailingIcon = {
                    IconButton(onClick = { showNew = !showNew }) {
                        Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                isError = newPassword.isNotBlank() && newPassword.length < 6
            )
            if (newPassword.isNotBlank() && newPassword.length < 6) {
                Text(
                    "Mật khẩu mới phải có ít nhất 6 ký tự",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Confirm password
            OutlinedTextField(
                value = confirmPass,
                onValueChange = { confirmPass = it },
                label = { Text("Xác nhận mật khẩu mới") },
                leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                isError = confirmPass.isNotBlank() && confirmPass != newPassword
            )
            if (confirmPass.isNotBlank() && confirmPass != newPassword) {
                Text(
                    "Mật khẩu xác nhận không khớp",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    when {
                        oldPassword.isBlank() -> { snackMsg = "Vui lòng nhập mật khẩu hiện tại"; return@Button }
                        newPassword.length < 6 -> { snackMsg = "Mật khẩu mới phải có ít nhất 6 ký tự"; return@Button }
                        confirmPass != newPassword -> { snackMsg = "Mật khẩu xác nhận không khớp"; return@Button }
                    }
                    isLoading = true
                    userVm.changePassword(ctx, oldPassword, newPassword) { ok, msg ->
                        isLoading = false
                        snackMsg = if (ok) "✅ $msg" else msg
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Security, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Đổi mật khẩu", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}
