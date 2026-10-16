package com.example.proyecto_seguimiento_habitos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Hoja inferior con las acciones de un hábito: ver progreso, editar, archivar/restaurar y eliminar
// onVerCalendario es opcional: si no se pasa, esa fila no se muestra
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpcionesHabitoSheet(
    habito: Habito,
    onEditar: () -> Unit,
    onArchivar: () -> Unit,
    onEliminar: () -> Unit,
    onCerrar: () -> Unit,
    onVerCalendario: (() -> Unit)? = null
) {
    ////////////////// Interfaz: hoja de opciones
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Encabezado: ícono + nombre + detalle
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoHabito(habito)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(habito.nombre, style = MaterialTheme.typography.titleLarge)
                    Text(
                        detalleHabito(habito),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            if (onVerCalendario != null) {
                FilaOpcion(
                    icono = Icons.Outlined.CalendarMonth,
                    titulo = "Ver progreso",
                    detalle = "Calendario del mes y rachas",
                    onClick = onVerCalendario
                )
            }
            if (!habito.archivado) {
                FilaOpcion(Icons.Outlined.Edit, "Editar hábito", "Nombre, frecuencia u horario", onClick = onEditar)
            }
            FilaOpcion(
                icono = if (habito.archivado) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                titulo = if (habito.archivado) "Restaurar hábito" else "Archivar hábito",
                detalle = if (habito.archivado) "Vuelve a aparecer en tu día"
                else "Se oculta y conserva su historial",
                onClick = onArchivar
            )
            FilaOpcion(
                icono = Icons.Outlined.DeleteOutline,
                titulo = "Eliminar hábito",
                detalle = "Borra el hábito y todo su historial",
                color = MaterialTheme.colorScheme.error,
                onClick = onEliminar
            )
        }
    }
    ////////////////////////
}

// Fila tocable de la hoja de opciones
@Composable
private fun FilaOpcion(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = color)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium, color = color)
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Confirmación explícita antes de eliminar
@Composable
fun DialogoEliminar(
    habito: Habito,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val dias = habito.completados.size
    ////////////////// Interfaz: diálogo de confirmación
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        icon = {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = { Text("¿Eliminar \"${habito.nombre}\"?", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Text(
                text = "Se borrará el hábito y todo su historial" +
                    (if (dias > 0) " ($dias día(s) completados)" else "") +
                    ". Esta acción no se puede deshacer.\n\nSi solo quieres ocultarlo, usa Archivar.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(
                    "Eliminar",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
    ////////////////////////
}

// Ícono del hábito en un cuadrado suave
@Composable
fun IconoHabito(habito: Habito, apagado: Boolean = false) {
    val colores = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (apagado) colores.surfaceContainer else colores.surfaceContainerLow,
        border = BorderStroke(1.dp, colores.surfaceVariant),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                iconoCategoria(habito.categoria),
                contentDescription = null,
                tint = if (apagado) colores.onSurfaceVariant else colores.primary
            )
        }
    }
}

// Pestañas de la barra inferior
enum class Pestana { INICIO, STATS, ANADIR, LOGROS, PERFIL }

// Barra de navegación inferior (como en el mockup del dashboard)
@Composable
fun BarraNavegacion(
    actual: Pestana,
    onSeleccionar: (Pestana) -> Unit
) {
    ////////////////// Interfaz: barra inferior
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .sombraSuave(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemNavegacion(Icons.Outlined.Home, Icons.Filled.Home, "Inicio", actual == Pestana.INICIO) {
                onSeleccionar(Pestana.INICIO)
            }
            ItemNavegacion(Icons.Outlined.Leaderboard, Icons.Outlined.Leaderboard, "Stats", actual == Pestana.STATS) {
                onSeleccionar(Pestana.STATS)
            }
            ItemNavegacion(Icons.Outlined.AddCircleOutline, Icons.Outlined.AddCircleOutline, "Añadir", false) {
                onSeleccionar(Pestana.ANADIR)
            }
            ItemNavegacion(Icons.Outlined.MilitaryTech, Icons.Outlined.MilitaryTech, "Logros", actual == Pestana.LOGROS) {
                onSeleccionar(Pestana.LOGROS)
            }
            ItemNavegacion(Icons.Outlined.Person, Icons.Filled.Person, "Perfil", actual == Pestana.PERFIL) {
                onSeleccionar(Pestana.PERFIL)
            }
        }
    }
    ////////////////////////
}

// Ítem de la barra: activo = píldora verde
@Composable
private fun ItemNavegacion(
    icono: ImageVector,
    iconoActivo: ImageVector,
    texto: String,
    activo: Boolean,
    onClick: () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (activo) colores.primaryContainer else Color.Transparent,
        contentColor = if (activo) colores.onPrimaryContainer else colores.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (activo) 16.dp else 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(if (activo) iconoActivo else icono, contentDescription = null)
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}
