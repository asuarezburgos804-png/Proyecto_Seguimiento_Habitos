package com.example.proyecto_seguimiento_habitos.modelo

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Reglas de los avisos (recordatorios y racha en peligro).
// Aquí solo se decide CUÁNDO avisar; mostrar la notificación está en la carpeta "notificaciones".

// Hora del aviso de "racha en peligro" (cerca del fin del día)
const val HORA_ALERTA_RACHA = "21:00"

// ¿Hay que recordar este hábito ahora?
// Sí, si tiene hora, no está archivado, le toca hoy y todavía no se marcó.
fun necesitaRecordatorio(habito: Habito): Boolean {
    if (habito.archivado) return false
    if (habito.horario.isEmpty()) return false
    if (!tocaHoy(habito)) return false
    if (estaCompletadoHoy(habito)) return false
    return true
}

// ¿La racha de este hábito está en peligro?
// Sí, si tiene una racha activa, le toca hoy y todavía no se marcó.
fun rachaEnPeligro(habito: Habito): Boolean {
    if (habito.archivado) return false
    if (!tocaHoy(habito)) return false
    if (estaCompletadoHoy(habito)) return false
    return rachaActual(habito) > 0
}

// Próxima vez que debe sonar una alarma a la hora "HH:mm":
// hoy a esa hora si todavía no pasó; si ya pasó, mañana a esa hora.
fun proximaAlarma(hora: String, ahora: LocalDateTime): LocalDateTime {
    val horaDelAviso = LocalTime.parse(hora)                   // "07:30" -> 07:30
    val hoyALaHora = LocalDate.from(ahora).atTime(horaDelAviso)
    if (hoyALaHora.isAfter(ahora)) return hoyALaHora
    return hoyALaHora.plusDays(1)
}
