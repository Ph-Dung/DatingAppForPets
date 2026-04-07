package com.petmatch.mobile.data.api

import com.petmatch.mobile.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {

    /** Danh sách conversation của user hiện tại (match-gated) */
    @GET("api/chat/conversations")
    suspend fun getConversations(): Response<List<ConversationItem>>

    /** Lấy lịch sử chat phân trang giữa 2 user */
    @GET("api/chat/history")
    suspend fun getChatHistory(
        @Query("user1Id") user1Id: Long,
        @Query("user2Id") user2Id: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): Response<List<MessageResponse>>

    /** Đánh dấu đã đọc */
    @PUT("api/chat/read")
    suspend fun markAsRead(
        @Query("senderId") senderId: Long,
        @Query("receiverId") receiverId: Long
    ): Response<Unit>

    /** Số tin chưa đọc */
    @GET("api/chat/unread/count")
    suspend fun getUnreadCount(
        @Query("senderId") senderId: Long,
        @Query("receiverId") receiverId: Long
    ): Response<Map<String, Long>>

    /** Xóa conversation (soft-delete từ phía mình) */
    @DELETE("api/chat/conversations/{otherId}")
    suspend fun deleteConversation(@Path("otherId") otherId: Long): Response<Unit>

    /** Kiểm tra trạng thái block giữa mình và người kia */
    @GET("api/chat/block-status/{otherUserId}")
    suspend fun getBlockStatus(@Path("otherUserId") otherUserId: Long): Response<BlockStatus>

    /**Upload ảnh hoặc voice message */
    @Multipart
    @POST("api/chat/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody
    ): Response<MediaUploadResponse>

    /** Gửi tin nhắn Text qua REST */
    @POST("api/chat/messages")
    suspend fun sendMessage(@Body req: MessageRequest): Response<MessageResponse>

    /** Lấy nickname */
    @GET("api/chat/nicknames/{receiverId}")
    suspend fun getNickname(@Path("receiverId") receiverId: Long): Response<Map<String, String>>

    /** Đặt nickname */
    @PUT("api/chat/nicknames/{receiverId}")
    suspend fun setNickname(@Path("receiverId") receiverId: Long, @Body req: Map<String, String>): Response<Map<String, String>>
}

interface CallApi {

    @POST("api/calls/start")
    suspend fun startCall(@Body req: CallRequest): Response<CallHistoryResponse>

    @PUT("api/calls/{callId}/end")
    suspend fun endCall(
        @Path("callId") callId: Long,
        @Query("status") status: String,
        @Query("durationSeconds") durationSeconds: Int? = null
    ): Response<CallHistoryResponse>

    @GET("api/calls/history/{userId}")
    suspend fun getCallHistory(@Path("userId") userId: Long): Response<List<CallHistoryResponse>>
}

interface AppointmentApi {

    @POST("api/appointments")
    suspend fun createAppointment(@Body req: AppointmentRequest): Response<AppointmentResponse>

    @GET("api/appointments/user/{userId}")
    suspend fun getUserAppointments(@Path("userId") userId: Long): Response<List<AppointmentResponse>>

    @PUT("api/appointments/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: Long,
        @Query("status") status: String
    ): Response<AppointmentResponse>

    @DELETE("api/appointments/{id}")
    suspend fun cancelAppointment(@Path("id") id: Long): Response<Unit>
}

interface ReviewApi {

    @POST("api/interactions/reviews/{revieweeId}")
    suspend fun submitReview(
        @Path("revieweeId") revieweeId: Long,
        @Body req: ReviewRequest
    ): Response<ReviewResponse>

    @GET("api/interactions/reviews/user/{userId}")
    suspend fun getUserReviews(@Path("userId") userId: Long): Response<List<ReviewResponse>>
}

interface GroupChatApi {

    @POST("api/groups")
    suspend fun createGroup(@Body req: GroupChatCreateRequest): Response<GroupChatResponse>

    @GET("api/groups")
    suspend fun getUserGroups(): Response<List<GroupChatResponse>>

    @POST("api/groups/{groupId}/messages")
    suspend fun sendGroupMessage(
        @Path("groupId") groupId: Long,
        @Body req: GroupMessageRequest
    ): Response<GroupMessageResponse>

    @GET("api/groups/{groupId}/history")
    suspend fun getGroupHistory(
        @Path("groupId") groupId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): Response<List<GroupMessageResponse>>

    @POST("api/groups/{groupId}/members/{newMemberId}")
    suspend fun addMember(
        @Path("groupId") groupId: Long,
        @Path("newMemberId") newMemberId: Long
    ): Response<Unit>
}
