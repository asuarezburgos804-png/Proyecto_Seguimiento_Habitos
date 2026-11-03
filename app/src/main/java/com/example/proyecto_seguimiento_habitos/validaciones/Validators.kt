package com.example.proyecto_seguimiento_habitos.validaciones

// Validaciones de login y registro.
// Cada función devuelve el mensaje de error, o null si el dato está bien.
//!c.lowercase().endsWith("@gmail.com") -> "Solo se permite usar gmail"
object Validators {

    // Forma básica de un correo: algo@algo.algo (sin espacios)
    private val formatoCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // Nombre: de 2 a 50 letras
    fun validarNombre(nombre: String): String? {
        val texto = nombre.trim()
        if (texto.isEmpty()) return "Ingresa tu nombre"
        if (texto.length < 2) return "El nombre es demasiado corto"
        if (texto.length > 50) return "El nombre es demasiado largo"
        return null
    }

    // Correo con formato válido
    fun validarCorreo(correo: String): String? {
        val texto = correo.trim()
        if (texto.isEmpty()) return "Ingresa tu correo"
        if (!formatoCorreo.matches(texto)) return "Correo no válido solo se puede usar @gmail"
        return null
    }
    /*
    fun validarTelefono(telefono: String): String?{

        val t = telefono.trim()
        return when {
            t.isEmpty() -> "Ingresar tu telefono"
            t.length != 9 -> "El telefono debe tener 9 digitos"
            !t.all {it.isDigit() } -> "Solo se permiten numeros"
            else -> null
        }
    }
    */

    // Contraseña del registro: mínimo 8, con letras y números
    fun validarPassword(password: String): String? {
        if (password.isEmpty()) return "Ingresa una contraseña"
        if (password.length < 8) return "Mínimo 8 caracteres"
        if (!password.any { it.isDigit() }) return "Debe incluir al menos un número"
        if (!password.any { it.isLetter() }) return "Debe incluir al menos una letra"
        return null
    }

    // Contraseña del login: solo que no esté vacía
    fun validarPasswordLogin(password: String): String? {
        if (password.isEmpty()) return "Ingresa tu contraseña"
        return null
    }

    // La confirmación debe ser igual a la contraseña
    fun validarConfirmacion(password: String, confirmacion: String): String? {
        if (confirmacion.isEmpty()) return "Confirma tu contraseña"
        if (confirmacion != password) return "Las contraseñas no coinciden"
        return null
    }
}
