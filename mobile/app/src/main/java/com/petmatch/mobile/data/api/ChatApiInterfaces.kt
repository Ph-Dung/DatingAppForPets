package com.petmatch.mobile.data.api

import com.petmatch.mobile.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {

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
}

interface CallApi {

    /** Bắt đầu cuộc gọi */
    @POST("api/calls/start")
    suspend fun startCall(@Body req: CallRequest): Response<CallHistoryResponse>

    /** Kết thúc cuộc gọi */
    @PUT("api/calls/{callId}/end")
    suspend fun endCall(
        @Path("callId") callId: Long,
        @Query("status") status: String  // ACCEPTED, MISSED, REJECTED
    ): Response<CallHistoryResponse>

    /** Lịch sử cuộc gọi */
    @GET("api/calls/history/{userId}")
    suspend fun getCallHistory(@Path("userId") userId: Long): Response<List<CallHistoryResponse>>
}

interface AppointmentApi {

    /** Tạo lịch hẹn */
    @POST("api/appointments")
    suspend fun createAppointment(@Body req: AppointmentRequest): Response<AppointmentResponse>

    /** Lịch hẹn của user */
    @GET("api/appointments/user/{userId}")
    suspend fun getUserAppointments(@Path("userId") userId: Long): Response<List<AppointmentResponse>>

    /** Cập nhật trạng thái lịch hẹn */
    @PUT("api/appointments/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: Long,
        @Query("status") status: String  // PENDING, CONFIRMED, COMPLETED, CANCELLED
    ): Response<AppointmentResponse>

    /** Xóa/hủy lịch hẹn */
    @DELETE("api/appointments/{id}")
    suspend fun cancelAppointment(@Path("id") id: Long): Response<Unit>
}

interface ReviewApi {

    /** Gửi đánh giá sau cuộc hẹn – POST /api/interactions/reviews/{revieweeId} */
    @POST("api/interactions/reviews/{revieweeId}")
    suspend fun submitReview(
        @Path("revieweeId") revieweeId: Long,
        @Body req: ReviewRequest
    ): Response<ReviewResponse>

    /** Xem đánh giá nhận được – GET /api/interactions/reviews/user/{userId} */
    @GET("api/interactions/reviews/user/{userId}")
    suspend fun getUserReviews(@Path("userId") userId: Long): Response<List<ReviewResponse>>
}

interface GroupChatApi {

    /** Tạo nhóm chat */
    @POST("api/groups")
    suspend fun createGroup(@Body req: GroupChatCreateRequest): Response<GroupChatResponse>

    /** Danh sách nhóm của user */
    @GET("api/groups")
    suspend fun getUserGroups(): Response<List<GroupChatResponse>>

    /** Gửi tin nhắn nhóm */
    @POST("api/groups/{groupId}/messages")
    suspend fun sendGroupMessage(
        @Path("groupId") groupId: Long,
        @Body req: GroupMessageRequest
    ): Response<GroupMessageResponse>

    /** Lịch sử nhóm */
    @GET("api/groups/{groupId}/history")
    suspend fun getGroupHistory(
        @Path("groupId") groupId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): Response<List<GroupMessageResponse>>

    /** Thêm thành viên */
    @POST("api/groups/{groupId}/members/{newMemberId}")
    suspend fun addMember(
        @Path("groupId") groupId: Long,
        @Path("newMemberId") newMemberId: Long
    ): Response<Unit>
}
