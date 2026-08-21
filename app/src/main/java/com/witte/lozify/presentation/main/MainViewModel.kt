package com.witte.lozify.presentation.main

import androidx.lifecycle.ViewModel
import com.witte.lozify.core.preferences.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * MainViewModel - Provides App Lock state for MainActivity.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    val preferencesManager: UserPreferencesManager
) : ViewModel() {
    val isAppLockEnabled: StateFlow<Boolean> = preferencesManager.isAppLockEnabled
    val appLockPin: StateFlow<String> = preferencesManager.appLockPin
    val isBiometricEnabled: StateFlow<Boolean> = preferencesManager.isBiometricEnabled
}
