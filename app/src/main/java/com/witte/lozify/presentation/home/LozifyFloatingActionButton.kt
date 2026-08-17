package com.witte.lozify.presentation.home

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Lozify custom floating action button for creating new notes.
 *
 * Stage 15: Rounded square shape with 16dp radius, centered on home screen.
 *
 * Design Specs:
 * - Theme green color (#00C853)
 * - Rounded square shape (16dp)
 * - White "+" icon
 * - Standard FAB elevation
 *
 * @param onClick Callback when FAB is clicked
 * @param modifier Optional modifier
 */
@Composable
fun LozifyFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFF00C853),
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 12.dp
        ),
        modifier = modifier.size(56.dp)
    ) {
        Icon(
            painter = painterResource(android.R.drawable.ic_input_add),
            contentDescription = "创建笔记",
            tint = Color.White
        )
    }
}
