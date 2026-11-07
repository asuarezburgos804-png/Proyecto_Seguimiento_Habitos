package com.example.proyecto_seguimiento_habitos.componentes

import com.example.proyecto_seguimiento_habitos.modelo.*

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Componentes del perfil del usuario (foto y edición)

// Foto de perfil redonda. Si el usuario no subió foto, muestra la inicial de su nombre.
@Composable
fun AvatarUsuario(avatar: String, inicial: String, tamano: Dp = 72.dp) {
    val colores = MaterialTheme.colorScheme
    // Solo se vuelve a convertir la foto cuando cambia (no en cada redibujado)
    val imagen = remember(avatar) { avatarABitmap(avatar) }

    ////////////////// Interfaz: foto de perfil
    Surface(
        shape = CircleShape,
        color = colores.primaryContainer,
        modifier = Modifier.size(tamano)
    ) {
        if (imagen != null) {
            Image(
                bitmap = imagen.asImageBitmap(),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = inicial,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colores.onPrimaryContainer
                )
            }
        }
    }
    ////////////////////////
}

// Ventana para cambiar el nombre y la foto del perfil.
// onGuardar devuelve el nombre y la foto nuevos (la foto va como texto, "" = sin foto).
@Composable
fun DialogoEditarPerfil(
    nombreActual: String,
    avatarActual: String,
    onGuardar: (String, String) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf(nombreActual) }
    var avatar by remember { mutableStateOf(avatarActual) }

    // Galería de fotos de Android (no pide permisos)
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val foto = uriAAvatar(context, uri)
            if (foto != null) {
                avatar = foto
            } else {
                Toast.makeText(context, "No se pudo leer esa imagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ////////////////// Interfaz: editar perfil
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text("Editar perfil", style = MaterialTheme.typography.headlineMedium) },
        text = {
            // verticalScroll: si la pantalla es chica (o está en horizontal) el contenido se desliza
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {

                ////////////////// Interfaz: foto y botones de la foto
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarUsuario(avatar = avatar, inicial = inicialDe(nombre, null), tamano = 72.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                elegirFoto.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (avatar.isBlank()) "Subir foto" else "Cambiar foto")
                        }
                        if (avatar.isNotBlank()) {
                            TextButton(onClick = { avatar = "" }) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Quitar foto")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                ////////////////// Interfaz: nombre
                CampoTexto(
                    valor = nombre,
                    onValorChange = { nombre = it },
                    placeholder = "Tu nombre",
                    icono = Icons.Outlined.Person,
                    etiqueta = "Nombre",
                    error = if (nombre.isBlank()) "Escribe tu nombre." else null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(nombre.trim(), avatar) },
                enabled = nombre.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
    ////////////////////////
}
