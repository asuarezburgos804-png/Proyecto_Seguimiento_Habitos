package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.modelo.*

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

// Requerimiento: sistema de rachas (actual y mejor) y calendario de progreso
class RachasTest {

    private val hoy = LocalDate.now()

    // Fechas "yyyy-MM-dd" de hace N días (0 = hoy, 1 = ayer...)
    private fun haceDias(vararg numeros: Long): List<String> {
        val lista = mutableListOf<String>()
        for (n in numeros) lista.add(hoy.minusDays(n).toString())
        return lista
    }

    // Convierte un LocalDate en Date (así guarda Firestore la fecha de creación)
    private fun comoDate(dia: LocalDate): Date {
        return Date.from(dia.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    ////////////////// Racha actual (diario)

    @Test
    fun sinMarcas_rachasEnCero() {
        val habito = Habito()
        assertEquals(0, rachaActual(habito))
        assertEquals(0, mejorRacha(habito))
    }

    @Test
    fun diario_tresDiasSeguidosHastaHoy() {
        val habito = Habito(completados = haceDias(0, 1, 2))
        assertEquals(3, rachaActual(habito))
    }

    @Test
    fun diario_hoyPendienteNoRompeLaRacha() {
        val habito = Habito(completados = haceDias(1, 2))
        assertEquals(2, rachaActual(habito))
    }

    @Test
    fun diario_unDiaFalladoCortaLaRacha() {
        // Hoy y anteayer, pero ayer no
        val habito = Habito(completados = haceDias(0, 2))
        assertEquals(1, rachaActual(habito))
    }

    @Test
    fun diario_siAyerNoSeMarcoLaRachaEsCero() {
        val habito = Habito(completados = haceDias(2, 3, 4))
        assertEquals(0, rachaActual(habito))
    }

    ////////////////// Mejor racha (diario)

    @Test
    fun diario_mejorRachaEsLaSecuenciaMasLarga() {
        // 4 seguidos hace 10..7 días, luego un hueco, luego hoy
        val habito = Habito(completados = haceDias(10, 9, 8, 7, 0))
        assertEquals(4, mejorRacha(habito))
        assertEquals(1, rachaActual(habito))
    }

    ////////////////// Días específicos

    @Test
    fun diasEspecificos_losDiasQueNoTocanSeSaltan() {
        // Toca hoy y hace 2 días; ayer no toca. Se marcaron los dos días que tocaban.
        val dias = listOf(hoy.dayOfWeek.value, hoy.minusDays(2).dayOfWeek.value)
        val habito = Habito(
            frecuencia = Frecuencias.DIAS_ESPECIFICOS,
            dias = dias,
            completados = haceDias(0, 2)
        )
        assertEquals(2, rachaActual(habito))
        assertEquals(2, mejorRacha(habito))
    }

    ////////////////// X veces por semana

    @Test
    fun vecesPorSemana_cuentaSemanasCumplidas() {
        val lunes = lunesDeLaSemana(hoy)
        val semanaPasada = lunes.minusWeeks(1)
        val semanaAntepasada = lunes.minusWeeks(2)
        val habito = Habito(
            frecuencia = Frecuencias.VECES_POR_SEMANA,
            vecesPorSemana = 2,
            completados = listOf(
                semanaPasada.toString(), semanaPasada.plusDays(1).toString(),
                semanaAntepasada.toString(), semanaAntepasada.plusDays(3).toString()
            )
        )
        // La semana actual todavía no termina, así que no rompe la racha
        assertEquals(2, rachaActual(habito))
        assertEquals(2, mejorRacha(habito))
    }

    @Test
    fun vecesPorSemana_semanaIncompletaCortaLaRacha() {
        val lunes = lunesDeLaSemana(hoy)
        val semanaPasada = lunes.minusWeeks(1)
        val semanaAntepasada = lunes.minusWeeks(2)
        val habito = Habito(
            frecuencia = Frecuencias.VECES_POR_SEMANA,
            vecesPorSemana = 2,
            // La semana pasada solo 1 vez (meta 2)
            completados = listOf(
                semanaPasada.toString(),
                semanaAntepasada.toString(), semanaAntepasada.plusDays(1).toString()
            )
        )
        assertEquals(0, rachaActual(habito))
        assertEquals(1, mejorRacha(habito))
    }

    @Test
    fun metaSemanal_nuncaEsMenorQueUno() {
        assertEquals(1, metaSemanal(Habito(vecesPorSemana = 0)))
        assertEquals(3, metaSemanal(Habito(vecesPorSemana = 3)))
    }

    @Test
    fun faltanEstaSemana_restaLoYaHecho() {
        val habito = Habito(
            frecuencia = Frecuencias.VECES_POR_SEMANA,
            vecesPorSemana = 3,
            completados = listOf(hoy.toString())
        )
        assertEquals(2, faltanEstaSemana(habito))
    }

    @Test
    fun lunesDeLaSemana_siempreEsLunes() {
        assertEquals(1, lunesDeLaSemana(hoy).dayOfWeek.value)
        assertEquals(LocalDate.of(2026, 10, 26), lunesDeLaSemana(LocalDate.of(2026, 11, 1)))
    }

    ////////////////// Texto de la racha

    @Test
    fun textoRacha_enDias() {
        val habito = Habito(frecuencia = Frecuencias.DIARIO)
        assertEquals("1 día", textoRacha(1, habito))
        assertEquals("5 días", textoRacha(5, habito))
    }

    @Test
    fun textoRacha_enSemanas() {
        val habito = Habito(frecuencia = Frecuencias.VECES_POR_SEMANA)
        assertEquals("1 semana", textoRacha(1, habito))
        assertEquals("2 semanas", textoRacha(2, habito))
    }

    ////////////////// Estado de cada día del calendario

    @Test
    fun estadoDelDia_todosLosCasos() {
        val creado = comoDate(hoy.minusDays(5))
        val habito = Habito(fechaCreacion = creado, completados = haceDias(2))

        assertEquals(EstadoDia.CUMPLIDO, estadoDelDia(habito, hoy.minusDays(2)))
        assertEquals(EstadoDia.FALLADO, estadoDelDia(habito, hoy.minusDays(1)))
        assertEquals(EstadoDia.PENDIENTE, estadoDelDia(habito, hoy))
        assertEquals(EstadoDia.PROGRAMADO, estadoDelDia(habito, hoy.plusDays(1)))
        assertEquals(EstadoDia.ANTES_DE_CREAR, estadoDelDia(habito, hoy.minusDays(10)))
    }

    @Test
    fun estadoDelDia_archivadoNoMuestraDiasPorVenir() {
        val habito = Habito(archivado = true)
        assertEquals(EstadoDia.FUTURO, estadoDelDia(habito, hoy.plusDays(1)))
    }

    @Test
    fun estadoDelDia_diaQueNoToca() {
        val habito = Habito(
            frecuencia = Frecuencias.DIAS_ESPECIFICOS,
            dias = listOf(hoy.dayOfWeek.value),
            fechaCreacion = comoDate(hoy.minusDays(30))
        )
        assertEquals(EstadoDia.NO_APLICA, estadoDelDia(habito, hoy.minusDays(1)))
    }

    ////////////////// Calendario del mes

    @Test
    fun casillasDelMes_octubre2026() {
        // El 1 de octubre de 2026 es jueves: 3 huecos antes (lun, mar, mié)
        val casillas = casillasDelMes(Habito(), YearMonth.of(2026, 10))

        assertEquals(0, casillas.size % 7)            // filas completas de 7
        assertEquals(35, casillas.size)                // 3 huecos + 31 días + 1 hueco
        assertEquals(null, casillas[2])
        assertEquals(1, casillas[3]?.numero)
        assertEquals(31, casillas.filterNotNull().size)
    }

    @Test
    fun nombreDelMes_enEspanolConMayuscula() {
        assertEquals("Octubre 2026", nombreDelMes(YearMonth.of(2026, 10)))
    }
}
