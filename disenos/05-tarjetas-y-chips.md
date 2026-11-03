# Tarjetas, chips y píldoras

Todas se hacen con `Surface`: una caja con color, forma, borde y (opcional) clic.

```kotlin
Surface(
    onClick = { /* opcional: la hace tocable */ },
    shape = RoundedCornerShape(24.dp),               // esquinas redondeadas
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    modifier = Modifier.fillMaxWidth()
) {
    // contenido
}
```

## 1. Tarjeta de hábito (inicio)

```
╭───────────────────────────────────────────╮
│ ┌──┐  Meditar 10 minutos              ( ) │
│ │♡ │  🕒 Pendiente · 07:30 • 🔥 3 días     │
│ └──┘                                      │
╰───────────────────────────────────────────╯
 Completado: fondo gris claro, nombre tachado y círculo verde con ✓
```

```kotlin
Surface(
    onClick = onAbrirOpciones,
    shape = RoundedCornerShape(24.dp),
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
                textDecoration = if (completado) TextDecoration.LineThrough else null   // tachado
            )
            Text(textoEstado, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        CirculoCheck(completado = completado, nombre = habito.nombre, onClick = onMarcar)
    }
}
```

## 2. Tarjeta verde de resumen del día (con barra de progreso)

```
╭───────────────────────────────────────────╮
│ Tu día de hoy                         67% │   fondo verde (primary)
│ 2 de 3 hábitos                            │
│ ████████████████░░░░░░░░                  │   barra: dos Box, uno dentro del otro
│ Te falta 1 hábito para cerrar el día.     │
╰───────────────────────────────────────────╯
```

La barra de progreso son **dos cajas**: el riel (claro) y lo avanzado (blanco).

```kotlin
val fraccion = if (total == 0) 0f else hechos.toFloat() / total   // de 0 a 1

Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(50))
        .background(colores.onPrimary.copy(alpha = 0.25f))   // riel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraccion)                          // ancho = porcentaje cumplido
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .background(colores.onPrimary)                   // avance
    )
}
```

## 3. Ícono del hábito (cuadrado suave)

```
┌────┐
│ ♡  │   48 dp, esquinas de 8 dp, ícono según la categoría
└────┘
```

```kotlin
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

// Ícono de cada categoría
fun iconoCategoria(categoria: String): ImageVector = when (categoria) {
    "Salud" -> Icons.Outlined.FavoriteBorder
    "Estudio" -> Icons.Outlined.School
    "Ejercicio" -> Icons.Outlined.FitnessCenter
    "Trabajo" -> Icons.Outlined.WorkOutline
    "Personal" -> Icons.Outlined.Person
    else -> Icons.Outlined.Spa
}
```

## 4. Chip del encabezado (racha / nivel)

```
(🔥 3)   (★ Nv. 2)
```

```kotlin
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
```

## 5. Píldora seleccionable (categorías y días)

```
(♡ Salud)   ← normal: blanca con borde gris
(♡ Salud)   ← seleccionada: verde con borde verde
```

```kotlin
@Composable
private fun PildoraOpcion(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    icono: ImageVector? = null
) {
    val colores = MaterialTheme.colorScheme
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
}
```

## 6. Botón de frecuencia (3 cuadros iguales)

```
┌─────────┐ ┌───────────────┐ ┌─────────────┐
│ Diario  │ │Días específicos│ │ X por semana│
└─────────┘ └───────────────┘ └─────────────┘
```

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    BotonFrecuencia("Diario", frecuencia == Frecuencias.DIARIO, { frecuencia = Frecuencias.DIARIO }, Modifier.weight(1f))
    // ... los otros dos igual, cada uno con weight(1f) para que midan lo mismo
}
```

## 7. Casilla del calendario (círculo con número)

| Estado | Cómo se ve |
|---|---|
| Cumplido | círculo verde, número blanco |
| Fallado | círculo rosado, número rojo oscuro |
| No tocaba | círculo gris claro |
| Hoy | aro verde grueso, número en negrita |
| Por venir (toca) | aro verde suave |
| Sin registro / futuro | solo el número, muy claro |

```kotlin
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
```

## 8. Aviso rojo de bloqueo (login)

```
╭───────────────────────────────────────────╮
│ 🔒 Demasiados intentos fallidos. Podrás   │   fondo errorContainer
│    intentarlo de nuevo en 42 s.           │
╰───────────────────────────────────────────╯
```

```kotlin
Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.errorContainer,
    modifier = Modifier.fillMaxWidth()
) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Demasiados intentos fallidos. Podrás intentarlo de nuevo en $segundos s.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
```
