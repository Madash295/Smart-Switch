package com.madash.smartswitch

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.smartswitch.LaunchViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.madash.smartswitch.Layouts.CreateReceiver
import com.madash.smartswitch.Layouts.FileTransfer
import com.madash.smartswitch.Layouts.PhoneClone
import com.madash.smartswitch.Layouts.ScanQrCode
import com.madash.smartswitch.Layouts.ScanningReceiver
import com.madash.smartswitch.Layouts.SelectFiles
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

    object  SelectFiles: Routes("selectfiles")

    object ScanningReceiver :
        Routes("scanningreceiver/{mediaCount}/{appCount}/{contactCount}/{fileCount}") {
        fun createRoute(mediaCount: Int, appCount: Int, contactCount: Int, fileCount: Int): String {
            return "scanningreceiver/$mediaCount/$appCount/$contactCount/$fileCount"
        }
    }



    object ScanQr: Routes("scanqr")


    object CreateReceiver: Routes("createreceiver")


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

        composable(Routes.SelectFiles.route) {
            SelectFiles(navController)
        }

        composable(Routes.Settings.route) {
            SettingsScreen(
                navController,
                dynamicColour = dynamicColour,
                onDynamicColourChange = onDynamicColourChange
            )
        }
        composable(Routes.ScanQr.route) {
            // Get the previous back stack entry to pass result back
            val previousBackStackEntry = remember(navController) {
                navController.previousBackStackEntry
            }

            ScanQrCode(navController) { qrData ->
                // Save the QR result to the previous screen's savedStateHandle
                previousBackStackEntry?.savedStateHandle?.set("qr_result", qrData)
            }
        }
        composable(
            Routes.ScanningReceiver.route,
            arguments = listOf(
                navArgument("mediaCount") { type = NavType.IntType },
                navArgument("appCount") { type = NavType.IntType },
                navArgument("contactCount") { type = NavType.IntType },
                navArgument("fileCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            // Listen for QR scan results
            val qrResult = backStackEntry.savedStateHandle.getLiveData<String>("qr_result").observeAsState()

            ScanningReceiver(
                navController = navController,
                mediaCount = backStackEntry.arguments?.getInt("mediaCount"),
                appCount = backStackEntry.arguments?.getInt("appCount"),
                contactCount = backStackEntry.arguments?.getInt("contactCount"),
                fileCount = backStackEntry.arguments?.getInt("fileCount"),
                qrScanResult = qrResult.value // Pass the QR result
            )
        }



        composable(Routes.CreateReceiver.route) {
            CreateReceiver(navController)
        }
    }
}