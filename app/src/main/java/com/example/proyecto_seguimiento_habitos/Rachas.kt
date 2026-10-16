package com.example.proyecto_seguimiento_habitos

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Rachas y estado de cada día.
//
// Todo se calcula a partir de habito.completados (las fechas "yyyy-MM-dd" que ya
// guarda Firestore), así que no hay nada extra que almacenar ni migrar.
//
// La idea es siempre la misma: se arma una lista de "periodos" en orden
// cronológico marcando si cada uno se cumplió, y las rachas se leen de esa lista.
//   - Diario y días específicos -> un periodo por cada día que tocaba
//   - X veces por semana        -> un periodo por cada semana (la meta es semanal)

////////////////// Estado de un día

enum class EstadoDia {
    CUMPLIDO,        // se marcó ese día
    FALLADO,         // tocaba y no se marcó
    PENDIENTE,       // es hoy y todavía se puede marcar
    NO_APLICA,       // ese día no tocaba
    ANTES_DE_CREAR,  // el hábito todavía no existía
    PROGRAMADO,      // día futuro en el que sí toca
    FUTURO           // día futuro en el que no toca
}

////////////////// Helpers de fecha
// Se usa java.util.Calendar porque el proyecto tiene minSdk 24 (LocalDate
// necesitaría activar desugaring). Están envueltos aquí para no repetir código.

private const val PATRON_FECHA = "yyyy-MM-dd"

// Hasta cuántos años atrás se revisa el historial (evita bucles enormes)
private const val ANIOS_DE_HISTORIAL = 2

// Calendar -> "yyyy-MM-dd"
internal fun textoDeFecha(calendario: Calendar): String =
    SimpleDateFormat(PATRON_FECHA, Locale.getDefault()).format(calendario.time)

// "yyyy-MM-dd" -> Calendar a las 00:00 (null si el texto no es una fecha válida)
internal fun calendarioDeTexto(fecha: String): Calendar? {
    val date: Date = runCatching {
        SimpleDateFormat(PATRON_FECHA, Locale.getDefault()).parse(fecha)
    }.getOrNull() ?: return null

    return Calendar.getInstance().apply {
        time = date
        alInicioDelDia()
    }
}

// Deja la hora en 00:00 para poder comparar solo fechas
internal fun Calendar.alInicioDelDia() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

// Hoy a las 00:00
internal fun hoyCalendario(): Calendar = Calendar.getInstance().apply { alInicioDelDia() }

// Día de la semana: 1 = Lunes ... 7 = Domingo
internal fun numeroDiaSemana(calendario: Calendar): Int {
    val dia = calendario.get(Calendar.DAY_OF_WEEK)
    return if (dia == Calendar.SUNDAY) 7 else dia - 1
}

// Lunes de la semana a la que pertenece esa fecha (así todas las semanas empiezan igual)
internal fun lunesDeLaSemana(calendario: Calendar): Calendar {
    val lunes = calendario.clone() as Calendar
    lunes.add(Calendar.DAY_OF_YEAR, -(numeroDiaSemana(calendario) - 1))
    lunes.alInicioDelDia()
    return lunes
}

// ¿Son el mismo día?
internal fun mismoDia(uno: Calendar, otro: Calendar): Boolean =
    textoDeFecha(uno) == textoDeFecha(otro)

////////////////// Qué días son obligatorios

// ¿Ese día concreto le toca al hábito?
// "X veces por semana" no fija días: la meta se mide por semana, no por día,
// así que ningún día individual es obligatorio.
fun diaObligatorio(habito: Habito, dia: Calendar): Boolean = when (habito.frecuencia) {
    Frecuencias.DIAS_ESPECIFICOS -> habito.dias.contains(numeroDiaSemana(dia))
    Frecuencias.VECES_POR_SEMANA -> false
    else -> true
}

// Día en que se creó el hábito (null si el documento todavía no guarda esa fecha)
internal fun diaDeCreacion(habito: Habito): Calendar? {
    val creado = habito.fechaCreacion ?: return null
    return Calendar.getInstance().apply {
        time = creado
        alInicioDelDia()
    }
}

// Estado de un día cualquiera para ese hábito (lo que pinta el calendario).
// El orden importa: primero lo que ya pasó de verdad (cumplido), luego los días
// que no se pueden juzgar (futuro o anteriores a la creación del hábito).
fun estadoDelDia(habito: Habito, dia: Calendar): EstadoDia {
    val hoy = hoyCalendario()
    val creacion = diaDeCreacion(habito)
    return when {
        habito.completados.contains(textoDeFecha(dia)) -> EstadoDia.CUMPLIDO
        // Días que todavía no llegan: se avisa cuáles toca cumplir
        dia.after(hoy) -> if (!habito.archivado && diaObligatorio(habito, dia)) {
            EstadoDia.PROGRAMADO
        } else {
            EstadoDia.FUTURO
        }
        creacion != null && dia.before(creacion) -> EstadoDia.ANTES_DE_CREAR
        !diaObligatorio(habito, dia) -> EstadoDia.NO_APLICA
        mismoDia(dia, hoy) -> EstadoDia.PENDIENTE
        else -> EstadoDia.FALLADO
    }
}

////////////////// Metas semanales (para "X veces por semana")

// Meta de la semana (mínimo 1 para no dividir ni comparar contra 0)
fun metaSemanal(habito: Habito): Int = habito.vecesPorSemana.coerceAtLeast(1)

// Cumplimientos registrados en la semana de esa fecha
fun cumplidosEnLaSemana(habito: Habito, dia: Calendar): Int {
    val lunes = textoDeFecha(lunesDeLaSemana(dia))
    return habito.completados.count { fecha ->
        val completado = calendarioDeTexto(fecha) ?: return@count false
        textoDeFecha(lunesDeLaSemana(completado)) == lunes
    }
}

// Cuántos cumplimientos faltan para la meta de esta semana
fun faltanEstaSemana(habito: Habito): Int =
    (metaSemanal(habito) - cumplidosEnLaSemana(habito, hoyCalendario())).coerceAtLeast(0)

////////////////// Periodos cumplidos (la base de las dos rachas)

// cumplidos = un true/false por periodo, del más viejo al más nuevo
// enCurso   = el último periodo todavía se puede cumplir (hoy / esta semana),
//             así que un false al final no debe romper la racha actual
private data class Periodos(val cumplidos: List<Boolean>, val enCurso: Boolean)

// Desde dónde conviene mirar: la primera marca, sin pasar de ANIOS_DE_HISTORIAL
private fun inicioDelHistorial(habito: Habito): Calendar? {
    val primeraMarca = habito.completados.minOrNull() ?: return null
    val desde = calendarioDeTexto(primeraMarca) ?: return null
    val limite = hoyCalendario().apply { add(Calendar.YEAR, -ANIOS_DE_HISTORIAL) }
    return if (desde.before(limite)) limite else desde
}

// Elige cómo contar según la frecuencia
private fun periodos(habito: Habito): Periodos =
    if (habito.frecuencia == Frecuencias.VECES_POR_SEMANA) periodosPorSemana(habito)
    else periodosPorDia(habito)

// Un periodo por cada día programado (diario y días específicos).
// Los días que no tocaban se saltan: ni suman ni rompen la racha.
private fun periodosPorDia(habito: Habito): Periodos {
    val inicio = inicioDelHistorial(habito) ?: return Periodos(emptyList(), false)
    val marcados = habito.completados.toHashSet()
    val hoy = hoyCalendario()
    val cumplidos = mutableListOf<Boolean>()

    val dia = inicio.clone() as Calendar
    while (!dia.after(hoy)) {
        if (diaObligatorio(habito, dia)) {
            cumplidos.add(marcados.contains(textoDeFecha(dia)))
        }
        dia.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Si hoy tocaba y aún no se marca, ese último "false" no cuenta como fallo
    val enCurso = diaObligatorio(habito, hoy) && !marcados.contains(textoDeFecha(hoy))
    return Periodos(cumplidos, enCurso)
}

// Un periodo por cada semana (X veces por semana).
// Una semana se cumple si tiene al menos "vecesPorSemana" marcas.
private fun periodosPorSemana(habito: Habito): Periodos {
    val inicio = inicioDelHistorial(habito) ?: return Periodos(emptyList(), false)
    val meta = metaSemanal(habito)

    // Cuántas marcas tiene cada semana (la clave es el lunes de esa semana)
    val marcasPorSemana = habito.completados
        .mapNotNull { calendarioDeTexto(it) }
        .groupingBy { textoDeFecha(lunesDeLaSemana(it)) }
        .eachCount()

    val semanaActual = lunesDeLaSemana(hoyCalendario())
    val cumplidos = mutableListOf<Boolean>()

    val semana = lunesDeLaSemana(inicio)
    while (!semana.after(semanaActual)) {
        val marcas = marcasPorSemana[textoDeFecha(semana)] ?: 0
        cumplidos.add(marcas >= meta)
        semana.add(Calendar.DAY_OF_YEAR, 7)
    }

    // La semana en curso no rompe la racha mientras no termine
    val marcasDeEstaSemana = marcasPorSemana[textoDeFecha(semanaActual)] ?: 0
    return Periodos(cumplidos, enCurso = marcasDeEstaSemana < meta)
}

////////////////// Las dos rachas

// Racha actual: periodos cumplidos seguidos hasta hoy
fun rachaActual(habito: Habito): Int {
    val calculo = periodos(habito)
    val lista = if (calculo.enCurso) calculo.cumplidos.dropLast(1) else calculo.cumplidos
    return lista.takeLastWhile { it }.size
}

// Mejor racha: la secuencia de cumplimientos más larga de todo el historial
fun mejorRacha(habito: Habito): Int {
    var mejor = 0
    var seguidos = 0
    periodos(habito).cumplidos.forEach { cumplido ->
        if (cumplido) {
            seguidos++
            if (seguidos > mejor) mejor = seguidos
        } else {
            seguidos = 0
        }
    }
    return mejor
}

// Texto de una racha con su unidad, ej. "3 días" o "2 semanas"
fun textoRacha(valor: Int, habito: Habito): String {
    val porSemanas = habito.frecuencia == Frecuencias.VECES_POR_SEMANA
    return when {
        porSemanas && valor == 1 -> "1 semana"
        porSemanas -> "$valor semanas"
        valor == 1 -> "1 día"
        else -> "$valor días"
    }
}

////////////////// Datos del calendario mensual

// Mes que se está viendo (mes: 0 = enero ... 11 = diciembre)
data class MesVisible(val anio: Int, val mes: Int)

// Una casilla con día del calendario
data class CasillaDia(
    val fecha: Calendar,
    val numero: Int,
    val estado: EstadoDia
)

// Resumen del mes que se muestra bajo la cuadrícula
data class ResumenMes(val cumplidos: Int, val fallados: Int) {
    val programados: Int get() = cumplidos + fallados
    val porcentaje: Int get() = if (programados == 0) 0 else cumplidos * 100 / programados
}

// Mes de hoy
fun mesActual(): MesVisible {
    val hoy = hoyCalendario()
    return MesVisible(hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH))
}

// El mes visible cabe en un solo número, así sobrevive al girar la pantalla
fun indiceDeMes(mes: MesVisible): Int = mes.anio * 12 + mes.mes

fun mesDeIndice(indice: Int): MesVisible = MesVisible(indice / 12, indice % 12)

// Nombre del mes con su año, ej. "Octubre 2026"
fun nombreDelMes(mes: MesVisible): String {
    val primerDia = primerDiaDelMes(mes)
    val texto = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-ES")).format(primerDia.time)
    return texto.replaceFirstChar { it.uppercase() }
}

// No tiene sentido navegar más allá del mes actual
fun hayMesSiguiente(mes: MesVisible): Boolean = indiceDeMes(mes) < indiceDeMes(mesActual())

// Casillas del mes para pintar la cuadrícula.
// La semana empieza en lunes, así que al inicio van los huecos (null) necesarios.
fun casillasDelMes(habito: Habito, mes: MesVisible): List<CasillaDia?> {
    val primerDia = primerDiaDelMes(mes)
    val huecosIniciales = numeroDiaSemana(primerDia) - 1   // si cae lunes, 0 huecos
    val totalDeDias = primerDia.getActualMaximum(Calendar.DAY_OF_MONTH)

    val casillas = MutableList<CasillaDia?>(huecosIniciales) { null }
    for (numero in 1..totalDeDias) {
        val dia = primerDiaDelMes(mes).apply { set(Calendar.DAY_OF_MONTH, numero) }
        casillas.add(CasillaDia(dia, numero, estadoDelDia(habito, dia)))
    }
    return casillas
}

// Cumplidos y fallados del mes (los días que no tocaban no entran en la cuenta)
fun resumenDelMes(habito: Habito, mes: MesVisible): ResumenMes {
    val dias = casillasDelMes(habito, mes).filterNotNull()
    return ResumenMes(
        cumplidos = dias.count { it.estado == EstadoDia.CUMPLIDO },
        fallados = dias.count { it.estado == EstadoDia.FALLADO }
    )
}

// Día 1 del mes a las 00:00
private fun primerDiaDelMes(mes: MesVisible): Calendar =
    Calendar.getInstance().apply {
        set(mes.anio, mes.mes, 1)
        alInicioDelDia()
    }
