package com.example.proyecto_seguimiento_habitos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Calendario de progreso: un mes por hábito con los días en colores.
// Los datos salen del historial real (habito.completados), no hay nada simulado.
@Composable
fun CalendarioScreen(
    habitos: List<Habito>,
    idInicial: String?,
    onVolver: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    // Hábito elegido (si el id ya no existe, cae al primero de la lista)
    var idSeleccionado by rememberSaveable { mutableStateOf(idInicial) }
    val habito = habitos.firstOrNull { it.id == idSeleccionado } ?: habitos.firstOrNull()

    // Mes visible, guardado como un solo número para que sobreviva al girar la pantalla
    var mesIndice by rememberSaveable { mutableStateOf(indiceDeMes(mesActual())) }
    val mes = mesDeIndice(mesIndice)

    // Botón atrás -> dashboard
    BackHandler { onVolver() }

    ////////////////// Interfaz: progreso
    Scaffold(
        bottomBar = barraInferior,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ////////////////// Interfaz: barra superior (volver + título)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Progreso",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(48.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
            ////////////////////////
        }
    ) { padding ->
        // Sin hábitos todavía
        if (habito == null) {
            MensajeSinHabitos(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // Los archivados van en su propio apartado para no mezclarlos con los de uso diario
            val activos = habitos.filter { !it.archivado }
            val archivados = habitos.filter { it.archivado }

            ////////////////// Interfaz: apartado "Hábitos activos"
            if (activos.isNotEmpty()) {
                TituloApartado(
                    texto = "Hábitos activos",
                    cantidad = activos.size,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                FilaDeHabitos(activos, habito.id) { idSeleccionado = it }
                Spacer(Modifier.height(20.dp))
            }
            ////////////////////////

            ////////////////// Interfaz: apartado "Hábitos archivados"
            if (archivados.isNotEmpty()) {
                TituloApartado(
                    texto = "Hábitos archivados",
                    cantidad = archivados.size,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                FilaDeHabitos(archivados, habito.id) { idSeleccionado = it }
                Spacer(Modifier.height(20.dp))
            }
            ////////////////////////

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {

                ////////////////// Interfaz: apartado "Resumen del hábito"
                TituloApartado("Resumen del hábito")
                TarjetaResumenHabito(habito)
                ////////////////////////

                // Aviso de la meta semanal (solo "X veces por semana")
                if (habito.frecuencia == Frecuencias.VECES_POR_SEMANA) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Meta: ${metaSemanal(habito)} por semana · esta semana llevas " +
                            "${cumplidosEnLaSemana(habito, hoyCalendario())}. " +
                            "Las rachas de este hábito se cuentan por semanas cumplidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(28.dp))

                ////////////////// Interfaz: apartado "Calendario del mes"
                TituloApartado("Calendario del mes")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { mesIndice-- }) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = "Mes anterior",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = nombreDelMes(mes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { mesIndice++ },
                        enabled = hayMesSiguiente(mes)
                    ) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Mes siguiente",
                            tint = if (hayMesSiguiente(mes)) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                ////////////////////////
                Spacer(Modifier.height(8.dp))

                ////////////////// Interfaz: cuadrícula del mes
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .sombraSuave(RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp)) {
                        // Encabezado: Lun ... Dom
                        Row(modifier = Modifier.fillMaxWidth()) {
                            NOMBRES_DIAS.forEach { nombre ->
                                Text(
                                    text = nombre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        // Semanas de 7 casillas (los huecos del inicio y del final son null)
                        semanasDelMes(habito, mes).forEach { semana ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                semana.forEach { casilla ->
                                    CasillaCalendario(
                                        casilla = casilla,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                ////////////////////////
                Spacer(Modifier.height(16.dp))

                ////////////////// Interfaz: leyenda de colores
                Leyenda()
                ////////////////////////
                Spacer(Modifier.height(16.dp))

                ////////////////// Interfaz: resumen del mes
                val resumen = resumenDelMes(habito, mes)
                Text(
                    text = if (resumen.programados == 0) {
                        "Este mes todavía no hay días que contar para este hábito."
                    } else {
                        "Este mes: ${resumen.cumplidos} cumplidos y ${resumen.fallados} fallados " +
                            "de ${resumen.programados} días que tocaban (${resumen.porcentaje}%)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ////////////////////////
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    ////////////////////////
}

// Título de un apartado de la pantalla, con la cantidad al lado
@Composable
private fun TituloApartado(
    texto: String,
    cantidad: Int? = null,
    modifier: Modifier = Modifier
) {
    ////////////////// Interfaz: título de apartado
    Row(
        modifier = modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (cantidad != null) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = "$cantidad",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
    ////////////////////////
}

// Fila deslizable con las tarjetas de un grupo de hábitos
@Composable
private fun FilaDeHabitos(
    lista: List<Habito>,
    idActual: String,
    onElegir: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
    ) {
        items(lista, key = { it.id }) { opcion ->
            TarjetaSelector(
                habito = opcion,
                seleccionada = opcion.id == idActual,
                onClick = { onElegir(opcion.id) }
            )
        }
    }
}

// Tarjeta del selector: ícono, nombre y racha actual del hábito
@Composable
private fun TarjetaSelector(
    habito: Habito,
    seleccionada: Boolean,
    onClick: () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(20.dp)
    val racha = rachaActual(habito)

    ////////////////// Interfaz: tarjeta del selector
    Surface(
        onClick = onClick,
        shape = forma,
        color = if (seleccionada) colores.primaryContainer else colores.surfaceContainerLowest,
        border = BorderStroke(
            if (seleccionada) 2.dp else 1.dp,
            if (seleccionada) colores.primary else colores.outlineVariant
        ),
        modifier = Modifier
            .width(164.dp)
            .sombraSuave(forma, 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Ícono de la categoría en un cuadrado suave
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (seleccionada) colores.primary else colores.surfaceContainerLow,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        iconoCategoria(habito.categoria),
                        contentDescription = null,
                        tint = if (seleccionada) colores.onPrimary else colores.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Text(
                text = habito.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = if (seleccionada) colores.onPrimaryContainer else colores.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))

            // Pie: racha actual, o el aviso de archivado
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (habito.archivado) Icons.Outlined.Archive
                    else Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (!habito.archivado && racha > 0) colores.tertiaryContainer
                    else colores.outlineVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = when {
                        habito.archivado -> "Archivado"
                        racha > 0 -> textoRacha(racha, habito)
                        else -> "Sin racha"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (seleccionada) colores.onPrimaryContainer else colores.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
    ////////////////////////
}

// Tarjeta grande del hábito elegido: nombre, frecuencia y las dos rachas
@Composable
private fun TarjetaResumenHabito(habito: Habito) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(24.dp)

    ////////////////// Interfaz: resumen del hábito
    Surface(
        shape = forma,
        color = colores.surfaceContainerLowest,
        border = BorderStroke(1.dp, colores.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .sombraSuave(forma)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Encabezado: ícono + nombre + frecuencia
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colores.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            iconoCategoria(habito.categoria),
                            contentDescription = null,
                            tint = colores.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habito.nombre,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colores.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = detalleHabito(habito),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = colores.surfaceVariant.copy(alpha = 0.7f))
            Spacer(Modifier.height(16.dp))

            // Pie: las dos rachas, separadas por una línea
            Row(verticalAlignment = Alignment.CenterVertically) {
                DatoRacha(
                    icono = Icons.Filled.LocalFireDepartment,
                    titulo = "Racha actual",
                    valor = textoRacha(rachaActual(habito), habito),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(colores.surfaceVariant)
                )
                DatoRacha(
                    icono = Icons.Outlined.EmojiEvents,
                    titulo = "Mejor racha",
                    valor = textoRacha(mejorRacha(habito), habito),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    ////////////////////////
}

// Una racha dentro de la tarjeta de resumen
@Composable
private fun DatoRacha(
    icono: ImageVector,
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = valor,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

// Una casilla del calendario: círculo con el número del día, pintado según su estado
@Composable
private fun CasillaCalendario(casilla: CasillaDia?, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme

    // Hueco de relleno (días de otro mes)
    if (casilla == null) {
        Box(modifier = modifier.aspectRatio(1f))
        return
    }

    // Color de fondo, de texto y de borde según el estado del día
    val fondo: Color
    val textoColor: Color
    var borde: Color? = null
    when (casilla.estado) {
        EstadoDia.CUMPLIDO -> {
            fondo = colores.primary
            textoColor = colores.onPrimary
        }
        EstadoDia.FALLADO -> {
            fondo = colores.errorContainer
            textoColor = colores.onErrorContainer
        }
        EstadoDia.NO_APLICA -> {
            fondo = colores.surfaceContainerLow
            textoColor = colores.onSurfaceVariant.copy(alpha = 0.6f)
        }
        EstadoDia.PENDIENTE -> {
            fondo = Color.Transparent
            textoColor = colores.onSurface
        }
        // Día futuro en el que toca cumplir: se marca con un aro para verlo venir
        EstadoDia.PROGRAMADO -> {
            fondo = Color.Transparent
            textoColor = colores.primary
            borde = colores.primary.copy(alpha = 0.45f)
        }
        // El hábito aún no existía, o es un día futuro que no toca: nada que juzgar
        EstadoDia.ANTES_DE_CREAR, EstadoDia.FUTURO -> {
            fondo = Color.Transparent
            textoColor = colores.onSurfaceVariant.copy(alpha = 0.35f)
        }
    }

    // Hoy se marca con un borde más fuerte, que manda sobre el del día programado
    val esHoy = mismoDia(casilla.fecha, hoyCalendario())
    val bordeFinal = if (esHoy) colores.primary else borde

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = fondo,
            border = bordeFinal?.let { BorderStroke(if (esHoy) 2.dp else 1.5.dp, it) },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${casilla.numero}",
                    style = MaterialTheme.typography.labelMedium,
                    color = textoColor,
                    fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// Leyenda de colores (se acomoda sola si no cabe en una línea)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Leyenda() {
    val colores = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ItemLeyenda("Cumplido", colores.primary, null)
        ItemLeyenda("Fallado", colores.errorContainer, null)
        ItemLeyenda("No tocaba", colores.surfaceContainerLow, null)
        ItemLeyenda("Hoy", Color.Transparent, colores.primary)
        ItemLeyenda("Por venir", Color.Transparent, colores.primary.copy(alpha = 0.45f))
        ItemLeyenda("Sin registro", Color.Transparent, colores.outlineVariant)
    }
}

// Punto de color + texto de la leyenda
@Composable
private fun ItemLeyenda(texto: String, fondo: Color, borde: Color?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = fondo,
            border = borde?.let { BorderStroke(2.dp, it) },
            modifier = Modifier.size(12.dp)
        ) {}
        Spacer(Modifier.width(6.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Mensaje cuando no hay ningún hábito que mostrar
@Composable
private fun MensajeSinHabitos(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 64.dp),
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
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Todavía no hay progreso que mostrar",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Crea un hábito y marca tus cumplimientos para ver el calendario.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Agrupa las casillas del mes en filas de 7, rellenando el final con huecos
private fun semanasDelMes(habito: Habito, mes: MesVisible): List<List<CasillaDia?>> {
    val casillas = casillasDelMes(habito, mes).toMutableList()
    while (casillas.size % 7 != 0) casillas.add(null)
    return casillas.chunked(7)
}
