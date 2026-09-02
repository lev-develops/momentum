package com.momentum.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.momentum.app.AppContainer
import com.momentum.app.ui.account.AccountScreen
import com.momentum.app.ui.addedit.AddEditHabitScreen
import com.momentum.app.ui.detail.HabitDetailScreen
import com.momentum.app.ui.insights.InsightsScreen
import com.momentum.app.ui.theme.LocalMomentumColors
import com.momentum.app.ui.today.TodayScreen
import com.momentum.app.ui.welcome.WelcomeScreen

object Routes {
    const val WELCOME = "welcome"
    const val TODAY = "today"
    const val INSIGHTS = "insights"
    const val DETAIL = "detail/{habitId}"
    const val ADD_EDIT = "addEdit?habitId={habitId}"
    const val ACCOUNT = "account"
    const val ARG_HABIT_ID = "habitId"

    fun detail(habitId: Long) = "detail/$habitId"
    fun addEdit(habitId: Long? = null) = if (habitId == null) "addEdit" else "addEdit?habitId=$habitId"
}

private val TopLevelDestinations = setOf(Routes.TODAY, Routes.INSIGHTS)

@Composable
fun MomentumNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val colors = LocalMomentumColors.current

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            if (currentRoute in TopLevelDestinations) {
                MomentumBottomBar(navController, currentRoute)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.WELCOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.WELCOME) {
                WelcomeScreen(
                    container = container,
                    onDone = {
                        navController.navigate(Routes.TODAY) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.TODAY) {
                TodayScreen(
                    container = container,
                    onHabitClick = { habitId -> navController.navigate(Routes.detail(habitId)) },
                    onAddHabit = { navController.navigate(Routes.addEdit()) },
                    onAccount = { navController.navigate(Routes.ACCOUNT) },
                )
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.INSIGHTS) {
                InsightsScreen(container = container, onAddHabit = { navController.navigate(Routes.addEdit()) })
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(Routes.ARG_HABIT_ID) { type = androidx.navigation.NavType.LongType }),
            ) { entry ->
                val habitId = entry.arguments?.getLong(Routes.ARG_HABIT_ID) ?: -1L
                HabitDetailScreen(
                    container = container,
                    habitId = habitId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.addEdit(habitId)) },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.ADD_EDIT,
                arguments = listOf(
                    navArgument(Routes.ARG_HABIT_ID) {
                        type = androidx.navigation.NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                val habitId = entry.arguments?.getLong(Routes.ARG_HABIT_ID) ?: -1L
                AddEditHabitScreen(
                    container = container,
                    habitId = habitId.takeIf { it >= 0L },
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun MomentumBottomBar(navController: NavHostController, currentRoute: String?) {
    val colors = LocalMomentumColors.current
    NavigationBar(containerColor = colors.background, contentColor = colors.textPrimary) {
        NavigationBarItem(
            selected = currentRoute == Routes.TODAY,
            onClick = {
                navController.navigate(Routes.TODAY) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
            label = { Text("Today") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colors.textPrimary,
                selectedTextColor = colors.textPrimary,
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary,
                indicatorColor = colors.surface,
            ),
        )
        NavigationBarItem(
            selected = currentRoute == Routes.INSIGHTS,
            onClick = {
                navController.navigate(Routes.INSIGHTS) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Rounded.Insights, contentDescription = null) },
            label = { Text("Insights") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = colors.textPrimary,
                selectedTextColor = colors.textPrimary,
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary,
                indicatorColor = colors.surface,
            ),
        )
    }
}
