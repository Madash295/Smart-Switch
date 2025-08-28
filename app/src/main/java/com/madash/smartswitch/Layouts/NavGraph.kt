package com.madash.smartswitch

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartswitch.LaunchViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.madash.smartswitch.Layouts.FileTransfer
import com.madash.smartswitch.Layouts.PhoneClone
import com.madash.smartswitch.Layouts.SettingsScreen

sealed class Routes(val route: String) {
    object Splash     : Routes("splash")
    object Onboarding : Routes("onboarding")
    object Home       : Routes("home")

    object FileTransfer: Routes("filetransfer")

    object FileStorage: Routes("filestorage")

    object PhoneClone: Routes("phoneclone")

    object MobileToPC: Routes("mobiletopc")

    object AIAssistant: Routes("aiassistant")

    object Settings: Routes("settings")

    object Help: Routes("help")

    object About: Routes("about")

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    vm: LaunchViewModel = viewModel(),
    onDynamicColourChange: (Boolean) -> Unit = {},
    dynamicColour: Boolean = true
) {
    val onboardingDone by vm.onboardingDone.collectAsState(initial = false)

    AnimatedNavHost(
        navController = navController,
        startDestination = Routes.Splash.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {

        composable(Routes.Splash.route) {
            AppSplash {
                navController.navigate(
                    if (onboardingDone)
                        Routes.Home.route
                    else
                        Routes.Onboarding.route
                ) {
                    popUpTo(Routes.Splash.route) { inclusive = true }
                }
            }
        }

        composable(Routes.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    vm.markDone()
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Home.route) {
               HomeScreen(navController)
        }

        composable(Routes.FileTransfer.route) {
           FileTransfer(navController)
        }

        composable(Routes.PhoneClone.route){
            PhoneClone(navController)
        }

        composable(Routes.Settings.route) {
            SettingsScreen(
                navController,
                dynamicColour = dynamicColour,
                onDynamicColourChange = onDynamicColourChange
            )
        }
    }
}
