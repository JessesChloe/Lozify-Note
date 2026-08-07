package com.witte.lozify.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.witte.lozify.presentation.home.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for Lozify application.
 * Single-activity architecture using Jetpack Compose.
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
fun LozifyApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LozifyAppPreview() {
    LozifyApp()
}
