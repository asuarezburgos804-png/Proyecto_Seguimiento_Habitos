package com.example.proyecto_seguimiento_habitos

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modelo del hábito (usuarios/{uid}/habitos/{id})
data class Habito(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val frecuencia: String = Frecuencias.DIARIO,
    val dias: List<Int> = emptyList(),             // 1 = Lunes ... 7 = Domingo
    val vecesPorSemana: Int = 0,
    val horario: String = "",                      // "HH:mm" o vacío
    val completados: List<String> = emptyList(),   // fechas "yyyy-MM-dd"
    val completadoEn: Map<String, Date> = emptyMap(), // fecha -> momento exacto del cumplimiento
    val archivado: Boolean = false,                // oculto, pero conserva su historial
    val fechaCreacion: Date? = null                // desde cuándo existe (antes de esto no hay días que juzgar)
)

// Categorías
object Categorias {
    val LISTA = listOf("Salud", "Estudio", "Ejercicio", "Trabajo", "Personal")
}

// Tipos de frecuencia
object Frecuencias {
    const val DIARIO = "DIARIO"
    const val DIAS_ESPECIFICOS = "DIAS_ESPECIFICOS"
    const val VECES_POR_SEMANA = "VECES_POR_SEMANA"
}

// Nombres cortos de los días (posición 0 = lunes)
val NOMBRES_DIAS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

// Las rachas, el estado de cada día y el calendario viven en Rachas.kt

// Fecha de hoy "yyyy-MM-dd"
fun fechaDeHoy(): String = textoDeFecha(hoyCalendario())

// Fecha de hoy para mostrar, ej. "Viernes, 19 de septiembre"
fun fechaLegibleDeHoy(): String {
    val formato = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES"))
    val texto = formato.format(Date())
    return texto.replaceFirstChar { it.uppercase() }
}

// Día de hoy: 1 = Lunes ... 7 = Domingo
fun diaDeLaSemanaHoy(): Int = numeroDiaSemana(hoyCalendario())

// ¿El hábito debe aparecer hoy en el día del usuario?
fun tocaHoy(habito: Habito): Boolean = when (habito.frecuencia) {
    Frecuencias.DIAS_ESPECIFICOS -> habito.dias.contains(diaDeLaSemanaHoy())
    // "X por semana" no fija días: aparece mientras falte cumplir la meta de la semana
    Frecuencias.VECES_POR_SEMANA -> estaCompletadoHoy(habito) || faltanEstaSemana(habito) > 0
    else -> true
}

// ¿Ya se completó hoy?
fun estaCompletadoHoy(habito: Habito): Boolean {
    return habito.completados.contains(fechaDeHoy())
}

// Hora exacta en que se completó hoy, ej. "08:15" (null si no hay registro)
fun horaCompletadoHoy(habito: Habito): String? {
    val momento = habito.completadoEn[fechaDeHoy()] ?: return null
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(momento)
}

// Texto de la frecuencia, ej. "Lun, Mié, Vie"
fun textoFrecuencia(habito: Habito): String {
    return when (habito.frecuencia) {
        Frecuencias.DIARIO -> "Todos los días"
        Frecuencias.DIAS_ESPECIFICOS -> habito.dias.sorted().joinToString(", ") { NOMBRES_DIAS[it - 1] }
        Frecuencias.VECES_POR_SEMANA -> "${habito.vecesPorSemana} veces por semana"
        else -> ""
    }
}

// Detalle de la tarjeta, ej. "Salud · Todos los días · 07:30"
fun detalleHabito(habito: Habito): String {
    val partes = mutableListOf(habito.categoria, textoFrecuencia(habito))
    if (habito.horario.isNotEmpty()) partes.add(habito.horario)
    return partes.filter { it.isNotEmpty() }.joinToString(" · ")
}
