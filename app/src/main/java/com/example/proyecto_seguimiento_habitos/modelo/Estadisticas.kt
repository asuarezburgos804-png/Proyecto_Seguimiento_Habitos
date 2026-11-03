package com.example.proyecto_seguimiento_habitos.modelo

import java.time.LocalDate
import java.time.YearMonth

// Estadísticas de un hábito, calculadas con su historial real (habito.completados).

// Una barra de un gráfico: su texto de abajo y su valor
data class Barra(val etiqueta: String, val valor: Int)

// Nombres cortos de los meses (posición 0 = enero)
val NOMBRES_MESES = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

// Total de veces que se completó el hábito
fun totalCompletado(habito: Habito): Int {
    return habito.completados.size
}

// Desde qué día se cuentan las estadísticas: la creación del hábito,
// o la primera marca si es más antigua (null si no hay ninguna de las dos)
fun inicioDelHabito(habito: Habito): LocalDate? {
    var inicio = diaDeCreacion(habito)
    val primeraMarca = habito.completados.minOrNull()
    if (primeraMarca != null) {
        val dia = LocalDate.parse(primeraMarca)
        if (inicio == null || dia.isBefore(inicio)) inicio = dia
    }
    return inicio
}

// Porcentaje de cumplimiento (0 a 100):
//   - Diario y días específicos: días cumplidos / días que tocaban
//   - X por semana: semanas que llegaron a la meta / semanas que ya terminaron
fun porcentajeCumplimiento(habito: Habito): Int {
    val hoy = LocalDate.now()
    var inicio = inicioDelHabito(habito) ?: return 0
    if (inicio.isBefore(hoy.minusYears(2))) inicio = hoy.minusYears(2)   // máximo 2 años atrás

    var cumplidos = 0
    var fallados = 0

    if (habito.frecuencia == Frecuencias.VECES_POR_SEMANA) {
        val semanaActual = lunesDeLaSemana(hoy)
        var semana = lunesDeLaSemana(inicio)
        while (!semana.isAfter(hoy)) {
            if (semanaCumplida(habito, semana)) {
                cumplidos++
            } else if (semana != semanaActual) {
                fallados++   // la semana actual todavía no termina, no cuenta como fallada
            }
            semana = semana.plusWeeks(1)
        }
    } else {
        var dia = inicio
        while (!dia.isAfter(hoy)) {
            val estado = estadoDelDia(habito, dia)
            if (estado == EstadoDia.CUMPLIDO) cumplidos++
            if (estado == EstadoDia.FALLADO) fallados++
            dia = dia.plusDays(1)
        }
    }

    val total = cumplidos + fallados
    if (total == 0) return 0
    return cumplidos * 100 / total
}

// Gráfico semanal: veces completado en cada una de las últimas semanas.
// La última barra es la semana actual. Etiqueta = día/mes del lunes, ej. "27/10".
fun graficoSemanal(habito: Habito, semanas: Int = 8): List<Barra> {
    val barras = mutableListOf<Barra>()
    val lunesActual = lunesDeLaSemana(LocalDate.now())
    for (i in semanas - 1 downTo 0) {
        val lunes = lunesActual.minusWeeks(i.toLong())
        val etiqueta = "${lunes.dayOfMonth}/${lunes.monthValue}"
        barras.add(Barra(etiqueta, cumplidosEnLaSemana(habito, lunes)))
    }
    return barras
}

// Gráfico mensual: veces completado en cada uno de los últimos meses.
// La última barra es el mes actual. Etiqueta = nombre corto del mes, ej. "Oct".
fun graficoMensual(habito: Habito, meses: Int = 6): List<Barra> {
    val barras = mutableListOf<Barra>()
    val mesActual = YearMonth.now()
    for (i in meses - 1 downTo 0) {
        val mes = mesActual.minusMonths(i.toLong())
        // Las fechas se guardan como "2026-10-27": las del mes empiezan con "2026-10"
        val veces = habito.completados.count { it.startsWith(mes.toString()) }
        barras.add(Barra(NOMBRES_MESES[mes.monthValue - 1], veces))
    }
    return barras
}
