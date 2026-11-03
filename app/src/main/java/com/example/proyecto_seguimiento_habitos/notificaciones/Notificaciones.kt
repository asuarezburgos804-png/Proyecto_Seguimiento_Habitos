package com.example.proyecto_seguimiento_habitos.notificaciones

import com.example.proyecto_seguimiento_habitos.MainActivity
import com.example.proyecto_seguimiento_habitos.R
import com.example.proyecto_seguimiento_habitos.ui.theme.Primary

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

// Muestra notificaciones en la barra del teléfono
object Notificaciones {

    private const val CANAL = "recordatorios"

    // ¿El usuario dio permiso para mostrar notificaciones? (se pide desde Android 13)
    fun hayPermiso(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        return permiso == PackageManager.PERMISSION_GRANTED
    }

    // Crea el "canal" de notificaciones (Android lo pide; si ya existe no pasa nada)
    private fun crearCanal(context: Context) {
        val canal = NotificationChannel(CANAL, "Recordatorios de hábitos", NotificationManager.IMPORTANCE_HIGH)
        canal.description = "Recordatorios a la hora de cada hábito y avisos de racha en peligro"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(canal)
    }

    // Muestra una notificación. Al tocarla se abre la app.
    // codigo = número de la notificación (con el mismo código se reemplaza la anterior)
    @SuppressLint("MissingPermission") // el permiso se revisa con hayPermiso()
    fun mostrar(context: Context, codigo: Int, titulo: String, texto: String) {
        if (!hayPermiso(context)) return
        crearCanal(context)

        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(Primary.toArgb())                                   // verde de la app
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))  // texto largo completo
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)                                          // se quita al tocarla
            .build()

        NotificationManagerCompat.from(context).notify(codigo, notificacion)
    }
}
