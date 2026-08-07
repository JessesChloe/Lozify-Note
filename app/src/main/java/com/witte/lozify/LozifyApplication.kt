package com.witte.lozify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Lozify.
 *
 * @HiltAndroidApp triggers Hilt's code generation and sets up dependency injection
 * for the entire application lifecycle.
 */
@HiltAndroidApp
class LozifyApplication : Application()
