package com.example.proyecto_seguimiento_habitos.modelo

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Rachas y estado de cada día.
// Todo se calcula con habito.completados (las fechas "yyyy-MM-dd" guardadas en Firestore).
//   - Diario y días específicos -> la racha se cuenta en días
//   - X veces por semana        -> la racha se cuenta en semanas

// Estado de un día en el calendario
enum class EstadoDia {
    CUMPLIDO,        // se marcó ese día
    FALLADO,         // tocaba y no se marcó
    PENDIENTE,       // es hoy y todavía se puede marcar
    NO_APLICA,       // ese día no tocaba
    ANTES_DE_CREAR,  // el hábito todavía no existía
    PROGRAMADO,      // día futuro en el que sí toca
    FUTURO           // día futuro en el que no toca
}

// Una casilla del calendario del mes
data class CasillaDia(
    val fecha: LocalDate,
    val numero: Int,
    val estado: EstadoDia
)

// Hasta cuántos días atrás se revisa una racha (2 años, para no hacer bucles enormes)
private const val DIAS_MAXIMOS = 730L

////////////////// Ayudas básicas

// ¿Se marcó el hábito en esa fecha?
fun marcadoEn(habito: Habito, dia: LocalDate): Boolean {
    return habito.completados.contains(dia.toString())
}

// ¿Ese día le toca al hábito?
// "X veces por semana" no tiene días fijos, así que ningún día es obligatorio.
fun diaObligatorio(habito: Habito, dia: LocalDate): Boolean {
    return when (habito.frecuencia) {
        Frecuencias.DIAS_ESPECIFICOS -> habito.dias.contains(dia.dayOfWeek.value)
        Frecuencias.VECES_POR_SEMANA -> false
        else -> true
    }
}

// Día en que se creó el hábito (null si no se guardó esa fecha)
fun diaDeCreacion(habito: Habito): LocalDate? {
    val fecha = habito.fechaCreacion ?: return null
    return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

// Lunes de la semana de esa fecha
fun lunesDeLaSemana(dia: LocalDate): LocalDate {
    return dia.minusDays(dia.dayOfWeek.value - 1L)
}

////////////////// Estado de un día (lo que pinta el calendario)

fun estadoDelDia(habito: Habito, dia: LocalDate): EstadoDia {
    val hoy = LocalDate.now()
    val creacion = diaDeCreacion(habito)

    if (marcadoEn(habito, dia)) return EstadoDia.CUMPLIDO

    // Días que todavía no llegan
    if (dia.isAfter(hoy)) {
        if (!habito.archivado && diaObligatorio(habito, dia)) return EstadoDia.PROGRAMADO
        return EstadoDia.FUTURO
    }

    if (creacion != null && dia.isBefore(creacion)) return EstadoDia.ANTES_DE_CREAR
    if (!diaObligatorio(habito, dia)) return EstadoDia.NO_APLICA
    if (dia == hoy) return EstadoDia.PENDIENTE
    return EstadoDia.FALLADO
}

////////////////// Metas semanales ("X veces por semana")

// Meta de la semana (mínimo 1)
fun metaSemanal(habito: Habito): Int {
    return maxOf(habito.vecesPorSemana, 1)
}

// Cuántas veces se marcó en la semana de esa fecha
fun cumplidosEnLaSemana(habito: Habito, dia: LocalDate): Int {
    val lunes = lunesDeLaSemana(dia)
    var cuenta = 0
    for (i in 0..6) {
        if (marcadoEn(habito, lunes.plusDays(i.toLong()))) cuenta++
    }
    return cuenta
}

// ¿Esa semana llegó a la meta?
fun semanaCumplida(habito: Habito, lunes: LocalDate): Boolean {
    return cumplidosEnLaSemana(habito, lunes) >= metaSemanal(habito)
}

// Cuántas veces faltan para la meta de esta semana
fun faltanEstaSemana(habito: Habito): Int {
    val faltan = metaSemanal(habito) - cumplidosEnLaSemana(habito, LocalDate.now())
    return maxOf(faltan, 0)
}

////////////////// Racha actual

fun rachaActual(habito: Habito): Int {
    if (habito.completados.isEmpty()) return 0
    val hoy = LocalDate.now()

    // Por semanas: cuenta semanas cumplidas seguidas hacia atrás
    if (habito.frecuencia == Frecuencias.VECES_POR_SEMANA) {
        var semana = lunesDeLaSemana(hoy)
        // La semana actual todavía no termina: si no llegó a la meta, no rompe la racha
        if (!semanaCumplida(habito, semana)) semana = semana.minusWeeks(1)

        var racha = 0
        while (semanaCumplida(habito, semana)) {
            racha++
            semana = semana.minusWeeks(1)
        }
        return racha
    }

    // Por días: cuenta días obligatorios marcados seguidos hacia atrás
    var dia = hoy
    // Hoy todavía se puede marcar: si falta, empezamos a contar desde ayer
    if (!marcadoEn(habito, hoy)) dia = hoy.minusDays(1)

    var racha = 0
    val limite = hoy.minusDays(DIAS_MAXIMOS)
    while (dia.isAfter(limite)) {
        if (diaObligatorio(habito, dia)) {
            if (marcadoEn(habito, dia)) racha++ else break   // un día fallado corta la racha
        }
        dia = dia.minusDays(1)
    }
    return racha
}

////////////////// Mejor racha

fun mejorRacha(habito: Habito): Int {
    val primeraMarca = habito.completados.minOrNull() ?: return 0
    val hoy = LocalDate.now()

    // Se empieza a revisar desde la primera marca (máximo 2 años atrás)
    var inicio = LocalDate.parse(primeraMarca)
    val limite = hoy.minusDays(DIAS_MAXIMOS)
    if (inicio.isBefore(limite)) inicio = limite

    var mejor = 0
    var seguidos = 0

    // Por semanas: recorre semana por semana hasta hoy
    if (habito.frecuencia == Frecuencias.VECES_POR_SEMANA) {
        var semana = lunesDeLaSemana(inicio)
        while (!semana.isAfter(hoy)) {
            if (semanaCumplida(habito, semana)) {
                seguidos++
                mejor = maxOf(mejor, seguidos)
            } else {
                seguidos = 0
            }
            semana = semana.plusWeeks(1)
        }
        return mejor
    }

    // Por días: recorre día por día hasta hoy (los días que no tocaban se saltan)
    var dia = inicio
    while (!dia.isAfter(hoy)) {
        if (diaObligatorio(habito, dia)) {
            if (marcadoEn(habito, dia)) {
                seguidos++
                mejor = maxOf(mejor, seguidos)
            } else {
                seguidos = 0
            }
        }
        dia = dia.plusDays(1)
    }
    return mejor
}

// Texto de una racha con su unidad, ej. "3 días" o "2 semanas"
fun textoRacha(valor: Int, habito: Habito): String {
    if (habito.frecuencia == Frecuencias.VECES_POR_SEMANA) {
        return if (valor == 1) "1 semana" else "$valor semanas"
    }
    return if (valor == 1) "1 día" else "$valor días"
}

////////////////// Calendario del mes

// Nombre del mes con su año, ej. "Octubre 2026"
fun nombreDelMes(mes: YearMonth): String {
    val texto = mes.format(DateTimeFormatter.ofPattern("MMMM yyyy", ESPANOL))
    return texto.replaceFirstChar { it.uppercase() }
}

// Casillas del mes en filas de 7 (la semana empieza en lunes).
// Los huecos antes del día 1 y después del último día son null.
fun casillasDelMes(habito: Habito, mes: YearMonth): List<CasillaDia?> {
    val casillas = mutableListOf<CasillaDia?>()

    // Huecos antes del día 1 (si el mes empieza en lunes, no hay huecos)
    val huecos = mes.atDay(1).dayOfWeek.value - 1
    repeat(huecos) { casillas.add(null) }

    // Un día por casilla
    for (numero in 1..mes.lengthOfMonth()) {
        val dia = mes.atDay(numero)
        casillas.add(CasillaDia(dia, numero, estadoDelDia(habito, dia)))
    }

    // Huecos al final para completar la última semana
    while (casillas.size % 7 != 0) casillas.add(null)
    return casillas
}
