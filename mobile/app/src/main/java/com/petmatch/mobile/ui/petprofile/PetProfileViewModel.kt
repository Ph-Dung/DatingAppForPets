package com.petmatch.mobile.ui.petprofile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PetUiState {
    object Idle : PetUiState()
    object Loading : PetUiState()
    data class Success(val pet: PetProfileResponse) : PetUiState()
    data class Error(val message: String) : PetUiState()
}

sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    object Success : ActionState()
    data class Error(val message: String) : ActionState()
}

class PetProfileViewModel : ViewModel() {

    private val _myPet = MutableStateFlow<PetUiState>(PetUiState.Idle)
    val myPet: StateFlow<PetUiState> = _myPet

    private val _viewedPet = MutableStateFlow<PetUiState>(PetUiState.Idle)
    val viewedPet: StateFlow<PetUiState> = _viewedPet

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState

    private val _vaccinations = MutableStateFlow<List<VaccinationResponse>>(emptyList())
    val vaccinations: StateFlow<List<VaccinationResponse>> = _vaccinations

    fun loadMyProfile(ctx: Context) = viewModelScope.launch {
        _myPet.value = PetUiState.Loading
        try {
            val res = RetrofitClient.petApi(ctx).getMyProfile()
            if (res.isSuccessful && res.body() != null)
                _myPet.value = PetUiState.Success(res.body()!!)
            else
                _myPet.value = PetUiState.Error(res.errorBody()?.string() ?: "Lỗi tải profile")
        } catch (e: Exception) {
            _myPet.value = PetUiState.Error(e.message ?: "Lỗi kết nối")
        }
    }

    fun loadPetById(ctx: Context, petId: Long) = viewModelScope.launch {
        _viewedPet.value = PetUiState.Loading
        try {
            val res = RetrofitClient.petApi(ctx).getPetById(petId)
            if (res.isSuccessful && res.body() != null)
                _viewedPet.value = PetUiState.Success(res.body()!!)
            else
                _viewedPet.value = PetUiState.Error("Không tìm thấy hồ sơ")
        } catch (e: Exception) {
            _viewedPet.value = PetUiState.Error(e.message ?: "Lỗi kết nối")
        }
    }

    fun createProfile(ctx: Context, req: PetProfileRequest, onDone: () -> Unit) = viewModelScope.launch {
        _actionState.value = ActionState.Loading
        try {
            val res = RetrofitClient.petApi(ctx).createProfile(req)
            if (res.isSuccessful) {
                _actionState.value = ActionState.Success
                onDone()
            } else {
                _actionState.value = ActionState.Error(res.errorBody()?.string() ?: "Lỗi tạo profile")
            }
        } catch (e: Exception) {
            _actionState.value = ActionState.Error(e.message ?: "Lỗi kết nối")
        }
    }

    fun updateProfile(ctx: Context, req: PetProfileRequest, onDone: () -> Unit) = viewModelScope.launch {
        _actionState.value = ActionState.Loading
        try {
            val res = RetrofitClient.petApi(ctx).updateProfile(req)
            if (res.isSuccessful) {
                _actionState.value = ActionState.Success
                onDone()
            } else {
                _actionState.value = ActionState.Error(res.errorBody()?.string() ?: "Lỗi cập nhật")
            }
        } catch (e: Exception) {
            _actionState.value = ActionState.Error(e.message ?: "Lỗi kết nối")
        }
    }

    fun toggleHidden(ctx: Context) = viewModelScope.launch {
        try { RetrofitClient.petApi(ctx).toggleHidden() } catch (_: Exception) {}
        loadMyProfile(ctx)
    }

    // ── Vaccinations ─────────────────────────────────────
    fun loadVaccinations(ctx: Context) = viewModelScope.launch {
        try {
            val res = RetrofitClient.petApi(ctx).getVaccinations()
            if (res.isSuccessful) _vaccinations.value = res.body() ?: emptyList()
        } catch (_: Exception) {}
    }

    fun addVaccination(ctx: Context, req: VaccinationRequest, onDone: () -> Unit) = viewModelScope.launch {
        _actionState.value = ActionState.Loading
        try {
            val res = RetrofitClient.petApi(ctx).addVaccination(req)
            if (res.isSuccessful) {
                _actionState.value = ActionState.Success
                loadVaccinations(ctx)
                onDone()
            } else _actionState.value = ActionState.Error("Lỗi thêm vaccine")
        } catch (e: Exception) {
            _actionState.value = ActionState.Error(e.message ?: "Lỗi kết nối")
        }
    }

    fun updateVaccination(ctx: Context, vacId: Long, req: VaccinationRequest, onDone: () -> Unit) = viewModelScope.launch {
        _actionState.value = ActionState.Loading
        try {
            val res = RetrofitClient.petApi(ctx).updateVaccination(vacId, req)
            if (res.isSuccessful) {
                _actionState.value = ActionState.Success
                loadVaccinations(ctx)
                onDone()
            } else _actionState.value = ActionState.Error("Lỗi cập nhật vaccine")
        } catch (e: Exception) {
            _actionState.value = ActionState.Error(e.message ?: "Lỗi kết nối")
        }
    }

    fun deleteVaccination(ctx: Context, vacId: Long) = viewModelScope.launch {
        try {
            RetrofitClient.petApi(ctx).deleteVaccination(vacId)
            loadVaccinations(ctx)
        } catch (_: Exception) {}
    }

    fun deletePhoto(ctx: Context, photoId: Long) = viewModelScope.launch {
        try {
            RetrofitClient.petApi(ctx).deletePhoto(photoId)
            loadMyProfile(ctx)
        } catch (_: Exception) {}
    }

    fun resetAction() { _actionState.value = ActionState.Idle }
}
