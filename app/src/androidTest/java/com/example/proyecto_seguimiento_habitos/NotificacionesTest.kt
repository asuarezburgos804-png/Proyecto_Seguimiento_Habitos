package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.modelo.*
import com.example.proyecto_seguimiento_habitos.notificaciones.*

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

// Requerimiento 1 / 17: notificaciones reales del teléfono y alarmas programadas.
// Se prueban en el emulador con hábitos de ejemplo (sin iniciar sesión).
@RunWith(AndroidJUnit4::class)
class NotificacionesTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val hoy = LocalDate.now()

    @Before
    fun preparar() {
        // Da el permiso de notificaciones (como si el usuario tocara "Permitir")
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        manager.cancelAll()
    }

    // ¿Hay una notificación visible con ese título? (espera hasta 3 segundos)
    private fun hayNotificacion(titulo: String): Boolean {
        repeat(30) {
            for (aviso in manager.activeNotifications) {
                if (aviso.notification.extras.getString(Notification.EXTRA_TITLE) == titulo) return true
            }
            Thread.sleep(100)
        }
        return false
    }

    @Test
    fun mostrar_publicaLaNotificacionEnElTelefono() {
        Notificaciones.mostrar(context, 1, "Prueba", "Hola")
        assertTrue(hayNotificacion("Prueba"))
    }

    @Test
    fun recordatorio_habitoPendienteAvisa() {
        val habitos = listOf(Habito(id = "r1", nombre = "Leer", horario = "07:30"))
        avisar(context, Recordatorios.RECORDATORIO, "r1", habitos)
        assertTrue(hayNotificacion("Es hora de: Leer"))
    }

    @Test
    fun recordatorio_habitoYaHechoNoAvisa() {
        val habitos = listOf(
            Habito(id = "r2", nombre = "Correr", horario = "07:30", completados = listOf(hoy.toString()))
        )
        avisar(context, Recordatorios.RECORDATORIO, "r2", habitos)
        assertFalse(hayNotificacion("Es hora de: Correr"))
    }

    @Test
    fun racha_enPeligroAvisa() {
        val conRacha = Habito(
            id = "r3",
            nombre = "Meditar",
            completados = listOf(hoy.minusDays(2).toString(), hoy.minusDays(1).toString())
        )
        avisar(context, Recordatorios.RACHA, "", listOf(conRacha))
        assertTrue(hayNotificacion("¡Tu racha está en peligro!"))
    }

    @Test
    fun racha_sinRachaNoAvisa() {
        avisar(context, Recordatorios.RACHA, "", listOf(Habito(id = "r4", nombre = "Nuevo")))
        assertFalse(hayNotificacion("¡Tu racha está en peligro!"))
    }

    @Test
    fun programar_creaLaAlarmaDelHabitoYLaDeRacha() {
        Recordatorios.programar(context, listOf(Habito(id = "p1", nombre = "Agua", horario = "08:00")))
        assertTrue(Recordatorios.estaProgramada(context, Recordatorios.RECORDATORIO, "p1"))
        assertTrue(Recordatorios.estaProgramada(context, Recordatorios.RACHA, ""))
    }

    @Test
    fun programar_sinHoraNoCreaAlarma() {
        Recordatorios.programar(context, listOf(Habito(id = "p2", nombre = "Sin hora")))
        assertFalse(Recordatorios.estaProgramada(context, Recordatorios.RECORDATORIO, "p2"))
    }
}
