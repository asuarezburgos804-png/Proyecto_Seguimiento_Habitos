package com.example.proyecto_seguimiento_habitos

import android.util.Patterns

/**
 * Validaciones reutilizables para los formularios de login y registro.
 * Cada función devuelve un mensaje de error (String) si algo está mal,
 * o null si el valor es válido.
 */
object Validators {

    fun validarNombre(nombre: String): String? {
        val n = nombre.trim()
        return when {
            n.isEmpty() -> "Ingresa tu nombre"
            n.length < 2 -> "El nombre es demasiado corto"
            n.length > 50 -> "El nombre es demasiado largo"
            else -> null
        }
    }

    fun validarCorreo(correo: String): String? {
        val c = correo.trim()
        return when {
            c.isEmpty() -> "Ingresa tu correo"
            !Patterns.EMAIL_ADDRESS.matcher(c).matches() -> "Correo no válido"
            else -> null
        }
    }

    /** Validación estricta para el REGISTRO (contraseña nueva). */
    fun validarPassword(password: String): String? {
        return when {
            password.isEmpty() -> "Ingresa una contraseña"
            password.length < 8 -> "Mínimo 8 caracteres"
            !password.any { it.isDigit() } -> "Debe incluir al menos un número"
            !password.any { it.isLetter() } -> "Debe incluir al menos una letra"
            else -> null
        }
    }

    /** Validación simple para el LOGIN (solo que no esté vacía). */
    fun validarPasswordLogin(password: String): String? {
        return if (password.isEmpty()) "Ingresa tu contraseña" else null
    }

    fun validarConfirmacion(password: String, confirmacion: String): String? {
        return when {
            confirmacion.isEmpty() -> "Confirma tu contraseña"
            confirmacion != password -> "Las contraseñas no coinciden"
            else -> null
        }
    }
}
