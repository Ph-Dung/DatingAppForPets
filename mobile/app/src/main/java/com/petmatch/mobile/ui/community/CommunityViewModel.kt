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

    fun createPostWithDeviceImages(
        ctx: Context,
        content: String,
        location: String?,
        imageUris: List<Uri>,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        _actionLoading.value = true
        _error.value = null
        _actionDone.value = false
        try {
            val api = RetrofitClient.communityApi(ctx)
            val res = if (imageUris.isNotEmpty()) {
                val contentPart = content.toRequestBody("text/plain".toMediaTypeOrNull())
                val locationPart = location?.takeIf { it.isNotBlank() }
                    ?.toRequestBody("text/plain".toMediaTypeOrNull())

                val imageParts = imageUris.mapNotNull { uri ->
                    val mimeType = ctx.contentResolver.getType(uri) ?: "image/jpeg"
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@mapNotNull null
                    val imageBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", "community_upload_${System.nanoTime()}.jpg", imageBody)
                }
                if (imageParts.isEmpty()) {
                    _actionLoading.value = false
                    _error.value = "Không đọc được ảnh đã chọn"
                    return@launch
                }

                api.createPostWithUpload(
                    content = contentPart,
                    location = locationPart,
                    image = null,
                    images = imageParts
                )
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

    fun updatePostWithDeviceImages(
        ctx: Context,
        id: Long,
        content: String,
        location: String?,
        imageUris: List<Uri>,
        onDone: (() -> Unit)? = null
    ) = viewModelScope.launch {
        _actionLoading.value = true
        _error.value = null
        try {
            val api = RetrofitClient.communityApi(ctx)

            val localUris = imageUris.filter { uri ->
                val s = uri.toString().lowercase()
                s.startsWith("content://") || s.startsWith("file://")
            }
            val remoteUrls = imageUris.map { it.toString().trim() }
                .filter { it.startsWith("http://") || it.startsWith("https://") }

            val contentPart = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val locationPart = location?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val existingUrlsPart = remoteUrls.joinToString(",").takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())

            val imageParts = localUris.mapNotNull { uri ->
                val mimeType = ctx.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@mapNotNull null
                val imageBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("images", "community_upload_${System.nanoTime()}.jpg", imageBody)
            }

            val res = api.updatePostWithUpload(
                id = id,
                content = contentPart,
                location = locationPart,
                existingImageUrls = existingUrlsPart,
                image = null,
                images = imageParts
            )

            if (res.isSuccessful) {
                loadFeed(ctx)
                loadMyPosts(ctx)
                onDone?.invoke()
            } else {
                _error.value = "Không thể cập nhật bài viết"
            }
        } catch (_: Exception) {
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
                val likedRaw = res.body()?.get("liked")
                val liked = when (likedRaw) {
                    is Boolean -> likedRaw
                    is String -> likedRaw.equals("true", ignoreCase = true)
                    else -> false
                }
                _feed.value = _feed.value.map { post ->
                    if (post.id == id) {
                        post.copy(
                            isLiked = liked,
                            likesCount = if (liked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                        )
                    } else post
                }
                _myPosts.value = _myPosts.value.map { post ->
                    if (post.id == id) {
                        post.copy(
                            isLiked = liked,
                            likesCount = if (liked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                        )
                    } else post
                }
            } else {
                _error.value = "Không thể cập nhật lượt thích"
            }
        } catch (_: Exception) {
            _error.value = "Không thể kết nối máy chủ"
        }
    }

    fun replyComment(ctx: Context, postId: Long, parentCommentId: Long, content: String, onDone: (() -> Unit)? = null) = viewModelScope.launch {
        _actionLoading.value = true
        _error.value = null
        try {
            val res = RetrofitClient.communityApi(ctx)
                .replyComment(parentCommentId, CommunityCreateCommentRequest(content.trim()))
            if (res.isSuccessful) {
                loadComments(ctx, postId)
                loadFeed(ctx)
                loadMyPosts(ctx)
                onDone?.invoke()
            } else {
                _error.value = "Không gửi được phản hồi"
            }
        } catch (_: Exception) {
            _error.value = "Không thể kết nối máy chủ"
        }
        _actionLoading.value = false
    }

    fun loadComments(ctx: Context, postId: Long) = viewModelScope.launch {
        _commentsLoading.value = true
        _error.value = null
        try {
            val res = RetrofitClient.communityApi(ctx).getComments(postId)
            if (res.isSuccessful) {
                _comments.value = res.body() ?: emptyList()
            } else {
                _error.value = "Không tải được bình luận"
            }
        } catch (_: Exception) {
            _error.value = "Không thể kết nối máy chủ"
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
                _error.value = "Không gửi được bình luận"
            }
        } catch (_: Exception) {
            _error.value = "Không thể kết nối máy chủ"
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
                _error.value = "Không gửi được báo cáo"
            }
        } catch (_: Exception) {
            _error.value = "Không thể kết nối máy chủ"
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
