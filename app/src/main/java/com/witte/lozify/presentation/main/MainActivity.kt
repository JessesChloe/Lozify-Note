package com.witte.lozify.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.witte.lozify.core.navigation.LozifyNavGraph
import com.witte.lozify.presentation.lock.AppLockOverlayScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for Lozify application.
 * Single-activity architecture using Jetpack Compose with Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LozifyApp()
        }
    }
}

@Composable
fun LozifyApp(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isAppLockEnabled by mainViewModel.isAppLockEnabled.collectAsState()
    val appLockPin by mainViewModel.appLockPin.collectAsState()
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()

    var isLocked by remember {
        mutableStateOf(isAppLockEnabled && appLockPin.isNotEmpty())
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isAppLockEnabled, appLockPin) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (isAppLockEnabled && appLockPin.isNotEmpty()) {
                    isLocked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LozifyNavGraph(navController = navController)

                if (isLocked && isAppLockEnabled && appLockPin.isNotEmpty()) {
                    AppLockOverlayScreen(
                        correctPin = appLockPin,
                        isBiometricEnabled = isBiometricEnabled,
                        onUnlock = { isLocked = false }
                    )
                }
            }
        }
    }
}
