package com.example.proyecto_seguimiento_habitos

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Estado de la lista de hábitos (compartido por dashboard, perfil y edición)
data class HabitosUiState(
    val habitos: List<Habito> = emptyList(),
    val cargando: Boolean = true,
    val mensaje: String? = null            // error o aviso para el snackbar
) {
    val activos: List<Habito> get() = habitos.filter { !it.archivado }
    val archivados: List<Habito> get() = habitos.filter { it.archivado }
}

// Escucha los hábitos en tiempo real y expone las acciones sobre ellos
class HabitosViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HabitosUiState())
    val uiState: StateFlow<HabitosUiState> = _uiState.asStateFlow()

    private var registro: ListenerRegistration? = null

    private val mostrarError: (String) -> Unit = { mensaje ->
        _uiState.value = _uiState.value.copy(mensaje = mensaje)
    }

    // Empieza a escuchar (al iniciar sesión)
    fun escuchar() {
        if (registro != null) return
        _uiState.value = HabitosUiState(cargando = true)
        registro = HabitosRepository.escucharHabitos(
            onCambio = { lista ->
                _uiState.value = _uiState.value.copy(habitos = lista, cargando = false)
            },
            onError = { mensaje ->
                _uiState.value = _uiState.value.copy(cargando = false, mensaje = mensaje)
            }
        )
    }

    // Deja de escuchar (antes de cerrar sesión)
    fun detener() {
        registro?.remove()
        registro = null
        _uiState.value = HabitosUiState()
    }

    // Busca un hábito por id
    fun buscar(id: String): Habito? = _uiState.value.habitos.firstOrNull { it.id == id }

    // Marca o desmarca hoy
    fun marcar(habito: Habito, completado: Boolean) {
        HabitosRepository.marcarComoCompletado(habito.id, completado, mostrarError)
    }

    // Archiva o restaura
    fun archivar(habito: Habito, archivar: Boolean) {
        HabitosRepository.archivarHabito(habito.id, archivar, mostrarError)
        _uiState.value = _uiState.value.copy(
            mensaje = if (archivar) "\"${habito.nombre}\" se archivó. Su historial se conserva."
            else "\"${habito.nombre}\" volvió a tus hábitos."
        )
    }

    // Elimina hábito e historial
    fun eliminar(habito: Habito) {
        HabitosRepository.eliminarHabito(habito.id, mostrarError)
        _uiState.value = _uiState.value.copy(mensaje = "\"${habito.nombre}\" se eliminó con su historial.")
    }

    // Muestra un aviso en el snackbar
    fun avisar(mensaje: String) {
        _uiState.value = _uiState.value.copy(mensaje = mensaje)
    }

    // Borra el mensaje ya mostrado
    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(mensaje = null)
    }

    override fun onCleared() {
        registro?.remove()
    }
}
