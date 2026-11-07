package com.example.proyecto_seguimiento_habitos.pantallas

import com.example.proyecto_seguimiento_habitos.modelo.*
import com.example.proyecto_seguimiento_habitos.viewmodels.*
import com.example.proyecto_seguimiento_habitos.componentes.*

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// Logros: resumen de rachas y lista de medallas obtenidas y pendientes
@Composable
fun LogrosScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    habitosViewModel: HabitosViewModel
) {
    // Todos los números salen de los hábitos y los puntos que ya tiene la app.
    // Se calculan con remember para no rehacer la cuenta en cada redibujado.
    val resumen = remember(habitosViewModel.habitos, habitosViewModel.puntos) {
        resumenDe(habitosViewModel.habitos, habitosViewModel.puntos)
    }
    val ganadas = authViewModel.medallas                      // las que ya estaban guardadas
    val obtenidas = medallasObtenidas(resumen, ganadas)
    val pendientes = medallasPendientes(resumen, ganadas)

    ////////////////// Interfaz: logros
    Scaffold(
        bottomBar = { BarraNavegacion(navController, rutaActual = "logros") },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ////////////////// Interfaz: título y resumen
            item {
                Text("Logros", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "Tus medallas se desbloquean solas al cumplir hitos reales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                TarjetaResumenLogros(resumen)
                Spacer(Modifier.height(20.dp))
            }
            ////////////////////////

            ////////////////// Interfaz: medallas obtenidas
            item {
                Text(
                    text = "Obtenidas (${obtenidas.size} de ${MEDALLAS.size})",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            if (obtenidas.isEmpty()) {
                item {
                    Text(
                        text = "Todavía no tienes medallas. Completa un hábito para ganar la primera.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(obtenidas, key = { it.id }) { medalla ->
                TarjetaMedalla(
                    medalla = medalla,
                    conseguida = true,
                    progreso = progresoDe(medalla, resumen)
                )
            }
            ////////////////////////

            ////////////////// Interfaz: medallas pendientes
            if (pendientes.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("Pendientes (${pendientes.size})", style = MaterialTheme.typography.headlineMedium)
                }

                items(pendientes, key = { it.id }) { medalla ->
                    TarjetaMedalla(
                        medalla = medalla,
                        conseguida = false,
                        progreso = progresoDe(medalla, resumen)
                    )
                }
            }
            ////////////////////////

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    ////////////////////////
}

// Tarjeta blanca con los tres números del usuario
@Composable
private fun TarjetaResumenLogros(resumen: ResumenLogros) {
    val forma = RoundedCornerShape(24.dp)
    ////////////////// Interfaz: resumen de rachas
    Surface(
        shape = forma,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .sombraSuave(forma)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            DatoLogro(Icons.Outlined.LocalFireDepartment, "${resumen.rachaGlobal}", "Racha actual")
            DatoLogro(Icons.Outlined.EmojiEvents, "${resumen.mejorRacha}", "Mejor racha")
            DatoLogro(Icons.Outlined.CheckCircle, "${resumen.totalCompletados}", "Completados")
        }
    }
    ////////////////////////
}

// Un dato del resumen (ícono, número y texto)
@Composable
private fun DatoLogro(icono: ImageVector, numero: String, texto: String) {
    val colores = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
            Icon(icono, contentDescription = null, tint = colores.primary)
        }
        Spacer(Modifier.height(6.dp))
        Text(numero, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
