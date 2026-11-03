package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.modelo.*
import com.example.proyecto_seguimiento_habitos.validaciones.*
import com.example.proyecto_seguimiento_habitos.pantallas.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Requerimiento: crear / editar un hábito valida nombre, categoría, frecuencia y hora
class FormularioHabitoTest {

    ////////////////// Nombre del hábito

    @Test
    fun nombreVacio_daError() {
        assertEquals("Ingresa el nombre del hábito", HabitoValidators.validarNombre(""))
    }

    @Test
    fun nombreCorto_daError() {
        assertEquals("El nombre es demasiado corto", HabitoValidators.validarNombre("Yo"))
    }

    @Test
    fun nombreLargo_daError() {
        assertEquals("El nombre es demasiado largo", HabitoValidators.validarNombre("a".repeat(41)))
    }

    @Test
    fun nombreCorrecto_noDaError() {
        assertNull(HabitoValidators.validarNombre("Meditar 10 minutos"))
    }

    ////////////////// Categoría

    @Test
    fun sinCategoria_daError() {
        assertEquals("Selecciona una categoría", HabitoValidators.validarCategoria(""))
    }

    @Test
    fun conCategoria_noDaError() {
        assertNull(HabitoValidators.validarCategoria("Salud"))
    }

    ////////////////// Días específicos

    @Test
    fun diasEspecificosSinDias_daError() {
        assertEquals(
            "Selecciona al menos un día",
            HabitoValidators.validarDias(Frecuencias.DIAS_ESPECIFICOS, emptyList())
        )
    }

    @Test
    fun diasEspecificosConDias_noDaError() {
        assertNull(HabitoValidators.validarDias(Frecuencias.DIAS_ESPECIFICOS, listOf(1, 3)))
    }

    @Test
    fun diarioSinDias_noDaError() {
        assertNull(HabitoValidators.validarDias(Frecuencias.DIARIO, emptyList()))
    }

    ////////////////// Veces por semana

    @Test
    fun vecesVacio_daError() {
        assertEquals(
            "Indica cuántas veces por semana",
            HabitoValidators.validarVecesPorSemana(Frecuencias.VECES_POR_SEMANA, "")
        )
    }

    @Test
    fun vecesConLetras_daError() {
        assertEquals(
            "Ingresa solo números",
            HabitoValidators.validarVecesPorSemana(Frecuencias.VECES_POR_SEMANA, "tres")
        )
    }

    @Test
    fun vecesFueraDeRango_daError() {
        val error = "Debe ser un número del 1 al 7"
        assertEquals(error, HabitoValidators.validarVecesPorSemana(Frecuencias.VECES_POR_SEMANA, "0"))
        assertEquals(error, HabitoValidators.validarVecesPorSemana(Frecuencias.VECES_POR_SEMANA, "8"))
    }

    @Test
    fun vecesCorrecto_noDaError() {
        assertNull(HabitoValidators.validarVecesPorSemana(Frecuencias.VECES_POR_SEMANA, "3"))
    }

    @Test
    fun vecesNoImportaSiNoEsXPorSemana() {
        assertNull(HabitoValidators.validarVecesPorSemana(Frecuencias.DIARIO, ""))
    }

    ////////////////// Hora (opcional)

    @Test
    fun horaVacia_noDaError() {
        assertNull(HabitoValidators.validarHorario(""))
    }

    @Test
    fun horaCorrecta_noDaError() {
        assertNull(HabitoValidators.validarHorario("07:30"))
        assertNull(HabitoValidators.validarHorario("23:59"))
    }

    @Test
    fun horaMal_daError() {
        val error = "Usa el formato HH:mm (ej. 07:30)"
        assertEquals(error, HabitoValidators.validarHorario("7:30"))
        assertEquals(error, HabitoValidators.validarHorario("24:00"))
        assertEquals(error, HabitoValidators.validarHorario("12:60"))
        assertEquals(error, HabitoValidators.validarHorario("hola"))
    }

    ////////////////// Valor inicial del campo "veces" al editar

    @Test
    fun textoVeces_habitoNuevoEsVacio() {
        assertEquals("", textoVeces(null))
    }

    @Test
    fun textoVeces_habitoConVecesMuestraElNumero() {
        assertEquals("4", textoVeces(Habito(vecesPorSemana = 4)))
    }
}
