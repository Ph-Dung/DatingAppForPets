package com.petmatch.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.petmatch.mobile.ui.admin.AdminLoginScreen
import com.petmatch.mobile.ui.admin.AdminAccountScreen
import com.petmatch.mobile.ui.admin.AdminDashboardScreen
import com.petmatch.mobile.ui.admin.AdminPetsScreen
import com.petmatch.mobile.ui.admin.AdminReportsScreen
import com.petmatch.mobile.ui.admin.AdminUsersScreen
import com.petmatch.mobile.ui.admin.AdminViewModel
import com.petmatch.mobile.ui.account.AccountScreen
import com.petmatch.mobile.ui.account.ChangePasswordScreen
import com.petmatch.mobile.ui.account.EditUserProfileScreen
import com.petmatch.mobile.ui.account.UserViewModel
import com.petmatch.mobile.ui.auth.AuthViewModel
import com.petmatch.mobile.ui.auth.LoginScreen
import com.petmatch.mobile.ui.auth.RegisterScreen
import com.petmatch.mobile.ui.chat.*
import com.petmatch.mobile.ui.chatbot.AiChatbotScreen
import com.petmatch.mobile.ui.chatbot.ChatbotViewModel
import com.petmatch.mobile.ui.community.AddPostScreen
import com.petmatch.mobile.ui.community.CommunityViewModel
import com.petmatch.mobile.ui.community.CommunityScreen
import com.petmatch.mobile.ui.community.PostManagementScreen
import com.petmatch.mobile.ui.interaction.BlockListScreen
import com.petmatch.mobile.ui.interaction.InteractionViewModel
import com.petmatch.mobile.ui.interaction.ReportScreen
import com.petmatch.mobile.ui.match.*
import com.petmatch.mobile.ui.petprofile.*

object Routes {
    const val LOGIN             = "login"
    const val ADMIN_LOGIN       = "admin/login"
    const val ADMIN_DASHBOARD   = "admin/dashboard"
    const val ADMIN_USERS       = "admin/users"
    const val ADMIN_PETS        = "admin/pets"
    const val ADMIN_REPORTS     = "admin/reports"
    const val ADMIN_ACCOUNT     = "admin/account"
    const val PET_SETUP         = "pet/setup"
    const val PET_EDIT          = "pet/edit"
    const val PET_ME            = "pet/me"
    const val PET_DETAIL        = "pet/detail/{petId}"
    const val PHOTO_MANAGE      = "pet/photos"
    const val VAC_LIST          = "pet/vaccinations"
    const val VAC_FORM          = "pet/vaccinations/form?vacId={vacId}"

    const val MATCH_SWIPE       = "match/swipe"
    const val MATCH_FILTER      = "match/filter"
    const val WHO_LIKED_ME      = "match/liked-me"
    const val MATCHED_LIST      = "match/matched"

    const val CHAT_LIST         = "chat"
    const val CHAT_DETAIL       = "chat/direct/{otherUserId}/{otherUserName}"
    // isCallee: false = caller, true = callee | incomingCallId: 0 nếu là caller
    const val CALL              = "chat/call/{peerId}/{peerName}/{callType}/{isCallee}/{incomingCallId}"
    const val GROUP_CHAT_DETAIL = "chat/group/{groupId}/{groupName}"
    const val GROUP_CHAT_CREATE = "chat/group/create"
    const val APPOINTMENT       = "chat/appointment/{recipientId}/{recipientName}"
    const val APPOINTMENT_LIST  = "chat/appointments/{userId}"
    const val REVIEW            = "chat/review/{revieweeId}/{revieweeName}"
    const val MESSENGER_PROFILE = "chat/profile/{userId}/{userName}"

    const val COMMUNITY         = "community"
    const val POST_MANAGEMENT   = "community/management"
    const val POST_MANAGEMENT_WITH_EDIT = "community/management?editPostId={editPostId}"
    const val POST_ADD          = "community/add"
    const val REGISTER          = "register"
    const val ACCOUNT           = "account"
    const val MY_PET            = "pet/mypet"

    const val REPORT            = "report/{petId}/{ownerId}"
    const val BLOCK_LIST        = "blocks"

    // User account management
    const val EDIT_USER_PROFILE = "account/edit"
    const val CHANGE_PASSWORD   = "account/change-password"

    // AI Chatbot
    const val AI_CHATBOT        = "chatbot"

    fun petDetail(petId: Long) = "pet/detail/$petId"
    fun report(petId: Long, ownerId: Long) = "report/$petId/$ownerId"
    fun vacForm(vacId: Long? = null) = if (vacId != null) "pet/vaccinations/form?vacId=$vacId"
                                       else "pet/vaccinations/form?vacId=-1"
    fun chatDetail(otherUserId: Long, otherUserName: String) = "chat/direct/$otherUserId/$otherUserName"
    fun call(
        peerId: Long,
        peerName: String,
        callType: String,
        isCallee: Boolean = false,
        incomingCallId: Long = 0L
    ) = "chat/call/$peerId/$peerName/$callType/$isCallee/$incomingCallId"
    fun groupChatDetail(groupId: Long, groupName: String) = "chat/group/$groupId/$groupName"
    fun appointment(recipientId: Long, recipientName: String) = "chat/appointment/$recipientId/$recipientName"
    fun appointmentList(userId: Long) = "chat/appointments/$userId"
    fun review(revieweeId: Long, revieweeName: String) = "chat/review/$revieweeId/$revieweeName"
    fun messengerProfile(userId: Long, userName: String) = "chat/profile/$userId/$userName"
    fun postManagement(editPostId: Long? = null) =
        if (editPostId != null) "community/management?editPostId=$editPostId" else POST_MANAGEMENT
}

@Composable
fun PetMatchNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.LOGIN
) {
    val ctx = LocalContext.current
    val petVm: PetProfileViewModel    = viewModel()
    val matchVm: MatchViewModel       = viewModel()
    val interVm: InteractionViewModel = viewModel()
    val authVm: AuthViewModel         = viewModel()
    val userVm: UserViewModel         = viewModel()
    val adminVm: AdminViewModel       = viewModel()
    val chatbotVm: ChatbotViewModel   = viewModel()
    val chatVm: ChatViewModel         = viewModel()
    val communityVm: CommunityViewModel = viewModel()

    // ── Keep signaling alive globally so INCOMING_CALL works on all screens ──
    LaunchedEffect(Unit) {
        chatVm.connectSignaling(ctx)
    }

    // ── Global IncomingCall Overlay (renders on top of NavHost) ─────────────
    val incomingCall by chatVm.incomingCall.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = startDestination) {

        // ── Auth ─────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LaunchedEffect(Unit) {
                chatbotVm.resetConversation()
            }
            LoginScreen(
                vm = authVm,
                onLoginSuccess = { hasPetProfile ->
                    val destination = if (hasPetProfile) Routes.MATCH_SWIPE else Routes.PET_SETUP
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateToAdminLogin = {
                    adminVm.resetAuthState()
                    navController.navigate(Routes.ADMIN_LOGIN)
                }
            )
        }

        composable(Routes.ADMIN_LOGIN) {
            AdminLoginScreen(
                vm = adminVm,
                onSuccess = {
                    navController.navigate(Routes.ADMIN_DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                vm = adminVm,
                onOpenUsers = { navController.navigate(Routes.ADMIN_USERS) },
                onOpenPets = { navController.navigate(Routes.ADMIN_PETS) },
                onOpenReports = { navController.navigate(Routes.ADMIN_REPORTS) }
            )
        }

        composable(Routes.ADMIN_USERS) {
            AdminUsersScreen(vm = adminVm)
        }

        composable(Routes.ADMIN_PETS) {
            AdminPetsScreen(vm = adminVm)
        }

        composable(Routes.ADMIN_REPORTS) {
            AdminReportsScreen(vm = adminVm)
        }

        composable(Routes.ADMIN_ACCOUNT) {
            AdminAccountScreen(
                vm = adminVm,
                onLogout = {
                    authVm.resetState()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                vm = authVm,
                onRegisterSuccess = { hasPetProfile ->
                    // luôn false với tài khoản mới, nhưng để tương thích
                    val dest = if (hasPetProfile) Routes.MATCH_SWIPE else Routes.PET_SETUP
                    navController.navigate(dest) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // ── Pet Profile ──────────────────────────────────
        composable(Routes.PET_SETUP)    { PetProfileSetupScreen(navController, petVm) }
        composable(Routes.PET_EDIT)     { PetProfileEditScreen(navController, petVm) }
        composable(Routes.PET_ME)       { AccountScreen(navController, petVm, authVm, userVm) }  // Bottom tab
        composable(Routes.MY_PET)       { MyProfileScreen(navController, petVm, authVm) }          // Sub-page: chi tiết pet
        composable(Routes.EDIT_USER_PROFILE) { EditUserProfileScreen(navController, userVm) }
        composable(Routes.CHANGE_PASSWORD)   { ChangePasswordScreen(navController, userVm) }
        composable(Routes.PHOTO_MANAGE) { PhotoManageScreen(navController, petVm) }

        composable(
            route = Routes.PET_DETAIL,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { back ->
            val petId = back.arguments!!.getLong("petId")
            PetProfileDetailScreen(navController, petId, petVm, matchVm, interVm)
        }

        // ── Vaccination ──────────────────────────────────────
        composable(Routes.VAC_LIST) { VaccinationListScreen(navController, petVm) }
        composable(
            route = Routes.VAC_FORM,
            arguments = listOf(navArgument("vacId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            val vacId = back.arguments!!.getLong("vacId").let { if (it == -1L) null else it }
            VaccinationFormScreen(navController, petVm, vacId)
        }

        // ── Match ─────────────────────────────────────────────
        composable(Routes.MATCH_SWIPE)  { MatchSwipeScreen(navController, matchVm, petVm) }
        composable(Routes.MATCH_FILTER) { MatchFilterScreen(navController, matchVm) }
        composable(Routes.WHO_LIKED_ME) { WhoLikedMeScreen(navController, matchVm, petVm, chatVm) }
        composable(Routes.MATCHED_LIST) { MatchedListScreen(navController, matchVm, petVm) }

        // ── Chat & Community ──────────────────────────────────
        composable(Routes.CHAT_LIST) { ChatListScreen(navController, chatVm) }
        composable(Routes.COMMUNITY)  { CommunityScreen(navController, communityVm) }
        composable(
            route = Routes.POST_MANAGEMENT_WITH_EDIT,
            arguments = listOf(navArgument("editPostId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            val editPostId = back.arguments?.getLong("editPostId")?.let { if (it == -1L) null else it }
            PostManagementScreen(navController, communityVm, editPostId)
        }
        composable(Routes.POST_ADD) { AddPostScreen(navController, communityVm) }

        // ── Chat Detail (Direct) ─────────────────────────────
        composable(
            route = Routes.CHAT_DETAIL,
            arguments = listOf(
                navArgument("otherUserId")   { type = NavType.LongType },
                navArgument("otherUserName") { type = NavType.StringType }
            )
        ) { back ->
            val otherUserId   = back.arguments!!.getLong("otherUserId")
            val otherUserName = back.arguments!!.getString("otherUserName") ?: ""
            ChatDetailScreen(
                navController = navController,
                otherUserId   = otherUserId,
                otherUserName = otherUserName,
                chatVm        = chatVm
            )
        }

        // ── Call (Audio / Video – Caller AND Callee) ──────────────────────
        composable(
            route = Routes.CALL,
            arguments = listOf(
                navArgument("peerId")         { type = NavType.LongType },
                navArgument("peerName")        { type = NavType.StringType },
                navArgument("callType")        { type = NavType.StringType },
                navArgument("isCallee")        { type = NavType.BoolType; defaultValue = false },
                navArgument("incomingCallId")  { type = NavType.LongType; defaultValue = 0L }
            )
        ) { back ->
            val peerId          = back.arguments!!.getLong("peerId")
            val peerName        = back.arguments!!.getString("peerName") ?: ""
            val callType        = back.arguments!!.getString("callType") ?: "AUDIO"
            val isCallee        = back.arguments!!.getBoolean("isCallee")
            val incomingCallId  = back.arguments!!.getLong("incomingCallId")
            CallScreen(
                navController  = navController,
                peerId         = peerId,
                peerName       = peerName,
                callType       = callType,
                isCallee       = isCallee,
                incomingCallId = incomingCallId,
                chatVm         = chatVm
            )
        }

        // ── Group Chat Detail ─────────────────────────────────
        composable(
            route = Routes.GROUP_CHAT_DETAIL,
            arguments = listOf(
                navArgument("groupId")   { type = NavType.LongType },
                navArgument("groupName") { type = NavType.StringType }
            )
        ) { back ->
            val groupId   = back.arguments!!.getLong("groupId")
            val groupName = back.arguments!!.getString("groupName") ?: ""
            GroupChatDetailScreen(
                navController = navController,
                groupId       = groupId,
                groupName     = groupName,
                chatVm        = chatVm
            )
        }

        // ── Create Group Chat ─────────────────────────────────
        composable(Routes.GROUP_CHAT_CREATE) {
            CreateGroupChatScreen(
                navController = navController,
                chatVm        = chatVm
            )
        }

        // ── Appointment ───────────────────────────────────────
        composable(
            route = Routes.APPOINTMENT,
            arguments = listOf(
                navArgument("recipientId")   { type = NavType.LongType },
                navArgument("recipientName") { type = NavType.StringType }
            )
        ) { back ->
            val recipientId   = back.arguments!!.getLong("recipientId")
            val recipientName = back.arguments!!.getString("recipientName") ?: ""
            AppointmentScreen(
                navController  = navController,
                recipientId    = recipientId,
                recipientName  = recipientName,
                chatVm         = chatVm
            )
        }

        // ── Appointment List ──────────────────────────────────
        composable(
            route = Routes.APPOINTMENT_LIST,
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType }
            )
        ) { back ->
            val userId = back.arguments!!.getLong("userId")
            AppointmentListScreen(
                navController = navController,
                userId        = userId,
                chatVm        = chatVm
            )
        }

        // ── Review After Meeting ──────────────────────────────
        composable(
            route = Routes.REVIEW,
            arguments = listOf(
                navArgument("revieweeId")   { type = NavType.LongType },
                navArgument("revieweeName") { type = NavType.StringType }
            )
        ) { back ->
            val revieweeId   = back.arguments!!.getLong("revieweeId")
            val revieweeName = back.arguments!!.getString("revieweeName") ?: ""
            ReviewScreen(
                navController = navController,
                revieweeId    = revieweeId,
                revieweeName  = revieweeName,
                chatVm        = chatVm
            )
        }

        // ── Messenger Profile ─────────────────────────────────
        composable(
            route = Routes.MESSENGER_PROFILE,
            arguments = listOf(
                navArgument("userId")   { type = NavType.LongType },
                navArgument("userName") { type = NavType.StringType }
            )
        ) { back ->
            val userId   = back.arguments!!.getLong("userId")
            val userName = back.arguments!!.getString("userName") ?: ""
            MessengerProfileScreen(
                navController = navController,
                userId        = userId,
                userName      = userName
            )
        }

        // ── Interaction ───────────────────────────────────────
        composable(
            route = Routes.REPORT,
            arguments = listOf(
                navArgument("petId")   { type = NavType.LongType },
                navArgument("ownerId") { type = NavType.LongType }
            )
        ) { back ->
            val petId   = back.arguments!!.getLong("petId")
            val ownerId = back.arguments!!.getLong("ownerId")
            ReportScreen(navController, petId, ownerId, interVm)
        }
        composable(Routes.BLOCK_LIST) { BlockListScreen(navController) }

        // ── AI Chatbot ────────────────────────────────────────
        composable(Routes.AI_CHATBOT) { AiChatbotScreen(navController, chatbotVm, petVm) }
    } // end NavHost

        // ── IncomingCall Overlay – trên cùng, nhìn thấy từ mọi màn hình ────────
        incomingCall?.let { state ->
            IncomingCallOverlay(
                state    = state,
                onAccept = {
                    chatVm.dismissIncomingCall()
                    navController.navigate(
                        Routes.call(
                            peerId         = state.callerId,
                            peerName       = state.callerName,
                            callType       = state.callType,
                            isCallee       = true,
                            incomingCallId = state.callId
                        )
                    )
                },
                onReject = {
                    chatVm.dismissIncomingCall()
                    chatVm.sendRtcSignal(
                        com.petmatch.mobile.data.model.SignalingMessage(
                            senderId   = 0L,
                            receiverId = state.callerId,
                            type       = "REJECT",
                            data       = null
                        )
                    )
                }
            )
        }
    } // end Box
}
