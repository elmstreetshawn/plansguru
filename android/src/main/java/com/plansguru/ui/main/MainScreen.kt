package com.plansguru.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.plansguru.ui.friends.FriendsScreen
import com.plansguru.ui.friends.FriendsViewModel
import com.plansguru.ui.planning.CoordinationScreen
import com.plansguru.ui.planning.NewPlanScreen
import com.plansguru.ui.planning.PlanningViewModel
import com.plansguru.ui.planning.RecommendationsScreen
import com.plansguru.ui.preferences.PreferencesScreen
import com.plansguru.ui.preferences.PreferencesViewModel

sealed class BottomTab(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object People : BottomTab("people", "Your people", Icons.Default.Person)
    object Preferences : BottomTab("preferences", "Your preferences", Icons.Default.Favorite)
    object Plans : BottomTab("plans", "One plan", Icons.Default.DateRange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val planningViewModel: androidx.lifecycle.ViewModelStoreOwner = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current!!
    val sharedPlanningViewModel: PlanningViewModel = androidx.lifecycle.viewmodel.compose.viewModel(planningViewModel)

    val items = listOf(
        BottomTab.People,
        BottomTab.Preferences,
        BottomTab.Plans
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.People.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomTab.People.route) {
                val viewModel: FriendsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                FriendsScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
            composable(BottomTab.Preferences.route) {
                val prefVM: PreferencesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                PreferencesScreen(
                    viewModel = prefVM,
                    onBack = {}
                )
            }
            composable(BottomTab.Plans.route) {
                NewPlanScreen(
                    onStartCoordination = { _ ->
                        navController.navigate("coordination")
                    },
                    onBack = {}
                )
            }
            composable("coordination") {
                CoordinationScreen(
                    viewModel = sharedPlanningViewModel,
                    onNavigateRecommendations = {
                        navController.navigate("recommendations") {
                            popUpTo("plans") { inclusive = false }
                        }
                    }
                )
            }
            composable("recommendations") {
                RecommendationsScreen(
                    viewModel = sharedPlanningViewModel,
                    onFinish = {
                        navController.navigate(BottomTab.Plans.route) {
                            popUpTo(BottomTab.Plans.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
