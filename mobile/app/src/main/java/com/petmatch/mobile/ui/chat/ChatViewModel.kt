package com.petmatch.mobile.ui.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.petmatch.mobile.Constants
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.api.SignalingClient
import com.petmatch.mobile.data.api.dataStore
import com.petmatch.mobile.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File

/** Trạng thái cuộc gọi đến – null = không có cuộc gọi nào. */
data class IncomingCallState(
    val callId: Long,
    val callType: String,   // "AUDIO" | "VIDEO"
    val callerId: Long,
    val callerName: String
)

class ChatViewModel : ViewModel() {

    var isCallCancelled = false

    // ── Current user ID (from DataStore) ──────────────────────────────────────
    private val _currentUserId = MutableStateFlow(0L)
    val currentUserId: StateFlow<Long> = _currentUserId

    fun loadCurrentUserId(ctx: Context) {
        viewModelScope.launch {
            val id = ctx.dataStore.data
                .map { it[stringPreferencesKey("current_user_id")] }
                .firstOrNull()?.toLongOrNull() ?: 0L
            _currentUserId.value = id
        }
    }

    // ── Signaling (WebRTC) ────────────────────────────────────────────────────
    private val gson = Gson()
    private var signalingClient: SignalingClient? = null

    /** Cuộc gọi đến – collect bởi màn hình đang hiển thị để show IncomingCallOverlay. */
    private val _incomingCall = MutableStateFlow<IncomingCallState?>(null)
    val incomingCall: StateFlow<IncomingCallState?> = _incomingCall

    /** Signal thô nhận từ người kia (OFFER / ANSWER / ICE_CANDIDATE / HANG_UP). */
    private val _rtcSignal = MutableStateFlow<SignalingMessage?>(null)
    val rtcSignal: StateFlow<SignalingMessage?> = _rtcSignal

    private val _signalingConnected = MutableStateFlow(false)
    val signalingConnected: StateFlow<Boolean> = _signalingConnected

    /**
     * Khởi tạo SignalingClient và kết nối STOMP.
     * Gọi một lần khi vào màn hình Call.
     */
    fun connectSignaling(ctx: Context, onConnected: () -> Unit = {}) {
        // Guard: if already connected, just fire the callback
        if (_signalingConnected.value && signalingClient != null) {
            onConnected()
            return
        }
        viewModelScope.launch {
            val token = ctx.dataStore.data
                .map { it[stringPreferencesKey(Constants.TOKEN_KEY)] }
                .firstOrNull() ?: return@launch

            val baseWsUrl = Constants.BASE_URL
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                .trimEnd('/')

            signalingClient?.disconnect()  // clean up any stale connection
            signalingClient = SignalingClient(
                baseWsUrl = baseWsUrl,
                token = token,
                onSignal = { msg -> handleIncomingSignal(msg) },
                onConnectionEstablished = {
                    _signalingConnected.value = true
                    onConnected()
                },
                onConnectionLost = { _signalingConnected.value = false }
            ).also { it.connect() }
        }
    }

    fun disconnectSignaling() {
        signalingClient?.disconnect()
        signalingClient = null
        _signalingConnected.value = false
        _rtcSignal.value = null
        _incomingCall.value = null
    }

    fun sendRtcSignal(msg: SignalingMessage) {
        signalingClient?.sendSignal(msg)
            ?: Log.w("ChatViewModel", "sendRtcSignal: signalingClient is null")
    }

    /** Clear đã xử lý signal. */
    fun consumeRtcSignal() { _rtcSignal.value = null }

    /** Dismiss incoming call (người dùng bác bỏ). */
    fun dismissIncomingCall() { _incomingCall.value = null }

    private fun handleIncomingSignal(msg: SignalingMessage) {
        when (msg.type) {
            "INCOMING_CALL" -> {
                // msg.data = { "callId": 42, "callType": "AUDIO" }
                try {
                    val map = gson.fromJson(msg.data, Map::class.java)
                    val callId = (map["callId"] as? Double)?.toLong() ?: return
                    val callType = map["callType"] as? String ?: "AUDIO"
                    _incomingCall.value = IncomingCallState(
                        callId = callId,
                        callType = callType,
                        callerId = msg.senderId,
                        callerName = "Cuộc gọi đến"   // Name resolved from conversation list
                    )
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Parse INCOMING_CALL error: ${e.message}")
                }
            }
            // OFFER, ANSWER, ICE_CANDIDATE, HANG_UP → relay tới CallScreen
            else -> { _rtcSignal.value = msg }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectSignaling()
    }

    // ── Conversations (match-gated list) ──────────────────────────────────────
    private val _conversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversations: StateFlow<List<ConversationItem>> = _conversations

    private val _conversationsLoading = MutableStateFlow(false)
    val conversationsLoading: StateFlow<Boolean> = _conversationsLoading

    fun loadConversations(ctx: Context) {
        viewModelScope.launch {
            _conversationsLoading.value = true
            try {
                val resp = RetrofitClient.chatApi(ctx).getConversations()
                if (resp.isSuccessful) {
                    _conversations.value = resp.body() ?: emptyList()
                }
            } catch (_: Exception) {}
            finally { _conversationsLoading.value = false }
        }
    }

    fun deleteConversation(ctx: Context, otherId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.chatApi(ctx).deleteConversation(otherId)
                // Update conversation: clear messages but keep conversation in list
                _conversations.value = _conversations.value.map { conv ->
                    if (conv.userId == otherId) {
                        conv.copy(lastMessage = null, lastMessageTime = null, unreadCount = 0)
                    } else {
                        conv
                    }
                }
                // Also clear local messages for this conversation
                _messages.value = emptyList()
            } catch (_: Exception) {}
        }
    }

    fun muteConversation(otherId: Long) {
        // Toggle mute locally (backend mute endpoint can be added later)
        _conversations.value = _conversations.value.map {
            if (it.userId == otherId) it.copy(isMuted = !it.isMuted) else it
        }
    }

    // ── Block Status ──────────────────────────────────────────────────────────
    private val _blockStatus = MutableStateFlow<BlockStatus?>(null)
    val blockStatus: StateFlow<BlockStatus?> = _blockStatus

    fun loadBlockStatus(ctx: Context, otherUserId: Long) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.chatApi(ctx).getBlockStatus(otherUserId)
                if (resp.isSuccessful) _blockStatus.value = resp.body()
            } catch (_: Exception) {}
        }
    }

    fun blockUser(ctx: Context, targetUserId: Long, level: String = "ALL") {
        viewModelScope.launch {
            try {
                RetrofitClient.interactionApi(ctx).blockUser(targetUserId, level)
                _blockStatus.value = _blockStatus.value?.copy(iBlockedThem = true, myBlockLevel = level)
                    ?: BlockStatus(iBlockedThem = true, theyBlockedMe = false, myBlockLevel = level)
            } catch (_: Exception) {}
        }
    }

    fun unblockUser(ctx: Context, targetUserId: Long) {
        viewModelScope.launch {
            try {
                RetrofitClient.interactionApi(ctx).unblockUser(targetUserId)
                _blockStatus.value = _blockStatus.value?.copy(iBlockedThem = false, myBlockLevel = "")
            } catch (_: Exception) {}
        }
    }

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
            try { RetrofitClient.chatApi(ctx).markAsRead(senderId, receiverId) }
            catch (_: Exception) {}
        }
    }

    fun sendTextMessage(ctx: Context, currentUserId: Long, receiverId: Long, text: String) {
        viewModelScope.launch {
            try {
                // Optimistic UI update
                val localMsg = MessageResponse(
                    id = System.currentTimeMillis(),
                    senderId = currentUserId,
                    receiverId = receiverId,
                    content = text,
                    sentAt = java.time.LocalDateTime.now().toString(),
                    isRead = false,
                    type = "TEXT"
                )
                addLocalMessage(localMsg)

                // Network request
                val req = MessageRequest(
                    senderId = currentUserId,
                    receiverId = receiverId,
                    content = text,
                    type = "TEXT"
                )
                val resp = RetrofitClient.chatApi(ctx).sendMessage(req)
                if (resp.isSuccessful) {
                    val serverMsg = resp.body()
                    if (serverMsg != null) {
                        // Cập nhật id thật từ server (optional: replace the local message by id match)
                        // Trong bài toán demo, optimistic UI đã hiển thị đủ.
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Lỗi gửi tin nhắn: ${e.message}")
            }
        }
    }

    // ── Media Upload (Image / Voice) ──────────────────────────────────────────
    private val _mediaUploadLoading = MutableStateFlow(false)
    val mediaUploadLoading: StateFlow<Boolean> = _mediaUploadLoading

    /**
     * Upload file từ Uri, gửi media message vào conversation.
     * type: "IMAGE" hoặc "VOICE"
     */
    fun uploadAndSendMedia(
        ctx: Context,
        fileUri: Uri,
        type: String,
        currentUserId: Long,
        receiverId: Long
    ) {
        viewModelScope.launch {
            _mediaUploadLoading.value = true
            try {
                val contentResolver = ctx.contentResolver
                val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
                val inputStream = contentResolver.openInputStream(fileUri) ?: return@launch
                val tempFile = File.createTempFile("upload_", null, ctx.cacheDir)
                tempFile.outputStream().use { inputStream.copyTo(it) }

                val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val typePart = type.toRequestBody("text/plain".toMediaTypeOrNull())

                val resp = RetrofitClient.chatApi(ctx).uploadMedia(filePart, typePart)
                if (resp.isSuccessful) {
                    val upload = resp.body() ?: return@launch
                    val localMsg = MessageResponse(
                        id = System.currentTimeMillis(),
                        senderId = currentUserId,
                        receiverId = receiverId,
                        content = upload.mediaUrl, // use mediaUrl as content for fallback or consistency
                        sentAt = java.time.LocalDateTime.now().toString(),
                        isRead = false,
                        type = upload.type,
                        mediaUrl = upload.mediaUrl
                    )
                    addLocalMessage(localMsg)

                    // Persist to backend
                    val req = MessageRequest(
                        senderId = currentUserId,
                        receiverId = receiverId,
                        content = upload.mediaUrl,
                        type = upload.type,
                        mediaUrl = upload.mediaUrl
                    )
                    RetrofitClient.chatApi(ctx).sendMessage(req)
                }
            } catch (_: Exception) {}
            finally {
                _mediaUploadLoading.value = false
            }
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
                val resp = RetrofitClient.callApi(ctx).startCall(CallRequest(calleeId, type))
                if (resp.isSuccessful) {
                    resp.body()?.let { _currentCall.value = it; onSuccess(it) }
                } else {
                    _callError.value = "Không thể bắt đầu cuộc gọi"
                }
            } catch (e: Exception) { _callError.value = e.message }
        }
    }

    fun endCall(ctx: Context, callId: Long, status: String = "ACCEPTED", durationSeconds: Int? = null, peerId: Long? = null) {
        viewModelScope.launch {
            try { 
                RetrofitClient.callApi(ctx).endCall(callId, status, durationSeconds)
                _currentCall.value = null
                if (peerId != null && _currentUserId.value > 0) {
                    loadChatHistory(ctx, _currentUserId.value, peerId)
                }
            }
            catch (_: Exception) {}
        }
    }

    fun loadCallHistory(ctx: Context, userId: Long) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.callApi(ctx).getCallHistory(userId)
                if (resp.isSuccessful) _callHistory.value = resp.body() ?: emptyList()
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
            _appointmentLoading.value = true; _appointmentError.value = null
            try {
                val resp = RetrofitClient.appointmentApi(ctx).createAppointment(req)
                if (resp.isSuccessful) { _appointmentSuccess.value = true; onSuccess() }
                else _appointmentError.value = "Không thể tạo lịch hẹn"
            } catch (e: Exception) { _appointmentError.value = e.message }
            finally { _appointmentLoading.value = false }
        }
    }

    fun loadUserAppointments(ctx: Context, userId: Long) {
        viewModelScope.launch {
            _appointmentLoading.value = true
            try {
                val resp = RetrofitClient.appointmentApi(ctx).getUserAppointments(userId)
                if (resp.isSuccessful) _appointments.value = resp.body() ?: emptyList()
            } catch (_: Exception) {}
            finally { _appointmentLoading.value = false }
        }
    }

    fun updateAppointmentStatus(ctx: Context, id: Long, status: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.appointmentApi(ctx).updateStatus(id, status)
                if (resp.isSuccessful)
                    _appointments.value = _appointments.value.map { if (it.id == id) it.copy(status = status) else it }
            } catch (_: Exception) {}
        }
    }

    fun resetAppointmentSuccess() { _appointmentSuccess.value = false }

    // ── Review ────────────────────────────────────────────────────────────────
    private val _reviewLoading = MutableStateFlow(false)
    val reviewLoading: StateFlow<Boolean> = _reviewLoading
    private val _reviewError = MutableStateFlow<String?>(null)
    val reviewError: StateFlow<String?> = _reviewError
    private val _reviewSuccess = MutableStateFlow(false)
    val reviewSuccess: StateFlow<Boolean> = _reviewSuccess

    fun submitReview(ctx: Context, revieweeId: Long, rating: Int, comment: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _reviewLoading.value = true; _reviewError.value = null
            try {
                val resp = RetrofitClient.reviewApi(ctx).submitReview(revieweeId, ReviewRequest(rating, comment))
                if (resp.isSuccessful) { _reviewSuccess.value = true; onSuccess() }
                else _reviewError.value = "Không thể gửi đánh giá"
            } catch (e: Exception) { _reviewError.value = e.message }
            finally { _reviewLoading.value = false }
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
                if (resp.isSuccessful) _groups.value = resp.body() ?: emptyList()
            } catch (_: Exception) {}
            finally { _groupLoading.value = false }
        }
    }

    fun createGroup(ctx: Context, name: String, avatarUri: android.net.Uri?, memberIds: List<Long>, onSuccess: (GroupChatResponse) -> Unit) {
        viewModelScope.launch {
            _groupLoading.value = true; _groupError.value = null
            try {
                var avatarUrl: String? = null
                
                // Upload avatar if provided
                if (avatarUri != null) {
                    try {
                        val contentResolver = ctx.contentResolver
                        val mimeType = contentResolver.getType(avatarUri) ?: "application/octet-stream"
                        val inputStream = contentResolver.openInputStream(avatarUri) ?: throw Exception("Không thể mở file hình ảnh")
                        val tempFile = File.createTempFile("group_avatar_", null, ctx.cacheDir)
                        tempFile.outputStream().use { inputStream.copyTo(it) }

                        val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                        val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                        val typePart = "IMAGE".toRequestBody("text/plain".toMediaTypeOrNull())

                        val uploadResp = RetrofitClient.chatApi(ctx).uploadMedia(filePart, typePart)
                        if (uploadResp.isSuccessful) {
                            avatarUrl = uploadResp.body()?.mediaUrl
                        } else {
                            throw Exception("Lỗi upload ảnh nhóm")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Lỗi upload avatar: ${e.message}")
                        _groupError.value = "Lỗi upload ảnh nhóm: ${e.message}"
                        _groupLoading.value = false
                        return@launch
                    }
                }
                
                // Create group with avatar URL
                val resp = RetrofitClient.groupChatApi(ctx).createGroup(GroupChatCreateRequest(name, avatarUrl, memberIds))
                if (resp.isSuccessful) resp.body()?.let { _groupCreateSuccess.value = true; onSuccess(it) }
                else _groupError.value = "Không thể tạo nhóm"
            } catch (e: Exception) { _groupError.value = e.message }
            finally { _groupLoading.value = false }
        }
    }

    fun loadGroupHistory(ctx: Context, groupId: Long) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.groupChatApi(ctx).getGroupHistory(groupId)
                if (resp.isSuccessful) {
                    // Sort messages by sentAt in ascending order (oldest first)
                    val messages = resp.body() ?: emptyList()
                    _groupMessages.value = messages.sortedBy { it.sentAt }
                }
            } catch (_: Exception) {}
        }
    }

    fun addLocalGroupMessage(msg: GroupMessageResponse) {
        // Add and keep sorted order (oldest first)
        _groupMessages.value = (_groupMessages.value + msg).sortedBy { it.sentAt }
    }

    fun sendGroupMessage(ctx: Context, groupId: Long, currentUserId: Long, text: String) {
        viewModelScope.launch {
            try {
                val req = GroupMessageRequest(content = text)
                RetrofitClient.groupChatApi(ctx).sendGroupMessage(groupId, req)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Lỗi gửi tin nhắn nhóm: ${e.message}")
            }
        }
    }

    fun uploadAndSendGroupMedia(ctx: Context, fileUri: android.net.Uri, type: String, groupId: Long) {
        viewModelScope.launch {
            _mediaUploadLoading.value = true
            try {
                val contentResolver = ctx.contentResolver
                val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
                val inputStream = contentResolver.openInputStream(fileUri) ?: return@launch
                val tempFile = File.createTempFile("upload_", null, ctx.cacheDir)
                tempFile.outputStream().use { inputStream.copyTo(it) }

                val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val typePart = type.toRequestBody("text/plain".toMediaTypeOrNull())

                val resp = RetrofitClient.chatApi(ctx).uploadMedia(filePart, typePart)
                if (resp.isSuccessful) {
                    val upload = resp.body() ?: return@launch
                    val req = GroupMessageRequest(content = upload.mediaUrl)
                    RetrofitClient.groupChatApi(ctx).sendGroupMessage(groupId, req)
                }
            } catch (_: Exception) {}
            finally {
                _mediaUploadLoading.value = false
            }
        }
    }

    fun resetGroupCreateSuccess() { _groupCreateSuccess.value = false }

    fun addGroupMember(ctx: Context, groupId: Long, newMemberId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.groupChatApi(ctx).addMember(groupId, newMemberId)
                if (resp.isSuccessful) {
                    loadUserGroups(ctx) // Reload để có danh sách member mới nhất
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Lỗi thêm thành viên: ${e.message}")
            }
        }
    }
}
