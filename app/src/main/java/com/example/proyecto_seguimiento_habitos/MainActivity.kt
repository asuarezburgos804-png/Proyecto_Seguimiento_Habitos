package com.example.proyecto_seguimiento_habitos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto_seguimiento_habitos.ui.theme.Proyecto_Seguimiento_HabitosTheme

private enum class Pantalla { LOGIN, REGISTER, DASHBOARD, CREAR_HABITO }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Proyecto_Seguimiento_HabitosTheme {
                AppNavegacion()
            }
        }
    }
}

@Composable
private fun AppNavegacion(
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    var pantalla by remember { mutableStateOf(Pantalla.LOGIN) }

    when {
        // Con la sesión iniciada se entra directo al dashboard.
        uiState.isAuthenticated && pantalla == Pantalla.CREAR_HABITO -> CrearHabitoScreen(
            onCancelar = { pantalla = Pantalla.DASHBOARD },
            onHabitoCreado = { pantalla = Pantalla.DASHBOARD }
        )

        uiState.isAuthenticated -> DashboardScreen(
            userEmail = uiState.userEmail,
            onCrearHabito = { pantalla = Pantalla.CREAR_HABITO },
            onLogout = {
                authViewModel.cerrarSesion()
                pantalla = Pantalla.LOGIN
            }
        )

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

        else -> LoginScreen(
            uiState = uiState,
            onLogin = { correo, password -> authViewModel.iniciarSesion(correo, password) },
            onNavigateToRegister = {
                authViewModel.limpiarError()
                pantalla = Pantalla.REGISTER
            },
            onErrorShown = { authViewModel.limpiarError() }
        )
    }
}
