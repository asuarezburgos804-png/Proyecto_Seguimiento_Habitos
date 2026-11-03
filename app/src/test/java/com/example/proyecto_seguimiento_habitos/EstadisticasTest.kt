package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.modelo.*

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

// Requerimiento 2 / 22: estadísticas por hábito y gráficos semanales y mensuales
class EstadisticasTest {

    private val hoy = LocalDate.now()

    // Fecha de creación de hace N días
    private fun creadoHace(dias: Long): Date {
        return Date.from(hoy.minusDays(dias).atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    // Fechas "yyyy-MM-dd" de hace N días (0 = hoy)
    private fun haceDias(vararg numeros: Long): List<String> {
        val lista = mutableListOf<String>()
        for (n in numeros) lista.add(hoy.minusDays(n).toString())
        return lista
    }

    ////////////////// Total y porcentaje

    @Test
    fun totalCompletado_cuentaTodasLasMarcas() {
        assertEquals(3, totalCompletado(Habito(completados = haceDias(0, 1, 5))))
        assertEquals(0, totalCompletado(Habito()))
    }

    @Test
    fun porcentaje_sinHistorialEsCero() {
        assertEquals(0, porcentajeCumplimiento(Habito()))
    }

    @Test
    fun porcentaje_diario_mitadCumplida() {
        // Creado hace 4 días: tocaban hace 4, 3, 2 y 1 día (hoy está pendiente y no cuenta)
        val habito = Habito(fechaCreacion = creadoHace(4), completados = haceDias(4, 2))
        assertEquals(50, porcentajeCumplimiento(habito))
    }

    @Test
    fun porcentaje_diario_todoCumplido() {
        val habito = Habito(fechaCreacion = creadoHace(2), completados = haceDias(0, 1, 2))
        assertEquals(100, porcentajeCumplimiento(habito))
    }

    @Test
    fun porcentaje_vecesPorSemana_cuentaSemanas() {
        val lunes = lunesDeLaSemana(hoy)
        val semanaPasada = lunes.minusWeeks(1)
        val semanaAntepasada = lunes.minusWeeks(2)
        val habito = Habito(
            frecuencia = Frecuencias.VECES_POR_SEMANA,
            vecesPorSemana = 2,
            completados = listOf(
                semanaAntepasada.toString(), semanaAntepasada.plusDays(1).toString(), // cumplida
                semanaPasada.toString()                                               // no llegó
            )
        )
        // 1 semana cumplida de 2 terminadas (la actual no cuenta todavía)
        assertEquals(50, porcentajeCumplimiento(habito))
    }

    ////////////////// Gráfico semanal

    @Test
    fun graficoSemanal_tiene8BarrasYLaUltimaEsEstaSemana() {
        val habito = Habito(completados = listOf(hoy.toString()))
        val barras = graficoSemanal(habito)
        assertEquals(8, barras.size)
        assertEquals(1, barras.last().valor)

        val lunes = lunesDeLaSemana(hoy)
        assertEquals("${lunes.dayOfMonth}/${lunes.monthValue}", barras.last().etiqueta)
    }

    @Test
    fun graficoSemanal_cuentaCadaSemanaPorSeparado() {
        val lunes = lunesDeLaSemana(hoy)
        val habito = Habito(
            completados = listOf(
                lunes.minusWeeks(1).toString(),
                lunes.minusWeeks(1).plusDays(2).toString(),
                lunes.minusWeeks(3).toString()
            )
        )
        val barras = graficoSemanal(habito)
        assertEquals(2, barras[6].valor)   // semana pasada
        assertEquals(0, barras[5].valor)   // hace 2 semanas
        assertEquals(1, barras[4].valor)   // hace 3 semanas
    }

    ////////////////// Gráfico mensual

    @Test
    fun graficoMensual_tiene6BarrasConNombreDelMes() {
        val barras = graficoMensual(Habito())
        assertEquals(6, barras.size)
        assertEquals(NOMBRES_MESES[YearMonth.now().monthValue - 1], barras.last().etiqueta)
    }

    @Test
    fun graficoMensual_cuentaLasMarcasDeCadaMes() {
        val esteMes = YearMonth.now()
        val mesPasado = esteMes.minusMonths(1)
        val habito = Habito(
            completados = listOf(
                esteMes.atDay(1).toString(),
                mesPasado.atDay(3).toString(),
                mesPasado.atDay(4).toString(),
                mesPasado.atDay(5).toString()
            )
        )
        val barras = graficoMensual(habito)
        assertEquals(1, barras[5].valor)   // este mes
        assertEquals(3, barras[4].valor)   // mes pasado
        assertEquals(0, barras[3].valor)
    }
}
