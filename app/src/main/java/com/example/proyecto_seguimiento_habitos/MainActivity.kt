package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.viewmodels.*
import com.example.proyecto_seguimiento_habitos.pantallas.*
import com.example.proyecto_seguimiento_habitos.notificaciones.*

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.proyecto_seguimiento_habitos.ui.theme.Proyecto_Seguimiento_HabitosTheme

// Actividad principal: aplica el tema y muestra la navegación
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Proyecto_Seguimiento_HabitosTheme {
                AppNavigation()
            }
        }
    }
}

// Navegación: una ruta (texto) por cada pantalla
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ViewModels compartidos por todas las pantallas
    val authViewModel: AuthViewModel = viewModel()
    val habitosViewModel: HabitosViewModel = viewModel()

    // Ventana que pide permiso para mostrar notificaciones (Android 13 o más)
    val context = LocalContext.current
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Cuando se inicia sesión: carga los hábitos y va al inicio (borra login y registro del historial)
    LaunchedEffect(authViewModel.sesionIniciada) {
        if (authViewModel.sesionIniciada) {
            habitosViewModel.escuchar()
            navController.navigate("inicio") {
                popUpTo(0) { inclusive = true }
            }
            if (!Notificaciones.hayPermiso(context)) {
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Cada vez que cambian los hábitos (nuevo, editado, marcado...) se reprograman las alarmas
    LaunchedEffect(habitosViewModel.habitos) {
        if (authViewModel.sesionIniciada) {
            Recordatorios.programar(context, habitosViewModel.habitos)
        }
    }

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(navController, authViewModel)
        }

        composable("register") {
            RegisterScreen(navController, authViewModel)
        }

        // Pantalla de práctica (NUEVA.kt): antes no estaba registrada y el botón "Nueva" cerraba la app
        composable("nueva") {
            button(
                onCancelar = { navController.popBackStack() },
                onHabitoGuardado = { navController.popBackStack() }
            )
        }

        composable("inicio") {
            DashboardScreen(navController, authViewModel, habitosViewModel)
        }

        composable("perfil") {
            PerfilScreen(navController, authViewModel, habitosViewModel)
        }

        composable("crear") {
            CrearHabitoScreen(navController, habitosViewModel, habitoExistente = null)
        }

        // Editar: el id del hábito viaja en la ruta, ej. "editar/abc123"
        composable("editar/{id}") { ruta ->
            val id = ruta.arguments?.getString("id")
            val habito = habitosViewModel.buscar(id)
            if (habito != null) {
                CrearHabitoScreen(navController, habitosViewModel, habitoExistente = habito)
            }
        }

        // Calendario sin hábito elegido (desde la barra inferior)
        composable("calendario") {
            CalendarioScreen(navController, habitosViewModel, idInicial = null)
        }

        // Calendario de un hábito concreto, ej. "calendario/abc123"
        composable("calendario/{id}") { ruta ->
            val id = ruta.arguments?.getString("id")
            CalendarioScreen(navController, habitosViewModel, idInicial = id)
        }

        composable("logros") {
            LogrosScreen()
        }
    }
}

// Vuelve al inicio sin apilar pantallas repetidas
fun irAInicio(navController: NavController) {
    navController.navigate("inicio") {
        popUpTo("inicio") { inclusive = true }
    }
}
