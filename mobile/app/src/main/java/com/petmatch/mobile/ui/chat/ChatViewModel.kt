package com.petmatch.mobile.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    // ── Chat History ──────────────────────────────────────────────────────────
    private val _messages = MutableStateFlow<List<MessageResponse>>(emptyList())
    val messages: StateFlow<List<MessageResponse>> = _messages

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError

    fun loadChatHistory(ctx: Context, user1Id: Long, user2Id: Long, page: Int = 0) {
        viewModelScope.launch {
            _chatLoading.value = true
            try {
                val resp = RetrofitClient.chatApi(ctx).getChatHistory(user1Id, user2Id, page)
                if (resp.isSuccessful) {
                    _messages.value = resp.body() ?: emptyList()
                } else {
                    _chatError.value = "Không tải được tin nhắn"
                }
            } catch (e: Exception) {
                _chatError.value = e.message
            } finally {
                _chatLoading.value = false
            }
        }
    }

    fun addLocalMessage(msg: MessageResponse) {
        _messages.value = _messages.value + msg
    }

    fun markAsRead(ctx: Context, senderId: Long, receiverId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.chatApi(ctx).markAsRead(senderId, receiverId)
            } catch (_: Exception) {}
        }
    }

    // ── Call ─────────────────────────────────────────────────────────────────
    private val _callHistory = MutableStateFlow<List<CallHistoryResponse>>(emptyList())
    val callHistory: StateFlow<List<CallHistoryResponse>> = _callHistory

    private val _currentCall = MutableStateFlow<CallHistoryResponse?>(null)
    val currentCall: StateFlow<CallHistoryResponse?> = _currentCall

    private val _callError = MutableStateFlow<String?>(null)
    val callError: StateFlow<String?> = _callError

    fun startCall(ctx: Context, calleeId: Long, type: String, onSuccess: (CallHistoryResponse) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.callApi(ctx).startCall(
                    CallRequest(calleeId = calleeId, type = type)
                )
                if (resp.isSuccessful) {
                    resp.body()?.let {
                        _currentCall.value = it
                        onSuccess(it)
                    }
                } else {
                    _callError.value = "Không thể bắt đầu cuộc gọi"
                }
            } catch (e: Exception) {
                _callError.value = e.message
            }
        }
    }

    fun endCall(ctx: Context, callId: Long, status: String = "ACCEPTED") {
        viewModelScope.launch {
            try {
                RetrofitClient.callApi(ctx).endCall(callId, status)
                _currentCall.value = null
            } catch (_: Exception) {}
        }
    }

    fun loadCallHistory(ctx: Context, userId: Long) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.callApi(ctx).getCallHistory(userId)
                if (resp.isSuccessful) {
                    _callHistory.value = resp.body() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    // ── Appointment ──────────────────────────────────────────────────────────
    private val _appointments = MutableStateFlow<List<AppointmentResponse>>(emptyList())
    val appointments: StateFlow<List<AppointmentResponse>> = _appointments

    private val _appointmentLoading = MutableStateFlow(false)
    val appointmentLoading: StateFlow<Boolean> = _appointmentLoading

    private val _appointmentError = MutableStateFlow<String?>(null)
    val appointmentError: StateFlow<String?> = _appointmentError

    private val _appointmentSuccess = MutableStateFlow(false)
    val appointmentSuccess: StateFlow<Boolean> = _appointmentSuccess

    fun createAppointment(ctx: Context, req: AppointmentRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _appointmentLoading.value = true
            _appointmentError.value = null
            try {
                val resp = RetrofitClient.appointmentApi(ctx).createAppointment(req)
                if (resp.isSuccessful) {
                    _appointmentSuccess.value = true
                    onSuccess()
                } else {
                    _appointmentError.value = "Không thể tạo lịch hẹn"
                }
            } catch (e: Exception) {
                _appointmentError.value = e.message
            } finally {
                _appointmentLoading.value = false
            }
        }
    }

    fun loadUserAppointments(ctx: Context, userId: Long) {
        viewModelScope.launch {
            _appointmentLoading.value = true
            try {
                val resp = RetrofitClient.appointmentApi(ctx).getUserAppointments(userId)
                if (resp.isSuccessful) {
                    _appointments.value = resp.body() ?: emptyList()
                }
            } catch (_: Exception) {}
            finally {
                _appointmentLoading.value = false
            }
        }
    }

    fun updateAppointmentStatus(ctx: Context, id: Long, status: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.appointmentApi(ctx).updateStatus(id, status)
                if (resp.isSuccessful) {
                    // Refresh list
                    _appointments.value = _appointments.value.map {
                        if (it.id == id) it.copy(status = status) else it
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun resetAppointmentSuccess() {
        _appointmentSuccess.value = false
    }

    // ── Review ────────────────────────────────────────────────────────────────
    private val _reviewLoading = MutableStateFlow(false)
    val reviewLoading: StateFlow<Boolean> = _reviewLoading

    private val _reviewError = MutableStateFlow<String?>(null)
    val reviewError: StateFlow<String?> = _reviewError

    private val _reviewSuccess = MutableStateFlow(false)
    val reviewSuccess: StateFlow<Boolean> = _reviewSuccess

    fun submitReview(ctx: Context, revieweeId: Long, rating: Int, comment: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _reviewLoading.value = true
            _reviewError.value = null
            try {
                val resp = RetrofitClient.reviewApi(ctx).submitReview(
                    revieweeId,
                    ReviewRequest(rating = rating, comment = comment)
                )
                if (resp.isSuccessful) {
                    _reviewSuccess.value = true
                    onSuccess()
                } else {
                    _reviewError.value = "Không thể gửi đánh giá"
                }
            } catch (e: Exception) {
                _reviewError.value = e.message
            } finally {
                _reviewLoading.value = false
            }
        }
    }

    fun resetReviewSuccess() { _reviewSuccess.value = false }

    // ── Group Chat ─────────────────────────────────────────────────────────────
    private val _groups = MutableStateFlow<List<GroupChatResponse>>(emptyList())
    val groups: StateFlow<List<GroupChatResponse>> = _groups

    private val _groupMessages = MutableStateFlow<List<GroupMessageResponse>>(emptyList())
    val groupMessages: StateFlow<List<GroupMessageResponse>> = _groupMessages

    private val _groupLoading = MutableStateFlow(false)
    val groupLoading: StateFlow<Boolean> = _groupLoading

    private val _groupError = MutableStateFlow<String?>(null)
    val groupError: StateFlow<String?> = _groupError

    private val _groupCreateSuccess = MutableStateFlow(false)
    val groupCreateSuccess: StateFlow<Boolean> = _groupCreateSuccess

    fun loadUserGroups(ctx: Context) {
        viewModelScope.launch {
            _groupLoading.value = true
            try {
                val resp = RetrofitClient.groupChatApi(ctx).getUserGroups()
                if (resp.isSuccessful) {
                    _groups.value = resp.body() ?: emptyList()
                }
            } catch (_: Exception) {} finally {
                _groupLoading.value = false
            }
        }
    }

    fun createGroup(ctx: Context, name: String, avatarUrl: String?, memberIds: List<Long>, onSuccess: (GroupChatResponse) -> Unit) {
        viewModelScope.launch {
            _groupLoading.value = true
            _groupError.value = null
            try {
                val resp = RetrofitClient.groupChatApi(ctx).createGroup(
                    GroupChatCreateRequest(name = name, avatarUrl = avatarUrl, memberIds = memberIds)
                )
                if (resp.isSuccessful) {
                    resp.body()?.let {
                        _groupCreateSuccess.value = true
                        onSuccess(it)
                    }
                } else {
                    _groupError.value = "Không thể tạo nhóm"
                }
            } catch (e: Exception) {
                _groupError.value = e.message
            } finally {
                _groupLoading.value = false
            }
        }
    }

    fun loadGroupHistory(ctx: Context, groupId: Long) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.groupChatApi(ctx).getGroupHistory(groupId)
                if (resp.isSuccessful) {
                    _groupMessages.value = resp.body() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun addLocalGroupMessage(msg: GroupMessageResponse) {
        _groupMessages.value = _groupMessages.value + msg
    }

    fun resetGroupCreateSuccess() { _groupCreateSuccess.value = false }
}
