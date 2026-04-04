package com.petmatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.petmatch.mobile.ui.navigation.PetMatchBottomNav
import com.petmatch.mobile.ui.navigation.PetMatchNavGraph
import com.petmatch.mobile.ui.navigation.Routes
import com.petmatch.mobile.ui.navigation.bottomNavRoutes
import com.petmatch.mobile.ui.theme.PetMatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetMatchTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute in bottomNavRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            PetMatchBottomNav(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    // Bọc NavHost trong Box với padding → inner screens không bị che
                    Box(modifier = Modifier.padding(innerPadding)) {
                        PetMatchNavGraph(
                            navController = navController,
                            startDestination = Routes.LOGIN
                        )
                    }
                }
            }
        }
    }
}