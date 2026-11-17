package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 12_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private val random = SecureRandom()

    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        val hash = pbkdf2(password.toCharArray(), salt)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "pbkdf2:$ITERATIONS:$saltB64:$hashB64"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 4 || parts[0] != "pbkdf2") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = Base64.decode(parts[2], Base64.NO_WRAP)
        val expected = Base64.decode(parts[3], Base64.NO_WRAP)
        val actual = pbkdf2(password.toCharArray(), salt, iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    fun generateSessionToken(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP) + "-" + UUID.randomUUID().toString()
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } catch (_: Exception) {
            // Fallback for older crypto providers if PBKDF2WithHmacSHA256 is unavailable
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            digest.digest(String(password).toByteArray(Charsets.UTF_8))
        } finally {
            spec.clearPassword()
        }
    }
}
