package com.example.proyecto_seguimiento_habitos.notificaciones

import com.example.proyecto_seguimiento_habitos.modelo.*
import com.example.proyecto_seguimiento_habitos.basededatos.*

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Se ejecuta cuando suena una alarma (aunque la app esté cerrada).
// Lee los hábitos de Firestore, muestra el aviso si hace falta y programa la siguiente alarma.
class AlarmaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tipo = intent.action ?: return
        val habitoId = intent.getStringExtra("habitoId") ?: ""

        // goAsync: le pide a Android unos segundos extra para esperar a Firestore
        val espera = goAsync()
        HabitosRepository.leerHabitos { habitos ->
            avisar(context, tipo, habitoId, habitos)
            Recordatorios.programar(context, habitos)   // la del día siguiente
            espera.finish()
        }
    }
}

// Decide qué notificación mostrar según el tipo de alarma
fun avisar(context: Context, tipo: String, habitoId: String, habitos: List<Habito>) {
    // Recordatorio a la hora del hábito
    if (tipo == Recordatorios.RECORDATORIO) {
        val habito = habitos.find { it.id == habitoId }
        if (habito != null && necesitaRecordatorio(habito)) {
            Notificaciones.mostrar(
                context,
                codigo = Recordatorios.codigoDe(habito.id),
                titulo = "Es hora de: ${habito.nombre}",
                texto = "Tienes este hábito programado a las ${habito.horario}. ¡Márcalo cuando lo termines!"
            )
        }
    }

    // Racha en peligro: un aviso por cada hábito con racha que sigue pendiente
    if (tipo == Recordatorios.RACHA) {
        for (habito in habitos) {
            if (rachaEnPeligro(habito)) {
                Notificaciones.mostrar(
                    context,
                    codigo = Recordatorios.codigoDe(habito.id) + 1,
                    titulo = "¡Tu racha está en peligro!",
                    texto = "\"${habito.nombre}\" sigue pendiente y llevas una racha de " +
                        "${textoRacha(rachaActual(habito), habito)}. Márcalo antes de medianoche."
                )
            }
        }
    }
}
