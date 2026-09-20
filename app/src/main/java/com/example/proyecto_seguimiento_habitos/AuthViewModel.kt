package com.example.proyecto_seguimiento_habitos

import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado de la pantalla de autenticación que observa la UI.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null
)

/**
 * Maneja el registro e inicio de sesión con Firebase Authentication
 * y guarda el perfil del usuario en Cloud Firestore (colección "usuarios").
 */
class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // La app siempre arranca en el login: aunque Firebase recuerde la sesión
    // anterior, no marcamos isAuthenticated hasta que el usuario escriba sus
    // credenciales. Al validarlas, iniciarSesion() lleva al dashboard.

    fun registrar(nombre: String, correo: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val correoLimpio = correo.trim()

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

                // Guardamos el perfil del usuario en Firestore.
                val perfil = hashMapOf(
                    "uid" to uid,
                    "nombre" to nombre.trim(),
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
                            errorMessage = null
                        )
                    }
                    .addOnFailureListener { e ->
                        // La cuenta se creó en Auth, pero falló guardar el perfil.
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userEmail = correoLimpio,
                            errorMessage = "Cuenta creada, pero no se pudo guardar el perfil: ${e.localizedMessage}"
                        )
                    }
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapearError(e)
                )
            }
    }

    fun iniciarSesion(correo: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val correoLimpio = correo.trim()

        auth.signInWithEmailAndPassword(correoLimpio, password)
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    userEmail = correoLimpio,
                    errorMessage = null
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapearError(e)
                )
            }
    }

    fun cerrarSesion() {
        auth.signOut()
        _uiState.value = AuthUiState()
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /** Traduce las excepciones de Firebase a mensajes claros en español. */
    private fun mapearError(e: Exception): String {
        return when (e) {
            is FirebaseAuthUserCollisionException -> "Este correo ya está registrado."
            is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil."
            is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
            is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
            is FirebaseNetworkException -> "Sin conexión a internet. Revisa tu red."
            else -> e.localizedMessage ?: "Ocurrió un error. Intenta de nuevo."
        }
    }
}
