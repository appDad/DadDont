package com.egabel.daddont.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.egabel.daddont.ui.screen.ImpulseDetailScreen
import com.egabel.daddont.ui.screen.ImpulseListScreen
import com.egabel.daddont.ui.screen.SettingsScreen
import com.egabel.daddont.ui.screen.StatsScreen

object Routes {
    const val IMPULSE_LIST = "impulseList"
    const val IMPULSE_DETAIL = "impulseDetail/{impulseId}"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    fun impulseDetail(impulseId: String) = "impulseDetail/$impulseId"
}

@Composable
fun DadDontNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.IMPULSE_LIST
    ) {
        composable(Routes.IMPULSE_LIST) {
            ImpulseListScreen(
                onImpulseClick = { id ->
                    navController.navigate(Routes.impulseDetail(id.toString()))
                },
                onStatsClick = {
                    navController.navigate(Routes.STATS)
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.IMPULSE_DETAIL,
            arguments = listOf(navArgument("impulseId") { type = NavType.StringType })
        ) {
            ImpulseDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STATS) {
            StatsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
