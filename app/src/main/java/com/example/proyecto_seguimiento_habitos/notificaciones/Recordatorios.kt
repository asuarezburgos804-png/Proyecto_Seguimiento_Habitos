package com.example.proyecto_seguimiento_habitos.notificaciones

import com.example.proyecto_seguimiento_habitos.modelo.*

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

// Programa las alarmas del teléfono (AlarmManager).
// Las alarmas siguen funcionando aunque la app esté cerrada.
// Cuando una alarma suena, AlarmaReceiver decide si mostrar la notificación.
object Recordatorios {

    // Tipos de alarma
    const val RECORDATORIO = "RECORDATORIO"   // a la hora de un hábito
    const val RACHA = "RACHA"                 // aviso de racha en peligro (21:00)

    // Número fijo para la alarma de racha (cada alarma necesita su propio número)
    private const val CODIGO_RACHA = 1000

    // Programa una alarma por cada hábito con hora, más la alarma diaria de racha
    fun programar(context: Context, habitos: List<Habito>) {
        for (habito in habitos) {
            if (!habito.archivado && habito.horario.isNotEmpty()) {
                programarAlarma(context, RECORDATORIO, habito.id, codigoDe(habito.id), habito.horario)
            }
        }
        programarAlarma(context, RACHA, "", CODIGO_RACHA, HORA_ALERTA_RACHA)
    }

    // Número de la alarma de un hábito (sale de su id, siempre el mismo para el mismo hábito)
    fun codigoDe(habitoId: String): Int {
        return habitoId.hashCode()
    }

    // Prepara el "aviso pendiente" que el teléfono entrega a AlarmaReceiver cuando suena la alarma
    private fun crearAviso(context: Context, tipo: String, habitoId: String, codigo: Int, flags: Int): PendingIntent? {
        val intent = Intent(context, AlarmaReceiver::class.java)
        intent.action = tipo
        intent.putExtra("habitoId", habitoId)
        return PendingIntent.getBroadcast(context, codigo, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    // Programa una alarma a la próxima "hora" (HH:mm).
    // Si ya existía una con el mismo número, se reemplaza (así no se duplican).
    private fun programarAlarma(context: Context, tipo: String, habitoId: String, codigo: Int, hora: String) {
        val aviso = crearAviso(context, tipo, habitoId, codigo, PendingIntent.FLAG_UPDATE_CURRENT) ?: return

        // Cuándo debe sonar, en milisegundos
        val cuando = proximaAlarma(hora, LocalDateTime.now())
        val milisegundos = cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val alarmas = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < 31 || alarmas.canScheduleExactAlarms()) {
            // Alarma exacta: suena a la hora justa, incluso con el teléfono en reposo
            alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, milisegundos, aviso)
        } else {
            // Sin permiso de alarma exacta: suena cerca de esa hora
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, milisegundos, aviso)
        }
    }

    // ¿Hay una alarma programada? (lo usan las pruebas)
    fun estaProgramada(context: Context, tipo: String, habitoId: String): Boolean {
        val codigo = if (tipo == RACHA) CODIGO_RACHA else codigoDe(habitoId)
        // FLAG_NO_CREATE: solo la busca, no crea una nueva
        val aviso = crearAviso(context, tipo, habitoId, codigo, PendingIntent.FLAG_NO_CREATE)
        return aviso != null
    }
}
