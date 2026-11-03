package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.validaciones.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Requerimiento: login y registro validan los datos antes de llamar a Firebase
class ValidacionesTest {

    ////////////////// Nombre

    @Test
    fun nombreVacio_daError() {
        assertEquals("Ingresa tu nombre", Validators.validarNombre("   "))
    }

    @Test
    fun nombreMuyCorto_daError() {
        assertEquals("El nombre es demasiado corto", Validators.validarNombre("A"))
    }

    @Test
    fun nombreMuyLargo_daError() {
        assertEquals("El nombre es demasiado largo", Validators.validarNombre("a".repeat(51)))
    }

    @Test
    fun nombreCorrecto_noDaError() {
        assertNull(Validators.validarNombre("Ana Pérez"))
    }

    ////////////////// Correo

    @Test
    fun correoVacio_daError() {
        assertEquals("Ingresa tu correo", Validators.validarCorreo(""))
    }

    @Test
    fun correoSinArroba_daError() {
        assertEquals("Correo no válido solo se puede usar @gmail", Validators.validarCorreo("anagmail.com"))
    }

    @Test
    fun correoSinDominio_daError() {
        assertEquals("Correo no válido solo se puede usar @gmail", Validators.validarCorreo("ana@gmail"))
    }

    @Test
    fun correoCorrecto_noDaError() {
        assertNull(Validators.validarCorreo("ana.perez@gmail.com"))
    }

    @Test
    fun correoConEspaciosAlrededor_noDaError() {
        assertNull(Validators.validarCorreo("  ana@gmail.com  "))
    }

    ////////////////// Contraseña del registro

    @Test
    fun passwordVacia_daError() {
        assertEquals("Ingresa una contraseña", Validators.validarPassword(""))
    }

    @Test
    fun passwordCorta_daError() {
        assertEquals("Mínimo 8 caracteres", Validators.validarPassword("abc12"))
    }

    @Test
    fun passwordSinNumeros_daError() {
        assertEquals("Debe incluir al menos un número", Validators.validarPassword("abcdefgh"))
    }

    @Test
    fun passwordSinLetras_daError() {
        assertEquals("Debe incluir al menos una letra", Validators.validarPassword("12345678"))
    }

    @Test
    fun passwordCorrecta_noDaError() {
        assertNull(Validators.validarPassword("habitos2026"))
    }

    ////////////////// Contraseña del login y confirmación

    @Test
    fun passwordLoginVacia_daError() {
        assertEquals("Ingresa tu contraseña", Validators.validarPasswordLogin(""))
    }

    @Test
    fun passwordLoginConTexto_noDaError() {
        assertNull(Validators.validarPasswordLogin("x"))
    }

    @Test
    fun confirmacionVacia_daError() {
        assertEquals("Confirma tu contraseña", Validators.validarConfirmacion("habitos2026", ""))
    }

    @Test
    fun confirmacionDistinta_daError() {
        assertEquals("Las contraseñas no coinciden", Validators.validarConfirmacion("habitos2026", "habitos2025"))
    }

    @Test
    fun confirmacionIgual_noDaError() {
        assertNull(Validators.validarConfirmacion("habitos2026", "habitos2026"))
    }
}
