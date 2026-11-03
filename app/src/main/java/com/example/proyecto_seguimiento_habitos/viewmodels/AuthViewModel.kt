package com.example.proyecto_seguimiento_habitos.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Login, registro y cierre de sesión (Firebase Auth + perfil en Firestore).
// Las pantallas leen estas variables directamente y se redibujan solas cuando cambian.
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    var cargando by mutableStateOf(false)            // esperando respuesta de Firebase
    var error by mutableStateOf<String?>(null)       // mensaje para el snackbar (null = sin error)
    var sesionIniciada by mutableStateOf(false)
    var correo by mutableStateOf<String?>(null)
    var nombre by mutableStateOf<String?>(null)
    var segundosBloqueo by mutableStateOf(0)         // > 0 = login bloqueado

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Datos guardados en el teléfono: "mantener sesión" e intentos fallidos
    private val prefs = application.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    private val maxIntentos = 5
    private val segundosDeBloqueo = 60

    // Se ejecuta al crear el ViewModel (al abrir la app)
    init {
        restaurarSesion()
        retomarBloqueo()
    }

    // Si el usuario marcó "mantener sesión iniciada", entra directo
    private fun restaurarSesion() {
        val usuario = auth.currentUser ?: return
        if (prefs.getBoolean("mantener_sesion", false)) {
            sesionIniciada = true
            correo = usuario.email
            cargarNombre(usuario.uid)
        } else {
            auth.signOut()
        }
    }

    // Si la app se cerró en medio de un bloqueo, lo continúa
    private fun retomarBloqueo() {
        val bloqueoHasta = prefs.getLong("bloqueo_hasta", 0L)
        val restanteMs = bloqueoHasta - System.currentTimeMillis()
        if (restanteMs > 0) {
            iniciarCuentaRegresiva((restanteMs / 1000).toInt() + 1)
        }
    }

    // Crea la cuenta y guarda el perfil en "usuarios/{uid}"
    fun registrar(nombreNuevo: String, correoNuevo: String, password: String) {
        cargando = true
        error = null
        val nombreLimpio = nombreNuevo.trim()
        val correoLimpio = correoNuevo.trim()

        auth.createUserWithEmailAndPassword(correoLimpio, password)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user!!.uid
                val perfil = hashMapOf(
                    "uid" to uid,
                    "nombre" to nombreLimpio,
                    "correo" to correoLimpio,
                    "puntos" to 0,                  // sistema de puntos: empieza en 0
                    "fechaCreacion" to Timestamp.now()
                )
                db.collection("usuarios").document(uid).set(perfil)
                    .addOnFailureListener {
                        error = "Cuenta creada, pero no se pudo guardar el perfil."
                    }

                // La cuenta ya existe: entra a la app
                cargando = false
                sesionIniciada = true
                correo = correoLimpio
                nombre = nombreLimpio
            }
            .addOnFailureListener { e ->
                cargando = false
                error = when (e) {
                    is FirebaseAuthUserCollisionException -> "Este correo ya está registrado."
                    is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil."
                    is FirebaseNetworkException -> "Sin conexión a internet. Revisa tu red."
                    else -> "Revisa el correo y la contraseña."
                }
            }
    }

    // Inicia sesión con correo y contraseña
    fun iniciarSesion(correoIngresado: String, password: String, mantenerSesion: Boolean) {
        if (segundosBloqueo > 0) return
        cargando = true
        error = null
        val correoLimpio = correoIngresado.trim()

        auth.signInWithEmailAndPassword(correoLimpio, password)
            .addOnSuccessListener { resultado ->
                // Guarda la preferencia y reinicia los intentos fallidos
                prefs.edit {
                    putBoolean("mantener_sesion", mantenerSesion)
                    putInt("intentos_fallidos", 0)
                    remove("bloqueo_hasta")
                }
                cargando = false
                sesionIniciada = true
                correo = correoLimpio
                cargarNombre(resultado.user!!.uid)
            }
            .addOnFailureListener { e ->
                cargando = false
                if (e is FirebaseNetworkException) {
                    // Sin internet: no cuenta como intento fallido
                    error = "Sin conexión a internet. Revisa tu red."
                } else if (e is FirebaseTooManyRequestsException) {
                    // Firebase también bloqueó por demasiados intentos
                    bloquear()
                } else {
                    sumarIntentoFallido()
                }
            }
    }

    // Suma un intento fallido; al llegar al máximo, bloquea
    private fun sumarIntentoFallido() {
        val intentos = prefs.getInt("intentos_fallidos", 0) + 1
        if (intentos >= maxIntentos) {
            bloquear()
            return
        }
        prefs.edit { putInt("intentos_fallidos", intentos) }

        val restantes = maxIntentos - intentos
        error = "Correo o contraseña incorrectos."
        if (restantes <= 2) {
            error += " Te quedan $restantes intento(s) antes del bloqueo temporal."
        }
    }

    // Bloquea el login durante 60 segundos
    private fun bloquear() {
        val bloqueoHasta = System.currentTimeMillis() + segundosDeBloqueo * 1000L
        prefs.edit {
            putInt("intentos_fallidos", 0)
            putLong("bloqueo_hasta", bloqueoHasta)
        }
        error = "Demasiados intentos fallidos. Espera $segundosDeBloqueo segundos e inténtalo de nuevo."
        iniciarCuentaRegresiva(segundosDeBloqueo)
    }

    // Baja segundosBloqueo de 1 en 1 cada segundo hasta llegar a 0
    private fun iniciarCuentaRegresiva(segundos: Int) {
        segundosBloqueo = segundos
        viewModelScope.launch {
            while (segundosBloqueo > 0) {
                delay(1000)
                segundosBloqueo--
            }
            prefs.edit { remove("bloqueo_hasta") }
        }
    }

    // Lee el nombre del perfil (para el saludo del inicio)
    private fun cargarNombre(uid: String) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { documento ->
                val nombreGuardado = documento.getString("nombre")
                if (!nombreGuardado.isNullOrBlank()) {
                    nombre = nombreGuardado
                }
            }
    }

    // Cierra la sesión
    fun cerrarSesion() {
        auth.signOut()
        prefs.edit { putBoolean("mantener_sesion", false) }
        sesionIniciada = false
        correo = null
        nombre = null
        error = null
        cargando = false
    }
}
