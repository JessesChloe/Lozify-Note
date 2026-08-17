package com.witte.lozify.presentation.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.witte.lozify.domain.model.Attachment
import java.io.File

/**
 * AttachmentGrid - Dynamic responsive image grid component for note cards.
 *
 * Stage 13 UI Refactor:
 * - 1 Image: Full width, adaptive height (max 240dp), 8dp corner radius.
 * - 2-3 Images: 1 row, equal weight, fixed 120dp height, 8dp corner radius.
 * - 4 Images: Special 2x2 grid, fixed 120dp height per image, 8dp corner radius.
 * - 5-9 Images: 3-column grid (up to 9 images), fixed 80dp height, 4dp corner radius.
 *
 * All images feature a subtle 0.5dp border (0xFFEEEEEE) to separate white images from backgrounds.
 *
 * @param attachments List of attachments for the note
 * @param filesDir Application files directory for resolving local image files
 * @param modifier Modifier applied to the root container
 */
@Composable
fun AttachmentGrid(
    attachments: List<Attachment>,
    filesDir: File?,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty() || filesDir == null) return

    val borderStrokeColor = Color(0xFFEEEEEE)

    when (attachments.size) {
        // 1 Image mode
        1 -> {
            val attachment = attachments.first()
            val imageFile = File(filesDir, attachment.filePath)
            val shape = RoundedCornerShape(8.dp)

            AsyncImage(
                model = imageFile,
                contentDescription = "附件图片",
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp)
                    .clip(shape)
                    .border(0.5.dp, borderStrokeColor, shape)
            )
        }

        // 2 or 3 Images mode (Single row)
        2, 3 -> {
            val shape = RoundedCornerShape(8.dp)

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = modifier.fillMaxWidth()
            ) {
                attachments.forEach { attachment ->
                    val imageFile = File(filesDir, attachment.filePath)
                    AsyncImage(
                        model = imageFile,
                        contentDescription = "附件图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(shape)
                            .border(0.5.dp, borderStrokeColor, shape)
                    )
                }
            }
        }

        // 4 Images mode (Special 2x2 grid)
        4 -> {
            val shape = RoundedCornerShape(8.dp)

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = modifier.fillMaxWidth()
            ) {
                attachments.chunked(2).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { attachment ->
                            val imageFile = File(filesDir, attachment.filePath)
                            AsyncImage(
                                model = imageFile,
                                contentDescription = "附件图片",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp)
                                    .clip(shape)
                                    .border(0.5.dp, borderStrokeColor, shape)
                            )
                        }
                    }
                }
            }
        }

        // 5+ Images mode (3-column grid, max 9 images)
        else -> {
            val displayAttachments = attachments.take(9)
            val shape = RoundedCornerShape(4.dp)

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = modifier.fillMaxWidth()
            ) {
                displayAttachments.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { attachment ->
                            val imageFile = File(filesDir, attachment.filePath)
                            AsyncImage(
                                model = imageFile,
                                contentDescription = "附件图片",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(shape)
                                    .border(0.5.dp, borderStrokeColor, shape)
                            )
                        }
                        // Fill empty slots with equal-weight spacers to keep alignment
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
