package com.petmatch.mobile.data.model

import com.google.gson.annotations.SerializedName

// ── Chat Message ──────────────────────────────────────────────────────────────

data class MessageResponse(
    val id: Long,
    val senderId: Long,
    val receiverId: Long,
    val content: String,
    val sentAt: String?,
    val isRead: Boolean
)

data class MessageRequest(
    val senderId: Long,
    val receiverId: Long,
    val content: String
)

// ── WebRTC Signaling ─────────────────────────────────────────────────────────

data class SignalingMessage(
    val senderId: Long,
    val receiverId: Long,
    val type: String,  // OFFER, ANSWER, ICE_CANDIDATE, HANG_UP, INCOMING_CALL
    val data: String?
)

// ── Call ─────────────────────────────────────────────────────────────────────

data class CallRequest(
    val calleeId: Long,
    val type: String  // AUDIO or VIDEO
)

data class CallHistoryResponse(
    val id: Long,
    val callerId: Long,
    val calleeId: Long,
    val type: String,
    val status: String,  // MISSED, ACCEPTED, REJECTED, ONGOING
    val startedAt: String?,
    val endedAt: String?,
    val durationSeconds: Int?
)

// ── Appointment ───────────────────────────────────────────────────────────────

data class AppointmentRequest(
    val recipientId: Long,
    val meetingTime: String,  // ISO-8601
    val location: String,
    val notes: String?
)

data class AppointmentResponse(
    val id: Long,
    val requesterId: Long,
    val recipientId: Long,
    val requesterName: String?,
    val recipientName: String?,
    val meetingTime: String,
    val location: String,
    val notes: String?,
    val status: String,  // PENDING, CONFIRMED, COMPLETED, CANCELLED
    val createdAt: String?
)

// ── Review ────────────────────────────────────────────────────────────────────

data class ReviewRequest(
    val rating: Int,  // 1..5
    val comment: String?
)

data class ReviewResponse(
    val id: Long,
    val reviewerId: Long,
    val revieweeId: Long,
    val rating: Int,
    val comment: String?,
    val createdAt: String?
)

// ── Group Chat ────────────────────────────────────────────────────────────────

data class GroupChatCreateRequest(
    val name: String,
    val avatarUrl: String?,
    val memberIds: List<Long>
)

data class GroupChatResponse(
    val id: Long,
    val name: String,
    val avatarUrl: String?,
    val creatorId: Long,
    val memberIds: List<Long>,
    val lastMessage: String?,
    val createdAt: String?
)

data class GroupMessageRequest(
    val content: String
)

data class GroupMessageResponse(
    val id: Long,
    val groupId: Long,
    val senderId: Long,
    val senderName: String?,
    val senderAvatarUrl: String?,
    val content: String,
    val sentAt: String?
)

// ── Chat Conversation (for list display) ─────────────────────────────────────

data class ConversationItem(
    val userId: Long,
    val userName: String,
    val userAvatar: String?,
    val lastMessage: String,
    val lastMessageTime: String?,
    val unreadCount: Long,
    val isOnline: Boolean = false
)
