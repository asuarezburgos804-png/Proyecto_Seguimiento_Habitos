package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.modelo.*

import org.junit.Assert.assertEquals
import org.junit.Test

// Requerimiento 3 / 15: sistema de puntos y niveles de usuario
class PuntosTest {

    @Test
    fun cadaHabitoCompletadoVale10Puntos() {
        assertEquals(10, PUNTOS_POR_HABITO)
    }

    @Test
    fun nivel_empiezaEnUno() {
        assertEquals(1, nivelDe(0))
        assertEquals(1, nivelDe(99))
    }

    @Test
    fun nivel_subeCada100Puntos() {
        assertEquals(2, nivelDe(100))
        assertEquals(2, nivelDe(199))
        assertEquals(3, nivelDe(200))
        assertEquals(11, nivelDe(1000))
    }

    @Test
    fun nivel_conPuntosNegativosSigueEnUno() {
        assertEquals(1, nivelDe(-30))
    }

    @Test
    fun marcarYDesmarcar_dejanLosPuntosIgual() {
        var puntos = 90
        puntos += PUNTOS_POR_HABITO   // marcar
        assertEquals(2, nivelDe(puntos))
        puntos -= PUNTOS_POR_HABITO   // desmarcar
        assertEquals(90, puntos)
        assertEquals(1, nivelDe(puntos))
    }

    @Test
    fun progresoDentroDelNivel() {
        assertEquals(50, puntosEnEsteNivel(250))
        assertEquals(50, puntosParaSubir(250))
        assertEquals(100, puntosParaSubir(0))
    }

    @Test
    fun puntosPorHistorial_sumaTodasLasMarcas() {
        val habitos = listOf(
            Habito(completados = listOf("2026-10-01", "2026-10-02")),
            Habito(completados = listOf("2026-10-03")),
            Habito()
        )
        assertEquals(30, puntosPorHistorial(habitos))
    }
}
