package com.petmatch.mobile.data.model

import com.google.gson.annotations.SerializedName

// ── Chat Message ──────────────────────────────────────────────────────────────

data class MessageResponse(
    val id: Long,
    val senderId: Long,
    val receiverId: Long,
    val content: String?,
    val sentAt: String?,
    val isRead: Boolean,
    val type: String = "TEXT",     // TEXT | IMAGE | VOICE
    val mediaUrl: String? = null
)

data class MessageRequest(
    val senderId: Long,
    val receiverId: Long,
    val content: String?,
    val type: String = "TEXT",
    val mediaUrl: String? = null
)

// ── Conversation (match-gated list) ──────────────────────────────────────────

data class ConversationItem(
    @SerializedName("matchedUserId")
    val userId: Long,
    val userName: String,
    @SerializedName("avatarUrl")
    val userAvatar: String?,
    val lastMessage: String?,
    val lastMessageTime: String?,
    val unreadCount: Long,
    val isOnline: Boolean = false,
    val isMuted: Boolean = false,
    val nickname: String? = null
)

// ── Block Status ──────────────────────────────────────────────────────────────

data class BlockStatus(
    val iBlockedThem: Boolean,
    val theyBlockedMe: Boolean,
    val myBlockLevel: String  // MESSAGE | CALL | ALL | ""
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
    val status: String,
    val startedAt: String?,
    val endedAt: String?,
    val durationSeconds: Int?
)

// ── Appointment ───────────────────────────────────────────────────────────────

data class AppointmentRequest(
    val recipientId: Long,
    val meetingTime: String,
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
    val status: String,
    val createdAt: String?
)

// ── Review ────────────────────────────────────────────────────────────────────

data class ReviewRequest(
    val rating: Int,
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
    @SerializedName("createdById") val creatorId: Long,
    val members: List<GroupMemberResponse> = emptyList(),
    val lastMessage: String? = null,
    val createdAt: String?
) {
    val memberIds: List<Long> get() = members.map { it.userId }
}

data class GroupMemberResponse(
    val userId: Long,
    val fullName: String,
    val avatarUrl: String?,
    val role: String,
    val joinedAt: String?
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

// ── Upload Media Response ─────────────────────────────────────────────────────

data class MediaUploadResponse(
    val mediaUrl: String,
    val type: String  // IMAGE | VOICE
)
