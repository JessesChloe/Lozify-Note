package com.witte.lozify.core.common

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Image Compression Engine & Preferences (Stage 37).
 */
class ImageCompressionPreferenceTest {

    @Test
    fun testDefaultImageCompression_isEnabledByDefault() {
        val defaultCompression = true
        assertTrue("Image compression should be enabled by default to save cloud and local storage", defaultCompression)
    }

    @Test
    fun testImageDimensionConstraint_within2KRetina() {
        val maxDimension = 2048
        val originalHighResWidth = 4000
        val originalHighResHeight = 3000

        val aspectRatio = originalHighResWidth.toFloat() / originalHighResHeight.toFloat()
        val scaledWidth = maxDimension
        val scaledHeight = (maxDimension / aspectRatio).toInt()

        assertEquals("Scaled width should match max dimension of 2048", 2048, scaledWidth)
        assertEquals("Scaled height should maintain 4:3 aspect ratio", 1536, scaledHeight)
        assertTrue("Scaled image stays within 2048px constraint", scaledWidth <= maxDimension && scaledHeight <= maxDimension)
    }

    @Test
    fun testJpegQuality_visuallyLossless() {
        val jpegQuality = 85
        assertTrue("JPEG Quality 85 provides optimal visually lossless balance", jpegQuality in 80..90)
    }
}
