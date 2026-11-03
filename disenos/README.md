# Diseños de la app (catálogo de componentes)

Esta carpeta **no forma parte de la app**: no se compila ni se instala.
Es un catálogo para ver, en un solo lugar, cada pieza de interfaz que usa el
proyecto (botones, campos, filas, tarjetas...) con su código y una idea de cómo se ve.

El código de cada archivo es una copia del que está en
`app/src/main/java/com/example/proyecto_seguimiento_habitos/`. Si cambias el diseño
en la app, actualiza también aquí.

## Dónde está cada cosa en el código

```
proyecto_seguimiento_habitos/
├── MainActivity.kt        ← arranque de la app y navegación (AppNavigation)
├── modelo/                ← los datos: Habito.kt, rachas, puntos, estadísticas y reglas de los avisos
├── notificaciones/        ← alarmas y notificaciones del teléfono (recordatorios y racha en peligro)
├── basededatos/           ← lo que lee y guarda en Firestore (HabitosRepository.kt)
├── viewmodels/            ← estado que usan las pantallas (AuthViewModel, HabitosViewModel)
├── validaciones/          ← reglas de los formularios (Validators, HabitoValidators)
├── pantallas/             ← cada ventana: Login, Registro, Inicio, Calendario, Crear, Perfil, Logros...
├── componentes/           ← piezas reutilizables: botones, campos, barra inferior, diálogos,
│                            tarjeta de nivel, tarjetas de estadística y gráfico de barras
└── ui/theme/              ← colores, letras y tema
```

## Índice

| Archivo | Qué contiene |
|---|---|
| [01-colores-y-letras.md](01-colores-y-letras.md) | Paleta de colores y tipografías (Stitch) |
| [02-botones.md](02-botones.md) | Botón principal, botón secundario, TextButton, IconButton, círculo de check |
| [03-campos-de-texto.md](03-campos-de-texto.md) | Campo de texto con ícono, texto de error, casilla (Checkbox) |
| [04-filas-columnas-y-cajas.md](04-filas-columnas-y-cajas.md) | Row, Column, Box, Spacer, FlowRow, LazyColumn, LazyRow |
| [05-tarjetas-y-chips.md](05-tarjetas-y-chips.md) | Tarjetas (Surface), chips, píldoras, ícono del hábito, barra de progreso |
| [06-barras-dialogos-y-hojas.md](06-barras-dialogos-y-hojas.md) | Scaffold, barra inferior, Snackbar, diálogo, hoja inferior, reloj |

## Regla general de los diseños

Casi todo se arma con **`Surface`** (una caja con color, forma redondeada y borde)
y dentro un **`Row`** (cosas en fila) o un **`Column`** (cosas una debajo de otra).

```
Surface  ← fondo, esquinas redondeadas, borde, sombra
 └─ Row / Column  ← cómo se ordena lo de adentro
     ├─ Icon
     ├─ Spacer   ← espacio vacío entre elementos
     └─ Text
```

Los colores **nunca** se escriben a mano en las pantallas: se piden al tema con
`MaterialTheme.colorScheme.primary`, `...surface`, etc. Las letras igual:
`MaterialTheme.typography.headlineLarge`, `...bodyMedium`, etc.
