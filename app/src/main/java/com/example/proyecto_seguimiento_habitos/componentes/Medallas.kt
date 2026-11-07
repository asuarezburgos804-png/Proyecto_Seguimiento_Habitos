package com.example.proyecto_seguimiento_habitos.componentes

import com.example.proyecto_seguimiento_habitos.modelo.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Componentes de las medallas (logros)

// Tarjeta de una medalla.
// Si ya se ganó se ve en verde; si falta, se ve apagada y con su barra de progreso.
@Composable
fun TarjetaMedalla(medalla: Medalla, conseguida: Boolean, progreso: Int) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(24.dp)

    // El progreso nunca pasa de la meta (ej. 12 de 7 se muestra como 7 de 7)
    val avanzado = minOf(progreso, medalla.meta)
    val avance = avanzado / medalla.meta.toFloat()

    ////////////////// Interfaz: tarjeta de medalla
    Surface(
        shape = forma,
        color = if (conseguida) colores.surfaceContainerLowest else colores.surfaceContainerLow,
        border = BorderStroke(1.dp, if (conseguida) colores.primary else colores.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .sombraSuave(forma, if (conseguida) 6.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ////////////////// Interfaz: círculo con el ícono
            Surface(
                shape = CircleShape,
                color = if (conseguida) colores.primaryContainer else colores.surfaceContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (conseguida) medalla.icono else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (conseguida) colores.primary else colores.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(16.dp))

            ////////////////// Interfaz: nombre, descripción y progreso
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medalla.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (conseguida) colores.onSurface else colores.onSurfaceVariant
                )
                Text(
                    text = medalla.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant
                )

                if (conseguida) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Desbloqueada",
                        style = MaterialTheme.typography.labelMedium,
                        color = colores.primary
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                    BarraProgreso(
                        avance = avance,
                        colorRiel = colores.surfaceVariant,
                        colorAvance = colores.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$avanzado de ${medalla.meta}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant
                    )
                }
            }
        }
    }
    ////////////////////////
}

// Medalla pequeña (círculo) para el resumen del perfil
@Composable
fun MedallaMini(medalla: Medalla, conseguida: Boolean) {
    val colores = MaterialTheme.colorScheme
    ////////////////// Interfaz: círculo pequeño
    Surface(
        shape = CircleShape,
        color = if (conseguida) colores.primaryContainer else colores.surfaceContainer,
        border = BorderStroke(1.dp, if (conseguida) colores.primary else colores.surfaceVariant),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (conseguida) medalla.icono else Icons.Outlined.Lock,
                contentDescription = medalla.nombre,
                tint = if (conseguida) colores.primary else colores.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
    ////////////////////////
}

// Fila con todas las medallas en pequeño (ganadas en verde, pendientes con candado)
@Composable
fun FilaMedallas(resumen: ResumenLogros, ganadas: List<String>) {
    ////////////////// Interfaz: fila de medallas
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (medalla in MEDALLAS) {
            MedallaMini(medalla, medallaConseguida(medalla, resumen, ganadas))
        }
    }
    ////////////////////////
}

// Aviso que sale cuando se desbloquea una medalla nueva
@Composable
fun DialogoMedallaNueva(medalla: Medalla, onCerrar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    ////////////////// Interfaz: aviso de medalla nueva
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = colores.surfaceContainerLowest,
        icon = {
            Surface(shape = CircleShape, color = colores.primaryContainer, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(medalla.icono, contentDescription = null, tint = colores.primary)
                }
            }
        },
        title = {
            Text(
                text = "¡Medalla desbloqueada!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(medalla.nombre, style = MaterialTheme.typography.titleLarge, color = colores.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = medalla.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Genial") }
        }
    )
    ////////////////////////
}
