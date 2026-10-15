package com.example.proyecto_seguimiento_habitos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Pantalla para crear un hábito nuevo o editar uno existente
// habitoExistente != null -> modo edición (no toca el historial)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrearHabitoScreen(
    onCancelar: () -> Unit,
    onHabitoGuardado: () -> Unit,
    habitoExistente: Habito? = null
) {
    val editando = habitoExistente != null

    // Campos del formulario (con los valores actuales si se edita)
    var nombre by rememberSaveable { mutableStateOf(habitoExistente?.nombre ?: "") }
    var categoria by rememberSaveable { mutableStateOf(habitoExistente?.categoria ?: "") }
    var frecuencia by rememberSaveable { mutableStateOf(habitoExistente?.frecuencia ?: Frecuencias.DIARIO) }
    var dias by rememberSaveable { mutableStateOf(habitoExistente?.dias ?: listOf()) }
    var veces by rememberSaveable {
        mutableStateOf(habitoExistente?.vecesPorSemana?.takeIf { it > 0 }?.toString() ?: "")
    }
    var horario by rememberSaveable { mutableStateOf(habitoExistente?.horario ?: "") }
    var mostrarReloj by rememberSaveable { mutableStateOf(false) }

    // Errores (null = sin error)
    var nombreError by rememberSaveable { mutableStateOf<String?>(null) }
    var categoriaError by rememberSaveable { mutableStateOf<String?>(null) }
    var diasError by rememberSaveable { mutableStateOf<String?>(null) }
    var vecesError by rememberSaveable { mutableStateOf<String?>(null) }
    var horarioError by rememberSaveable { mutableStateOf<String?>(null) }

    // Guardando y error de Firestore
    var guardando by rememberSaveable { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Muestra errores de Firestore
    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            mensajeError = null
        }
    }

    // Botón atrás -> dashboard
    BackHandler { onCancelar() }

    // Valida todos los campos
    fun validar(): Boolean {
        nombreError = HabitoValidators.validarNombre(nombre)
        categoriaError = HabitoValidators.validarCategoria(categoria)
        diasError = HabitoValidators.validarDias(frecuencia, dias)
        vecesError = HabitoValidators.validarVecesPorSemana(frecuencia, veces)
        horarioError = HabitoValidators.validarHorario(horario)
        return nombreError == null && categoriaError == null && diasError == null &&
                vecesError == null && horarioError == null
    }

    // Valida y guarda (crea o actualiza) en Firestore
    fun guardar() {
        if (!validar()) return

        val datos = Habito(
            id = habitoExistente?.id ?: "",
            nombre = nombre,
            categoria = categoria,
            frecuencia = frecuencia,
            dias = if (frecuencia == Frecuencias.DIAS_ESPECIFICOS) dias.sorted() else emptyList(),
            vecesPorSemana = if (frecuencia == Frecuencias.VECES_POR_SEMANA) {
                veces.trim().toIntOrNull() ?: 0
            } else {
                0
            },
            horario = horario
        )

        val onExito = {
            guardando = false
            onHabitoGuardado()
        }
        val onError = { mensaje: String ->
            guardando = false
            mensajeError = mensaje
        }

        guardando = true
        if (editando) {
            HabitosRepository.actualizarHabito(datos, onExito, onError)
        } else {
            HabitosRepository.crearHabito(datos, onExito, onError)
        }
    }

    ////////////////// Interfaz: formulario
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ////////////////// Interfaz: barra superior (cerrar + título)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancelar, enabled = !guardando) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (editando) "Editar hábito" else "Nuevo hábito",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            ////////////////// Interfaz: nombre
            CampoTexto(
                valor = nombre,
                onValorChange = {
                    nombre = it
                    if (nombreError != null) nombreError = null
                },
                etiqueta = "Nombre del hábito",
                placeholder = "Ej. Meditar 10 minutos",
                icono = Icons.Outlined.Edit,
                error = nombreError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                )
            )
            ////////////////////////
            Spacer(Modifier.height(16.dp))

            ////////////////// Interfaz: categoría (píldoras con ícono)
            EtiquetaCampo("Categoría")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Categorias.LISTA.forEach { opcion ->
                    PildoraOpcion(
                        texto = opcion,
                        icono = iconoCategoria(opcion),
                        seleccionada = categoria == opcion,
                        onClick = {
                            categoria = opcion
                            categoriaError = null
                        }
                    )
                }
            }
            TextoError(categoriaError)
            ////////////////////////
            Spacer(Modifier.height(24.dp))

            ////////////////// Interfaz: frecuencia (3 opciones en cuadrícula)
            EtiquetaCampo("Frecuencia")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Frecuencias.DIARIO to "Diario",
                    Frecuencias.DIAS_ESPECIFICOS to "Días específicos",
                    Frecuencias.VECES_POR_SEMANA to "X por semana"
                ).forEach { (valor, texto) ->
                    BotonFrecuencia(
                        texto = texto,
                        seleccionada = frecuencia == valor,
                        onClick = { frecuencia = valor },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            ////////////////////////

            ////////////////// Interfaz: días (solo "Días específicos")
            if (frecuencia == Frecuencias.DIAS_ESPECIFICOS) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NOMBRES_DIAS.forEachIndexed { indice, nombreDia ->
                        val numeroDia = indice + 1
                        PildoraOpcion(
                            texto = nombreDia,
                            seleccionada = dias.contains(numeroDia),
                            // Agrega o quita el día
                            onClick = {
                                dias = if (dias.contains(numeroDia)) dias - numeroDia else dias + numeroDia
                                diasError = null
                            }
                        )
                    }
                }
                TextoError(diasError)
            }
            ////////////////////////

            ////////////////// Interfaz: veces por semana (solo "X por semana")
            if (frecuencia == Frecuencias.VECES_POR_SEMANA) {
                Spacer(Modifier.height(12.dp))
                CampoTexto(
                    valor = veces,
                    onValorChange = {
                        veces = it
                        if (vecesError != null) vecesError = null
                    },
                    placeholder = "Veces por semana (1 a 7)",
                    icono = Icons.Outlined.Repeat,
                    error = vecesError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
            ////////////////////////
            Spacer(Modifier.height(24.dp))

            ////////////////// Interfaz: hora (opcional, con selector de reloj)
            EtiquetaCampo("Hora (opcional)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { mostrarReloj = true },
                    enabled = !guardando,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(
                        1.dp,
                        if (horarioError != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .sombraSuave(RoundedCornerShape(12.dp), 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = horario.ifEmpty { "Sin hora" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (horario.isEmpty()) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (horario.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { horario = ""; horarioError = null }) {
                        Text("Quitar")
                    }
                }
            }
            TextoError(horarioError)
            ////////////////////////

            ////////////////// Interfaz: aviso en modo edición
            if (editando) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Los cambios no alteran el historial ya registrado de este hábito.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ////////////////////////
            Spacer(Modifier.height(48.dp))

            ////////////////// Interfaz: botón Guardar
            BotonPrincipal(
                texto = if (editando) "Guardar cambios" else "Guardar hábito",
                onClick = { guardar() },
                cargando = guardando,
                iconoInicio = Icons.Outlined.Save,
                iconoFinal = null
            )
            ////////////////////////
            Spacer(Modifier.height(24.dp))
        }
    }
    ////////////////////////

    ////////////////// Interfaz: selector de hora
    if (mostrarReloj) {
        SelectorHora(
            horaInicial = horario,
            onConfirmar = {
                horario = it
                horarioError = null
                mostrarReloj = false
            },
            onCancelar = { mostrarReloj = false }
        )
    }
    ////////////////////////
}

// Etiqueta gris sobre un grupo de opciones
@Composable
private fun EtiquetaCampo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}

// Píldora seleccionable (categorías y días)
@Composable
private fun PildoraOpcion(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    icono: ImageVector? = null
) {
    val colores = MaterialTheme.colorScheme
    ////////////////// Interfaz: píldora
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (seleccionada) colores.primaryContainer else colores.surfaceContainerLowest,
        contentColor = if (seleccionada) colores.onPrimaryContainer else colores.onSurfaceVariant,
        border = BorderStroke(1.dp, if (seleccionada) colores.primary else colores.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icono != null) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
    ////////////////////////
}

// Botón de frecuencia (cuadrícula de 3)
@Composable
private fun BotonFrecuencia(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    ////////////////// Interfaz: opción de frecuencia
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionada) colores.primaryContainer.copy(alpha = 0.1f) else colores.surfaceContainerLowest,
        contentColor = if (seleccionada) colores.primary else colores.onSurfaceVariant,
        border = BorderStroke(1.dp, if (seleccionada) colores.primary else colores.outlineVariant),
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
    ////////////////////////
}

// Diálogo con reloj para elegir la hora (devuelve "HH:mm")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorHora(
    horaInicial: String,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    val partes = horaInicial.split(":")
    val estado = rememberTimePickerState(
        initialHour = partes.getOrNull(0)?.toIntOrNull() ?: 7,
        initialMinute = partes.getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )
    ////////////////// Interfaz: diálogo del reloj
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text("Elige la hora", style = MaterialTheme.typography.headlineMedium) },
        text = {
            TimePicker(
                state = estado,
                colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    selectorColor = MaterialTheme.colorScheme.primary,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirmar("%02d:%02d".format(estado.hour, estado.minute))
            }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
    ////////////////////////
}
