package com.witte.lozify.presentation.pro

import androidx.lifecycle.ViewModel
import com.witte.lozify.core.preferences.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ProViewModel - Manages state and VIP activation for ProMembershipScreen.
 */
@HiltViewModel
class ProViewModel @Inject constructor(
    val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val isProUser: StateFlow<Boolean> = preferencesManager.isProUser
    val proPlanType: StateFlow<String> = preferencesManager.proPlanType
    val userName: StateFlow<String> = preferencesManager.userName

    fun activateLicenseCode(code: String): Result<String> {
        return preferencesManager.activateLicenseCode(code)
    }
}
