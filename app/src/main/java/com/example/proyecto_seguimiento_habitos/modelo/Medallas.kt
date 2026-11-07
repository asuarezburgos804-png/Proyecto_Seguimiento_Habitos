package com.example.proyecto_seguimiento_habitos.modelo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate

// Medallas (logros) del usuario.
// Todo se calcula con los hábitos y los puntos que ya existen, así que las medallas
// solo se ganan con hitos reales: rachas, hábitos completados y nivel.

////////////////// Tipos de medalla

// De dónde sale el número que se compara con la meta de la medalla
object TipoMedalla {
    const val RACHA = "RACHA"     // días seguidos cumpliendo
    const val TOTAL = "TOTAL"     // hábitos completados en total
    const val NIVEL = "NIVEL"     // nivel alcanzado
}

// Una medalla de la lista
data class Medalla(
    val id: String,               // nombre corto que se guarda en Firestore
    val nombre: String,
    val descripcion: String,
    val icono: ImageVector,
    val tipo: String,             // TipoMedalla.RACHA, TOTAL o NIVEL
    val meta: Int                 // número que hay que alcanzar
)

// Todas las medallas del juego (para agregar una nueva, solo añade una línea aquí)
val MEDALLAS = listOf(
    Medalla(
        id = "primer_paso",
        nombre = "Primer paso",
        descripcion = "Completa tu primer hábito",
        icono = Icons.Outlined.Flag,
        tipo = TipoMedalla.TOTAL,
        meta = 1
    ),
    Medalla(
        id = "racha_7",
        nombre = "Una semana",
        descripcion = "7 días seguidos cumpliendo",
        icono = Icons.Outlined.LocalFireDepartment,
        tipo = TipoMedalla.RACHA,
        meta = 7
    ),
    Medalla(
        id = "constante_50",
        nombre = "Constante",
        descripcion = "50 hábitos completados en total",
        icono = Icons.Outlined.CheckCircle,
        tipo = TipoMedalla.TOTAL,
        meta = 50
    ),
    Medalla(
        id = "racha_30",
        nombre = "Un mes",
        descripcion = "30 días seguidos cumpliendo",
        icono = Icons.Outlined.Whatshot,
        tipo = TipoMedalla.RACHA,
        meta = 30
    ),
    Medalla(
        id = "nivel_5",
        nombre = "Nivel 5",
        descripcion = "Llega al nivel 5",
        icono = Icons.Outlined.Star,
        tipo = TipoMedalla.NIVEL,
        meta = 5
    ),
    Medalla(
        id = "racha_100",
        nombre = "Cien días",
        descripcion = "100 días seguidos cumpliendo",
        icono = Icons.Outlined.EmojiEvents,
        tipo = TipoMedalla.RACHA,
        meta = 100
    )
)

// Busca una medalla por su id (null si no existe)
fun buscarMedalla(id: String): Medalla? {
    return MEDALLAS.find { it.id == id }
}

////////////////// Racha global (de toda la cuenta, no de un solo hábito)

// Días (sin repetir) en los que se completó al menos un hábito
fun diasConActividad(habitos: List<Habito>): Set<LocalDate> {
    val dias = mutableSetOf<LocalDate>()
    for (habito in habitos) {
        for (fecha in habito.completados) {
            try {
                dias.add(LocalDate.parse(fecha))
            } catch (e: Exception) {
                // fecha con formato raro: se ignora
            }
        }
    }
    return dias
}

// Racha global actual: días seguidos, contando hacia atrás desde hoy.
// Hoy todavía se puede cumplir, así que si falta hoy se empieza a contar desde ayer.
fun rachaGlobal(habitos: List<Habito>): Int {
    val dias = diasConActividad(habitos)
    if (dias.isEmpty()) return 0

    val hoy = LocalDate.now()
    var dia = if (dias.contains(hoy)) hoy else hoy.minusDays(1)

    var racha = 0
    while (dias.contains(dia)) {
        racha++
        dia = dia.minusDays(1)
    }
    return racha
}

// Mejor racha global: la cadena más larga de días seguidos de todo el historial.
// Las medallas usan esta, así que una medalla ganada nunca se pierde.
fun mejorRachaGlobal(habitos: List<Habito>): Int {
    val dias = diasConActividad(habitos)
    var mejor = 0

    for (dia in dias) {
        // Solo se cuenta desde el primer día de cada cadena
        if (dias.contains(dia.minusDays(1))) continue

        var seguidos = 0
        var actual = dia
        while (dias.contains(actual)) {
            seguidos++
            actual = actual.plusDays(1)
        }
        mejor = maxOf(mejor, seguidos)
    }
    return mejor
}

// Cuántas veces se completó un hábito en total (sumando todos los hábitos)
fun totalCompletados(habitos: List<Habito>): Int {
    var total = 0
    for (habito in habitos) {
        total += habito.completados.size
    }
    return total
}

////////////////// Resumen y progreso de las medallas

// Los números del usuario, ya calculados, para no repetir cuentas en las pantallas
data class ResumenLogros(
    val rachaGlobal: Int,
    val mejorRacha: Int,
    val totalCompletados: Int,
    val nivel: Int,
    val puntos: Int
)

fun resumenDe(habitos: List<Habito>, puntos: Int): ResumenLogros {
    return ResumenLogros(
        rachaGlobal = rachaGlobal(habitos),
        mejorRacha = mejorRachaGlobal(habitos),
        totalCompletados = totalCompletados(habitos),
        nivel = nivelDe(puntos),
        puntos = maxOf(puntos, 0)
    )
}

// Lo que lleva el usuario para esa medalla (se compara con medalla.meta)
fun progresoDe(medalla: Medalla, resumen: ResumenLogros): Int {
    return when (medalla.tipo) {
        TipoMedalla.RACHA -> resumen.mejorRacha
        TipoMedalla.TOTAL -> resumen.totalCompletados
        else -> resumen.nivel
    }
}

// ¿Ya se ganó esa medalla?
// "ganadas" son los ids guardados en Firestore: una medalla ganada no se pierde nunca,
// aunque después el usuario borre hábitos o desmarque días.
fun medallaConseguida(medalla: Medalla, resumen: ResumenLogros, ganadas: List<String>): Boolean {
    if (ganadas.contains(medalla.id)) return true
    return progresoDe(medalla, resumen) >= medalla.meta
}

// Medallas ya ganadas
fun medallasObtenidas(resumen: ResumenLogros, ganadas: List<String>): List<Medalla> {
    return MEDALLAS.filter { medallaConseguida(it, resumen, ganadas) }
}

// Medallas que todavía faltan
fun medallasPendientes(resumen: ResumenLogros, ganadas: List<String>): List<Medalla> {
    return MEDALLAS.filter { !medallaConseguida(it, resumen, ganadas) }
}

// Ids de las medallas ganadas (así se guardan en Firestore)
fun idsObtenidos(resumen: ResumenLogros, ganadas: List<String>): List<String> {
    return medallasObtenidas(resumen, ganadas).map { it.id }
}
