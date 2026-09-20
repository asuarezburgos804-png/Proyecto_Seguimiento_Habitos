package com.example.proyecto_seguimiento_habitos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Formulario para crear un hábito nuevo.
 * Campos obligatorios: nombre, categoría y frecuencia. El horario es opcional.
 */
@Composable
fun CrearHabitoScreen(
    onCancelar: () -> Unit,
    onHabitoCreado: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("") }
    var frecuencia by rememberSaveable { mutableStateOf(Frecuencias.DIARIO) }
    var dias by remember { mutableStateOf(listOf<Int>()) }
    var veces by rememberSaveable { mutableStateOf("") }
    var horario by rememberSaveable { mutableStateOf("") }

    var nombreError by rememberSaveable { mutableStateOf<String?>(null) }
    var categoriaError by rememberSaveable { mutableStateOf<String?>(null) }
    var diasError by rememberSaveable { mutableStateOf<String?>(null) }
    var vecesError by rememberSaveable { mutableStateOf<String?>(null) }
    var horarioError by rememberSaveable { mutableStateOf<String?>(null) }

    var guardando by rememberSaveable { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Muestra en un snackbar los errores que devuelve Firestore.
    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            mensajeError = null
        }
    }

    // El botón "atrás" del teléfono regresa al dashboard.
    BackHandler { onCancelar() }

    fun validar(): Boolean {
        nombreError = HabitoValidators.validarNombre(nombre)
        categoriaError = HabitoValidators.validarCategoria(categoria)
        diasError = HabitoValidators.validarDias(frecuencia, dias)
        vecesError = HabitoValidators.validarVecesPorSemana(frecuencia, veces)
        horarioError = HabitoValidators.validarHorario(horario)
        return nombreError == null && categoriaError == null && diasError == null &&
                vecesError == null && horarioError == null
    }

    fun guardar() {
        if (!validar()) return

        val habito = Habito(
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

        guardando = true
        HabitosRepository.crearHabito(
            habito = habito,
            onExito = {
                guardando = false
                onHabitoCreado()
            },
            onError = { mensaje ->
                guardando = false
                mensajeError = mensaje
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Nuevo hábito",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Completa los datos para empezar a hacerle seguimiento",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    if (nombreError != null) nombreError = null
                },
                label = { Text("Nombre del hábito") },
                singleLine = true,
                isError = nombreError != null,
                supportingText = { nombreError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // ---------- Categoría ----------
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Categorias.LISTA.forEach { opcion ->
                    FilterChip(
                        selected = categoria == opcion,
                        onClick = {
                            categoria = opcion
                            categoriaError = null
                        },
                        label = { Text(opcion) }
                    )
                }
            }
            if (categoriaError != null) {
                Text(
                    text = categoriaError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(16.dp))

            // ---------- Frecuencia ----------
            Text(
                text = "Frecuencia",
                style = MaterialTheme.typography.titleMedium
            )
            OpcionFrecuencia(
                texto = "Todos los días",
                seleccionada = frecuencia == Frecuencias.DIARIO,
                onSeleccionar = { frecuencia = Frecuencias.DIARIO }
            )
            OpcionFrecuencia(
                texto = "Días específicos",
                seleccionada = frecuencia == Frecuencias.DIAS_ESPECIFICOS,
                onSeleccionar = { frecuencia = Frecuencias.DIAS_ESPECIFICOS }
            )
            OpcionFrecuencia(
                texto = "X veces por semana",
                seleccionada = frecuencia == Frecuencias.VECES_POR_SEMANA,
                onSeleccionar = { frecuencia = Frecuencias.VECES_POR_SEMANA }
            )

            // Campos extra que dependen de la frecuencia elegida.
            if (frecuencia == Frecuencias.DIAS_ESPECIFICOS) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NOMBRES_DIAS.forEachIndexed { indice, nombreDia ->
                        val numeroDia = indice + 1
                        FilterChip(
                            selected = dias.contains(numeroDia),
                            onClick = {
                                dias = if (dias.contains(numeroDia)) {
                                    dias - numeroDia
                                } else {
                                    dias + numeroDia
                                }
                                diasError = null
                            },
                            label = { Text(nombreDia) }
                        )
                    }
                }
                if (diasError != null) {
                    Text(
                        text = diasError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (frecuencia == Frecuencias.VECES_POR_SEMANA) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = veces,
                    onValueChange = {
                        veces = it
                        if (vecesError != null) vecesError = null
                    },
                    label = { Text("Veces por semana") },
                    singleLine = true,
                    isError = vecesError != null,
                    supportingText = { vecesError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))

            // ---------- Horario (opcional) ----------
            OutlinedTextField(
                value = horario,
                onValueChange = {
                    horario = it
                    if (horarioError != null) horarioError = null
                },
                label = { Text("Horario (opcional)") },
                singleLine = true,
                isError = horarioError != null,
                supportingText = {
                    horarioError?.let { Text(it) } ?: Text("Formato HH:mm, por ejemplo 07:30")
                },
                // Teclado de texto para poder escribir los dos puntos.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { guardar() },
                enabled = !guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar hábito")
                }
            }
            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onCancelar,
                enabled = !guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Una opción de frecuencia con su botón de selección. */
@Composable
private fun OpcionFrecuencia(
    texto: String,
    seleccionada: Boolean,
    onSeleccionar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, onClick = onSeleccionar),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = seleccionada,
            onClick = onSeleccionar
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
