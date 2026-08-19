package com.witte.lozify.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * TwoLineMenuIcon - 1:1 Flomo-styled two-line minimalist hamburger menu icon.
 *
 * Top bar: 17dp width, 2.2dp height, rounded corners.
 * Bottom bar: 12.5dp width, 2.2dp height, rounded corners.
 */
@Composable
fun TwoLineMenuIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF222222)
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .width(17.dp)
                .height(2.2.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(12.5.dp)
                .height(2.2.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
    }
}

/**
 * LozifyBrandLogotype - Pixel-perfect geometric vector brand logotype (Flomo-styled).
 *
 * Renders all-lowercase "lozify" using mathematical vector curves and geometric symmetry:
 * - 'l': clean vertical ascender with rounded cap.
 * - 'o': perfect geometric circle with uniform mono-weight stroke.
 * - 'z': crisp diagonal slash with horizontal bars.
 * - 'i': vertical stem with floating dot circle.
 * - 'f': graceful ascender hook and crossbar.
 * - 'y': balanced descending diagonals.
 *
 * 100% offline, zero font-file download dependencies, immune to system font scaling distortion.
 *
 * @param modifier Modifier for styling
 * @param height Target height in Dp (default 19.dp)
 * @param color Text color (default deep charcoal #1C1C1E)
 */
@Composable
fun LozifyBrandLogotype(
    modifier: Modifier = Modifier,
    height: Dp = 19.dp,
    color: Color = Color(0xFF1C1C1E)
) {
    // Proportional width for height 19dp is ~53.2dp (aspect ratio ~2.8)
    val width = height * 2.8f

    Canvas(
        modifier = modifier.size(width = width, height = height)
    ) {
        val scale = size.height / 20f
        val strokeWidth = 2.4f * scale
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // 1. 'l' (x ~ 2.5)
        drawLine(
            color = color,
            start = Offset(2.5f * scale, 2.0f * scale),
            end = Offset(2.5f * scale, 14.5f * scale),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 2. 'o' (center x = 10.5, y = 10.5, radius = 4.0)
        drawCircle(
            color = color,
            radius = 4.0f * scale,
            center = Offset(10.5f * scale, 10.5f * scale),
            style = stroke
        )

        // 3. 'z' (x: 17.5 -> 25.0, y: 6.5 -> 14.5)
        val zPath = Path().apply {
            moveTo(17.5f * scale, 6.5f * scale)
            lineTo(25.0f * scale, 6.5f * scale)
            lineTo(17.5f * scale, 14.5f * scale)
            lineTo(25.0f * scale, 14.5f * scale)
        }
        drawPath(path = zPath, color = color, style = stroke)

        // 4. 'i' (stem x = 29.5, y: 6.5 -> 14.5; dot x = 29.5, y = 2.8)
        drawLine(
            color = color,
            start = Offset(29.5f * scale, 6.5f * scale),
            end = Offset(29.5f * scale, 14.5f * scale),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = 1.3f * scale,
            center = Offset(29.5f * scale, 2.8f * scale)
        )

        // 5. 'f' (stem x = 36.0, y: 14.5 -> 5.5, hook to 39.5, crossbar x: 33.5 -> 38.5)
        val fPath = Path().apply {
            moveTo(36.0f * scale, 14.5f * scale)
            lineTo(36.0f * scale, 5.0f * scale)
            quadraticBezierTo(36.0f * scale, 2.0f * scale, 39.5f * scale, 2.0f * scale)
        }
        drawPath(path = fPath, color = color, style = stroke)
        drawLine(
            color = color,
            start = Offset(33.5f * scale, 6.5f * scale),
            end = Offset(38.5f * scale, 6.5f * scale),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 6. 'y' (left diagonal: 43.0, 6.5 -> 46.5, 14.5; right diagonal: 50.5, 6.5 -> 44.0, 18.5)
        drawLine(
            color = color,
            start = Offset(43.0f * scale, 6.5f * scale),
            end = Offset(46.5f * scale, 14.5f * scale),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(50.5f * scale, 6.5f * scale),
            end = Offset(44.0f * scale, 18.5f * scale),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Backward compatibility alias for LozifyLogo.
 */
@Composable
fun LozifyLogo(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 20.dp,
    color: Color = Color(0xFF1C1C1E)
) {
    LozifyBrandLogotype(
        modifier = modifier,
        height = sizeDp,
        color = color
    )
}
