package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.modelo.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

// Requerimiento: el inicio muestra los hábitos que tocan hoy y si ya se cumplieron
class HabitosDeHoyTest {

    private val hoy = LocalDate.now()
    private val diaDeHoy = hoy.dayOfWeek.value   // 1 = Lunes ... 7 = Domingo

    ////////////////// ¿Toca hoy?

    @Test
    fun diario_siempreTocaHoy() {
        assertTrue(tocaHoy(Habito(frecuencia = Frecuencias.DIARIO)))
    }

    @Test
    fun diasEspecificos_tocaSiHoyEstaEnLaLista() {
        val habito = Habito(frecuencia = Frecuencias.DIAS_ESPECIFICOS, dias = listOf(diaDeHoy))
        assertTrue(tocaHoy(habito))
    }

    @Test
    fun diasEspecificos_noTocaSiHoyNoEstaEnLaLista() {
        val otroDia = if (diaDeHoy == 7) 1 else diaDeHoy + 1
        val habito = Habito(frecuencia = Frecuencias.DIAS_ESPECIFICOS, dias = listOf(otroDia))
        assertFalse(tocaHoy(habito))
    }

    @Test
    fun vecesPorSemana_tocaMientrasFalteLaMeta() {
        val habito = Habito(frecuencia = Frecuencias.VECES_POR_SEMANA, vecesPorSemana = 3)
        assertTrue(tocaHoy(habito))
    }

    @Test
    fun vecesPorSemana_siguePorHoySiSeMarcoHoy() {
        val habito = Habito(
            frecuencia = Frecuencias.VECES_POR_SEMANA,
            vecesPorSemana = 1,
            completados = listOf(hoy.toString())
        )
        assertTrue(tocaHoy(habito))
    }

    @Test
    fun vecesPorSemana_noTocaSiYaCumplioLaMetaOtroDia() {
        // Otro día de esta misma semana (lunes, o martes si hoy es lunes)
        val lunes = lunesDeLaSemana(hoy)
        val otroDia = if (lunes == hoy) lunes.plusDays(1) else lunes
        val habito = Habito(
            frecuencia = Frecuencias.VECES_POR_SEMANA,
            vecesPorSemana = 1,
            completados = listOf(otroDia.toString())
        )
        assertFalse(tocaHoy(habito))
    }

    ////////////////// Marcado de hoy

    @Test
    fun completadoHoy_seDetecta() {
        assertTrue(estaCompletadoHoy(Habito(completados = listOf(hoy.toString()))))
        assertFalse(estaCompletadoHoy(Habito(completados = listOf(hoy.minusDays(1).toString()))))
    }

    @Test
    fun fechaDeHoy_tieneFormatoDeFirestore() {
        // Firestore guarda "yyyy-MM-dd"
        assertTrue(Regex("\\d{4}-\\d{2}-\\d{2}").matches(fechaDeHoy()))
        assertEquals(hoy.toString(), fechaDeHoy())
    }

    @Test
    fun horaCompletadoHoy_muestraLaHoraGuardada() {
        val momento = Date.from(hoy.atTime(8, 15).atZone(ZoneId.systemDefault()).toInstant())
        val habito = Habito(completadoEn = mapOf(hoy.toString() to momento))
        assertEquals("08:15", horaCompletadoHoy(habito))
    }

    @Test
    fun horaCompletadoHoy_sinRegistroEsNull() {
        assertNull(horaCompletadoHoy(Habito()))
    }

    ////////////////// Textos de las tarjetas

    @Test
    fun textoFrecuencia_diario() {
        assertEquals("Todos los días", textoFrecuencia(Habito(frecuencia = Frecuencias.DIARIO)))
    }

    @Test
    fun textoFrecuencia_diasOrdenados() {
        val habito = Habito(frecuencia = Frecuencias.DIAS_ESPECIFICOS, dias = listOf(5, 1, 3))
        assertEquals("Lun, Mié, Vie", textoFrecuencia(habito))
    }

    @Test
    fun textoFrecuencia_vecesPorSemana() {
        val habito = Habito(frecuencia = Frecuencias.VECES_POR_SEMANA, vecesPorSemana = 3)
        assertEquals("3 veces por semana", textoFrecuencia(habito))
    }

    @Test
    fun detalleHabito_conHorario() {
        val habito = Habito(categoria = "Salud", frecuencia = Frecuencias.DIARIO, horario = "07:30")
        assertEquals("Salud · Todos los días · 07:30", detalleHabito(habito))
    }

    @Test
    fun detalleHabito_sinHorario() {
        val habito = Habito(categoria = "Estudio", frecuencia = Frecuencias.DIARIO)
        assertEquals("Estudio · Todos los días", detalleHabito(habito))
    }

    @Test
    fun fechaLegible_empiezaConMayuscula() {
        assertTrue(fechaLegibleDeHoy().first().isUpperCase())
        assertTrue(fechaLegibleDeHoy().contains(" de "))
    }
}
