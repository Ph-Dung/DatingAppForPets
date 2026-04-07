package com.petmatch.mobile.ui.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.model.ChangePasswordRequest
import com.petmatch.mobile.data.model.UpdateUserRequest
import com.petmatch.mobile.data.model.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UserInfoState {
    object Idle : UserInfoState()
    object Loading : UserInfoState()
    data class Success(val user: UserResponse) : UserInfoState()
    data class Error(val msg: String) : UserInfoState()
}

class UserViewModel : ViewModel() {

    private val _userInfo = MutableStateFlow<UserInfoState>(UserInfoState.Idle)
    val userInfo: StateFlow<UserInfoState> = _userInfo

    private val _updateState = MutableStateFlow<String?>(null)
    val updateState: StateFlow<String?> = _updateState

    fun loadMyInfo(ctx: Context) = viewModelScope.launch {
        _userInfo.value = UserInfoState.Loading
        try {
            val res = RetrofitClient.userApi(ctx).getMyInfo()
            if (res.isSuccessful && res.body() != null) {
                _userInfo.value = UserInfoState.Success(res.body()!!)
            } else {
                _userInfo.value = UserInfoState.Error("Không thể tải thông tin")
            }
        } catch (e: Exception) {
            _userInfo.value = UserInfoState.Error("Lỗi kết nối: ${e.message}")
        }
    }

    fun updateMyInfo(ctx: Context, fullName: String, phone: String?, address: String?) = viewModelScope.launch {
        _updateState.value = null
        try {
            val res = RetrofitClient.userApi(ctx).updateMyInfo(
                UpdateUserRequest(fullName, phone?.ifBlank { null }, address?.ifBlank { null })
            )
            if (res.isSuccessful && res.body() != null) {
                _userInfo.value = UserInfoState.Success(res.body()!!)
                _updateState.value = "success"
            } else {
                _updateState.value = "Cập nhật thất bại"
            }
        } catch (e: Exception) {
            _updateState.value = "Lỗi kết nối: ${e.message}"
        }
    }

    fun changePassword(ctx: Context, oldPass: String, newPass: String, onResult: (Boolean, String) -> Unit) =
        viewModelScope.launch {
            try {
                val res = RetrofitClient.userApi(ctx).changePassword(
                    ChangePasswordRequest(oldPass, newPass)
                )
                if (res.isSuccessful) {
                    onResult(true, "Đổi mật khẩu thành công!")
                } else {
                    val body = res.errorBody()?.string() ?: ""
                    val msg = when {
                        body.contains("Mật khẩu cũ", ignoreCase = true) -> "Mật khẩu cũ không đúng"
                        else -> "Đổi mật khẩu thất bại"
                    }
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                onResult(false, "Lỗi kết nối: ${e.message}")
            }
        }

    fun resetUpdateState() { _updateState.value = null }

    fun updateLocation(ctx: Context, lat: Double, lon: Double) = viewModelScope.launch {
        try {
            RetrofitClient.userApi(ctx).updateLocation(
                com.petmatch.mobile.data.model.UpdateLocationRequest(lat, lon)
            )
        } catch (_: Exception) {}
    }
}
