package com.petmatch.mobile.data.api

import com.petmatch.mobile.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface PetApi {

    @POST("api/pets")
    suspend fun createProfile(@Body req: PetProfileRequest): Response<PetProfileResponse>

    @PUT("api/pets")
    suspend fun updateProfile(@Body req: PetProfileRequest): Response<PetProfileResponse>

    @GET("api/pets/me")
    suspend fun getMyProfile(): Response<PetProfileResponse>

    @GET("api/pets/{petId}")
    suspend fun getPetById(@Path("petId") petId: Long): Response<PetProfileResponse>

    @PATCH("api/pets/toggle-hidden")
    suspend fun toggleHidden(): Response<Unit>

    @GET("api/pets/suggestions")
    suspend fun getSuggestions(
        @Query("page")  page: Int = 0,
        @Query("size")  size: Int = 10,
        @Query("smart") smart: Boolean = false
    ): Response<PageResponse<PetProfileResponse>>

    @GET("api/pets/search")
    suspend fun search(
        @Query("species") species: String? = null,
        @Query("breed") breed: String? = null,
        @Query("gender") gender: String? = null,
        @Query("lookingFor") lookingFor: String? = null,
        @Query("healthStatus") healthStatus: String? = null,
        @Query("minWeight") minWeight: Double? = null,
        @Query("maxWeight") maxWeight: Double? = null,
        @Query("minAge") minAge: Int? = null,
        @Query("maxAge") maxAge: Int? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PageResponse<PetProfileResponse>>

    // Vaccinations
    @GET("api/pets/vaccinations")
    suspend fun getVaccinations(): Response<List<VaccinationResponse>>

    @POST("api/pets/vaccinations")
    suspend fun addVaccination(@Body req: VaccinationRequest): Response<VaccinationResponse>

    @PUT("api/pets/vaccinations/{vacId}")
    suspend fun updateVaccination(
        @Path("vacId") vacId: Long,
        @Body req: VaccinationRequest
    ): Response<VaccinationResponse>

    @DELETE("api/pets/vaccinations/{vacId}")
    suspend fun deleteVaccination(@Path("vacId") vacId: Long): Response<Unit>

    // Photos
    @Multipart
    @POST("api/pets/photos")
    suspend fun addPhoto(
        @Part file: MultipartBody.Part,
        @Query("setAsAvatar") setAsAvatar: Boolean = false
    ): Response<Any>

    @DELETE("api/pets/photos/{photoId}")
    suspend fun deletePhoto(@Path("photoId") photoId: Long): Response<Unit>
}

interface MatchApi {

    @POST("api/matches")
    suspend fun sendMatchRequest(@Body req: SendMatchRequest): Response<MatchRequestResponse>

    @GET("api/matches/super-like-status")
    suspend fun getSuperLikeStatus(): Response<SuperLikeStatusResponse>

    @GET("api/matches/sent")
    suspend fun getSentRequests(): Response<List<MatchRequestResponse>>

    @GET("api/matches/received")
    suspend fun getWhoLikedMe(): Response<List<MatchRequestResponse>>

    @GET("api/matches/matched")
    suspend fun getMatchedList(): Response<List<MatchRequestResponse>>

    @PATCH("api/matches/{matchId}/respond")
    suspend fun respondToMatch(
        @Path("matchId") matchId: Long,
        @Query("status") status: String
    ): Response<MatchRequestResponse>
}

interface InteractionApi {

    @POST("api/interactions/blocks/{targetUserId}")
    suspend fun blockUser(
        @Path("targetUserId") targetUserId: Long,
        @Query("level") level: String = "ALL"
    ): Response<Any>

    @DELETE("api/interactions/blocks/{targetUserId}")
    suspend fun unblockUser(@Path("targetUserId") targetUserId: Long): Response<Unit>

    @GET("api/interactions/blocks")
    suspend fun getMyBlocks(): Response<List<com.petmatch.mobile.data.model.BlockResponse>>

    @POST("api/interactions/reports")
    suspend fun submitReport(@Body req: ReportRequest): Response<Any>
}

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<AuthResponse>
}

interface AdminAuthApi {
    @POST("api/auth/admin/login")
    suspend fun adminLogin(@Body req: LoginRequest): Response<AuthResponse>
}

interface AdminApi {
    @GET("api/admin/dashboard")
    suspend fun getDashboard(): Response<AdminDashboardResponse>

    @GET("api/admin/users")
    suspend fun getUsers(
        @Query("query") query: String? = null,
        @Query("locked") locked: Boolean? = null,
        @Query("warned") warned: Boolean? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<AdminUserItemResponse>>

    @PATCH("api/admin/users/{userId}/lock")
    suspend fun setUserLocked(
        @Path("userId") userId: Long,
        @Query("locked") locked: Boolean
    ): Response<AdminUserItemResponse>

    @POST("api/admin/users/{userId}/warn")
    suspend fun warnUser(
        @Path("userId") userId: Long,
        @Query("note") note: String? = null
    ): Response<AdminUserItemResponse>

    @GET("api/admin/pets")
    suspend fun getPets(
        @Query("query") query: String? = null,
        @Query("hidden") hidden: Boolean? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<AdminPetItemResponse>>

    @PATCH("api/admin/pets/{petId}/hidden")
    suspend fun setPetHidden(
        @Path("petId") petId: Long,
        @Query("hidden") hidden: Boolean
    ): Response<AdminPetItemResponse>

    @GET("api/admin/reports")
    suspend fun getReports(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<AdminReportItemResponse>>

    @POST("api/admin/reports/{reportId}/handle")
    suspend fun handleReport(
        @Path("reportId") reportId: Long,
        @Body req: AdminHandleReportRequest
    ): Response<AdminReportItemResponse>
}

interface UserApi {

    @GET("api/users/me")
    suspend fun getMyInfo(): Response<UserResponse>

    @PUT("api/users/me")
    suspend fun updateMyInfo(@Body req: UpdateUserRequest): Response<UserResponse>

    @PUT("api/users/me/password")
    suspend fun changePassword(@Body req: ChangePasswordRequest): Response<Unit>

    @Multipart
    @POST("api/users/me/avatar")
    suspend fun updateAvatar(
        @Part file: MultipartBody.Part
    ): Response<UserResponse>
}

interface ChatbotApi {

    @POST("api/chatbot/message")
    suspend fun sendMessage(@Body req: ChatbotRequest): Response<ChatbotResponse>
}

