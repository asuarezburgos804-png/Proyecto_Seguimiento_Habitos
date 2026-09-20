package com.example.proyecto_seguimiento_habitos

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Modelo de un hábito guardado en Firestore.
 * Ruta en la base de datos: usuarios/{uid}/habitos/{idHabito}
 */
data class Habito(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val frecuencia: String = Frecuencias.DIARIO,
    /** Solo para frecuencia por días específicos. 1 = Lunes ... 7 = Domingo. */
    val dias: List<Int> = emptyList(),
    /** Solo para frecuencia "X veces por semana". */
    val vecesPorSemana: Int = 0,
    /** Opcional, en formato "HH:mm". Vacío si el usuario no puso horario. */
    val horario: String = "",
    /** Fechas en las que se completó el hábito, en formato "yyyy-MM-dd". */
    val completados: List<String> = emptyList()
)

/** Categorías disponibles al crear un hábito. */
object Categorias {
    val LISTA = listOf("Salud", "Estudio", "Ejercicio", "Trabajo", "Personal")
}

/** Tipos de frecuencia soportados. */
object Frecuencias {
    const val DIARIO = "DIARIO"
    const val DIAS_ESPECIFICOS = "DIAS_ESPECIFICOS"
    const val VECES_POR_SEMANA = "VECES_POR_SEMANA"
}

/** Nombres cortos de los días. La posición 0 es el lunes (día 1). */
val NOMBRES_DIAS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

/** Fecha de hoy en formato "yyyy-MM-dd". Se usa como clave de los días completados. */
fun fechaDeHoy(): String {
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formato.format(Date())
}

/** Fecha de hoy escrita para mostrarla en pantalla, por ejemplo "Viernes 19 de septiembre". */
fun fechaLegibleDeHoy(): String {
    val formato = SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES"))
    val texto = formato.format(Date())
    return texto.replaceFirstChar { it.uppercase() }
}

/** Día de la semana de hoy con 1 = Lunes ... 7 = Domingo. */
fun diaDeLaSemanaHoy(): Int {
    val dia = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return if (dia == Calendar.SUNDAY) 7 else dia - 1
}

/** Indica si el hábito debe mostrarse en el dashboard de hoy. */
fun tocaHoy(habito: Habito): Boolean {
    return when (habito.frecuencia) {
        Frecuencias.DIAS_ESPECIFICOS -> habito.dias.contains(diaDeLaSemanaHoy())
        else -> true
    }
}

/** Indica si el hábito ya fue marcado como completado el día de hoy. */
fun estaCompletadoHoy(habito: Habito): Boolean {
    return habito.completados.contains(fechaDeHoy())
}

/** Texto de la frecuencia para mostrarlo en la tarjeta del hábito. */
fun textoFrecuencia(habito: Habito): String {
    return when (habito.frecuencia) {
        Frecuencias.DIARIO -> "Todos los días"
        Frecuencias.DIAS_ESPECIFICOS -> habito.dias.sorted().joinToString(", ") { NOMBRES_DIAS[it - 1] }
        Frecuencias.VECES_POR_SEMANA -> "${habito.vecesPorSemana} veces por semana"
        else -> ""
    }
}

/** Línea de detalle de la tarjeta: categoría, frecuencia y horario (si tiene). */
fun detalleHabito(habito: Habito): String {
    val partes = mutableListOf(habito.categoria, textoFrecuencia(habito))
    if (habito.horario.isNotEmpty()) partes.add(habito.horario)
    return partes.filter { it.isNotEmpty() }.joinToString(" · ")
}
