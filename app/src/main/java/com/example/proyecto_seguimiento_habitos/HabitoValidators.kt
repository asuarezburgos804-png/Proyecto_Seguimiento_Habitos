package com.example.proyecto_seguimiento_habitos

// Validaciones del formulario de hábitos (devuelven el error o null)
object HabitoValidators {

    // Nombre: 3 a 40 caracteres
    fun validarNombre(nombre: String): String? {
        val n = nombre.trim()
        return when {
            n.isEmpty() -> "Ingresa el nombre del hábito"
            n.length < 3 -> "El nombre es demasiado corto"
            n.length > 40 -> "El nombre es demasiado largo"
            else -> null
        }
    }

    // Categoría obligatoria
    fun validarCategoria(categoria: String): String? {
        return if (categoria.isEmpty()) "Selecciona una categoría" else null
    }

    // Días específicos: al menos un día
    fun validarDias(frecuencia: String, dias: List<Int>): String? {
        return if (frecuencia == Frecuencias.DIAS_ESPECIFICOS && dias.isEmpty()) {
            "Selecciona al menos un día"
        } else {
            null
        }
    }

    // Veces por semana: número del 1 al 7
    fun validarVecesPorSemana(frecuencia: String, veces: String): String? {
        if (frecuencia != Frecuencias.VECES_POR_SEMANA) return null
        val numero = veces.trim().toIntOrNull()
        return when {
            veces.trim().isEmpty() -> "Indica cuántas veces por semana"
            numero == null -> "Ingresa solo números"
            numero < 1 || numero > 7 -> "Debe ser un número del 1 al 7"
            else -> null
        }
    }

    // Horario opcional, formato HH:mm
    fun validarHorario(horario: String): String? {
        val h = horario.trim()
        if (h.isEmpty()) return null
        val mensaje = "Usa el formato HH:mm (ej. 07:30)"
        val partes = h.split(":")
        if (partes.size != 2) return mensaje

        val hora = partes[0].toIntOrNull()
        val minuto = partes[1].toIntOrNull()
        val esValido = partes[0].length == 2 && partes[1].length == 2 &&
                hora != null && minuto != null &&
                hora in 0..23 && minuto in 0..59

        return if (esValido) null else mensaje
    }
}
