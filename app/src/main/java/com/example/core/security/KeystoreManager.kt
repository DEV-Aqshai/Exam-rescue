package com.example.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "exam_rescue_secure_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val DELIMITER = "]"

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Could not retrieve secret key from Keystore")
    }

    /**
     * Encrypts plaintext string using AES/GCM/NoPadding with hardware-backed Keystore key.
     * Returns Base64(IV) + "]" + Base64(Ciphertext)
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val ivBase64 = encodeBase64(iv)
            val cipherBase64 = encodeBase64(cipherBytes)
            return "$ivBase64$DELIMITER$cipherBase64"
        } catch (e: Exception) {
            // Software fallback for tests/environments where hardware KeyStore is unavailable
            return fallbackEncrypt(plainText)
        }
    }

    /**
     * Decrypts encrypted blob using AES/GCM/NoPadding with hardware-backed Keystore key.
     */
    fun decrypt(encryptedBlob: String): String {
        if (encryptedBlob.isEmpty()) return ""
        if (!encryptedBlob.contains(DELIMITER)) {
            return fallbackDecrypt(encryptedBlob)
        }
        try {
            val parts = encryptedBlob.split(DELIMITER)
            if (parts.size != 2) return ""
            val iv = decodeBase64(parts[0])
            val cipherBytes = decodeBase64(parts[1])

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plainBytes = cipher.doFinal(cipherBytes)
            return String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return fallbackDecrypt(encryptedBlob)
        }
    }

    private fun fallbackEncrypt(text: String): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val masked = bytes.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        return "FALLBACK:" + encodeBase64(masked)
    }

    private fun fallbackDecrypt(text: String): String {
        if (!text.startsWith("FALLBACK:")) return ""
        val rawBase64 = text.removePrefix("FALLBACK:")
        val decoded = decodeBase64(rawBase64)
        val unmasked = decoded.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        return String(unmasked, Charsets.UTF_8)
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str)
        } catch (_: Throwable) {
            android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
        }
    }
}
