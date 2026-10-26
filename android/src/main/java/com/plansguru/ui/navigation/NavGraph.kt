package com.plansguru.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plansguru.ui.friends.FriendsScreen
import com.plansguru.ui.friends.FriendsViewModel
import com.plansguru.ui.planning.CoordinationScreen
import com.plansguru.ui.planning.NewPlanScreen
import com.plansguru.ui.planning.PlanningViewModel
import com.plansguru.ui.planning.RecommendationsScreen
import com.plansguru.ui.preferences.PreferencesScreen
import com.plansguru.ui.preferences.PreferencesViewModel
import com.plansguru.ui.profile.ProfileScreen
import com.plansguru.ui.profile.ProfileViewModel

sealed class Screen(val route: String) {
    object Profile : Screen("profile")
    object Preferences : Screen("preferences")
    object Friends : Screen("friends")
    object NewPlan : Screen("new_plan")
    object Coordination : Screen("coordination")
    object Recommendations : Screen("recommendations")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val planningViewModel: PlanningViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.Profile.route) {
        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                viewModel = viewModel,
                onNavigatePreferences = { navController.navigate(Screen.Preferences.route) },
                onNavigateFriends = { navController.navigate(Screen.Friends.route) },
                onNavigateNewPlan = { navController.navigate(Screen.NewPlan.route) }
            )
        }
        composable(Screen.Preferences.route) {
            val viewModel: PreferencesViewModel = viewModel()
            PreferencesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Friends.route) {
            val viewModel: FriendsViewModel = viewModel()
            FriendsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.NewPlan.route) {
            NewPlanScreen(
                onStartCoordination = { _ ->
                    navController.navigate(Screen.Coordination.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Coordination.route) {
            CoordinationScreen(
                viewModel = planningViewModel,
                onNavigateRecommendations = {
                    navController.navigate(Screen.Recommendations.route) {
                        popUpTo(Screen.NewPlan.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Recommendations.route) {
            RecommendationsScreen(
                viewModel = planningViewModel,
                onFinish = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
