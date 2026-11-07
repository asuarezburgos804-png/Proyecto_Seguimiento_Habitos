package com.example.proyecto_seguimiento_habitos.pantallas

import com.example.proyecto_seguimiento_habitos.modelo.*
import com.example.proyecto_seguimiento_habitos.viewmodels.*
import com.example.proyecto_seguimiento_habitos.componentes.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// Perfil: foto y nombre del usuario, nivel, puntos, racha global, medallas,
// hábitos archivados y cerrar sesión
@Composable
fun PerfilScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    habitosViewModel: HabitosViewModel
) {
    val nombreUsuario = authViewModel.nombre
    val correo = authViewModel.correo
    val avatar = authViewModel.avatar

    var idMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var idBorrar by rememberSaveable { mutableStateOf<String?>(null) }
    var editando by rememberSaveable { mutableStateOf(false) }   // ventana de editar perfil abierta
    val archivados = habitosViewModel.archivados()

    // Números del usuario (racha global, medallas, nivel...).
    // Se calculan con remember para no rehacer la cuenta en cada redibujado.
    val resumen = remember(habitosViewModel.habitos, habitosViewModel.puntos) {
        resumenDe(habitosViewModel.habitos, habitosViewModel.puntos)
    }
    val ganadas = authViewModel.medallas                       // las que ya estaban guardadas
    val cuantasGanadas = medallasObtenidas(resumen, ganadas).size

    // Snackbar: avisos de los hábitos (restaurado, eliminado, errores...)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(habitosViewModel.mensaje) {
        val mensaje = habitosViewModel.mensaje
        if (mensaje != null) {
            habitosViewModel.mensaje = null
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    // Cierra sesión y vuelve al login (borra todo el historial de pantallas)
    fun cerrarSesion() {
        habitosViewModel.detener()
        authViewModel.cerrarSesion()
        navController.navigate("login") {
            popUpTo(0) { inclusive = true }
        }
    }

    ////////////////// Interfaz: perfil
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BarraNavegacion(navController, rutaActual = "perfil") },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ////////////////// Interfaz: tarjeta del usuario (foto, nombre y botón de editar)
            item {
                Text("Perfil", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .sombraSuave(RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarUsuario(
                            avatar = avatar,
                            inicial = inicialDe(nombreUsuario, correo),
                            tamano = 64.dp
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nombreUsuario ?: "Usuario",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = correo ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { editando = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil")
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            ////////////////////////

            ////////////////// Interfaz: nivel y puntos
            item {
                TarjetaNivel(puntos = habitosViewModel.puntos)
                Spacer(Modifier.height(12.dp))
            }
            ////////////////////////

            ////////////////// Interfaz: racha global y medallas ganadas
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaDato(
                        icono = Icons.Outlined.LocalFireDepartment,
                        numero = "${resumen.rachaGlobal}",
                        texto = if (resumen.rachaGlobal == 1) "día de racha" else "días de racha",
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaDato(
                        icono = Icons.Outlined.MilitaryTech,
                        numero = "$cuantasGanadas de ${MEDALLAS.size}",
                        texto = "medallas",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            ////////////////////////

            ////////////////// Interfaz: medallas en pequeño (se tocan para ver todas)
            item {
                Surface(
                    onClick = { navController.navigate("logros") { launchSingleTop = true } },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .sombraSuave(RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mis medallas", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Toca para ver todas tus medallas y las que te faltan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        FilaMedallas(resumen, ganadas)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            ////////////////////////

            ////////////////// Interfaz: hábitos archivados
            item {
                Text("Hábitos archivados", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Ocultos de tu día, con su historial intacto. Tócalos para restaurarlos o eliminarlos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (archivados.isEmpty()) {
                item {
                    Text(
                        text = "No tienes hábitos archivados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(archivados, key = { it.id }) { habito ->
                Surface(
                    onClick = { idMenu = habito.id },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconoHabito(habito, apagado = true)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                habito.nombre,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${habito.completados.size} día(s) registrados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { idMenu = habito.id }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Opciones de ${habito.nombre}")
                        }
                    }
                }
            }
            ////////////////////////

            ////////////////// Interfaz: cerrar sesión
            item {
                Spacer(Modifier.height(24.dp))
                BotonSecundario(
                    texto = "Cerrar sesión",
                    onClick = { cerrarSesion() },
                    icono = Icons.AutoMirrored.Outlined.Logout
                )
            }
            ////////////////////////
        }
    }
    ////////////////////////

    ////////////////// Interfaz: editar nombre y foto
    if (editando) {
        DialogoEditarPerfil(
            nombreActual = nombreUsuario ?: "",
            avatarActual = avatar,
            onGuardar = { nombreNuevo, avatarNuevo ->
                editando = false
                authViewModel.guardarNombre(nombreNuevo)
                authViewModel.guardarAvatar(avatarNuevo)
                habitosViewModel.mensaje = "Perfil actualizado."
            },
            onCancelar = { editando = false }
        )
    }
    ////////////////////////

    ////////////////// Interfaz: opciones del archivado y confirmación de borrado
    val habitoMenu = archivados.find { it.id == idMenu }
    if (habitoMenu != null) {
        OpcionesHabitoSheet(
            habito = habitoMenu,
            onEditar = {},
            onArchivar = {
                idMenu = null
                habitosViewModel.archivar(habitoMenu, archivar = false)
            },
            onEliminar = {
                idMenu = null
                idBorrar = habitoMenu.id
            },
            onCerrar = { idMenu = null }
        )
    }

    val habitoBorrar = archivados.find { it.id == idBorrar }
    if (habitoBorrar != null) {
        DialogoEliminar(
            habito = habitoBorrar,
            onConfirmar = {
                idBorrar = null
                habitosViewModel.eliminar(habitoBorrar)
            },
            onCancelar = { idBorrar = null }
        )
    }
    ////////////////////////
}

// Tarjeta pequeña con un número del usuario (racha o medallas)
@Composable
private fun TarjetaDato(
    icono: ImageVector,
    numero: String,
    texto: String,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(24.dp)
    ////////////////// Interfaz: tarjeta de un dato
    Surface(
        shape = forma,
        color = colores.surfaceContainerLowest,
        modifier = modifier.sombraSuave(forma)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                Icon(icono, contentDescription = null, tint = colores.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(numero, style = MaterialTheme.typography.headlineMedium)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        }
    }
    ////////////////////////
}
