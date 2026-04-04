package com.petmatch.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.petmatch.mobile.ui.account.AccountScreen
import com.petmatch.mobile.ui.auth.AuthViewModel
import com.petmatch.mobile.ui.auth.LoginScreen
import com.petmatch.mobile.ui.auth.RegisterScreen
import com.petmatch.mobile.ui.chat.ChatListScreen
import com.petmatch.mobile.ui.community.CommunityScreen
import com.petmatch.mobile.ui.interaction.BlockListScreen
import com.petmatch.mobile.ui.interaction.InteractionViewModel
import com.petmatch.mobile.ui.interaction.ReportScreen
import com.petmatch.mobile.ui.match.*
import com.petmatch.mobile.ui.petprofile.*

object Routes {
    const val LOGIN        = "login"
    const val PET_SETUP    = "pet/setup"
    const val PET_EDIT     = "pet/edit"
    const val PET_ME       = "pet/me"
    const val PET_DETAIL   = "pet/detail/{petId}"
    const val PHOTO_MANAGE = "pet/photos"
    const val VAC_LIST     = "pet/vaccinations"
    const val VAC_FORM     = "pet/vaccinations/form?vacId={vacId}"

    const val MATCH_SWIPE  = "match/swipe"
    const val MATCH_FILTER = "match/filter"
    const val WHO_LIKED_ME = "match/liked-me"
    const val MATCHED_LIST = "match/matched"

    const val CHAT_LIST    = "chat"
    const val COMMUNITY    = "community"
    const val REGISTER     = "register"
    const val ACCOUNT      = "account"   // Tab bottom nav → AccountScreen
    const val MY_PET       = "pet/mypet" // Sub-page: chi tiết pet của tôi

    const val REPORT       = "report/{petId}/{ownerId}"
    const val BLOCK_LIST   = "blocks"

    fun petDetail(petId: Long) = "pet/detail/$petId"
    fun report(petId: Long, ownerId: Long) = "report/$petId/$ownerId"
    fun vacForm(vacId: Long? = null) = if (vacId != null) "pet/vaccinations/form?vacId=$vacId"
                                       else "pet/vaccinations/form?vacId=-1"
}

@Composable
fun PetMatchNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.LOGIN
) {
    val petVm: PetProfileViewModel    = viewModel()
    val matchVm: MatchViewModel       = viewModel()
    val interVm: InteractionViewModel = viewModel()
    val authVm: AuthViewModel         = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Auth ─────────────────────────────────────────────
        composable(Routes.LOGIN) {
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
        composable(Routes.PET_ME)       { AccountScreen(navController, petVm, authVm) }   // Bottom tab
        composable(Routes.MY_PET)       { MyProfileScreen(navController, petVm, authVm) } // Sub-page: chi tiết pet
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
        composable(Routes.WHO_LIKED_ME) { WhoLikedMeScreen(navController, matchVm, petVm) }
        composable(Routes.MATCHED_LIST) { MatchedListScreen(navController, matchVm) }

        // ── Chat & Community ──────────────────────────────────
        composable(Routes.CHAT_LIST) { ChatListScreen(navController) }
        composable(Routes.COMMUNITY)  { CommunityScreen(navController) }

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
    }
}
