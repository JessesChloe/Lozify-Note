package com.witte.lozify.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * EmptyState component displayed when there are no notes in the stream.
 *
 * Flomo-aligned minimal typography and gentle gray visual styling.
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "还没有任何笔记",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "点击底部的 + 按钮\n记录此刻的想法",
            fontSize = 13.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}
