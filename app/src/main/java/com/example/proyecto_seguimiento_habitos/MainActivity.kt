package com.example.proyecto_seguimiento_habitos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto_seguimiento_habitos.ui.theme.Proyecto_Seguimiento_HabitosTheme

// Pantallas de la app
private enum class Pantalla { LOGIN, REGISTER, PRINCIPAL, CREAR_HABITO, EDITAR_HABITO }

// Actividad principal
class MainActivity : ComponentActivity() {

    // Arranca la app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ////////////////// Interfaz: tema + navegación
        setContent {
            Proyecto_Seguimiento_HabitosTheme {
                AppNavegacion()
            }
        }
        ////////////////////////
    }
}

// Navegación: decide qué pantalla mostrar
@Composable
private fun AppNavegacion(
    authViewModel: AuthViewModel = viewModel(),
    habitosViewModel: HabitosViewModel = viewModel()
) {
    // Estado de la sesión y de los hábitos
    val uiState by authViewModel.uiState.collectAsState()
    val habitosState by habitosViewModel.uiState.collectAsState()

    // Pantalla y pestaña actuales
    var pantalla by rememberSaveable { mutableStateOf(Pantalla.LOGIN) }
    var pestana by rememberSaveable { mutableStateOf(Pestana.INICIO) }
    var idEditar by rememberSaveable { mutableStateOf<String?>(null) }

    // Snackbar compartido por inicio y perfil
    val snackbarHostState = remember { SnackbarHostState() }

    // Con sesión: escucha los hábitos en tiempo real
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) habitosViewModel.escuchar()
    }

    // Avisos de los hábitos (errores, archivado, eliminado)
    LaunchedEffect(habitosState.mensaje) {
        habitosState.mensaje?.let {
            habitosViewModel.limpiarMensaje()
            snackbarHostState.showSnackbar(it)
        }
    }

    // Barra inferior (Stats y Logros aún no existen)
    val barraInferior: @Composable () -> Unit = {
        BarraNavegacion(actual = pestana) { elegida ->
            when (elegida) {
                Pestana.ANADIR -> pantalla = Pantalla.CREAR_HABITO
                Pestana.STATS, Pestana.LOGROS -> habitosViewModel.avisar("Esta sección estará disponible pronto.")
                else -> pestana = elegida
            }
        }
    }

    ////////////////// Interfaz: pantalla actual
    val habitoEditar = idEditar?.let { habitosViewModel.buscar(it) }
    when {
        // Crear hábito
        uiState.isAuthenticated && pantalla == Pantalla.CREAR_HABITO -> CrearHabitoScreen(
            onCancelar = { pantalla = Pantalla.PRINCIPAL },
            onHabitoGuardado = {
                pestana = Pestana.INICIO
                pantalla = Pantalla.PRINCIPAL
            }
        )

        // Editar hábito
        uiState.isAuthenticated && pantalla == Pantalla.EDITAR_HABITO && habitoEditar != null -> CrearHabitoScreen(
            habitoExistente = habitoEditar,
            onCancelar = { pantalla = Pantalla.PRINCIPAL },
            onHabitoGuardado = {
                habitosViewModel.avisar("Cambios guardados.")
                pantalla = Pantalla.PRINCIPAL
            }
        )

        // Perfil
        uiState.isAuthenticated && pestana == Pestana.PERFIL -> PerfilScreen(
            nombreUsuario = uiState.userName,
            correo = uiState.userEmail,
            estado = habitosState,
            snackbarHostState = snackbarHostState,
            onRestaurar = { habitosViewModel.archivar(it, archivar = false) },
            onEliminar = { habitosViewModel.eliminar(it) },
            // Salir -> login
            onCerrarSesion = {
                habitosViewModel.detener()
                authViewModel.cerrarSesion()
                pestana = Pestana.INICIO
                pantalla = Pantalla.LOGIN
            },
            barraInferior = barraInferior
        )

        // Dashboard
        uiState.isAuthenticated -> DashboardScreen(
            nombreUsuario = uiState.userName,
            estado = habitosState,
            snackbarHostState = snackbarHostState,
            onMarcar = { habito, completado -> habitosViewModel.marcar(habito, completado) },
            onEditar = {
                idEditar = it.id
                pantalla = Pantalla.EDITAR_HABITO
            },
            onArchivar = { habitosViewModel.archivar(it, archivar = true) },
            onEliminar = { habitosViewModel.eliminar(it) },
            barraInferior = barraInferior
        )

        // Registro
        pantalla == Pantalla.REGISTER -> RegisterScreen(
            uiState = uiState,
            onRegister = { nombre, correo, password ->
                authViewModel.registrar(nombre, correo, password)
            },
            onNavigateToLogin = {
                authViewModel.limpiarError()
                pantalla = Pantalla.LOGIN
            },
            onErrorShown = { authViewModel.limpiarError() }
        )

        // Login
        else -> LoginScreen(
            uiState = uiState,
            onLogin = { correo, password, mantener ->
                authViewModel.iniciarSesion(correo, password, mantener)
            },
            onNavigateToRegister = {
                authViewModel.limpiarError()
                pantalla = Pantalla.REGISTER
            },
            onErrorShown = { authViewModel.limpiarError() }
        )
    }
    ////////////////////////
}
