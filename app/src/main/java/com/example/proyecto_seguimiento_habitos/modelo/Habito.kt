package com.example.proyecto_seguimiento_habitos.modelo

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// Un hábito tal como se guarda en Firestore (usuarios/{uid}/habitos/{id})
data class Habito(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val frecuencia: String = Frecuencias.DIARIO,
    val dias: List<Int> = emptyList(),               // 1 = Lunes ... 7 = Domingo
    val vecesPorSemana: Int = 0,
    val horario: String = "",                        // "HH:mm" o vacío
    val completados: List<String> = emptyList(),     // fechas "yyyy-MM-dd" en que se cumplió
    val completadoEn: Map<String, Date> = emptyMap(), // fecha -> hora exacta en que se marcó
    val archivado: Boolean = false,                  // oculto, pero conserva su historial
    val fechaCreacion: Date? = null,                 // cuándo se creó el hábito
    val nota: String = ""
)

// Categorías que se pueden elegir
object Categorias {
    val LISTA = listOf("Salud", "Estudio", "Ejercicio", "Trabajo", "Personal")
}

// Tipos de frecuencia (así se guardan en Firestore)
object Frecuencias {
    const val DIARIO = "DIARIO"
    const val DIAS_ESPECIFICOS = "DIAS_ESPECIFICOS"
    const val VECES_POR_SEMANA = "VECES_POR_SEMANA"
}

// Nombres cortos de los días (posición 0 = lunes)
val NOMBRES_DIAS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

// Idioma español para las fechas
val ESPANOL: Locale = Locale.forLanguageTag("es-ES")

// Fecha de hoy como texto, ej. "2026-10-27" (el mismo formato que se guarda en Firestore)
fun fechaDeHoy(): String {
    return LocalDate.now().toString()
}

// Fecha de hoy para mostrar, ej. "Martes, 27 de octubre"
fun fechaLegibleDeHoy(): String {
    val formato = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ESPANOL)
    val texto = LocalDate.now().format(formato)
    return texto.replaceFirstChar { it.uppercase() }
}

// ¿Ya se marcó hoy?
fun estaCompletadoHoy(habito: Habito): Boolean {
    return habito.completados.contains(fechaDeHoy())
}

// ¿El hábito aparece hoy en el inicio?
fun tocaHoy(habito: Habito): Boolean {
    val diaDeHoy = LocalDate.now().dayOfWeek.value   // 1 = Lunes ... 7 = Domingo

    if (habito.frecuencia == Frecuencias.DIAS_ESPECIFICOS) {
        return habito.dias.contains(diaDeHoy)
    }
    if (habito.frecuencia == Frecuencias.VECES_POR_SEMANA) {
        // Aparece mientras falte cumplir la meta de la semana (o si ya se marcó hoy)
        return estaCompletadoHoy(habito) || faltanEstaSemana(habito) > 0
    }
    return true // DIARIO
}

// Hora en que se marcó hoy, ej. "08:15" (null si no hay hora guardada)
fun horaCompletadoHoy(habito: Habito): String? {
    val momento = habito.completadoEn[fechaDeHoy()]
    if (momento == null) return null
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(momento)
}

// Texto de la frecuencia, ej. "Todos los días" o "Lun, Mié, Vie"
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
    val partes = mutableListOf<String>()
    if (habito.categoria.isNotEmpty()) partes.add(habito.categoria)
    partes.add(textoFrecuencia(habito))
    if (habito.horario.isNotEmpty()) partes.add(habito.horario)
    return partes.joinToString(" · ")
}
