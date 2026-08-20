package com.witte.lozify.core.common

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FileUtils (Stage 43).
 */
class FileUtilsTest {

    @Test
    fun testFormatFileSize_bytes() {
        assertEquals("500 B", FileUtils.formatFileSize(500))
        assertEquals("0 B", FileUtils.formatFileSize(0))
    }

    @Test
    fun testFormatFileSize_kilobytes() {
        assertEquals("1.0 KB", FileUtils.formatFileSize(1024))
        assertEquals("1.5 KB", FileUtils.formatFileSize(1536))
    }

    @Test
    fun testFormatFileSize_megabytes() {
        assertEquals("1.0 MB", FileUtils.formatFileSize(1024 * 1024))
        assertEquals("2.5 MB", FileUtils.formatFileSize((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatFileSize_gigabytes() {
        assertEquals("1.00 GB", FileUtils.formatFileSize(1024L * 1024L * 1024L))
    }

    @Test
    fun testIsImageFile() {
        assertTrue(FileUtils.isImageFile("photo.jpg"))
        assertTrue(FileUtils.isImageFile("image.png"))
        assertTrue(FileUtils.isImageFile("picture.webp"))
        assertTrue(FileUtils.isImageFile("anything", "image/jpeg"))

        assertFalse(FileUtils.isImageFile("document.pdf"))
        assertFalse(FileUtils.isImageFile("archive.zip"))
        assertFalse(FileUtils.isImageFile("audio.mp3"))
    }

    @Test
    fun testGetFileCategory() {
        assertEquals(FileUtils.FileCategory.PDF, FileUtils.getFileCategory("document.pdf"))
        assertEquals(FileUtils.FileCategory.DOCUMENT, FileUtils.getFileCategory("notes.docx"))
        assertEquals(FileUtils.FileCategory.DOCUMENT, FileUtils.getFileCategory("readme.txt"))
        assertEquals(FileUtils.FileCategory.SPREADSHEET, FileUtils.getFileCategory("budget.xlsx"))
        assertEquals(FileUtils.FileCategory.SPREADSHEET, FileUtils.getFileCategory("data.csv"))
        assertEquals(FileUtils.FileCategory.PRESENTATION, FileUtils.getFileCategory("slides.pptx"))
        assertEquals(FileUtils.FileCategory.ARCHIVE, FileUtils.getFileCategory("backup.zip"))
        assertEquals(FileUtils.FileCategory.ARCHIVE, FileUtils.getFileCategory("package.7z"))
        assertEquals(FileUtils.FileCategory.AUDIO, FileUtils.getFileCategory("voice.mp3"))
        assertEquals(FileUtils.FileCategory.VIDEO, FileUtils.getFileCategory("clip.mp4"))
        assertEquals(FileUtils.FileCategory.CODE, FileUtils.getFileCategory("Main.kt"))
        assertEquals(FileUtils.FileCategory.CODE, FileUtils.getFileCategory("config.json"))
        assertEquals(FileUtils.FileCategory.OTHER, FileUtils.getFileCategory("unknown.xyz"))
    }

    @Test
    fun testGetDisplayFileName_stripsUuidPrefix() {
        assertEquals("my_document.pdf", FileUtils.getDisplayFileName("files/a1b2c3d4_my_document.pdf"))
        assertEquals("report.docx", FileUtils.getDisplayFileName("e5f6g7h8_report.docx"))
        assertEquals("plain_file.txt", FileUtils.getDisplayFileName("plain_file.txt"))
    }
}
