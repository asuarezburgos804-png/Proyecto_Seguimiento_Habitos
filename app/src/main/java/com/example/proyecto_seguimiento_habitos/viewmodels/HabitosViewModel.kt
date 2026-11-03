package com.example.proyecto_seguimiento_habitos.viewmodels

import com.example.proyecto_seguimiento_habitos.modelo.*
import com.example.proyecto_seguimiento_habitos.basededatos.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration

// Guarda la lista de hábitos y las acciones sobre ellos.
// Las pantallas leen estas variables directamente y se redibujan solas cuando cambian.
class HabitosViewModel : ViewModel() {

    var habitos by mutableStateOf(listOf<Habito>())   // todos los hábitos del usuario
    var cargando by mutableStateOf(true)              // true mientras llega la primera lista
    var mensaje by mutableStateOf<String?>(null)      // aviso para el snackbar (null = nada)
    var puntos by mutableStateOf(0)                   // puntos del usuario (el nivel sale de aquí)

    // Conexiones en tiempo real con Firestore
    private var escucha: ListenerRegistration? = null
    private var escuchaPuntos: ListenerRegistration? = null

    // Hábitos activos y archivados
    fun activos(): List<Habito> = habitos.filter { !it.archivado }
    fun archivados(): List<Habito> = habitos.filter { it.archivado }

    // Busca un hábito por su id
    fun buscar(id: String?): Habito? = habitos.find { it.id == id }

    // Empieza a escuchar los hábitos (al iniciar sesión)
    fun escuchar() {
        if (escucha != null) return
        cargando = true
        escucha = HabitosRepository.escucharHabitos(
            onCambio = { lista ->
                habitos = lista
                cargando = false
                // Los puntos se escuchan después de tener los hábitos (ver escucharPuntos)
                if (escuchaPuntos == null) escucharPuntos()
            },
            onError = { error ->
                cargando = false
                mensaje = error
            }
        )
    }

    // Escucha los puntos guardados en la cuenta del usuario
    private fun escucharPuntos() {
        escuchaPuntos = HabitosRepository.escucharPuntos { valor ->
            if (valor == null) {
                // Cuenta creada antes del sistema de puntos: empieza con los puntos de su historial
                HabitosRepository.guardarPuntos(puntosPorHistorial(habitos))
            } else {
                puntos = valor
            }
        }
    }

    // Deja de escuchar y limpia todo (al cerrar sesión)
    fun detener() {
        escucha?.remove()
        escucha = null
        escuchaPuntos?.remove()
        escuchaPuntos = null
        habitos = emptyList()
        puntos = 0
        cargando = true
        mensaje = null
    }

    // Crea o edita un hábito (si tiene id es una edición)
    fun guardar(habito: Habito, onExito: () -> Unit, onError: (String) -> Unit) {
        if (habito.id.isEmpty()) {
            HabitosRepository.crearHabito(habito, onExito, onError)
        } else {
            HabitosRepository.actualizarHabito(habito, onExito, onError)
        }
    }

    // Marca o desmarca hoy
    // Al marcar se ganan puntos; al desmarcar se quitan
    fun marcar(habito: Habito, completado: Boolean) {
        HabitosRepository.marcarComoCompletado(habito.id, completado) { error -> mensaje = error }
        if (completado) {
            HabitosRepository.sumarPuntos(PUNTOS_POR_HABITO)
        } else {
            HabitosRepository.sumarPuntos(-PUNTOS_POR_HABITO)
        }
    }

    // Archiva o restaura
    fun archivar(habito: Habito, archivar: Boolean) {
        HabitosRepository.archivarHabito(habito.id, archivar) { error -> mensaje = error }
        if (archivar) {
            mensaje = "\"${habito.nombre}\" se archivó. Su historial se conserva."
        } else {
            mensaje = "\"${habito.nombre}\" volvió a tus hábitos."
        }
    }

    // Elimina el hábito y su historial
    fun eliminar(habito: Habito) {
        HabitosRepository.eliminarHabito(habito.id) { error -> mensaje = error }
        mensaje = "\"${habito.nombre}\" se eliminó con su historial."
    }

    // Se llama cuando se cierra la app
    override fun onCleared() {
        escucha?.remove()
        escuchaPuntos?.remove()
    }
}
