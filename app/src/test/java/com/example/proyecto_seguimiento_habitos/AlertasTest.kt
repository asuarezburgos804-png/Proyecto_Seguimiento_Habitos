package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.modelo.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

// Requerimiento 1 / 17: recordatorios a la hora del hábito y alerta de racha en peligro
class AlertasTest {

    private val hoy = LocalDate.now()
    private val ayer = hoy.minusDays(1).toString()
    private val anteayer = hoy.minusDays(2).toString()

    ////////////////// ¿Cuándo suena la alarma?

    @Test
    fun proximaAlarma_hoySiTodaviaNoPasaLaHora() {
        val ahora = LocalDateTime.of(2026, 11, 2, 7, 0)
        assertEquals(LocalDateTime.of(2026, 11, 2, 7, 30), proximaAlarma("07:30", ahora))
    }

    @Test
    fun proximaAlarma_mananaSiLaHoraYaPaso() {
        val ahora = LocalDateTime.of(2026, 11, 2, 22, 0)
        assertEquals(LocalDateTime.of(2026, 11, 3, 21, 0), proximaAlarma(HORA_ALERTA_RACHA, ahora))
    }

    @Test
    fun proximaAlarma_justoALaHoraPasaAMañana() {
        val ahora = LocalDateTime.of(2026, 11, 2, 7, 30)
        assertEquals(LocalDateTime.of(2026, 11, 3, 7, 30), proximaAlarma("07:30", ahora))
    }

    @Test
    fun alertaDeRacha_esCercaDelFinDelDia() {
        assertEquals("21:00", HORA_ALERTA_RACHA)
    }

    ////////////////// Recordatorio a la hora del hábito

    @Test
    fun recordatorio_habitoPendienteConHora() {
        assertTrue(necesitaRecordatorio(Habito(horario = "07:30")))
    }

    @Test
    fun recordatorio_noSiYaSeMarcoHoy() {
        assertFalse(necesitaRecordatorio(Habito(horario = "07:30", completados = listOf(hoy.toString()))))
    }

    @Test
    fun recordatorio_noSiNoTieneHora() {
        assertFalse(necesitaRecordatorio(Habito()))
    }

    @Test
    fun recordatorio_noSiEstaArchivado() {
        assertFalse(necesitaRecordatorio(Habito(horario = "07:30", archivado = true)))
    }

    @Test
    fun recordatorio_noSiHoyNoLeToca() {
        val otroDia = if (hoy.dayOfWeek.value == 7) 1 else hoy.dayOfWeek.value + 1
        val habito = Habito(horario = "07:30", frecuencia = Frecuencias.DIAS_ESPECIFICOS, dias = listOf(otroDia))
        assertFalse(necesitaRecordatorio(habito))
    }

    ////////////////// Racha en peligro

    @Test
    fun racha_enPeligroSiTieneRachaYHoyEstaPendiente() {
        assertTrue(rachaEnPeligro(Habito(completados = listOf(anteayer, ayer))))
    }

    @Test
    fun racha_noEnPeligroSiYaSeMarcoHoy() {
        assertFalse(rachaEnPeligro(Habito(completados = listOf(anteayer, ayer, hoy.toString()))))
    }

    @Test
    fun racha_noEnPeligroSiNoHayRacha() {
        assertFalse(rachaEnPeligro(Habito()))
    }

    @Test
    fun racha_noEnPeligroSiEstaArchivado() {
        assertFalse(rachaEnPeligro(Habito(completados = listOf(anteayer, ayer), archivado = true)))
    }
}
