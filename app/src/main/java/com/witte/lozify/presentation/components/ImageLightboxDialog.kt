package com.witte.lozify.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File

/**
 * ImageLightboxDialog - Fullscreen image viewer with gesture zoom and swipe paging.
 *
 * Features:
 * - Immersive pure black background with edge-to-edge rendering.
 * - Horizontal swipe paging for multiple images.
 * - Double tap to toggle zoom (1.0x <-> 2.5x).
 * - Pinch to zoom (1.0x to 5.0x) with boundary constraint.
 * - Single finger pan when zoomed in.
 * - Single tap on background or close button to dismiss.
 * - Top page counter indicator ("1 / 4").
 *
 * @param images List of image files to display
 * @param initialIndex Starting index in the image list
 * @param onDismiss Callback when lightbox is closed
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ImageLightboxDialog(
    images: List<File>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return

    val validInitialIndex = initialIndex.coerceIn(0, images.size - 1)
    val pagerState = rememberPagerState(
        initialPage = validInitialIndex,
        pageCount = { images.size }
    )

    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Horizontal Pager for multi-image swipe
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentPageZoomed,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val imageFile = images[page]
                ZoomableImage(
                    file = imageFile,
                    onDismiss = onDismiss,
                    onZoomStateChange = { isZoomed ->
                        if (page == pagerState.currentPage) {
                            isCurrentPageZoomed = isZoomed
                        }
                    }
                )
            }

            // Top Control Bar (Page Counter & Close Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator Badge (e.g. 2 / 5)
                if (images.size > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x66000000))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${images.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * ZoomableImage - Single image view with pinch-to-zoom, pan, and double-tap support.
 */
@Composable
private fun ZoomableImage(
    file: File,
    onDismiss: () -> Unit,
    onZoomStateChange: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Notify parent pager if current image is zoomed in
    LaunchedEffect(scale) {
        onZoomStateChange(scale > 1.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (scale <= 1.05f) {
                            onDismiss()
                        } else {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    },
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            // Center zoom on double-tap point
                            offset = Offset(
                                x = (size.width / 2 - tapOffset.x) * 1.5f,
                                y = (size.height / 2 - tapOffset.y) * 1.5f
                            )
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                    val maxOffsetY = (size.height * (newScale - 1f)) / 2f

                    scale = newScale
                    offset = if (newScale > 1.05f) {
                        Offset(
                            x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    } else {
                        Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = file,
            contentDescription = "全屏大图预览",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
