package com.witte.lozify.core.security

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Unit tests for AES-256-GCM End-to-End Encryption Engine.
 *
 * Stage 27: WebDAV E2EE & Anti-Censorship System.
 */
class CryptoUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testEncryptDecrypt_roundtrip_stringText() {
        val originalText = "这是一段私密日记：包含个人隐私、账单与敏感灵感笔记 #隐私 #日记"
        val password = "MySecretPassword@2026"

        val plainBytes = originalText.toByteArray(StandardCharsets.UTF_8)
        val encryptedBytes = CryptoUtils.encryptBytes(plainBytes, password)

        assertNotNull(encryptedBytes)
        assertTrue(encryptedBytes.size > plainBytes.size)
        assertTrue(CryptoUtils.isEncryptedData(encryptedBytes))

        val decryptedBytes = CryptoUtils.decryptBytes(encryptedBytes, password)
        val recoveredText = String(decryptedBytes, StandardCharsets.UTF_8)

        assertEquals(originalText, recoveredText)
    }

    @Test
    fun testEncryptDecrypt_wrongPassword_failsSecurely() {
        val originalText = "机密数据"
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword999"

        val plainBytes = originalText.toByteArray(StandardCharsets.UTF_8)
        val encryptedBytes = CryptoUtils.encryptBytes(plainBytes, correctPassword)

        try {
            CryptoUtils.decryptBytes(encryptedBytes, wrongPassword)
            fail("Expected exception when decrypting with wrong password")
        } catch (e: Exception) {
            // Expected AEADBadTagException or GeneralSecurityException
            assertTrue(e is javax.crypto.AEADBadTagException || e is java.security.GeneralSecurityException || e is IllegalArgumentException)
        }
    }

    @Test
    fun testEncryptDecrypt_tamperedCiphertext_failsIntegrityCheck() {
        val originalText = "防篡改测试"
        val password = "StrongPassword!"

        val plainBytes = originalText.toByteArray(StandardCharsets.UTF_8)
        val encryptedBytes = CryptoUtils.encryptBytes(plainBytes, password)

        // Tamper with the last byte (auth tag)
        encryptedBytes[encryptedBytes.size - 1] = (encryptedBytes[encryptedBytes.size - 1].toInt() xor 0xFF).toByte()

        try {
            CryptoUtils.decryptBytes(encryptedBytes, password)
            fail("Expected exception when decrypting tampered data")
        } catch (e: Exception) {
            assertTrue(e is javax.crypto.AEADBadTagException || e is java.security.GeneralSecurityException)
        }
    }

    @Test
    fun testEncryptDecrypt_largeBinary_simulatedImageFile() {
        // Simulated 1MB image byte buffer
        val fakeImageBytes = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val password = "PhotoEncryptionKey"

        val encryptedBytes = CryptoUtils.encryptBytes(fakeImageBytes, password)
        assertTrue(CryptoUtils.isEncryptedData(encryptedBytes))

        val decryptedBytes = CryptoUtils.decryptBytes(encryptedBytes, password)
        assertArrayEquals(fakeImageBytes, decryptedBytes)
    }

    @Test
    fun testEncryptFile_decryptFile_fileRoundtrip() {
        val sourceFile = tempFolder.newFile("test_photo.jpg")
        val encFile = File(tempFolder.root, "test_photo.jpg.enc")
        val restoredFile = File(tempFolder.root, "restored_photo.jpg")

        val testData = "JPEG_SIMULATED_BINARY_DATA_${System.currentTimeMillis()}".toByteArray()
        sourceFile.writeBytes(testData)

        val password = "FilePassword456"
        CryptoUtils.encryptFile(sourceFile, encFile, password)

        assertTrue(encFile.exists())
        assertTrue(encFile.length() > sourceFile.length())
        assertTrue(CryptoUtils.isEncryptedData(encFile.readBytes()))

        CryptoUtils.decryptFile(encFile, restoredFile, password)

        assertTrue(restoredFile.exists())
        assertArrayEquals(testData, restoredFile.readBytes())
    }

    @Test
    fun testIsEncryptedData_detectsNonEncryptedPayload() {
        val plainJson = "{ \"version\": 1, \"notes\": [] }".toByteArray()
        assertFalse(CryptoUtils.isEncryptedData(plainJson))

        val randomBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        assertFalse(CryptoUtils.isEncryptedData(randomBytes))
    }
}
