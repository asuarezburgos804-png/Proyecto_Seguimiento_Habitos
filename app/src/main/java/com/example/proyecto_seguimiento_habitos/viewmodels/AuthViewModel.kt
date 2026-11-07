package com.example.proyecto_seguimiento_habitos.viewmodels

import com.example.proyecto_seguimiento_habitos.modelo.*

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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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

    // Perfil del usuario (se leen de usuarios/{uid} y se actualizan solos)
    var avatar by mutableStateOf("")                     // foto de perfil en texto ("" = sin foto)
    var medallas by mutableStateOf(listOf<String>())     // ids de las medallas ya desbloqueadas
    var perfilCargado by mutableStateOf(false)           // true cuando ya llegaron los datos del perfil
    var medallaNueva by mutableStateOf<Medalla?>(null)   // medalla recién ganada (para el aviso)

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Conexión en tiempo real con el perfil del usuario
    private var escuchaPerfil: ListenerRegistration? = null

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
            escucharPerfil(usuario.uid)
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
                escucharPerfil(uid)
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
                escucharPerfil(resultado.user!!.uid)
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

    ////////////////// Perfil del usuario (usuarios/{uid})

    // Escucha el perfil en tiempo real: nombre, foto y medallas ganadas
    @Suppress("UNCHECKED_CAST")
    private fun escucharPerfil(uid: String) {
        escuchaPerfil?.remove()
        escuchaPerfil = db.collection("usuarios").document(uid)
            .addSnapshotListener { documento, fallo ->
                if (fallo != null || documento == null) return@addSnapshotListener

                val nombreGuardado = documento.getString("nombre")
                if (!nombreGuardado.isNullOrBlank()) {
                    nombre = nombreGuardado
                }
                avatar = documento.getString("avatar") ?: ""
                medallas = documento.get("medallas") as? List<String> ?: emptyList()
                perfilCargado = true
            }
    }

    // Guarda el nombre nuevo (merge = solo cambia ese campo del perfil)
    fun guardarNombre(nombreNuevo: String) {
        val uid = auth.currentUser?.uid ?: return
        val nombreLimpio = nombreNuevo.trim()
        if (nombreLimpio.isEmpty()) {
            error = "El nombre no puede estar vacío."
            return
        }
        nombre = nombreLimpio
        db.collection("usuarios").document(uid)
            .set(hashMapOf("nombre" to nombreLimpio), SetOptions.merge())
            .addOnFailureListener { error = "No se pudo guardar el nombre." }
    }

    // Guarda la foto de perfil ("" para quitarla)
    fun guardarAvatar(avatarNuevo: String) {
        val uid = auth.currentUser?.uid ?: return
        avatar = avatarNuevo
        db.collection("usuarios").document(uid)
            .set(hashMapOf("avatar" to avatarNuevo), SetOptions.merge())
            .addOnFailureListener { error = "No se pudo guardar la foto." }
    }

    ////////////////// Medallas

    // Revisa si el usuario acaba de ganar medallas nuevas.
    // Devuelve solo las nuevas (las que ya tenía no se avisan otra vez).
    fun revisarMedallas(habitos: List<Habito>, puntos: Int): List<Medalla> {
        if (!perfilCargado) return emptyList()          // todavía no llegaron las medallas guardadas
        val uid = auth.currentUser?.uid ?: return emptyList()

        val resumen = resumenDe(habitos, puntos)
        val conseguidas = idsObtenidos(resumen, medallas)   // incluye las que ya tenía
        val nuevas = conseguidas.filter { !medallas.contains(it) }
        if (nuevas.isEmpty()) return emptyList()

        // Se guardan para que el aviso salga una sola vez
        medallas = conseguidas
        db.collection("usuarios").document(uid)
            .set(hashMapOf("medallas" to conseguidas), SetOptions.merge())
            .addOnFailureListener { error = "No se pudieron guardar las medallas." }

        val medallasNuevas = nuevas.mapNotNull { buscarMedalla(it) }
        medallaNueva = medallasNuevas.firstOrNull()     // la primera se muestra en el aviso
        return medallasNuevas
    }

    // Cierra la sesión
    fun cerrarSesion() {
        auth.signOut()
        prefs.edit { putBoolean("mantener_sesion", false) }
        escuchaPerfil?.remove()
        escuchaPerfil = null
        sesionIniciada = false
        correo = null
        nombre = null
        avatar = ""
        medallas = emptyList()
        perfilCargado = false
        medallaNueva = null
        error = null
        cargando = false
    }

    // Se llama cuando se cierra la app
    override fun onCleared() {
        escuchaPerfil?.remove()
    }
}
