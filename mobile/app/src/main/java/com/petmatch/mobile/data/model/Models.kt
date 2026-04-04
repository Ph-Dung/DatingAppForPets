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
    val createdAt: String?
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
