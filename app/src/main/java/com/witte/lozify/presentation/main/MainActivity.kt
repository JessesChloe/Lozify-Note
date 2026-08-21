package com.witte.lozify.presentation.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.witte.lozify.core.navigation.LozifyNavGraph
import com.witte.lozify.core.preferences.UserPreferencesManager
import com.witte.lozify.presentation.lock.AppLockOverlayScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for Lozify application.
 * Single-activity architecture using Jetpack Compose with Navigation.
 *
 * Stage 59: App Lock & Biometrics support via FragmentActivity.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesManager: UserPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LozifyApp(preferencesManager = preferencesManager)
        }
    }
}

@Composable
fun LozifyApp(preferencesManager: UserPreferencesManager) {
    val navController = rememberNavController()
    val isAppLockEnabled by preferencesManager.isAppLockEnabled.collectAsState()
    val appLockPin by preferencesManager.appLockPin.collectAsState()
    val isBiometricEnabled by preferencesManager.isBiometricEnabled.collectAsState()

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
