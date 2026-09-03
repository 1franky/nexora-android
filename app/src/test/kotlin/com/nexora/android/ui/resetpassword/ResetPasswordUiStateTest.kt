package com.nexora.android.ui.resetpassword

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ResetPasswordUiState.isSubmittable] combina varias reglas de validación de cliente (A11) —
 * vale la pena testearla como función pura, sin necesidad de mockear [AuthRepository]
 * (que requiere un Context real para TokenStore, ver AppContainer).
 */
class ResetPasswordUiStateTest {

    private val valid = ResetPasswordUiState(
        email = "user@example.com",
        code = "123456",
        newPassword = "password123",
        confirmPassword = "password123",
    )

    @Test
    fun `formulario completo y valido es enviable`() {
        assertTrue(valid.isSubmittable)
    }

    @Test
    fun `email en blanco no es enviable`() {
        assertFalse(valid.copy(email = "").isSubmittable)
    }

    @Test
    fun `codigo con menos de 6 digitos no es enviable`() {
        assertFalse(valid.copy(code = "12345").isSubmittable)
    }

    @Test
    fun `codigo con caracteres no numericos no es enviable`() {
        assertFalse(valid.copy(code = "12a456").isSubmittable)
    }

    @Test
    fun `contrasena con menos de 8 caracteres no es enviable`() {
        assertFalse(valid.copy(newPassword = "short1", confirmPassword = "short1").isSubmittable)
    }

    @Test
    fun `contrasenas que no coinciden no son enviables`() {
        assertFalse(valid.copy(confirmPassword = "otraPassword123").isSubmittable)
    }

    @Test
    fun `mientras esta cargando no es enviable aunque el resto sea valido`() {
        assertFalse(valid.copy(isLoading = true).isSubmittable)
    }
}
