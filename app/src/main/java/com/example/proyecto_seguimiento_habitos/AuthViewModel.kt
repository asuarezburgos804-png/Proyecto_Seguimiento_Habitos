package com.example.proyecto_seguimiento_habitos

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Estado de login/registro que observa la UI
data class AuthUiState(
    val isLoading: Boolean = false,        // esperando a Firebase
    val errorMessage: String? = null,      // null = sin error
    val isAuthenticated: Boolean = false,  // sesión iniciada
    val userEmail: String? = null,         // correo del usuario
    val userName: String? = null,          // nombre del perfil
    val segundosBloqueo: Int = 0           // > 0 = login bloqueado temporalmente
)

// Registro e inicio de sesión (Firebase Auth + perfil en Firestore)
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_INTENTOS = 5                 // intentos fallidos antes del bloqueo
        private const val BLOQUEO_MS = 60_000L     // duración del bloqueo (1 minuto)

        // Mensaje genérico: no revela si el correo existe
        const val ERROR_GENERICO = "Correo o contraseña incorrectos."

        private const val PREFS = "sesion"
        private const val KEY_MANTENER = "mantener_sesion"
        private const val KEY_INTENTOS = "intentos_fallidos"
        private const val KEY_BLOQUEO_HASTA = "bloqueo_hasta"
    }

    // Firebase Auth y Firestore
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Preferencias locales (mantener sesión e intentos fallidos)
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Estado interno y su versión de solo lectura para la UI
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Cuenta regresiva del bloqueo
    private var cuentaRegresiva: Job? = null

    init {
        restaurarSesion()
        reanudarBloqueo()
    }

    // Al abrir la app: entra directo solo si el usuario marcó "mantener sesión iniciada"
    private fun restaurarSesion() {
        val usuario = auth.currentUser ?: return
        if (prefs.getBoolean(KEY_MANTENER, false)) {
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true,
                userEmail = usuario.email
            )
            cargarNombre(usuario.uid)
        } else {
            auth.signOut()
        }
    }

    // Si la app se cerró durante un bloqueo, lo retoma
    private fun reanudarBloqueo() {
        val restante = prefs.getLong(KEY_BLOQUEO_HASTA, 0L) - System.currentTimeMillis()
        if (restante > 0) iniciarCuentaRegresiva(restante)
    }

    // Crea la cuenta y guarda el perfil en "usuarios/{uid}"
    fun registrar(nombre: String, correo: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val correoLimpio = correo.trim()
        val nombreLimpio = nombre.trim()

        auth.createUserWithEmailAndPassword(correoLimpio, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No se pudo crear el usuario. Intenta de nuevo."
                    )
                    return@addOnSuccessListener
                }

                // Perfil del usuario
                val perfil = hashMapOf(
                    "uid" to uid,
                    "nombre" to nombreLimpio,
                    "correo" to correoLimpio,
                    "fechaCreacion" to Timestamp.now()
                )
                db.collection("usuarios").document(uid)
                    .set(perfil)
                    .addOnSuccessListener {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userEmail = correoLimpio,
                            userName = nombreLimpio,
                            errorMessage = null
                        )
                    }
                    .addOnFailureListener { e ->
                        // Cuenta creada, pero falló el perfil
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userEmail = correoLimpio,
                            userName = nombreLimpio,
                            errorMessage = "Cuenta creada, pero no se pudo guardar el perfil: ${e.localizedMessage}"
                        )
                    }
            }
            .addOnFailureListener { e ->
                // No se pudo crear la cuenta
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapearErrorRegistro(e)
                )
            }
    }

    // Inicia sesión con correo y contraseña
    fun iniciarSesion(correo: String, password: String, mantenerSesion: Boolean) {
        // Bloqueado: no se intenta
        if (_uiState.value.segundosBloqueo > 0) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val correoLimpio = correo.trim()

        auth.signInWithEmailAndPassword(correoLimpio, password)
            .addOnSuccessListener { result ->
                // Éxito: reinicia los intentos y guarda la preferencia
                prefs.edit {
                    putBoolean(KEY_MANTENER, mantenerSesion)
                    putInt(KEY_INTENTOS, 0)
                    remove(KEY_BLOQUEO_HASTA)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    userEmail = correoLimpio,
                    errorMessage = null
                )
                result.user?.uid?.let { cargarNombre(it) }
            }
            .addOnFailureListener { e ->
                when (e) {
                    // Sin internet: no cuenta como intento fallido
                    is FirebaseNetworkException -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Sin conexión a internet. Revisa tu red."
                    )
                    // Firebase también bloqueó por demasiados intentos
                    is FirebaseTooManyRequestsException -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        bloquear()
                    }
                    // Cualquier otro fallo: mensaje genérico + cuenta el intento
                    else -> registrarIntentoFallido()
                }
            }
    }

    // Suma un intento fallido y bloquea al llegar al máximo
    private fun registrarIntentoFallido() {
        val intentos = prefs.getInt(KEY_INTENTOS, 0) + 1
        if (intentos >= MAX_INTENTOS) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            bloquear()
            return
        }
        prefs.edit { putInt(KEY_INTENTOS, intentos) }
        val restantes = MAX_INTENTOS - intentos
        val aviso = if (restantes <= 2) {
            " Te quedan $restantes intento(s) antes del bloqueo temporal."
        } else {
            ""
        }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = ERROR_GENERICO + aviso
        )
    }

    // Bloquea el login durante BLOQUEO_MS
    private fun bloquear() {
        val hasta = System.currentTimeMillis() + BLOQUEO_MS
        prefs.edit {
            putInt(KEY_INTENTOS, 0)
            putLong(KEY_BLOQUEO_HASTA, hasta)
        }
        _uiState.value = _uiState.value.copy(
            errorMessage = "Demasiados intentos fallidos. Espera ${BLOQUEO_MS / 1000} segundos e inténtalo de nuevo."
        )
        iniciarCuentaRegresiva(BLOQUEO_MS)
    }

    // Actualiza los segundos restantes cada segundo
    private fun iniciarCuentaRegresiva(duracionMs: Long) {
        cuentaRegresiva?.cancel()
        cuentaRegresiva = viewModelScope.launch {
            val fin = System.currentTimeMillis() + duracionMs
            while (true) {
                val restante = fin - System.currentTimeMillis()
                if (restante <= 0) break
                _uiState.value = _uiState.value.copy(
                    segundosBloqueo = ((restante + 999) / 1000).toInt()
                )
                delay(1000)
            }
            prefs.edit { remove(KEY_BLOQUEO_HASTA) }
            _uiState.value = _uiState.value.copy(segundosBloqueo = 0)
        }
    }

    // Lee el nombre del perfil (para el saludo del dashboard)
    private fun cargarNombre(uid: String) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre")
                if (!nombre.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(userName = nombre)
                }
            }
    }

    // Cierra sesión y vuelve al login
    fun cerrarSesion() {
        auth.signOut()
        prefs.edit { putBoolean(KEY_MANTENER, false) }
        _uiState.value = AuthUiState(segundosBloqueo = _uiState.value.segundosBloqueo)
    }

    // Borra el error ya mostrado
    fun limpiarError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Errores de registro -> mensajes en español
    private fun mapearErrorRegistro(e: Exception): String {
        return when (e) {
            is FirebaseAuthUserCollisionException -> "Este correo ya está registrado."
            is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil."
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> "Revisa el correo y la contraseña."
            is FirebaseNetworkException -> "Sin conexión a internet. Revisa tu red."
            else -> e.localizedMessage ?: "Ocurrió un error. Intenta de nuevo."
        }
    }
}
