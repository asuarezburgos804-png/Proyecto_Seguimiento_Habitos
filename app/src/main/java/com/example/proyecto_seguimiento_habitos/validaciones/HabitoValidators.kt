package com.example.proyecto_seguimiento_habitos.validaciones

import com.example.proyecto_seguimiento_habitos.modelo.*

// Validaciones del formulario de hábitos.
// Cada función devuelve el mensaje de error, o null si el dato está bien.
object HabitoValidators {

    // Nombre: de 3 a 40 letras
    fun validarNombre(nombre: String): String? {
        val texto = nombre.trim()
        if (texto.isEmpty()) return "Ingresa el nombre del hábito"
        if (texto.length < 3) return "El nombre es demasiado corto"
        if (texto.length > 40) return "El nombre es demasiado largo"
        return null
    }

    // Hay que elegir una categoría
    fun validarCategoria(categoria: String): String? {
        if (categoria.isEmpty()) return "Selecciona una categoría"
        return null
    }

    // "Días específicos" necesita al menos un día marcado
    fun validarDias(frecuencia: String, dias: List<Int>): String? {
        if (frecuencia == Frecuencias.DIAS_ESPECIFICOS && dias.isEmpty()) {
            return "Selecciona al menos un día"
        }
        return null
    }

    // "X por semana" necesita un número del 1 al 7
    fun validarVecesPorSemana(frecuencia: String, veces: String): String? {
        if (frecuencia != Frecuencias.VECES_POR_SEMANA) return null

        val texto = veces.trim()
        if (texto.isEmpty()) return "Indica cuántas veces por semana"

        val numero = texto.toIntOrNull()
        if (numero == null) return "Ingresa solo números"
        if (numero < 1 || numero > 7) return "Debe ser un número del 1 al 7"
        return null
    }

    // La hora es opcional; si se escribe debe ser "HH:mm" (ej. 07:30)
    fun validarHorario(horario: String): String? {
        val texto = horario.trim()
        if (texto.isEmpty()) return null

        val formatoHora = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")
        if (!formatoHora.matches(texto)) return "Usa el formato HH:mm (ej. 07:30)"
        return null
    }
}
