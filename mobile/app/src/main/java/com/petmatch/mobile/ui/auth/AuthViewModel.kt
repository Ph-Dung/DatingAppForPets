package com.petmatch.mobile.ui.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmatch.mobile.Constants
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.api.dataStore
import com.petmatch.mobile.data.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    /** hasPetProfile = true → navigate to Swipe, false → navigate to Setup */
    data class Success(val hasPetProfile: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(ctx: Context, email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val res = RetrofitClient.authApi(ctx).login(LoginRequest(email, password))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                ctx.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.TOKEN_KEY)] = body.token
                    body.userId?.let { uid ->
                        prefs[stringPreferencesKey("current_user_id")] = uid.toString()
                    }
                    prefs.remove(stringPreferencesKey(Constants.ADMIN_TOKEN_KEY))
                }
                _authState.value = AuthState.Success(hasPetProfile = body.hasPetProfile)
            } else {
                _authState.value = AuthState.Error(
                    readErrorMessage(res.errorBody()?.string(), "Email hoặc mật khẩu không đúng")
                )
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Lỗi kết nối: ${e.message}")
        }
    }

    private fun readErrorMessage(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(raw)
            json.optString("error", fallback)
        } catch (_: Exception) {
            fallback
        }
    }

    fun register(ctx: Context, fullName: String, email: String, password: String, phone: String?) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val res = RetrofitClient.authApi(ctx).register(
                com.petmatch.mobile.data.model.RegisterRequest(fullName, email, password, phone?.ifBlank { null })
            )
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                ctx.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.TOKEN_KEY)] = body.token
                    body.userId?.let { uid ->
                        prefs[stringPreferencesKey("current_user_id")] = uid.toString()
                    }
                }
                // Tài khoản mới → hasPetProfile luôn false
                _authState.value = AuthState.Success(hasPetProfile = body.hasPetProfile)
            } else {
                val err = res.errorBody()?.string() ?: ""
                _authState.value = AuthState.Error(
                    if (err.contains("email", ignoreCase = true)) "Email đã được sử dụng"
                    else "Đăng ký thất bại. Vui lòng thử lại."
                )
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Lỗi kết nối: ${e.message}")
        }
    }

    fun logout(ctx: Context) = viewModelScope.launch {
        _authState.value = AuthState.Idle
        ctx.dataStore.edit {
            it.remove(stringPreferencesKey(Constants.TOKEN_KEY))
            it.remove(stringPreferencesKey("current_user_id"))
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun checkToken(ctx: Context, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val token = ctx.dataStore.data.map { prefs ->
            prefs[stringPreferencesKey(Constants.TOKEN_KEY)]
        }
        token.collect { t -> onResult(!t.isNullOrBlank()); return@collect }
    }
}
