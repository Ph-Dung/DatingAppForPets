package com.petmatch.mobile.ui.admin

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmatch.mobile.Constants
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.api.dataStore
import com.petmatch.mobile.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class AdminAuthState {
    object Idle : AdminAuthState()
    object Loading : AdminAuthState()
    object Success : AdminAuthState()
    data class Error(val message: String) : AdminAuthState()
}

data class AdminUiState(
    val loading: Boolean = false,
    val detailLoading: Boolean = false,
    val dashboard: AdminDashboardResponse? = null,
    val users: List<AdminUserItemResponse> = emptyList(),
    val usersHasMore: Boolean = true,
    val pets: List<AdminPetItemResponse> = emptyList(),
    val petsHasMore: Boolean = true,
    val reports: List<AdminReportItemResponse> = emptyList(),
    val userDetail: AdminUserDetailResponse? = null,
    val petDetail: AdminPetDetailResponse? = null,
    val adminProfile: UserResponse? = null,
    val error: String? = null
)

class AdminViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AdminAuthState>(AdminAuthState.Idle)
    val authState: StateFlow<AdminAuthState> = _authState

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    private var userPage = 0
    private var userQuery: String? = null
    private var userLocked: Boolean? = null
    private var userWarned: Boolean? = null
    private var userLoadingMore = false

    private var petPage = 0
    private var petQuery: String? = null
    private var petHidden: Boolean? = null
    private var petLoadingMore = false

    fun loginAdmin(ctx: Context, email: String, password: String) = viewModelScope.launch {
        _authState.value = AdminAuthState.Loading
        try {
            val res = RetrofitClient.adminAuthApi(ctx).adminLogin(LoginRequest(email, password))
            if (res.isSuccessful && res.body() != null) {
                ctx.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.ADMIN_TOKEN_KEY)] = res.body()!!.token
                }
                _authState.value = AdminAuthState.Success
            } else {
                _authState.value = AdminAuthState.Error(readErrorMessage(res.errorBody()?.string(), "Tài khoản không có quyền admin hoặc sai mật khẩu"))
            }
        } catch (e: Exception) {
            _authState.value = AdminAuthState.Error("Lỗi kết nối: ${e.message}")
        }
    }

    fun logoutAdmin(ctx: Context) = viewModelScope.launch {
        ctx.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(Constants.ADMIN_TOKEN_KEY))
        }
        _authState.value = AdminAuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AdminAuthState.Idle
    }

    private fun readErrorMessage(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(raw)
            json.optString("error").ifBlank { fallback }
        } catch (_: Exception) {
            raw.ifBlank { fallback }
        }
    }

    fun loadDashboard(ctx: Context) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        try {
            val res = RetrofitClient.adminApi(ctx).getDashboard()
            if (res.isSuccessful && res.body() != null) {
                _uiState.value = _uiState.value.copy(loading = false, dashboard = res.body())
            } else {
                _uiState.value = _uiState.value.copy(loading = false, error = "Không tải được dashboard")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(loading = false, error = "Lỗi: ${e.message}")
        }
    }

    fun loadUsers(ctx: Context, query: String? = null, locked: Boolean? = null, warned: Boolean? = null) = viewModelScope.launch {
        userPage = 0
        userQuery = query?.ifBlank { null }
        userLocked = locked
        userWarned = warned
        userLoadingMore = false

        _uiState.value = _uiState.value.copy(loading = true, users = emptyList(), usersHasMore = true, error = null)
        try {
            val res = RetrofitClient.adminApi(ctx).getUsers(query = userQuery, locked = userLocked, warned = userWarned, page = 0, size = 20)
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    users = body.content,
                    usersHasMore = !body.last,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(loading = false, error = "Không tải được danh sách user")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(loading = false, error = "Lỗi: ${e.message}")
        }
    }

    fun loadMoreUsers(ctx: Context) = viewModelScope.launch {
        if (userLoadingMore || !_uiState.value.usersHasMore) return@launch
        userLoadingMore = true
        try {
            val nextPage = userPage + 1
            val res = RetrofitClient.adminApi(ctx).getUsers(query = userQuery, locked = userLocked, warned = userWarned, page = nextPage, size = 20)
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                userPage = nextPage
                _uiState.value = _uiState.value.copy(
                    users = _uiState.value.users + body.content,
                    usersHasMore = !body.last
                )
            }
        } catch (_: Exception) {
        } finally {
            userLoadingMore = false
        }
    }

    fun loadPets(ctx: Context, query: String? = null, hidden: Boolean? = null) = viewModelScope.launch {
        petPage = 0
        petQuery = query?.ifBlank { null }
        petHidden = hidden
        petLoadingMore = false

        _uiState.value = _uiState.value.copy(loading = true, pets = emptyList(), petsHasMore = true, error = null)
        try {
            val res = RetrofitClient.adminApi(ctx).getPets(query = petQuery, hidden = petHidden, page = 0, size = 20)
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    pets = body.content,
                    petsHasMore = !body.last,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(loading = false, error = "Không tải được hồ sơ thú cưng")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(loading = false, error = "Lỗi: ${e.message}")
        }
    }

    fun loadUserDetail(ctx: Context, userId: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(detailLoading = true, error = null, userDetail = null)
        try {
            val res = RetrofitClient.adminApi(ctx).getUserDetail(userId)
            if (res.isSuccessful && res.body() != null) {
                _uiState.value = _uiState.value.copy(detailLoading = false, userDetail = res.body())
            } else {
                _uiState.value = _uiState.value.copy(detailLoading = false, error = "Không tải được chi tiết user")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(detailLoading = false, error = "Lỗi: ${e.message}")
        }
    }

    fun loadPetDetail(ctx: Context, petId: Long) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(detailLoading = true, error = null, petDetail = null)
        try {
            val res = RetrofitClient.adminApi(ctx).getPetDetail(petId)
            if (res.isSuccessful && res.body() != null) {
                _uiState.value = _uiState.value.copy(detailLoading = false, petDetail = res.body())
            } else {
                _uiState.value = _uiState.value.copy(detailLoading = false, error = "Không tải được chi tiết hồ sơ thú cưng")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(detailLoading = false, error = "Lỗi: ${e.message}")
        }
    }

    fun loadMorePets(ctx: Context) = viewModelScope.launch {
        if (petLoadingMore || !_uiState.value.petsHasMore) return@launch
        petLoadingMore = true
        try {
            val nextPage = petPage + 1
            val res = RetrofitClient.adminApi(ctx).getPets(query = petQuery, hidden = petHidden, page = nextPage, size = 20)
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                petPage = nextPage
                _uiState.value = _uiState.value.copy(
                    pets = _uiState.value.pets + body.content,
                    petsHasMore = !body.last
                )
            }
        } catch (_: Exception) {
        } finally {
            petLoadingMore = false
        }
    }

    fun loadReports(ctx: Context, status: String = "PENDING") = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        try {
            val res = RetrofitClient.adminApi(ctx).getReports(status = status)
            if (res.isSuccessful && res.body() != null) {
                _uiState.value = _uiState.value.copy(loading = false, reports = res.body()!!.content)
            } else {
                _uiState.value = _uiState.value.copy(loading = false, error = "Không tải được báo cáo")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(loading = false, error = "Lỗi: ${e.message}")
        }
    }

    fun setUserLocked(ctx: Context, userId: Long, locked: Boolean, onDone: (() -> Unit)? = null) = viewModelScope.launch {
        try {
            RetrofitClient.adminApi(ctx).setUserLocked(userId, locked)
            loadUsers(ctx, userQuery, userLocked, userWarned)
            onDone?.invoke()
        } catch (_: Exception) {
        }
    }

    fun warnUser(ctx: Context, userId: Long, note: String? = null, onDone: (() -> Unit)? = null) = viewModelScope.launch {
        try {
            RetrofitClient.adminApi(ctx).warnUser(userId, note)
            loadUsers(ctx, userQuery, userLocked, userWarned)
            onDone?.invoke()
        } catch (_: Exception) {
        }
    }

    fun setPetHidden(ctx: Context, petId: Long, hidden: Boolean, onDone: (() -> Unit)? = null) = viewModelScope.launch {
        try {
            RetrofitClient.adminApi(ctx).setPetHidden(petId, hidden)
            loadPets(ctx, petQuery, petHidden)
            onDone?.invoke()
        } catch (_: Exception) {
        }
    }

    fun deletePet(ctx: Context, petId: Long, onDone: (() -> Unit)? = null) = viewModelScope.launch {
        try {
            RetrofitClient.adminApi(ctx).deletePet(petId)
            loadPets(ctx, petQuery, petHidden)
            loadDashboard(ctx)
            _uiState.value = _uiState.value.copy(petDetail = null)
            onDone?.invoke()
        } catch (_: Exception) {
        }
    }

    fun handleReport(
        ctx: Context,
        reportId: Long,
        action: String,
        note: String? = null,
        onDone: (() -> Unit)? = null
    ) = viewModelScope.launch {
        try {
            RetrofitClient.adminApi(ctx).handleReport(reportId, AdminHandleReportRequest(action, note))
            loadReports(ctx)
            loadDashboard(ctx)
            loadPets(ctx, petQuery, petHidden)
            loadUsers(ctx, userQuery, userLocked, userWarned)
            onDone?.invoke()
        } catch (_: Exception) {
        }
    }

    fun clearUserDetail() {
        _uiState.value = _uiState.value.copy(userDetail = null)
    }

    fun clearPetDetail() {
        _uiState.value = _uiState.value.copy(petDetail = null)
    }

    fun loadAdminProfile(ctx: Context) = viewModelScope.launch {
        try {
            val res = RetrofitClient.adminUserApi(ctx).getMyInfo()
            if (res.isSuccessful && res.body() != null) {
                _uiState.value = _uiState.value.copy(adminProfile = res.body())
            }
        } catch (_: Exception) {
        }
    }

    fun updateAdminProfile(ctx: Context, fullName: String, phone: String?, address: String?, onDone: (() -> Unit)? = null) = viewModelScope.launch {
        try {
            val res = RetrofitClient.adminUserApi(ctx)
                .updateMyInfo(UpdateUserRequest(fullName, phone?.ifBlank { null }, address?.ifBlank { null }))
            if (res.isSuccessful && res.body() != null) {
                _uiState.value = _uiState.value.copy(adminProfile = res.body())
                onDone?.invoke()
            }
        } catch (_: Exception) {
        }
    }
}
