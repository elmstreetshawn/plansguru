package com.plansguru.data.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object ChannelCryptoHelper {
    fun computeChannelTopic(personA: String, personB: String): String {
        val sorted = listOf(personA.trim().lowercase(), personB.trim().lowercase()).sorted()
        val combined = sorted.joinToString("::")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(combined.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "topic_${hex.take(16)}"
    }

    private fun deriveSharedKey(personA: String, personB: String): SecretKeySpec {
        val sorted = listOf(personA.trim().lowercase(), personB.trim().lowercase()).sorted()
        val combined = "E2E_KEY::" + sorted.joinToString("::")
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest(combined.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun deriveSessionKey(sessionId: String): SecretKeySpec {
        val combined = "E2E_SESSION_KEY::" + sessionId.trim().lowercase()
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest(combined.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(payload: String, personA: String, personB: String): String {
        val secretKey = deriveSharedKey(personA, personB)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val encrypted = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedPayloadBase64: String, personA: String, personB: String): String {
        val secretKey = deriveSharedKey(personA, personB)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val combined = Base64.decode(encryptedPayloadBase64, Base64.NO_WRAP)
        
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(encrypted)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun encryptSession(payload: String, sessionId: String): String {
        val secretKey = deriveSessionKey(sessionId)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val encrypted = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptSession(encryptedPayloadBase64: String, sessionId: String): String {
        val secretKey = deriveSessionKey(sessionId)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val combined = Base64.decode(encryptedPayloadBase64, Base64.NO_WRAP)
        
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(encrypted)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
