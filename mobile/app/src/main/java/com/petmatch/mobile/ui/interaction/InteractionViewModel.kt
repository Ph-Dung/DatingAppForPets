package com.petmatch.mobile.ui.interaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.model.ReportRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InteractionViewModel : ViewModel() {

    private val _actionDone = MutableStateFlow(false)
    val actionDone: StateFlow<Boolean> = _actionDone

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun blockUser(ctx: Context, targetUserId: Long, onDone: () -> Unit) = viewModelScope.launch {
        _loading.value = true
        try {
            val res = RetrofitClient.interactionApi(ctx).blockUser(targetUserId)
            if (res.isSuccessful) { _actionDone.value = true; onDone() }
            else _error.value = "Không thể chặn người dùng này"
        } catch (e: Exception) { _error.value = e.message }
        _loading.value = false
    }

    fun submitReport(ctx: Context, req: ReportRequest, onDone: () -> Unit) = viewModelScope.launch {
        _loading.value = true
        try {
            val res = RetrofitClient.interactionApi(ctx).submitReport(req)
            if (res.isSuccessful) { _actionDone.value = true; onDone() }
            else _error.value = "Không thể gửi báo cáo"
        } catch (e: Exception) { _error.value = e.message }
        _loading.value = false
    }

    fun resetState() { _actionDone.value = false; _error.value = null }
}
