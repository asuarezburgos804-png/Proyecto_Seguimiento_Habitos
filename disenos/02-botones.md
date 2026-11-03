# Botones

## 1. Botón principal (píldora verde)

```
╭──────────────────────────────────────╮
│        Iniciar sesión   →            │   verde, texto blanco, alto 56 dp
╰──────────────────────────────────────╯
```

Se usa en: Iniciar sesión, Crear cuenta, Guardar hábito.
Si `cargando = true` muestra una ruedita en vez del texto.

```kotlin
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    iconoInicio: ImageVector? = null,
    iconoFinal: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward
) {
    val forma = RoundedCornerShape(50)          // 50 % = bordes totalmente redondos
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        shape = forma,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .sombraSuave(forma)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            if (iconoInicio != null) {
                Icon(iconoInicio, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge)
            if (iconoFinal != null) {
                Spacer(Modifier.width(8.dp))
                Icon(iconoFinal, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
```

Ejemplos de uso:

```kotlin
BotonPrincipal(texto = "Iniciar sesión", onClick = { iniciarSesion() })

BotonPrincipal(
    texto = "Guardar hábito",
    onClick = { guardar() },
    cargando = guardando,
    iconoInicio = Icons.Outlined.Save,
    iconoFinal = null
)
```

## 2. Botón secundario (píldora con borde verde)

```
╭──────────────────────────────────────╮
│  ⎋  Cerrar sesión                    │   fondo transparente, borde verde de 2 dp
╰──────────────────────────────────────╯
```

Se usa en: "Crear una cuenta" (login) y "Cerrar sesión" (perfil).

```kotlin
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    icono: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(50),
        border = BorderStroke(2.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}
```

## 3. TextButton (botón solo de texto)

```
¿Ya tienes una cuenta?  Iniciar sesión      ← "Iniciar sesión" es un TextButton
```

Se usa en: "Iniciar sesión" del registro, "Quitar" la hora, "Aceptar"/"Cancelar" de los diálogos.

```kotlin
TextButton(onClick = { volverAlLogin() }) {
    Text(
        text = "Iniciar sesión",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

// En rojo (diálogo de eliminar)
TextButton(onClick = onConfirmar) {
    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
}
```

## 4. IconButton (botón de solo ícono)

Se usa en: ojo de la contraseña, flecha de volver, cerrar (X), cambiar de mes (‹ ›), los tres puntos.

```kotlin
IconButton(onClick = { passwordVisible = !passwordVisible }) {
    Icon(
        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
        contentDescription = "Mostrar contraseña",
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

## 5. Botón redondo de volver (registro)

```
 ( ← )     círculo blanco de 40 dp con borde finito
```

```kotlin
Surface(
    onClick = { volverAlLogin() },
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    modifier = Modifier.size(40.dp)
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Volver",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
```

## 6. Círculo de check (marcar un hábito)

```
 ( )   pendiente: círculo vacío con borde gris
 (✓)   completado: círculo verde con check blanco
```

Área tocable de 48 dp, círculo visible de 32 dp. Vibra al tocar.
Está en `DashboardScreen.kt`.

```kotlin
@Composable
private fun CirculoCheck(completado: Boolean, nombre: String, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    // El color cambia con una pequeña animación
    val fondo by animateColorAsState(
        if (completado) colores.primary else Color.Transparent,
        label = "fondoCheck"
    )
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
}
```
