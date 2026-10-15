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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyecto_seguimiento_habitos.ui.theme.Crema

// Pantalla de registro
@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (nombre: String, correo: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onErrorShown: () -> Unit
) {
    // Campos del formulario
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var aceptaTerminos by rememberSaveable { mutableStateOf(false) }

    // Errores (null = sin error)
    var nombreError by rememberSaveable { mutableStateOf<String?>(null) }
    var correoError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmacionError by rememberSaveable { mutableStateOf<String?>(null) }
    var terminosError by rememberSaveable { mutableStateOf<String?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Muestra errores de Firebase
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    // Valida todos los campos
    fun validar(): Boolean {
        nombreError = Validators.validarNombre(nombre)
        correoError = Validators.validarCorreo(correo)
        passwordError = Validators.validarPassword(password)
        confirmacionError = Validators.validarConfirmacion(password, confirmacion)
        terminosError = if (aceptaTerminos) null else "Debes aceptar los términos para continuar"
        return nombreError == null &&
            correoError == null &&
            passwordError == null &&
            confirmacionError == null &&
            terminosError == null
    }

    ////////////////// Interfaz: registro (fondo con aura suave)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Crema, MaterialTheme.colorScheme.surface),
                        radius = 1400f
                    )
                )
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            ////////////////// Interfaz: barra superior (volver + marca)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onNavigateToLogin,
                    enabled = !uiState.isLoading,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Spa,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "MINDFUL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.size(40.dp))
            }
            ////////////////////////
            Spacer(Modifier.height(16.dp))

            ////////////////// Interfaz: ícono y título
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(56.dp)
                        .sombraSuave(RoundedCornerShape(16.dp), 8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.EnergySavingsLeaf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Comienza tu viaje",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Cultiva hábitos positivos día a día en un espacio sereno",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(260.dp)
                )
            }
            ////////////////////////
            Spacer(Modifier.height(24.dp))

            ////////////////// Interfaz: nombre
            CampoTexto(
                valor = nombre,
                onValorChange = {
                    nombre = it
                    if (nombreError != null) nombreError = null
                },
                etiqueta = "Nombre completo",
                placeholder = "Tu nombre completo",
                icono = Icons.Outlined.Person,
                error = nombreError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(Modifier.height(4.dp))

            ////////////////// Interfaz: correo
            CampoTexto(
                valor = correo,
                onValorChange = {
                    correo = it
                    if (correoError != null) correoError = null
                },
                etiqueta = "Correo electrónico",
                placeholder = "tucorreo@ejemplo.com",
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
                    // Revalida la confirmación
                    if (confirmacionError != null) {
                        confirmacionError = Validators.validarConfirmacion(it, confirmacion)
                    }
                },
                etiqueta = "Contraseña",
                placeholder = "Mínimo 8 caracteres",
                icono = Icons.Outlined.Lock,
                error = passwordError,
                ayuda = "Mínimo 8 caracteres, con letras y números",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
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
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            )
            Spacer(Modifier.height(4.dp))

            ////////////////// Interfaz: confirmar contraseña
            CampoTexto(
                valor = confirmacion,
                onValorChange = {
                    confirmacion = it
                    if (confirmacionError != null) confirmacionError = null
                },
                etiqueta = "Confirmar contraseña",
                placeholder = "Repite tu contraseña",
                icono = Icons.Outlined.VerifiedUser,
                error = confirmacionError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation()
            )
            ////////////////////////

            ////////////////// Interfaz: términos y privacidad
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = aceptaTerminos,
                        role = Role.Checkbox,
                        onValueChange = {
                            aceptaTerminos = it
                            if (it) terminosError = null
                        }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = aceptaTerminos,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Acepto los Términos de servicio y la Política de privacidad.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            TextoError(terminosError)
            ////////////////////////
            Spacer(Modifier.height(16.dp))

            ////////////////// Interfaz: botón Crear cuenta
            BotonPrincipal(
                texto = "Crear cuenta",
                onClick = { if (validar()) onRegister(nombre, correo, password) },
                cargando = uiState.isLoading
            )
            ////////////////////////
            Spacer(Modifier.height(24.dp))

            ////////////////// Interfaz: separador "o" e ir al login
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.secondaryContainer)
                Text(
                    text = "o",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Ya tienes una cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            ////////////////////////
        }
    }
    ////////////////////////
}
