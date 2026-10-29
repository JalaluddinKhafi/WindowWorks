package com.example.core

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object SecurityHelper {
  private val random = SecureRandom()

  fun generateSalt(): String {
    val bytes = ByteArray(16)
    random.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun hashPassword(password: String, salt: String): String {
    val input = "$password:$salt"
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
    val actualHash = hashPassword(password, salt)
    return actualHash.equals(expectedHash, ignoreCase = true)
  }

  fun generateUuid(): String {
    return UUID.randomUUID().toString()
  }
}
