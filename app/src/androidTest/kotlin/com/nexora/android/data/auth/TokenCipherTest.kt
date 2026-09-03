package com.nexora.android.data.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentado (no test unitario JVM): TokenCipher usa el proveedor real
 * "AndroidKeyStore" (KeyGenParameterSpec, KeyGenerator, Cipher AES/GCM,
 * respaldado por el Keystore del dispositivo/emulador) — no existe en el JVM
 * de unit tests locales, solo en un dispositivo/emulador real.
 */
@RunWith(AndroidJUnit4::class)
class TokenCipherTest {

    @Test
    fun encryptThenDecrypt_returnsOriginalPlainText() {
        val cipher = TokenCipher()
        val plainText = "access-token-de-prueba-12345"

        val encrypted = cipher.encrypt(plainText)

        assertNotEquals("el blob cifrado no debe coincidir con el texto plano", plainText, encrypted)
        assertEquals(plainText, cipher.decrypt(encrypted))
    }

    @Test
    fun decrypt_ofPlainTextLegado_devuelveNull() {
        // TokenStore.decryptOrLegacy es quien decide usar este null como "es texto plano legado";
        // TokenCipher en sí nunca lanza, solo reporta que no pudo descifrarlo.
        val cipher = TokenCipher()
        assertNull(cipher.decrypt("legacy-plaintext-access-token-12345"))
    }

    @Test
    fun encrypt_ofSameValue_producesDifferentCiphertext() {
        // El IV es aleatorio por cada llamada (generado por el propio Cipher) — dos
        // cifrados del mismo texto no deben coincidir byte a byte.
        val cipher = TokenCipher()
        val plainText = "refresh-token-de-prueba-67890"

        val first = cipher.encrypt(plainText)
        val second = cipher.encrypt(plainText)

        assertNotEquals(first, second)
        assertEquals(plainText, cipher.decrypt(first))
        assertEquals(plainText, cipher.decrypt(second))
    }
}
