# Estructura de pantalla, barras, diálogos y hojas

## 1. Scaffold (el "esqueleto" de cada pantalla)

```
┌───────────────────────────┐
│ topBar      (opcional)    │
├───────────────────────────┤
│                           │
│ contenido                 │
│                           │
│        [ snackbar ]       │
├───────────────────────────┤
│ bottomBar   (opcional)    │
└───────────────────────────┘
```

```kotlin
val snackbarHostState = remember { SnackbarHostState() }

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = { BarraNavegacion(navController, rutaActual = "inicio") },
    containerColor = MaterialTheme.colorScheme.background
) { padding ->
    // padding = espacio que ocupan las barras; hay que aplicarlo al contenido
    Column(modifier = Modifier.padding(padding)) {
        // ...
    }
}
```

## 2. Barra superior (volver + título centrado)

```
 ←            Progreso
─────────────────────────────
```

```kotlin
Column {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()                  // deja libre la barra del reloj/batería
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { volver() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Text(
            text = "Progreso",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(48.dp))             // mismo ancho que el botón: el título queda centrado
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
}
```

## 3. Barra de navegación inferior

```
╭────────────────────────────────────────────────╮
│ (⌂ Inicio)   📊      ⊕       🏅       👤       │
│              Stats  Añadir  Logros  Perfil     │
╰────────────────────────────────────────────────╯
  la pestaña activa es una píldora verde
```

Está en `HabitoAcciones.kt`. Recibe el `navController` y navega ella misma.

```kotlin
@Composable
fun BarraNavegacion(navController: NavController, rutaActual: String) {
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
            ItemNavegacion(Icons.Outlined.Home, Icons.Filled.Home, "Inicio", rutaActual == "inicio") {
                irAInicio(navController)
            }
            ItemNavegacion(Icons.Outlined.Leaderboard, Icons.Outlined.Leaderboard, "Stats", rutaActual == "calendario") {
                navController.navigate("calendario") { launchSingleTop = true }
            }
            // ... Añadir, Logros, Perfil
        }
    }
}

// Un botón de la barra: si está activo se pinta como píldora verde
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
```

## 4. Snackbar (aviso abajo que desaparece solo)

```
╭──────────────────────────────────────╮
│ "Leer" se archivó. Su historial se   │
│ conserva.                            │
╰──────────────────────────────────────╯
```

Patrón usado en todas las pantallas: el ViewModel pone un texto en `mensaje`,
la pantalla lo muestra y lo vuelve a poner en `null`.

```kotlin
LaunchedEffect(habitosViewModel.mensaje) {      // se ejecuta cada vez que cambia "mensaje"
    val mensaje = habitosViewModel.mensaje
    if (mensaje != null) {
        habitosViewModel.mensaje = null
        snackbarHostState.showSnackbar(mensaje)
    }
}
```

## 5. Diálogo de confirmación (AlertDialog)

```
╭─────────────────────────────────╮
│              🗑                 │
│     ¿Eliminar "Leer"?           │
│ Se borrará el hábito y todo su  │
│ historial...                    │
│            Cancelar   Eliminar  │
╰─────────────────────────────────╯
```

```kotlin
AlertDialog(
    onDismissRequest = onCancelar,               // tocar afuera = cancelar
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    icon = {
        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
    },
    title = { Text("¿Eliminar \"${habito.nombre}\"?", style = MaterialTheme.typography.headlineMedium) },
    text = { Text("Se borrará el hábito y todo su historial. Esta acción no se puede deshacer.") },
    confirmButton = {
        TextButton(onClick = onConfirmar) {
            Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }
    },
    dismissButton = {
        TextButton(onClick = onCancelar) { Text("Cancelar") }
    }
)
```

Para mostrarlo se usa una variable: si no es `null`, el diálogo aparece.

```kotlin
var idEliminar by rememberSaveable { mutableStateOf<String?>(null) }

val habitoEliminar = activos.find { it.id == idEliminar }
if (habitoEliminar != null) {
    DialogoEliminar(
        habito = habitoEliminar,
        onConfirmar = {
            idEliminar = null
            habitosViewModel.eliminar(habitoEliminar)
        },
        onCancelar = { idEliminar = null }
    )
}
```

## 6. Hoja inferior de opciones (ModalBottomSheet)

```
╭─────────────────────────────────╮
│            ───                  │
│ ┌──┐ Meditar                    │
│ │♡ │ Salud · Todos los días     │
│ └──┘                            │
│ 📅 Ver progreso                 │
│ ✏ Editar hábito                 │
│ 🗄 Archivar hábito              │
│ 🗑 Eliminar hábito   (en rojo)  │
╰─────────────────────────────────╯
```

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpcionesHabitoSheet(/* ... */) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            // encabezado ...
            FilaOpcion(Icons.Outlined.Edit, "Editar hábito", "Nombre, frecuencia u horario", onClick = onEditar)
            FilaOpcion(
                icono = Icons.Outlined.DeleteOutline,
                titulo = "Eliminar hábito",
                detalle = "Borra el hábito y todo su historial",
                color = MaterialTheme.colorScheme.error,
                onClick = onEliminar
            )
        }
    }
}

// Una fila tocable: ícono + título + detalle
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
                Text(detalle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
```

## 7. Reloj para elegir la hora (TimePicker dentro de un diálogo)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorHora(horaInicial: String, onConfirmar: (String) -> Unit, onCancelar: () -> Unit) {
    val partes = horaInicial.split(":")
    val estado = rememberTimePickerState(
        initialHour = partes.getOrNull(0)?.toIntOrNull() ?: 7,
        initialMinute = partes.getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Elige la hora") },
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = { onConfirmar("%02d:%02d".format(estado.hour, estado.minute)) }) {
                Text("Aceptar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
```
