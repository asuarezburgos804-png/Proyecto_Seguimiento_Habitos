package com.example.proyecto_seguimiento_habitos.modelo

// Sistema de puntos y niveles.
// Cada vez que se completa un hábito se ganan 10 puntos (y se pierden si se desmarca).
// Cada 100 puntos se sube un nivel: 0-99 = Nivel 1, 100-199 = Nivel 2, etc.

const val PUNTOS_POR_HABITO = 10
const val PUNTOS_POR_NIVEL = 100

// Nivel según los puntos acumulados
fun nivelDe(puntos: Int): Int {
    val puntosValidos = maxOf(puntos, 0)   // nunca menos de 0
    return puntosValidos / PUNTOS_POR_NIVEL + 1
}

// Puntos que ya se llevan dentro del nivel actual (de 0 a 99)
fun puntosEnEsteNivel(puntos: Int): Int {
    val puntosValidos = maxOf(puntos, 0)
    return puntosValidos % PUNTOS_POR_NIVEL
}

// Puntos que faltan para subir al siguiente nivel
fun puntosParaSubir(puntos: Int): Int {
    return PUNTOS_POR_NIVEL - puntosEnEsteNivel(puntos)
}

// Puntos que corresponden a todo el historial (para cuentas creadas antes de este sistema)
fun puntosPorHistorial(habitos: List<Habito>): Int {
    var total = 0
    for (habito in habitos) {
        total += habito.completados.size * PUNTOS_POR_HABITO
    }
    return total
}
