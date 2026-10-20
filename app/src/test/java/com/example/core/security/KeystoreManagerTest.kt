package com.example.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeystoreManagerTest {

    @Test
    fun `encryption and decryption preserves plain text`() {
        val originalKey = "sk-ant-api03-test-secret-key-12345"
        val encrypted = KeystoreManager.encrypt(originalKey)

        assertTrue(encrypted.isNotEmpty())
        assertTrue("Ciphertext should not contain raw secret", !encrypted.contains(originalKey))

        val decrypted = KeystoreManager.decrypt(encrypted)
        assertEquals(originalKey, decrypted)
    }

    @Test
    fun `empty input returns empty string`() {
        assertEquals("", KeystoreManager.encrypt(""))
        assertEquals("", KeystoreManager.decrypt(""))
    }
}
