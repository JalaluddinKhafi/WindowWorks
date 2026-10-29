package com.example.core

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing utility.
 *
 * New hashes use PBKDF2WithHmacSHA256 (stored as `pbkdf2$<iterations>$<hex>`).
 * Legacy SHA-256 hashes (plain 64 char hex) are still verifiable so that users
 * created by older builds can log in and get their hash upgraded transparently.
 */
object SecurityHelper {
  private val random = SecureRandom()

  private const val ALGORITHM = "PBKDF2WithHmacSHA256"
  private const val ITERATIONS = 12_000
  private const val KEY_LENGTH_BITS = 256
  private const val SALT_BYTES = 16
  private const val PREFIX = "pbkdf2"
  private const val SEPARATOR = "\u0024"

  fun generateSalt(): String {
    val bytes = ByteArray(SALT_BYTES)
    random.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun hashPassword(password: String, salt: String): String {
    val saltBytes = salt.toByteArray(Charsets.UTF_8)
    val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance(ALGORITHM)
    val hash = factory.generateSecret(spec).encoded
    spec.clearPassword()
    return "$PREFIX$SEPARATOR$ITERATIONS$SEPARATOR${hash.toHex()}"
  }

  fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
    if (expectedHash.isBlank()) return false
    val actualHash = if (expectedHash.startsWith("$PREFIX$SEPARATOR")) {
      hashPassword(password, salt)
    } else {
      // Legacy SHA-256 password:salt hash
      legacySha256Hash(password, salt)
    }
    return MessageDigest.isEqual(
      actualHash.lowercase().toByteArray(Charsets.UTF_8),
      expectedHash.lowercase().toByteArray(Charsets.UTF_8)
    )
  }

  /** True when the stored hash uses an outdated algorithm and should be re-hashed. */
  fun needsRehash(expectedHash: String): Boolean = !expectedHash.startsWith("$PREFIX$SEPARATOR")

  private fun legacySha256Hash(password: String, salt: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest("$password:$salt".toByteArray(Charsets.UTF_8)).toHex()
  }

  private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

  fun generateUuid(): String {
    return UUID.randomUUID().toString()
  }
}
