package com.example.proyecto_seguimiento_habitos

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Perfil: datos del usuario, hábitos archivados y cerrar sesión
@Composable
fun PerfilScreen(
    nombreUsuario: String?,
    correo: String?,
    estado: HabitosUiState,
    snackbarHostState: SnackbarHostState,
    onRestaurar: (Habito) -> Unit,
    onEliminar: (Habito) -> Unit,
    onCerrarSesion: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    var idOpciones by rememberSaveable { mutableStateOf<String?>(null) }
    var idEliminar by rememberSaveable { mutableStateOf<String?>(null) }
    val archivados = estado.archivados

    ////////////////// Interfaz: perfil
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = barraInferior,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ////////////////// Interfaz: tarjeta del usuario
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (nombreUsuario?.firstOrNull() ?: correo?.firstOrNull() ?: '?')
                                        .uppercaseChar().toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
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
                    onClick = { idOpciones = habito.id },
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
                        IconButton(onClick = { idOpciones = habito.id }) {
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
                    onClick = onCerrarSesion,
                    icono = Icons.AutoMirrored.Outlined.Logout
                )
            }
            ////////////////////////
        }
    }
    ////////////////////////

    ////////////////// Interfaz: opciones del archivado y confirmación de borrado
    val habitoOpciones = idOpciones?.let { id -> archivados.firstOrNull { it.id == id } }
    if (habitoOpciones != null) {
        OpcionesHabitoSheet(
            habito = habitoOpciones,
            onEditar = {},
            onArchivar = {
                idOpciones = null
                onRestaurar(habitoOpciones)
            },
            onEliminar = {
                idOpciones = null
                idEliminar = habitoOpciones.id
            },
            onCerrar = { idOpciones = null }
        )
    }

    val habitoEliminar = idEliminar?.let { id -> archivados.firstOrNull { it.id == id } }
    if (habitoEliminar != null) {
        DialogoEliminar(
            habito = habitoEliminar,
            onConfirmar = {
                idEliminar = null
                onEliminar(habitoEliminar)
            },
            onCancelar = { idEliminar = null }
        )
    }
    ////////////////////////
}
