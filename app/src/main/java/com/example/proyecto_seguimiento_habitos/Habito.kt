package com.example.proyecto_seguimiento_habitos

import java.text.SimpleDateFormat
import java.util.Calendar
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
    val archivado: Boolean = false                 // oculto, pero conserva su historial
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

// Fecha "yyyy-MM-dd" de un día
private fun formatoFecha(fecha: Date): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(fecha)

// Fecha de hoy "yyyy-MM-dd"
fun fechaDeHoy(): String = formatoFecha(Date())

// Fecha de hoy para mostrar, ej. "Viernes, 19 de septiembre"
fun fechaLegibleDeHoy(): String {
    val formato = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES"))
    val texto = formato.format(Date())
    return texto.replaceFirstChar { it.uppercase() }
}

// Día de la semana de un Calendar: 1 = Lunes ... 7 = Domingo
private fun numeroDia(calendario: Calendar): Int {
    val dia = calendario.get(Calendar.DAY_OF_WEEK)
    return if (dia == Calendar.SUNDAY) 7 else dia - 1
}

// Día de hoy: 1 = Lunes ... 7 = Domingo
fun diaDeLaSemanaHoy(): Int = numeroDia(Calendar.getInstance())

// ¿El hábito toca ese día de la semana?
private fun tocaElDia(habito: Habito, dia: Int): Boolean {
    return when (habito.frecuencia) {
        Frecuencias.DIAS_ESPECIFICOS -> habito.dias.contains(dia)
        else -> true
    }
}

// ¿El hábito toca hoy?
fun tocaHoy(habito: Habito): Boolean = tocaElDia(habito, diaDeLaSemanaHoy())

// ¿Ya se completó hoy?
fun estaCompletadoHoy(habito: Habito): Boolean {
    return habito.completados.contains(fechaDeHoy())
}

// Hora exacta en que se completó hoy, ej. "08:15" (null si no hay registro)
fun horaCompletadoHoy(habito: Habito): String? {
    val momento = habito.completadoEn[fechaDeHoy()] ?: return null
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(momento)
}

// Racha: días programados seguidos con cumplimiento (hoy cuenta si ya se hizo)
fun rachaActual(habito: Habito): Int {
    val hechos = habito.completados.toHashSet()
    val dia = Calendar.getInstance()
    // Si hoy todavía no se hizo, la racha sigue viva desde ayer
    if (!hechos.contains(formatoFecha(dia.time))) dia.add(Calendar.DAY_OF_YEAR, -1)

    var racha = 0
    repeat(366) {
        val fecha = formatoFecha(dia.time)
        when {
            hechos.contains(fecha) -> racha++
            tocaElDia(habito, numeroDia(dia)) -> return racha  // día programado sin hacer
        }
        dia.add(Calendar.DAY_OF_YEAR, -1)
    }
    return racha
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
