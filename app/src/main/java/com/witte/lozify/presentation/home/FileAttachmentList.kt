package com.witte.lozify.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.core.common.FileUtils
import com.witte.lozify.domain.model.Attachment
import java.io.File

/**
 * FileAttachmentList - Component for rendering generic document/file attachments on note cards.
 *
 * Stage 43: Visually distinct from bidirectional links (@relations) with dedicated format-themed icons,
 * file sizes, and FileProvider invocation.
 */
@Composable
fun FileAttachmentList(
    attachments: List<Attachment>,
    filesDir: File,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        attachments.forEach { attachment ->
            val displayName = attachment.getDisplayName()
            val fileSize = attachment.getFormattedFileSize()
            val category = FileUtils.getFileCategory(displayName, attachment.mimeType)

            val (icon, iconColor, bgColor) = when (category) {
                FileUtils.FileCategory.PDF -> Triple(Icons.Outlined.Description, Color(0xFFE53935), Color(0xFFFFEBEE))
                FileUtils.FileCategory.DOCUMENT -> Triple(Icons.Outlined.Description, Color(0xFF1E88E5), Color(0xFFE3F2FD))
                FileUtils.FileCategory.SPREADSHEET -> Triple(Icons.Outlined.TableChart, Color(0xFF43A047), Color(0xFFE8F5E9))
                FileUtils.FileCategory.PRESENTATION -> Triple(Icons.Outlined.Slideshow, Color(0xFFFB8C00), Color(0xFFFFF3E0))
                FileUtils.FileCategory.ARCHIVE -> Triple(Icons.Outlined.FolderZip, Color(0xFFFFB300), Color(0xFFFFF8E1))
                FileUtils.FileCategory.AUDIO -> Triple(Icons.Outlined.AudioFile, Color(0xFF8E24AA), Color(0xFFF3E5F5))
                FileUtils.FileCategory.VIDEO -> Triple(Icons.Outlined.VideoFile, Color(0xFF00ACC1), Color(0xFFE0F7FA))
                FileUtils.FileCategory.CODE -> Triple(Icons.Outlined.Code, Color(0xFF5E35B1), Color(0xFFEDE7F6))
                FileUtils.FileCategory.OTHER -> Triple(Icons.Outlined.AttachFile, Color(0xFF757575), Color(0xFFF5F5F5))
            }

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
                border = BorderStroke(0.6.dp, Color(0xFFEBEBEB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val file = File(filesDir, attachment.filePath)
                        FileUtils.openFileWithSystemApp(context, file, attachment.mimeType)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Icon with subtle tinted container
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // File info (Name + Size)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (fileSize.isNotBlank()) {
                            Text(
                                text = fileSize,
                                fontSize = 11.sp,
                                color = Color(0xFF8E8E93),
                                lineHeight = 13.sp
                            )
                        }
                    }

                    // Open external indicator icon
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "打开文件",
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
