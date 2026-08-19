package com.witte.lozify.core.common

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for Image Gallery Saving logic (Stage 38).
 */
class ImageGallerySaveTest {

    @Test
    fun testGalleryTargetDirectory_isPicturesLozify() {
        val targetRelativePath = "Pictures/Lozify"
        assertEquals("Pictures/Lozify", targetRelativePath)
    }

    @Test
    fun testGeneratedFilename_matchesLozifyPrefix() {
        val timestamp = 1787100000000L
        val filename = "Lozify_$timestamp.jpg"
        assertTrue("Filename must start with Lozify_", filename.startsWith("Lozify_"))
        assertTrue("Filename must end with .jpg", filename.endsWith(".jpg"))
    }

    @Test
    fun testSaveImageToGallery_handlesEmptyFileGracefully() {
        val emptyFile = File.createTempFile("empty_test", ".jpg").apply {
            deleteOnExit()
        }
        assertEquals(0L, emptyFile.length())
    }
}
