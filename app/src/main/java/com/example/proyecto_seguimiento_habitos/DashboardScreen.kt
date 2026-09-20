package com.example.proyecto_seguimiento_habitos

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * Pantalla principal. Muestra los hábitos que tocan hoy separados en
 * "Pendientes" y "Completados", y permite marcarlos sin salir de aquí.
 */
@Composable
fun DashboardScreen(
    userEmail: String?,
    onCrearHabito: () -> Unit,
    onLogout: () -> Unit
) {
    var habitos by remember { mutableStateOf<List<Habito>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Escucha los hábitos en Firestore mientras esta pantalla esté visible.
    // Así un hábito nuevo aparece de inmediato, sin recargar nada a mano.
    DisposableEffect(Unit) {
        val registro = HabitosRepository.escucharHabitos(
            onCambio = { lista ->
                habitos = lista
                cargando = false
            },
            onError = { mensaje ->
                mensajeError = mensaje
                cargando = false
            }
        )
        onDispose { registro?.remove() }
    }

    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            mensajeError = null
        }
    }

    // Separamos los hábitos de hoy en pendientes y completados.
    val habitosDeHoy = habitos.filter { tocaHoy(it) }
    val pendientes = habitosDeHoy.filter { !estaCompletadoHoy(it) }
    val completados = habitosDeHoy.filter { estaCompletadoHoy(it) }
    val otrosDias = habitos.size - habitosDeHoy.size

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCrearHabito) {
                Text("Nuevo hábito")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mis hábitos",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = fechaLegibleDeHoy(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (userEmail != null) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onLogout) {
                    Text("Salir")
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                habitosDeHoy.isEmpty() -> MensajeVacio(hayOtrosHabitos = habitos.isNotEmpty())

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    item {
                        TituloSeccion("Pendientes", pendientes.size)
                    }

                    if (pendientes.isEmpty()) {
                        item {
                            Text(
                                text = "Ya cumpliste todos tus hábitos de hoy.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(pendientes, key = { it.id }) { habito ->
                        TarjetaHabito(
                            habito = habito,
                            completado = false,
                            onMarcar = { marcado ->
                                HabitosRepository.marcarComoCompletado(
                                    habitoId = habito.id,
                                    completado = marcado,
                                    onError = { mensajeError = it }
                                )
                            }
                        )
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        TituloSeccion("Completados", completados.size)
                    }

                    items(completados, key = { it.id }) { habito ->
                        TarjetaHabito(
                            habito = habito,
                            completado = true,
                            onMarcar = { marcado ->
                                HabitosRepository.marcarComoCompletado(
                                    habitoId = habito.id,
                                    completado = marcado,
                                    onError = { mensajeError = it }
                                )
                            }
                        )
                    }

                    if (otrosDias > 0) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Tienes $otrosDias hábito(s) programados para otros días.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Título de cada sección del dashboard, con la cantidad de hábitos. */
@Composable
private fun TituloSeccion(titulo: String, cantidad: Int) {
    Text(
        text = "$titulo ($cantidad)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

/** Tarjeta de un hábito con su casilla para marcarlo como completado. */
@Composable
private fun TarjetaHabito(
    habito: Habito,
    completado: Boolean,
    onMarcar: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = completado,
                onCheckedChange = onMarcar
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habito.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (completado) TextDecoration.LineThrough else null
                )
                Text(
                    text = detalleHabito(habito),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Mensaje que se muestra cuando no hay hábitos que cumplir hoy. */
@Composable
private fun MensajeVacio(hayOtrosHabitos: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hayOtrosHabitos) {
                "Hoy no tienes hábitos programados."
            } else {
                "Todavía no tienes hábitos."
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Usa el botón Nuevo hábito para crear uno.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
