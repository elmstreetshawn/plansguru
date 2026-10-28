package com.plansguru.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyPair
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object KeyManager {
    private const val KEYSTORE_ALIAS = "PlansGuruMasterKey"
    private const val PREFS_FILE = "plansguru_secure_prefs"
    private const val IDENTITY_KEYSTORE_ALIAS = "PlansGuruIdentityKey"

    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context, KEYSTORE_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var passphraseHex = sharedPreferences.getString("sqlcipher_passphrase", null)
        if (passphraseHex == null) {
            val randomBytes = ByteArray(32)
            java.security.SecureRandom().nextBytes(randomBytes)
            passphraseHex = randomBytes.joinToString("") { "%02x".format(it) }
            sharedPreferences.edit().putString("sqlcipher_passphrase", passphraseHex).apply()
        }

        return passphraseHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun getOrCreateIdentityKeyPair(context: Context): KeyPair {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        // Check if key already exists
        if (keyStore.containsAlias(IDENTITY_KEYSTORE_ALIAS)) {
            val privateKey = keyStore.getKey(IDENTITY_KEYSTORE_ALIAS, null) as java.security.PrivateKey
            val publicKey = keyStore.getCertificate(IDENTITY_KEYSTORE_ALIAS).publicKey
            return KeyPair(publicKey, privateKey)
        }

        // Generate new ECDSA keypair
        val keyGen = java.security.KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )

        val spec = KeyGenParameterSpec.Builder(
            IDENTITY_KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGen.initialize(spec)
        return keyGen.generateKeyPair()
    }

    fun getPublicKeyHex(context: Context): String {
        val keyPair = getOrCreateIdentityKeyPair(context)
        return keyPair.public.encoded.joinToString("") { "%02x".format(it) }
    }
}

