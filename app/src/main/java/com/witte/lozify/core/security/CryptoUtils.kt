package com.witte.lozify.core.security

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * CryptoUtils - Hardware-accelerated AES-256-GCM End-to-End Encryption Engine.
 *
 * Envelope Format (.enc):
 * [Magic: 4 bytes "LZNC"] (0x4C, 0x5A, 0x4E, 0x43)
 * [Version: 1 byte 0x01]
 * [Salt: 16 bytes]
 * [IV/Nonce: 12 bytes]
 * [Ciphertext + 16 bytes GCM Auth Tag]
 *
 * Security Characteristics:
 * - Cipher: AES-256-GCM (Authenticated Encryption with Associated Data / AEAD)
 * - Key Derivation: PBKDF2WithHmacSHA256, 10,000 iterations
 * - Tamper-Proof: Any modification to ciphertext or wrong password throws AEADBadTagException
 * - Zero-Knowledge: Passwords never leave local device memory
 *
 * Stage 27: WebDAV End-to-End Encryption (E2EE).
 */
object CryptoUtils {

    private val MAGIC_HEADER = byteArrayOf(0x4C, 0x5A, 0x4E, 0x43) // "LZNC"
    private const val FORMAT_VERSION: Byte = 0x01
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BIT = 128
    private const val KEY_LENGTH_BIT = 256
    private const val PBKDF2_ITERATIONS = 10_000

    private val secureRandom = SecureRandom()

    /**
     * Check if a byte array starts with the Lozify Encrypted header ("LZNC").
     */
    fun isEncryptedData(bytes: ByteArray): Boolean {
        if (bytes.size < 5) return false
        return bytes[0] == MAGIC_HEADER[0] &&
                bytes[1] == MAGIC_HEADER[1] &&
                bytes[2] == MAGIC_HEADER[2] &&
                bytes[3] == MAGIC_HEADER[3] &&
                bytes[4] == FORMAT_VERSION
    }

    /**
     * Derive a 256-bit AES SecretKey from password and salt using PBKDF2.
     */
    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BIT)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypt in-memory byte array using AES-256-GCM.
     */
    fun encryptBytes(plainBytes: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH).apply { secureRandom.nextBytes(this) }
        val secretKey = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherText = cipher.doFinal(plainBytes)

        val result = ByteArray(4 + 1 + SALT_LENGTH + IV_LENGTH + cipherText.size)
        var offset = 0

        System.arraycopy(MAGIC_HEADER, 0, result, offset, 4)
        offset += 4

        result[offset] = FORMAT_VERSION
        offset += 1

        System.arraycopy(salt, 0, result, offset, SALT_LENGTH)
        offset += SALT_LENGTH

        System.arraycopy(iv, 0, result, offset, IV_LENGTH)
        offset += IV_LENGTH

        System.arraycopy(cipherText, 0, result, offset, cipherText.size)
        return result
    }

    /**
     * Decrypt in-memory byte array using AES-256-GCM.
     * Throws IllegalArgumentException or GeneralSecurityException if password wrong or format invalid.
     */
    fun decryptBytes(encryptedBytes: ByteArray, password: String): ByteArray {
        if (!isEncryptedData(encryptedBytes)) {
            throw IllegalArgumentException("非合法 Lozify 加密文件格式 (Magic Header 不匹配)")
        }

        var offset = 5 // Skip magic + version

        val salt = ByteArray(SALT_LENGTH)
        System.arraycopy(encryptedBytes, offset, salt, 0, SALT_LENGTH)
        offset += SALT_LENGTH

        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(encryptedBytes, offset, iv, 0, IV_LENGTH)
        offset += IV_LENGTH

        val cipherTextSize = encryptedBytes.size - offset
        val cipherText = ByteArray(cipherTextSize)
        System.arraycopy(encryptedBytes, offset, cipherText, 0, cipherTextSize)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(cipherText)
    }

    /**
     * Encrypt a physical file on disk to a target file.
     */
    fun encryptFile(sourceFile: File, targetEncFile: File, password: String) {
        val plainBytes = sourceFile.readBytes()
        val encBytes = encryptBytes(plainBytes, password)
        targetEncFile.parentFile?.mkdirs()
        targetEncFile.writeBytes(encBytes)
    }

    /**
     * Decrypt an encrypted physical file on disk to target file.
     */
    fun decryptFile(sourceEncFile: File, targetPlainFile: File, password: String) {
        val encBytes = sourceEncFile.readBytes()
        val plainBytes = decryptBytes(encBytes, password)
        targetPlainFile.parentFile?.mkdirs()
        targetPlainFile.writeBytes(plainBytes)
    }
}
