package com.nexora.android.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "nexora_token_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val GCM_IV_LENGTH_BYTES = 12

/**
 * Cifra/descifra strings cortos (los tokens de TokenStore) con AES-256-GCM,
 * clave respaldada por Android Keystore — el material de la clave nunca sale
 * del hardware/TEE del dispositivo (plan.md 14.3).
 *
 * No usa `androidx.security:security-crypto` (EncryptedSharedPreferences):
 * deprecado desde abril 2025 por Google (problemas de fiabilidad del
 * Keystore entre fabricantes) sin un reemplazo directo equivalente — la ruta
 * oficial que lo sustituye (Proto DataStore + Tink) es una migración mucho
 * más pesada de la que amerita cifrar dos strings. Esto usa directamente las
 * APIs estándar de Keystore/Cipher, estables desde API 23 (muy por debajo
 * del minSdk 26 del proyecto), sin agregar una dependencia externa.
 *
 * Sin `setUserAuthenticationRequired(true)`: esta clave protege los tokens
 * en reposo (para que no queden en texto plano en disco), no está atada al
 * desbloqueo con huella de A10 — eso es un endurecimiento explícitamente
 * fuera de alcance (plan.md 14.3, "atar el BiometricPrompt a un
 * CryptoObject").
 */
class TokenCipher {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    // Cacheado tras el primer uso: evita una llamada al daemon de Keystore por
    // cada encrypt/decrypt — TokenStore.getTokensBlocking() se invoca en cada
    // petición de red (AuthInterceptor), así que esto corre en ese camino caliente.
    private val secretKey: SecretKey by lazy {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(spec)
        }.generateKey()
    }

    /** IV (12 bytes, generado por el propio Cipher) + ciphertext, todo en un único string Base64. */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey) }
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + cipherBytes, Base64.NO_WRAP)
    }

    /**
     * @return el texto plano, o `null` si [cipherText] no se pudo descifrar —
     * nunca lanza. TokenStore usa ese `null` para reconocer un valor legado
     * (guardado en texto plano por una versión anterior de la app, previa a
     * este cifrado) en vez de tratarlo como sesión corrupta.
     */
    fun decrypt(cipherText: String): String? = runCatching {
        val payload = Base64.decode(cipherText, Base64.NO_WRAP)
        val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val cipherBytes = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }
        String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }.getOrNull()
}
