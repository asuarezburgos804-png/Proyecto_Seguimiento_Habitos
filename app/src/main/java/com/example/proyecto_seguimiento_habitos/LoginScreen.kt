package com.example.proyecto_seguimiento_habitos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Pantalla de inicio de sesión
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (correo: String, password: String, mantenerSesion: Boolean) -> Unit,
    onNavigateToRegister: () -> Unit,
    onErrorShown: () -> Unit
) {
    // Campos del formulario
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var mantenerSesion by rememberSaveable { mutableStateOf(false) }

    // Errores (null = sin error)
    var correoError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

    // Bloqueo temporal por intentos fallidos
    val bloqueado = uiState.segundosBloqueo > 0

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Muestra errores de Firebase
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    // Valida correo y contraseña
    fun validar(): Boolean {
        correoError = Validators.validarCorreo(correo)
        passwordError = Validators.validarPasswordLogin(password)
        return correoError == null && passwordError == null
    }

    ////////////////// Interfaz: login
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ////////////////// Interfaz: logo y título
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .size(64.dp)
                    .sombraSuave(RoundedCornerShape(24.dp), 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Bienvenido de vuelta",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Retoma tus hábitos donde los dejaste ayer",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            ////////////////////////
            Spacer(Modifier.height(32.dp))

            ////////////////// Interfaz: correo
            CampoTexto(
                valor = correo,
                onValorChange = {
                    correo = it
                    if (correoError != null) correoError = null
                },
                placeholder = "Correo electrónico",
                icono = Icons.Outlined.Mail,
                error = correoError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(Modifier.height(4.dp))

            ////////////////// Interfaz: contraseña
            CampoTexto(
                valor = password,
                onValorChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                placeholder = "Contraseña",
                icono = Icons.Outlined.Lock,
                error = passwordError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                iconoFinal = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña"
                            else "Mostrar contraseña",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            ////////////////////////

            ////////////////// Interfaz: mantener sesión iniciada
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = mantenerSesion,
                        role = Role.Checkbox,
                        onValueChange = { mantenerSesion = it }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = mantenerSesion,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Mantener sesión iniciada",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            ////////////////////////
            Spacer(Modifier.height(16.dp))

            ////////////////// Interfaz: aviso de bloqueo temporal
            if (bloqueado) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.LockClock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Demasiados intentos fallidos. Podrás intentarlo de nuevo en " +
                                "${uiState.segundosBloqueo} s.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            ////////////////////////

            ////////////////// Interfaz: botón Iniciar sesión
            BotonPrincipal(
                texto = if (bloqueado) "Bloqueado (${uiState.segundosBloqueo} s)" else "Iniciar sesión",
                onClick = { if (validar()) onLogin(correo, password, mantenerSesion) },
                habilitado = !bloqueado,
                cargando = uiState.isLoading,
                iconoFinal = if (bloqueado) null else Icons.AutoMirrored.Filled.ArrowForward
            )
            ////////////////////////
            Spacer(Modifier.height(32.dp))

            ////////////////// Interfaz: ir a registro
            Text(
                text = "¿No tienes una cuenta?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            BotonSecundario(
                texto = "Crear una cuenta",
                onClick = onNavigateToRegister,
                habilitado = !uiState.isLoading
            )
            ////////////////////////
        }
    }
    ////////////////////////
}

