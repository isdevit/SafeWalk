package com.example.safewalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.safewalk.auth.LoginScreen
import com.example.safewalk.auth.RegisterScreen
import com.example.safewalk.dashboard.DashboardScreen
import com.example.safewalk.ui.theme.SafeWalkTheme
import com.example.safewalk.utils.PreferencesManager
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SafeWalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                        "dashboard"
                    } else {
                        "login"
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateToDashboard = { navController.navigate("dashboard") }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onNavigateToLogin = { navController.navigate("login") },
                                onNavigateToDashboard = { navController.navigate("dashboard") }
                            )
                        }
                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo(0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
