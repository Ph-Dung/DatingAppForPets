package com.petmatch.mobile.data.model

data class BlockResponse(
    val id: Long,
    val blockedUserId: Long,
    val blockedUserName: String,
    val blockedUserAvatarUrl: String?,
    val createdAt: String
)
