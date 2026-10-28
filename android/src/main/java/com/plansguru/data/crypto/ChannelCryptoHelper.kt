package com.plansguru.data.crypto

import java.security.MessageDigest

object ChannelCryptoHelper {
    fun computeChannelTopic(personA: String, personB: String): String {
        val sorted = listOf(personA.trim().lowercase(), personB.trim().lowercase()).sorted()
        val combined = sorted.joinToString("::")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(combined.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "topic_${hex.take(16)}"
    }
}
