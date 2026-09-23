package com.trace.game.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trace.game.ui.home.HomeScreen
import com.trace.game.ui.journey.JourneyStubScreen
import com.trace.game.ui.play.GameViewModel
import com.trace.game.ui.play.PlayScreen
import com.trace.game.ui.settings.SettingsScreen
import com.trace.game.ui.shop.ShopScreen

object Routes {
    const val Home = "home"
    const val Play = "play"
    const val Shop = "shop"
    const val Journey = "journey"
    const val Settings = "settings"
}

@Composable
fun TraceNavHost(vm: GameViewModel) {
    val nav = rememberNavController()
    val run by vm.runState.collectAsState()
    val prefs by vm.userPrefs.collectAsState()

    NavHost(navController = nav, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(
                gems = prefs.gems,
                highExp = prefs.highExp,
                onPlay = {
                    vm.startNewRun()
                    nav.navigate(Routes.Play)
                },
                onJourney = { nav.navigate(Routes.Journey) },
                onShop = { nav.navigate(Routes.Shop) },
                onSettings = { nav.navigate(Routes.Settings) },
                onUiTap = vm::uiTap,
            )
        }
        composable(Routes.Play) {
            PlayScreen(
                vm = vm,
                onExitHome = {
                    nav.popBackStack(Routes.Home, inclusive = false)
                },
            )
        }
        composable(Routes.Shop) {
            ShopScreen(
                gems = prefs.gems,
                onBuy = { amount -> vm.buyGemsLocal(amount) },
                onBack = { nav.popBackStack() },
                onUiTap = vm::uiTap,
            )
        }
        composable(Routes.Journey) {
            JourneyStubScreen(
                level = run.level.coerceAtLeast(1),
                onBack = { nav.popBackStack() },
                onUiTap = vm::uiTap,
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                prefs = prefs,
                onHaptics = vm::setHaptics,
                onVolume = vm::setSfxVolume,
                onMute = vm::setSfxMuted,
                onReduceMotion = vm::setReduceMotion,
                onBack = { nav.popBackStack() },
                onUiTap = vm::uiTap,
            )
        }
    }
}
