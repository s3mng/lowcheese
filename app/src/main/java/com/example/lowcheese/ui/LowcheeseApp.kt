package com.example.lowcheese.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lowcheese.LowcheeseApplication
import com.example.lowcheese.auth.NaverLoginScreen
import com.example.lowcheese.ui.home.HomeScreen
import com.example.lowcheese.ui.home.HomeViewModel
import com.example.lowcheese.ui.settings.SettingsScreen

@Composable
fun LowcheeseApp(
    sharedUrl: String?,
    onSharedUrlConsumed: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as LowcheeseApplication

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            viewModel.consumeSharedUrl(sharedUrl)
            onSharedUrlConsumed()
        }
    }

    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate("settings") },
                onLogin = { navController.navigate("login") },
            )
        }
        composable("settings") {
            SettingsScreen(
                isLoggedIn = state.isLoggedIn,
                onBack = { navController.popBackStack() },
                onLogin = { navController.navigate("login") },
                onLogout = viewModel::logout,
            )
        }
        composable("login") {
            NaverLoginScreen(
                store = app.graph.cookies,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
