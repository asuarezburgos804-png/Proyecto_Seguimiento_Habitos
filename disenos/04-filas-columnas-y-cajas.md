# Filas, columnas y cajas (cómo se acomodan las cosas)

| Componente | Qué hace | Dibujo |
|---|---|---|
| `Row` | Pone los hijos **uno al lado del otro** | `[A][B][C]` |
| `Column` | Pone los hijos **uno debajo del otro** | `[A]` / `[B]` / `[C]` |
| `Box` | Pone los hijos **encima** del otro (capas) | `[A sobre B]` |
| `Spacer` | Espacio vacío | `[A]   [B]` |
| `FlowRow` | Fila que salta de línea si no cabe | `[A][B][C]` / `[D][E]` |
| `LazyColumn` | Lista vertical con scroll (solo dibuja lo visible) | lista larga |
| `LazyRow` | Lista horizontal con scroll | carrusel |

## Row

Ejemplo real: encabezado del inicio (saludo a la izquierda, chips a la derecha).

```
Hola, Ana                         (🔥 3) (★ Nv. 2)
Martes, 27 de octubre
```

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
) {
    Column(modifier = Modifier.weight(1f)) {     // weight(1f) = ocupa todo el espacio que sobra
        Text("Hola, Ana", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text(fechaLegibleDeHoy(), style = MaterialTheme.typography.bodyMedium)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {   // 8 dp entre cada chip
        ChipEncabezado(/* racha */)
        ChipEncabezado(/* nivel */)
    }
}
```

Opciones útiles de `Row`:

```kotlin
Row(
    horizontalArrangement = Arrangement.SpaceBetween,   // separa al máximo (izquierda ... derecha)
    // Arrangement.Center, Arrangement.SpaceAround, Arrangement.spacedBy(8.dp)
    verticalAlignment = Alignment.CenterVertically      // centra en vertical
) { /* ... */ }
```

## Column

Ejemplo real: pantalla de login (todo centrado y con scroll).

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(padding)                         // espacio que deja el Scaffold
        .imePadding()                             // sube el contenido cuando sale el teclado
        .verticalScroll(rememberScrollState())    // permite hacer scroll
        .padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    Text("Bienvenido de vuelta", style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(8.dp))
    Text("Retoma tus hábitos donde los dejaste ayer")
}
```

## Box

Sirve para **centrar** algo o para poner una cosa encima de otra.

Ejemplo real: ícono centrado dentro de un cuadrado.

```kotlin
Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
    Box(contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.EnergySavingsLeaf, contentDescription = null)
    }
}
```

Ejemplo real: línea con una "o" encima (registro).

```
──────────── o ────────────
```

```kotlin
Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    HorizontalDivider(color = MaterialTheme.colorScheme.secondaryContainer)
    Text(
        text = "o",
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)   // tapa la línea detrás de la "o"
            .padding(horizontal = 12.dp)
    )
}
```

## Spacer

```kotlin
Spacer(Modifier.height(16.dp))   // espacio vertical (dentro de un Column)
Spacer(Modifier.width(8.dp))     // espacio horizontal (dentro de un Row)
```

## FlowRow

Ejemplo real: categorías y días en "Nuevo hábito".

```
(♡ Salud) (🎓 Estudio) (🏋 Ejercicio)
(💼 Trabajo) (👤 Personal)
```

```kotlin
@OptIn(ExperimentalLayoutApi::class)   // va arriba de la función que lo usa
FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Categorias.LISTA.forEach { opcion ->
        PildoraOpcion(
            texto = opcion,
            icono = iconoCategoria(opcion),
            seleccionada = categoria == opcion,
            onClick = { categoria = opcion }
        )
    }
}
```

## LazyColumn

Ejemplo real: lista de hábitos del inicio.

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
) {
    // Un elemento suelto
    item {
        TituloSeccion("Pendientes hoy", cantidad = pendientes.size)
    }
    // Un elemento por cada hábito de la lista
    items(pendientes, key = { it.id }) { habito ->
        TarjetaHabito(
            habito = habito,
            completado = false,
            onMarcar = { habitosViewModel.marcar(habito, true) },
            onAbrirOpciones = { idOpciones = habito.id },
            modifier = Modifier.animateItem()     // se mueve con animación al cambiar de lista
        )
    }
}
```

## LazyRow

Ejemplo real: carrusel de hábitos en "Progreso".

```
╭──────────╮ ╭──────────╮ ╭──────
│ ♡        │ │ 🎓       │ │ 🏋
│ Meditar  │ │ Leer     │ │ Corr
│ 🔥 3 días│ │ Sin racha│ │ 🔥 1
╰──────────╯ ╰──────────╯ ╰──────   ← se desliza a los lados
```

```kotlin
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
```

## Cuadrícula del calendario (Row dentro de Column)

```
Lun Mar Mié Jue Vie Sáb Dom
         1   2   3   4   5
 6   7   8   9  10  11  12
```

```kotlin
Column {
    // Encabezado
    Row(modifier = Modifier.fillMaxWidth()) {
        NOMBRES_DIAS.forEach { nombre ->
            Text(nombre, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    // chunked(7) parte la lista de casillas en semanas de 7
    casillasDelMes(habito, mes).chunked(7).forEach { semana ->
        Row(modifier = Modifier.fillMaxWidth()) {
            semana.forEach { casilla ->
                CasillaCalendario(casilla = casilla, modifier = Modifier.weight(1f))
            }
        }
    }
}
```
