package com.petmatch.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.petmatch.mobile.ui.theme.PrimaryPink

data class BottomNavItem(
    val route: String,
    val labelText: String,
    val iconFilled: ImageVector,
    val iconOutline: ImageVector   // dùng icon outline nếu có, hoặc dùng cùng icon + màu khác
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Routes.MATCH_SWIPE,
        labelText = "Khám phá",
        iconFilled  = Icons.Default.Pets,
        iconOutline = Icons.Default.Pets
    ),
    BottomNavItem(
        route = Routes.WHO_LIKED_ME,
        labelText = "Đã thích",
        iconFilled  = Icons.Default.Favorite,
        iconOutline = Icons.Default.FavoriteBorder
    ),
    BottomNavItem(
        route = Routes.CHAT_LIST,
        labelText = "Chat",
        iconFilled  = Icons.Default.Chat,
        iconOutline = Icons.Default.ChatBubbleOutline
    ),
    BottomNavItem(
        route = Routes.COMMUNITY,
        labelText = "Cộng đồng",
        iconFilled  = Icons.Default.Groups,
        iconOutline = Icons.Default.Groups
    ),
    BottomNavItem(
        route = Routes.PET_ME,
        labelText = "Hồ sơ",
        iconFilled  = Icons.Default.AccountCircle,
        iconOutline = Icons.Default.AccountCircle
    )
)

val adminBottomNavItems = listOf(
    BottomNavItem(
        route = Routes.ADMIN_DASHBOARD,
        labelText = "Tổng quan",
        iconFilled = Icons.Default.Home,
        iconOutline = Icons.Default.Home
    ),
    BottomNavItem(
        route = Routes.ADMIN_USERS,
        labelText = "Người dùng",
        iconFilled = Icons.Default.SupervisedUserCircle,
        iconOutline = Icons.Default.SupervisedUserCircle
    ),
    BottomNavItem(
        route = Routes.ADMIN_PETS,
        labelText = "Thú cưng",
        iconFilled = Icons.Default.Pets,
        iconOutline = Icons.Default.Pets
    ),
    BottomNavItem(
        route = Routes.ADMIN_REPORTS,
        labelText = "Báo cáo",
        iconFilled = Icons.Default.Report,
        iconOutline = Icons.Default.Report
    ),
    BottomNavItem(
        route = Routes.ADMIN_ACCOUNT,
        labelText = "Tài khoản",
        iconFilled = Icons.Default.AccountCircle,
        iconOutline = Icons.Default.AccountCircle
    )
)

/** Routes nào thì HIỆN bottom nav */
val bottomNavRoutes = setOf(
    Routes.MATCH_SWIPE,
    Routes.WHO_LIKED_ME,
    Routes.CHAT_LIST,
    Routes.COMMUNITY,
    Routes.PET_ME
)

val adminBottomNavRoutes = setOf(
    Routes.ADMIN_DASHBOARD,
    Routes.ADMIN_USERS,
    Routes.ADMIN_PETS,
    Routes.ADMIN_REPORTS,
    Routes.ADMIN_ACCOUNT
)

@Composable
fun PetMatchBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.iconFilled else item.iconOutline,
                        contentDescription = item.labelText
                    )
                },
                label = { Text(item.labelText) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = PrimaryPink,
                    selectedTextColor   = PrimaryPink,
                    indicatorColor      = PrimaryPink.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun AdminBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        adminBottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.ADMIN_DASHBOARD)
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.iconFilled else item.iconOutline,
                        contentDescription = item.labelText
                    )
                },
                label = { Text(item.labelText) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryPink,
                    selectedTextColor = PrimaryPink,
                    indicatorColor = PrimaryPink.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
