package com.witte.lozify.presentation.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.witte.lozify.core.common.ImageUtils
import java.io.File

/**
 * ImageLightboxDialog - Fullscreen image viewer with gesture zoom, swipe paging, and save to gallery.
 *
 * Stage 16: Basic Lightbox Viewer.
 * Stage 38: Added Save to Gallery (MediaStore Scoped Storage) via top action & long-press bottom sheet.
 *
 * Features:
 * - Immersive pure black background with edge-to-edge rendering.
 * - Horizontal swipe paging for multiple images.
 * - Double tap to toggle zoom (1.0x <-> 2.5x).
 * - Pinch to zoom (1.0x to 5.0x) with boundary constraint.
 * - Single finger pan when zoomed in.
 * - Top page counter indicator ("1 / 4").
 * - Save button in top-right and long-press menu to save image to system gallery.
 *
 * @param images List of image files to display
 * @param initialIndex Starting index in the image list
 * @param onDismiss Callback when lightbox is closed
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageLightboxDialog(
    images: List<File>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return

    val context = LocalContext.current
    val validInitialIndex = initialIndex.coerceIn(0, images.size - 1)
    val pagerState = rememberPagerState(
        initialPage = validInitialIndex,
        pageCount = { images.size }
    )

    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var showLongPressMenu by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
    }

    fun saveCurrentImage() {
        val currentImage = images.getOrNull(pagerState.currentPage)
        if (currentImage != null) {
            val success = ImageUtils.saveImageToGallery(context, currentImage)
            if (success) {
                Toast.makeText(context, "图片已保存至系统相册 (Pictures/Lozify)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "保存失败，请检查存储空间", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    onLongClick = { showLongPressMenu = true },
                    onZoomStateChange = { isZoomed ->
                        if (page == pagerState.currentPage) {
                            isCurrentPageZoomed = isZoomed
                        }
                    }
                )
            }

            // Top Control Bar (Page Counter, Download Button & Close Button)
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

                // Right Action Buttons (Save & Close)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save to Gallery Button
                    IconButton(
                        onClick = { saveCurrentImage() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "保存到相册",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
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

            // Long Press Action Bottom Sheet
            if (showLongPressMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showLongPressMenu = false },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        // Option: 保存图片到相册
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showLongPressMenu = false
                                    saveCurrentImage()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "保存图片",
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "保存图片到系统相册",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = "原画质保存至手机 Pictures/Lozify 目录",
                                    fontSize = 12.sp,
                                    color = Color(0xFF999999)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * ZoomableImage - Single image view with pinch-to-zoom, pan, double-tap, and long-press support.
 */
@Composable
private fun ZoomableImage(
    file: File,
    onDismiss: () -> Unit,
    onLongClick: () -> Unit,
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
                    },
                    onLongPress = {
                        onLongClick()
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val downCount = event.changes.count { it.pressed }
                        val currentlyZoomed = scale > 1.05f

                        if (downCount >= 2 || currentlyZoomed) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                val maxOffsetY = (size.height * (newScale - 1f)) / 2f

                                scale = newScale
                                offset = if (newScale > 1.05f) {
                                    Offset(
                                        x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                                        y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                } else {
                                    Offset.Zero
                                }

                                event.changes.forEach {
                                    if (it.positionChanged()) {
                                        it.consume()
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
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
