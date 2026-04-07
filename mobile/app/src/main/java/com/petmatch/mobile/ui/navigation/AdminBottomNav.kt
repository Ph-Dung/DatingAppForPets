package com.petmatch.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

data class AdminBottomNavItem(
    val route: String,
    val labelText: String,
    val iconFilled: ImageVector,
    val iconOutline: ImageVector
)

val adminBottomNavItems = listOf(
    AdminBottomNavItem(
        route = Routes.ADMIN_DASHBOARD,
        labelText = "Dashboard",
        iconFilled = Icons.Default.Dashboard,
        iconOutline = Icons.Default.Dashboard
    ),
    AdminBottomNavItem(
        route = Routes.ADMIN_USERS,
        labelText = "Users",
        iconFilled = Icons.Default.SupervisedUserCircle,
        iconOutline = Icons.Default.SupervisedUserCircle
    ),
    AdminBottomNavItem(
        route = Routes.ADMIN_PETS,
        labelText = "Pets",
        iconFilled = Icons.Default.Pets,
        iconOutline = Icons.Default.Pets
    ),
    AdminBottomNavItem(
        route = Routes.ADMIN_REPORTS,
        labelText = "Reports",
        iconFilled = Icons.Default.Report,
        iconOutline = Icons.Default.Report
    ),
    AdminBottomNavItem(
        route = Routes.ADMIN_ACCOUNT,
        labelText = "Account",
        iconFilled = Icons.Default.AdminPanelSettings,
        iconOutline = Icons.Default.AdminPanelSettings
    )
)

val adminBottomNavRoutes = setOf(
    Routes.ADMIN_DASHBOARD,
    Routes.ADMIN_USERS,
    Routes.ADMIN_PETS,
    Routes.ADMIN_REPORTS,
    Routes.ADMIN_ACCOUNT
)

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
                label = { Text(item.labelText) }
            )
        }
    }
}