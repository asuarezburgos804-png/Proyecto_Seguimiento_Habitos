package com.example.proyecto_seguimiento_habitos.notificaciones

import com.example.proyecto_seguimiento_habitos.basededatos.*

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Al reiniciar el teléfono, Android borra todas las alarmas.
// Este receptor se ejecuta al encender y las vuelve a programar.
class ReinicioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val espera = goAsync()
        HabitosRepository.leerHabitos { habitos ->
            if (habitos.isNotEmpty()) Recordatorios.programar(context, habitos)
            espera.finish()
        }
    }
}
