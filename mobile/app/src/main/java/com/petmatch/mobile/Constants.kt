package com.petmatch.mobile

object Constants {
    // Đổi thành IP máy tính nếu test trên thiết bị thật
    // Emulator: 10.0.2.2 = localhost của máy host
    const val BASE_URL = "http://10.0.2.2:8080/"
    const val TOKEN_KEY = "auth_token"
    const val DATASTORE_NAME = "petmatch_prefs"

    // Personality tags predefined (tiếng Việt)
    val PERSONALITY_TAGS = listOf(
        "Năng động", "Thân thiện", "Lười biếng", "Tinh nghịch", "Ngoan ngoãn",
        "Hung hăng", "Nhút nhát", "Thích ôm", "Độc lập", "Ham ăn",
        "Thích chơi", "Yên lặng", "Thông minh", "Tò mò", "Bảo vệ chủ"
    )

    val SPECIES_LIST = listOf("Chó", "Mèo", "Thỏ", "Hamster", "Khác")

    val LOOKING_FOR_LABELS = mapOf(
        "BREEDING" to "Phối giống",
        "FRIENDSHIP" to "Kết bạn",
        "PLAY" to "Vui chơi"
    )

    val HEALTH_STATUS_LABELS = mapOf(
        "HEALTHY" to "Khỏe mạnh",
        "SICK" to "Đang bệnh",
        "RECOVERING" to "Đang hồi phục",
        "CHRONIC" to "Bệnh mãn tính"
    )

    val GENDER_LABELS = mapOf(
        "MALE" to "Đực",
        "FEMALE" to "Cái",
        "OTHER" to "Khác"
    )

    val REPRODUCTIVE_STATUS_LABELS = mapOf(
        "INTACT" to "Còn nguyên vẹn",
        "NEUTERED" to "Đã triệt sản",
        "SPAYED" to "Đã thiến"
    )

    val SIZE_LABELS = mapOf(
        "small" to "Nhỏ",
        "medium" to "Vừa",
        "large" to "Lớn"
    )
}
