package com.petmatch.mobile.data.model

data class PetProfileResponse(
    val id: Long,
    val ownerId: Long,
    val ownerName: String?,
    val name: String,
    val species: String,
    val breed: String?,
    val gender: String?,
    val dateOfBirth: String?,   // "yyyy-MM-dd"
    val age: Int?,
    val weightKg: Double?,
    val color: String?,
    val size: String?,
    val reproductiveStatus: String?,
    val isVaccinated: Boolean,
    val lastVaccineDate: String?,
    val vaccinationCount: Int,
    val healthStatus: String?,
    val healthNotes: String?,
    val personalityTags: String?,   // JSON array string
    val lookingFor: String?,
    val notes: String?,
    val isHidden: Boolean,
    val avatarUrl: String?,
    val photoUrls: List<String>,
    val createdAt: String?,
    
    // Geolocation
    val distanceKm: Double? = null,
    val ownerAddress: String? = null
)

data class PetProfileRequest(
    val name: String,
    val species: String,
    val breed: String?,
    val gender: String,
    val dateOfBirth: String?,
    val weightKg: Double?,
    val color: String?,
    val size: String?,
    val reproductiveStatus: String,
    val isVaccinated: Boolean,
    val lastVaccineDate: String?,
    val healthStatus: String,
    val healthNotes: String?,
    val personalityTags: String?,
    val lookingFor: String,
    val notes: String?
)

data class MatchRequestResponse(
    val id: Long,
    val senderPetId: Long,
    val senderPetName: String?,
    val senderPetAvatarUrl: String?,
    val receiverPetId: Long,
    val receiverPetName: String?,
    val receiverPetAvatarUrl: String?,
    val status: String,
    val isSuperLike: Boolean,
    val createdAt: String?,
    val canOpenConversation: Boolean
)

data class SendMatchRequest(
    val receiverPetId: Long,
    val isSuperLike: Boolean = false
)

data class SuperLikeStatusResponse(
    val canSuperLike: Boolean,
    val nextResetAt: String?,
    val usedToday: Int
)

data class VaccinationResponse(
    val id: Long,
    val vaccineName: String,
    val vaccinatedDate: String,
    val nextDueDate: String?,
    val clinicName: String?,
    val notes: String?,
    val createdAt: String?
)

data class VaccinationRequest(
    val vaccineName: String,
    val vaccinatedDate: String,
    val nextDueDate: String?,
    val clinicName: String?,
    val notes: String?
)

data class ReportRequest(
    val targetId: Long,
    val targetType: String,
    val reason: String
)

data class BlockUserRequest(
    val targetUserId: Long
)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val last: Boolean
)

data class AuthResponse(
    val token: String,
    val userId: Long?,
    val email: String?,
    val fullName: String?,
    val hasPetProfile: Boolean = false
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val phone: String? = null
)

// ── User Account ──────────────────────────────────────────
data class UserResponse(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String?,
    val address: String?,
    val avatarUrl: String?,
    val createdAt: String?
)

data class UpdateUserRequest(
    val fullName: String,
    val phone: String?,
    val address: String?
)

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

// ── AI Chatbot ────────────────────────────────────────────
data class ChatMessage(
    val role: String,   // "user" or "assistant"
    val content: String
)

data class ChatbotRequest(
    val messages: List<ChatMessage>
)

data class ChatbotResponse(
    val reply: String,
    val isReadyToSuggest: Boolean,
    val suggestions: List<PetProfileResponse> = emptyList()
)

data class CommunityPostResponse(
    val id: Long,
    val content: String,
    val imageUrl: String?,
    val location: String?,
    val ownerName: String,
    val ownerAvatar: String?,
    val ownerId: Long,
    val createdAt: String?,
    val likesCount: Long,
    val commentsCount: Long,
    val isLiked: Boolean
)

data class CommunityCreatePostRequest(
    val content: String,
    val imageUrl: String?,
    val location: String?
)

data class CommunityUpdatePostRequest(
    val content: String,
    val imageUrl: String?,
    val location: String?
)

data class CommunityCreateCommentRequest(
    val content: String
)

data class CommunityCommentResponse(
    val id: Long,
    val content: String,
    val userId: Long,
    val userName: String,
    val userAvatar: String?,
    val createdAt: String?,
    val postId: Long,
    val parentCommentId: Long?,
    val replies: List<CommunityCommentResponse> = emptyList()
)

data class CommunityReportRequest(
    val targetId: Long,
    val targetType: String,
    val reason: String
)

data class AdminDashboardResponse(
    val totalUsers: Long,
    val lockedUsers: Long,
    val totalPets: Long,
    val hiddenPets: Long,
    val pendingReports: Long
)

data class AdminUserItemResponse(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String?,
    val avatarUrl: String?,
    val locked: Boolean,
    val warned: Boolean,
    val warningCount: Int,
    val lastWarnedAt: String?,
    val createdAt: String?
)

data class AdminPetItemResponse(
    val id: Long,
    val ownerId: Long,
    val ownerName: String,
    val name: String,
    val species: String,
    val avatarUrl: String?,
    val hidden: Boolean,
    val createdAt: String?
)

data class AdminReportItemResponse(
    val id: Long,
    val reporterId: Long,
    val reporterName: String,
    val targetType: String,
    val targetId: Long,
    val reason: String,
    val status: String,
    val createdAt: String?,
    val handledById: Long?,
    val handledByName: String?,
    val action: String?,
    val adminNote: String?,
    val handledAt: String?
)

data class AdminHandleReportRequest(
    val action: String,
    val note: String?
)

data class UpdateLocationRequest(
    val latitude: Double,
    val longitude: Double
)
