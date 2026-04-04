package com.petmatch.mobile.ui.chatbot

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.model.ChatMessage
import com.petmatch.mobile.data.model.ChatbotRequest
import com.petmatch.mobile.data.model.ChatbotResponse
import com.petmatch.mobile.data.model.PetProfileResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatbotViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _suggestions = MutableStateFlow<List<PetProfileResponse>>(emptyList())
    val suggestions: StateFlow<List<PetProfileResponse>> = _suggestions

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun sendMessage(ctx: Context, userText: String) = viewModelScope.launch {
        if (userText.isBlank()) return@launch

        // Add user message
        val newMessages = _messages.value + ChatMessage("user", userText.trim())
        _messages.value = newMessages
        _isLoading.value = true
        _suggestions.value = emptyList() // Xóa gợi ý cũ ngay lập tức
        _error.value = null

        try {
            val res = RetrofitClient.chatbotApi(ctx).sendMessage(
                ChatbotRequest(newMessages)
            )
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                // Add assistant reply
                _messages.value = _messages.value + ChatMessage("assistant", body.reply)
                if (body.isReadyToSuggest) {
                    _suggestions.value = body.suggestions
                }
            } else {
                val beError = try {
                    val errorJson = res.errorBody()?.string()
                    val p = org.json.JSONObject(errorJson)
                    p.optString("error", "Lỗi rỗng")
                } catch (e: Exception) {
                    "Lỗi HTTP ${res.code()}"
                }
                val errMsg = "Không thể kết nối AI. $beError"
                _messages.value = _messages.value + ChatMessage("assistant", errMsg)
                _error.value = errMsg
            }
        } catch (e: Exception) {
            val errMsg = "Lỗi hệ thống: ${e.message}"
            _messages.value = _messages.value + ChatMessage("assistant", errMsg)
            _error.value = errMsg
        }
        _isLoading.value = false
    }

    fun clearSuggestions() { _suggestions.value = emptyList() }

    fun resetConversation() {
        _messages.value = emptyList()
        _suggestions.value = emptyList()
        _error.value = null
    }
}
