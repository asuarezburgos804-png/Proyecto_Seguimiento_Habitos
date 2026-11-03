# Colores y letras

Definidos en `ui/theme/Color.kt`, `ui/theme/Type.kt` y `ui/theme/Theme.kt`.
Paleta "Mindful Habit Tracker" tomada de los mockups de Stitch.

## Colores principales

| Nombre en el tema | Color | Dónde se ve |
|---|---|---|
| `primary` | `#14422D` verde bosque | Botón principal, check marcado, títulos de barra |
| `onPrimary` | `#FFFFFF` blanco | Texto encima del verde |
| `primaryContainer` | `#2D5A43` verde medio | Píldora activa de la barra inferior, chips seleccionados |
| `onPrimaryContainer` | `#9FCFB2` verde claro | Texto/ícono sobre `primaryContainer` |
| `tertiaryContainer` | `#804000` naranja | Fueguito de las rachas |
| `TertiaryFixed` | `#FFDCC5` durazno | Fondo del chip de racha del inicio |
| `error` | `#BA1A1A` rojo | Errores, "Eliminar hábito" |
| `errorContainer` | `#FFDAD6` rosado | Días fallados del calendario, aviso de bloqueo |
| `background` / `surface` | `#FCF9F8` blanco cálido | Fondo de las pantallas |
| `Crema` | `#F5F2E8` | Aura del fondo del registro |
| `surfaceContainerLowest` | `#FFFFFF` | Tarjetas blancas, campos de texto |
| `surfaceContainerLow` | `#F6F3F2` | Tarjetas apagadas (completados, archivados) |
| `onSurfaceVariant` | `#414943` gris verdoso | Textos secundarios |
| `outlineVariant` | `#C0C9C1` | Bordes finos |

Uso en código:

```kotlin
Text("Hola", color = MaterialTheme.colorScheme.primary)
Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) { /* ... */ }
```

## Letras

| Estilo | Fuente | Tamaño | Dónde se usa |
|---|---|---|---|
| `headlineLarge` | Playfair Display | 28 sp | "Hola, Ana", "Bienvenido de vuelta", "Progreso" |
| `headlineMedium` | Playfair Display | 24 sp | Títulos de sección ("Pendientes hoy") |
| `titleLarge` | Playfair Display | 18 sp | Nombre del hábito en la tarjeta |
| `bodyLarge` | Inter | 16 sp | Texto dentro de los campos |
| `bodyMedium` | Inter | 14 sp | Textos normales |
| `bodySmall` | Inter | 12 sp | Detalles, ayudas, errores |
| `labelLarge` | Inter | 14 sp | Texto de botones |
| `labelMedium` / `labelSmall` | Inter | 12 / 11 sp | Chips, leyendas, barra inferior |

```kotlin
Text("Pendientes hoy", style = MaterialTheme.typography.headlineMedium)
```

## Sombra suave

Las tarjetas usan una sombra muy difusa (como en Stitch: `0 4px 20px rgba(0,0,0,0.04)`).
Está en `Componentes.kt`:

```kotlin
fun Modifier.sombraSuave(shape: Shape, elevacion: Dp = 6.dp): Modifier = this.shadow(
    elevation = elevacion,
    shape = shape,
    ambientColor = Color.Black.copy(alpha = 0.04f),
    spotColor = Color.Black.copy(alpha = 0.08f)
)

// Uso
Modifier.sombraSuave(RoundedCornerShape(24.dp))
```
