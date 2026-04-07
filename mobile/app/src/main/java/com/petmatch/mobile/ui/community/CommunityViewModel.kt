package com.petmatch.mobile.ui.community

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.model.CommunityCommentResponse
import com.petmatch.mobile.data.model.CommunityCreateCommentRequest
import com.petmatch.mobile.data.model.CommunityCreatePostRequest
import com.petmatch.mobile.data.model.CommunityPostResponse
import com.petmatch.mobile.data.model.CommunityReportRequest
import com.petmatch.mobile.data.model.CommunityUpdatePostRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class CommunityViewModel : ViewModel() {

    private val _feed = MutableStateFlow<List<CommunityPostResponse>>(emptyList())
    val feed: StateFlow<List<CommunityPostResponse>> = _feed

    private val _myPosts = MutableStateFlow<List<CommunityPostResponse>>(emptyList())
    val myPosts: StateFlow<List<CommunityPostResponse>> = _myPosts

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _actionLoading = MutableStateFlow(false)
    val actionLoading: StateFlow<Boolean> = _actionLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _comments = MutableStateFlow<List<CommunityCommentResponse>>(emptyList())
    val comments: StateFlow<List<CommunityCommentResponse>> = _comments

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading

    private val _actionDone = MutableStateFlow(false)
    val actionDone: StateFlow<Boolean> = _actionDone

    fun loadFeed(ctx: Context) = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        try {
            val res = RetrofitClient.communityApi(ctx).getFeed()
            if (res.isSuccessful) {
                _feed.value = res.body() ?: emptyList()
            } else {
                _error.value = "Không tải được bảng tin cộng đồng"
            }
        } catch (e: Exception) {
            _error.value = "Không thể kết nối máy chủ"
        }
        _loading.value = false
    }

    fun loadMyPosts(ctx: Context) = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        try {
            val res = RetrofitClient.communityApi(ctx).getMyPosts()
            if (res.isSuccessful) {
                _myPosts.value = res.body() ?: emptyList()
            } else {
                _error.value = "Không tải được danh sách bài viết của bạn"
            }
        } catch (e: Exception) {
            _error.value = "Không thể kết nối máy chủ"
        }
        _loading.value = false
    }

    fun createPost(
        ctx: Context,
        content: String,
        imageUrl: String?,
        location: String?,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        _actionLoading.value = true
        _error.value = null
        _actionDone.value = false
        try {
            val req = CommunityCreatePostRequest(content, imageUrl?.takeIf { it.isNotBlank() }, location?.takeIf { it.isNotBlank() })
            val res = RetrofitClient.communityApi(ctx).createPost(req)
            if (res.isSuccessful) {
                _actionDone.value = true
                loadFeed(ctx)
                loadMyPosts(ctx)
                onDone()
            } else {
                _error.value = "Không thể đăng bài"
            }
        } catch (e: Exception) {
            _error.value = "Không thể kết nối máy chủ"
        }
        _actionLoading.value = false
    }

    fun createPostWithDeviceImage(
        ctx: Context,
        content: String,
        location: String?,
        imageUri: Uri?,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        _actionLoading.value = true
        _error.value = null
        _actionDone.value = false
        try {
            val api = RetrofitClient.communityApi(ctx)
            val res = if (imageUri != null) {
                val mimeType = ctx.contentResolver.getType(imageUri) ?: "image/jpeg"
                val bytes = ctx.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                if (bytes == null) {
                    _actionLoading.value = false
                    _error.value = "Không đọc được ảnh đã chọn"
                    return@launch
                }

                val contentPart = content.toRequestBody("text/plain".toMediaTypeOrNull())
                val locationPart = location?.takeIf { it.isNotBlank() }
                    ?.toRequestBody("text/plain".toMediaTypeOrNull())
                val imageBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", "community_upload.jpg", imageBody)

                api.createPostWithUpload(contentPart, locationPart, imagePart)
            } else {
                val req = CommunityCreatePostRequest(
                    content,
                    null,
                    location?.takeIf { it.isNotBlank() }
                )
                api.createPost(req)
            }

            if (res.isSuccessful) {
                _actionDone.value = true
                loadFeed(ctx)
                loadMyPosts(ctx)
                onDone()
            } else {
                _error.value = "Không thể đăng bài"
            }
        } catch (e: Exception) {
            _error.value = "Không thể kết nối máy chủ"
        }
        _actionLoading.value = false
    }

    fun updatePost(
        ctx: Context,
        id: Long,
        content: String,
        imageUrl: String?,
        location: String?,
        onDone: (() -> Unit)? = null
    ) = viewModelScope.launch {
        _actionLoading.value = true
        _error.value = null
        try {
            val req = CommunityUpdatePostRequest(content, imageUrl?.takeIf { it.isNotBlank() }, location?.takeIf { it.isNotBlank() })
            val res = RetrofitClient.communityApi(ctx).updatePost(id, req)
            if (res.isSuccessful) {
                loadFeed(ctx)
                loadMyPosts(ctx)
                onDone?.invoke()
            } else {
                _error.value = "Không thể cập nhật bài viết"
            }
        } catch (e: Exception) {
            _error.value = "Không thể kết nối máy chủ"
        }
        _actionLoading.value = false
    }

    fun deletePost(ctx: Context, id: Long) = viewModelScope.launch {
        _actionLoading.value = true
        _error.value = null
        try {
            val res = RetrofitClient.communityApi(ctx).deletePost(id)
            if (res.isSuccessful) {
                _myPosts.value = _myPosts.value.filterNot { it.id == id }
                _feed.value = _feed.value.filterNot { it.id == id }
            } else {
                _error.value = "Không thể xóa bài viết"
            }
        } catch (e: Exception) {
            _error.value = "Không thể kết nối máy chủ"
        }
        _actionLoading.value = false
    }

    fun toggleLike(ctx: Context, id: Long) = viewModelScope.launch {
        _error.value = null
        try {
            val res = RetrofitClient.communityApi(ctx).toggleLike(id)
            if (res.isSuccessful) {
                loadFeed(ctx)
                loadMyPosts(ctx)
            } else {
                _error.value = "Khong the cap nhat luot thich"
            }
        } catch (_: Exception) {
            _error.value = "Khong the ket noi may chu"
        }
    }

    fun loadComments(ctx: Context, postId: Long) = viewModelScope.launch {
        _commentsLoading.value = true
        _error.value = null
        try {
            val res = RetrofitClient.communityApi(ctx).getComments(postId)
            if (res.isSuccessful) {
                _comments.value = res.body() ?: emptyList()
            } else {
                _error.value = "Khong tai duoc binh luan"
            }
        } catch (_: Exception) {
            _error.value = "Khong the ket noi may chu"
        }
        _commentsLoading.value = false
    }

    fun addComment(ctx: Context, postId: Long, content: String, onDone: (() -> Unit)? = null) = viewModelScope.launch {
        _actionLoading.value = true
        _error.value = null
        try {
            val res = RetrofitClient.communityApi(ctx)
                .addComment(postId, CommunityCreateCommentRequest(content.trim()))
            if (res.isSuccessful) {
                loadComments(ctx, postId)
                loadFeed(ctx)
                loadMyPosts(ctx)
                onDone?.invoke()
            } else {
                _error.value = "Khong gui duoc binh luan"
            }
        } catch (_: Exception) {
            _error.value = "Khong the ket noi may chu"
        }
        _actionLoading.value = false
    }

    fun submitReport(ctx: Context, postId: Long, reason: String, onDone: (() -> Unit)? = null) = viewModelScope.launch {
        _actionLoading.value = true
        _error.value = null
        try {
            val req = CommunityReportRequest(
                targetId = postId,
                targetType = "POST",
                reason = reason.trim()
            )
            val res = RetrofitClient.communityApi(ctx).submitReport(req)
            if (res.isSuccessful) {
                onDone?.invoke()
            } else {
                _error.value = "Khong gui duoc bao cao"
            }
        } catch (_: Exception) {
            _error.value = "Khong the ket noi may chu"
        }
        _actionLoading.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun clearActionState() {
        _actionDone.value = false
        _error.value = null
    }
}
