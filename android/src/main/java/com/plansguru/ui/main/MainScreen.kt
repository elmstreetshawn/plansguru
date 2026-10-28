package com.plansguru.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.plansguru.ui.people.GroupSetupScreen
import com.plansguru.ui.people.PeopleScreen
import com.plansguru.ui.places.PlacesScreen
import com.plansguru.ui.planning.GuruPlanningScreen
import com.plansguru.ui.planning.PlanningViewModel
import com.plansguru.ui.planning.PlansHistoryScreen
import com.plansguru.ui.planning.RecommendationsScreen
import com.plansguru.ui.preferences.AgentChatBottomSheet
import com.plansguru.ui.preferences.PreferencesScreen

sealed class BottomTab(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object People : BottomTab("people", "People", Icons.Default.Person)
    object Places : BottomTab("places", "Places", Icons.Default.LocationOn)
    object Preferences : BottomTab("preferences", "Preferences", Icons.Default.Favorite)
    object Plans : BottomTab("plans", "Plans", Icons.Default.DateRange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialShowChat: Boolean = false
) {
    val navController = rememberNavController()
    val planningViewModel: androidx.lifecycle.ViewModelStoreOwner = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current!!
    val sharedPlanningViewModel: PlanningViewModel = androidx.lifecycle.viewmodel.compose.viewModel(planningViewModel)

    val items = listOf(
        BottomTab.People,
        BottomTab.Places,
        BottomTab.Preferences,
        BottomTab.Plans
    )

    var currentGroupTitle by remember { mutableStateOf("Dinner Group") }
    var showGlobalChatSheet by remember { mutableStateOf(initialShowChat) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("plansguru_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("pending_navigate_to_plans", false)) {
            prefs.edit().remove("pending_navigate_to_plans").apply()
            navController.navigate(BottomTab.Plans.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(BottomTab.People.route) {
                PeopleScreen(
                    onProceedToGroup = { members ->
                        val membersParam = members.joinToString(",")
                        navController.navigate("group_setup?members=$membersParam")
                    },
                    onOpenGuruChat = { showGlobalChatSheet = true }
                )
            }
            composable(BottomTab.Places.route) {
                PlacesScreen(
                    onOpenGuruChat = { showGlobalChatSheet = true }
                )
            }
            composable("group_setup?members={members}") { backStackEntry ->
                val membersStr = backStackEntry.arguments?.getString("members") ?: ""
                val members = membersStr.split(",").filter { it.isNotBlank() }
                GroupSetupScreen(
                    initialMembers = members,
                    onBack = { navController.popBackStack() },
                    onAskGuru = { groupName, selectedList ->
                        currentGroupTitle = groupName
                        val selectedStr = selectedList.joinToString(",")
                        navController.navigate("guru_planning?members=$selectedStr")
                    }
                )
            }
            composable("guru_planning?members={members}") { backStackEntry ->
                val membersStr = backStackEntry.arguments?.getString("members") ?: "Maya,Jo,Priya"
                val members = membersStr.split(",").filter { it.isNotBlank() }
                GuruPlanningScreen(
                    groupTitle = currentGroupTitle,
                    members = members,
                    viewModel = sharedPlanningViewModel,
                    onPlanningComplete = {
                        navController.navigate("recommendations") {
                            popUpTo(BottomTab.People.route) { inclusive = false }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(BottomTab.Preferences.route) {
                PreferencesScreen()
            }
            composable(BottomTab.Plans.route) {
                PlansHistoryScreen(
                    onCreateNewPlan = {
                        navController.navigate("group_setup?members=")
                    },
                    onOpenPlanChat = { planId, title ->
                        navController.navigate("recommendations?planId=$planId&title=$title")
                    },
                    onOpenGuruChat = { showGlobalChatSheet = true }
                )
            }
            composable("recommendations?planId={planId}&title={title}") { backStackEntry ->
                val pId = backStackEntry.arguments?.getString("planId") ?: "group_discussion_session"
                val t = backStackEntry.arguments?.getString("title") ?: "Dinner Plan"
                RecommendationsScreen(
                    viewModel = sharedPlanningViewModel,
                    planId = pId,
                    groupTitle = t,
                    onFinish = {
                        navController.navigate(BottomTab.Plans.route) {
                            popUpTo(BottomTab.People.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    if (showGlobalChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGlobalChatSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AgentChatBottomSheet()
        }
    }
}
