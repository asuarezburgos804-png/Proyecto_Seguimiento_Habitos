package com.example.proyecto_seguimiento_habitos

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.proyecto_seguimiento_habitos.ui.theme.TertiaryFixed

// Pantalla principal: hábitos de hoy (pendientes y completados)
@Composable
fun DashboardScreen(
    nombreUsuario: String?,
    estado: HabitosUiState,
    snackbarHostState: SnackbarHostState,
    onMarcar: (Habito, Boolean) -> Unit,
    onEditar: (Habito) -> Unit,
    onArchivar: (Habito) -> Unit,
    onEliminar: (Habito) -> Unit,
    onVerCalendario: (Habito) -> Unit,
    barraInferior: @Composable () -> Unit
) {
    // Hábito con la hoja de opciones abierta / pendiente de confirmar borrado
    var idOpciones by rememberSaveable { mutableStateOf<String?>(null) }
    var idEliminar by rememberSaveable { mutableStateOf<String?>(null) }

    // Hábitos activos de hoy: pendientes y completados (se recalculan con cada cambio de Firestore)
    val activos = estado.activos
    val habitosDeHoy = activos.filter { tocaHoy(it) }
    val pendientes = habitosDeHoy.filter { !estaCompletadoHoy(it) }
    val completados = habitosDeHoy.filter { estaCompletadoHoy(it) }
    val otrosDias = activos.size - habitosDeHoy.size

    // Chips del encabezado: mejor racha de todos los hábitos y nivel (1 nivel cada 10 cumplimientos)
    val mejorRachaGlobal = activos.maxOfOrNull { mejorRacha(it) } ?: 0
    val nivel = activos.sumOf { it.completados.size } / 10 + 1

    ////////////////// Interfaz: dashboard
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = barraInferior,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            ////////////////// Interfaz: encabezado (saludo, fecha y chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (nombreUsuario.isNullOrBlank()) "Hola" else "Hola, ${nombreUsuario.substringBefore(" ")}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = fechaLegibleDeHoy(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipEncabezado(
                        icono = Icons.Filled.LocalFireDepartment,
                        texto = "$mejorRachaGlobal",
                        fondo = TertiaryFixed,
                        colorIcono = MaterialTheme.colorScheme.tertiaryContainer,
                        colorTexto = MaterialTheme.colorScheme.tertiary,
                        borde = Color(0xFFF3E0D2)
                    )
                    ChipEncabezado(
                        icono = Icons.Filled.Star,
                        texto = "Nv. $nivel",
                        fondo = MaterialTheme.colorScheme.primaryContainer,
                        colorIcono = MaterialTheme.colorScheme.onPrimaryContainer,
                        colorTexto = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            ////////////////////////
            Spacer(Modifier.height(24.dp))

            ////////////////// Interfaz: contenido
            when {
                // Cargando
                estado.cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                // Sin hábitos hoy
                habitosDeHoy.isEmpty() -> MensajeVacio(hayOtrosHabitos = activos.isNotEmpty())

                // Lista de hábitos
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    ////////////////// Interfaz: resumen del día
                    item(key = "resumen-dia") {
                        TarjetaResumenDia(
                            hechos = completados.size,
                            total = habitosDeHoy.size
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    ////////////////////////

                    ////////////////// Interfaz: Pendientes hoy
                    item(key = "titulo-pendientes") {
                        TituloSeccion("Pendientes hoy", cantidad = pendientes.size)
                    }

                    if (pendientes.isEmpty()) {
                        item(key = "todo-listo") {
                            TarjetaTodoListo()
                        }
                    }

                    items(pendientes, key = { it.id }) { habito ->
                        TarjetaHabito(
                            habito = habito,
                            completado = false,
                            onMarcar = { onMarcar(habito, true) },
                            onAbrirOpciones = { idOpciones = habito.id },
                            modifier = Modifier.animateItem()
                        )
                    }
                    ////////////////////////

                    ////////////////// Interfaz: Completados hoy
                    if (completados.isNotEmpty()) {
                        item(key = "titulo-completados") {
                            Spacer(Modifier.height(16.dp))
                            TituloSeccion("Completados hoy", cantidad = completados.size, apagado = true)
                        }

                        items(completados, key = { it.id }) { habito ->
                            TarjetaHabito(
                                habito = habito,
                                completado = true,
                                onMarcar = { onMarcar(habito, false) },
                                onAbrirOpciones = { idOpciones = habito.id },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    ////////////////////////

                    ////////////////// Interfaz: aviso de otros días
                    if (otrosDias > 0) {
                        item(key = "otros-dias") {
                            Spacer(Modifier.height(8.dp))
                            FilaOtrosDias(otrosDias)
                        }
                    }
                    ////////////////////////
                }
            }
            ////////////////////////
        }
    }
    ////////////////////////

    ////////////////// Interfaz: opciones del hábito
    val habitoOpciones = idOpciones?.let { id -> activos.firstOrNull { it.id == id } }
    if (habitoOpciones != null) {
        OpcionesHabitoSheet(
            habito = habitoOpciones,
            onVerCalendario = {
                idOpciones = null
                onVerCalendario(habitoOpciones)
            },
            onEditar = {
                idOpciones = null
                onEditar(habitoOpciones)
            },
            onArchivar = {
                idOpciones = null
                onArchivar(habitoOpciones)
            },
            onEliminar = {
                idOpciones = null
                idEliminar = habitoOpciones.id
            },
            onCerrar = { idOpciones = null }
        )
    }

    // Confirmación explícita antes de borrar
    val habitoEliminar = idEliminar?.let { id -> activos.firstOrNull { it.id == id } }
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

// Tarjeta verde con el avance del día y una barra de progreso
@Composable
private fun TarjetaResumenDia(hechos: Int, total: Int) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(24.dp)

    // Fracción para la barra (0 a 1) y porcentaje para mostrar
    val fraccion = if (total == 0) 0f else hechos.toFloat() / total
    val porcentaje = (fraccion * 100).toInt()
    val faltan = total - hechos

    ////////////////// Interfaz: resumen del día
    Surface(
        shape = forma,
        color = colores.primary,
        modifier = Modifier
            .fillMaxWidth()
            .sombraSuave(forma)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tu día de hoy",
                        style = MaterialTheme.typography.labelMedium,
                        color = colores.onPrimary.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$hechos de $total hábitos",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colores.onPrimary
                    )
                }
                Text(
                    text = "$porcentaje%",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colores.onPrimary
                )
            }
            Spacer(Modifier.height(16.dp))

            // Barra de progreso (dos cajas: el riel y lo avanzado)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colores.onPrimary.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraccion)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(colores.onPrimary)
                )
            }
            Spacer(Modifier.height(10.dp))

            Text(
                text = when {
                    total == 0 -> "Hoy no tienes hábitos programados."
                    faltan == 0 -> "¡Día completo! Cumpliste todo lo que te tocaba."
                    faltan == 1 -> "Te falta 1 hábito para cerrar el día."
                    else -> "Te faltan $faltan hábitos para cerrar el día."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colores.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
    ////////////////////////
}

// Tarjeta que aparece cuando ya no quedan pendientes
@Composable
private fun TarjetaTodoListo() {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(20.dp)

    ////////////////// Interfaz: sin pendientes
    Surface(
        shape = forma,
        color = colores.surfaceContainerLowest,
        border = BorderStroke(1.dp, colores.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colores.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.TaskAlt,
                        contentDescription = null,
                        tint = colores.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Nada pendiente",
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onBackground
                )
                Text(
                    text = "Ya cumpliste todos tus hábitos de hoy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant
                )
            }
        }
    }
    ////////////////////////
}

// Aviso de los hábitos que están programados para otros días
@Composable
private fun FilaOtrosDias(cantidad: Int) {
    val colores = MaterialTheme.colorScheme
    ////////////////// Interfaz: aviso de otros días
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colores.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = colores.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (cantidad == 1) "Tienes 1 hábito programado para otros días."
                else "Tienes $cantidad hábitos programados para otros días.",
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant
            )
        }
    }
    ////////////////////////
}

// Chip del encabezado (racha / nivel)
@Composable
private fun ChipEncabezado(
    icono: ImageVector,
    texto: String,
    fondo: Color,
    colorIcono: Color,
    colorTexto: Color,
    borde: Color? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = fondo,
        border = borde?.let { BorderStroke(1.dp, it) },
        modifier = Modifier.sombraSuave(RoundedCornerShape(50), 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(texto, style = MaterialTheme.typography.labelMedium, color = colorTexto)
        }
    }
}

// Título de sección con la cantidad al lado, ej. "Pendientes hoy (2)"
@Composable
private fun TituloSeccion(titulo: String, cantidad: Int, apagado: Boolean = false) {
    val colores = MaterialTheme.colorScheme
    ////////////////// Interfaz: título de sección
    Row(
        modifier = Modifier.padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = if (apagado) colores.onSurfaceVariant.copy(alpha = 0.8f) else colores.onBackground
        )
        if (cantidad > 0) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = colores.surfaceContainerLow
            ) {
                Text(
                    text = "$cantidad",
                    style = MaterialTheme.typography.labelMedium,
                    color = colores.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
    ////////////////////////
}

// Tarjeta de un hábito: un toque en el círculo lo marca; tocar la tarjeta abre las opciones
@Composable
private fun TarjetaHabito(
    habito: Habito,
    completado: Boolean,
    onMarcar: () -> Unit,
    onAbrirOpciones: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(24.dp)
    val racha = rachaActual(habito)

    ////////////////// Interfaz: tarjeta del hábito
    Surface(
        onClick = onAbrirOpciones,
        shape = forma,
        color = if (completado) colores.surfaceContainerLow else colores.surfaceContainerLowest,
        border = BorderStroke(1.dp, if (completado) colores.surfaceVariant else Color.White.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .then(if (completado) Modifier else Modifier.sombraSuave(forma))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconoHabito(habito, apagado = completado)
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habito.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (completado) colores.onSurfaceVariant else colores.onBackground,
                    textDecoration = if (completado) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Estado: hora exacta de cumplimiento, u horario pendiente
                    val (iconoEstado, textoEstado) = when {
                        completado -> Icons.Outlined.CheckCircle to
                            (horaCompletadoHoy(habito)?.let { "Completado a las $it" } ?: "Completado")
                        habito.horario.isNotEmpty() -> Icons.Outlined.Schedule to "Pendiente · ${habito.horario}"
                        else -> null to textoFrecuencia(habito)
                    }
                    if (iconoEstado != null) {
                        Icon(
                            iconoEstado,
                            contentDescription = null,
                            tint = colores.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = textoEstado,
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Racha
                    if (racha > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(4.dp).background(colores.outlineVariant, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = colores.tertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = textoRacha(racha, habito),
                            style = MaterialTheme.typography.labelSmall,
                            color = colores.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))
            CirculoCheck(completado = completado, nombre = habito.nombre, onClick = onMarcar)
        }
    }
    ////////////////////////
}

// Círculo de cumplimiento: vacío (pendiente) o verde con check (completado)
@Composable
private fun CirculoCheck(completado: Boolean, nombre: String, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val fondo by animateColorAsState(
        if (completado) colores.primary else Color.Transparent,
        label = "fondoCheck"
    )
    ////////////////// Interfaz: círculo de check (área táctil de 48dp)
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = fondo,
                border = if (completado) null else BorderStroke(2.dp, colores.outlineVariant),
                modifier = Modifier.size(32.dp)
            ) {
                if (completado) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Desmarcar $nombre",
                            tint = colores.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
    ////////////////////////
}

// Mensaje cuando no hay hábitos hoy
@Composable
private fun MensajeVacio(hayOtrosHabitos: Boolean) {
    ////////////////// Interfaz: mensaje sin hábitos
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (hayOtrosHabitos) "Hoy no tienes hábitos programados" else "Todavía no tienes hábitos",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Toca Añadir en la barra inferior para crear uno.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
    ////////////////////////
}
