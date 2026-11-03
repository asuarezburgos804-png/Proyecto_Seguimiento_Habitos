# Campos de texto y casillas

## 1. Campo de texto con ícono

```
Correo electrónico                      ← etiqueta (opcional)
╭──────────────────────────────────────╮
│ ✉  tucorreo@ejemplo.com          👁   │  fondo blanco, esquinas de 12 dp
╰──────────────────────────────────────╯
Correo no válido                        ← error en rojo (o ayuda en gris)
```

Se usa en: login, registro y crear hábito. Está en `Componentes.kt`.

```kotlin
@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    placeholder: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    etiqueta: String? = null,
    error: String? = null,
    ayuda: String? = null,
    habilitado: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    iconoFinal: (@Composable () -> Unit)? = null
) {
    val forma = RoundedCornerShape(12.dp)
    Column(modifier = modifier.fillMaxWidth()) {
        if (etiqueta != null) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        OutlinedTextField(
            value = valor,
            onValueChange = onValorChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline) },
            leadingIcon = {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = iconoFinal,
            singleLine = true,
            enabled = habilitado,
            isError = error != null,
            supportingText = when {
                error != null -> { { Text(error) } }
                ayuda != null -> { { Text(ayuda) } }
                else -> null
            },
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = forma,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

Ejemplo (correo):

```kotlin
var correo by rememberSaveable { mutableStateOf("") }
var correoError by rememberSaveable { mutableStateOf<String?>(null) }

CampoTexto(
    valor = correo,
    onValorChange = {
        correo = it
        correoError = null          // al escribir se borra el error
    },
    placeholder = "Correo electrónico",
    icono = Icons.Outlined.Mail,
    error = correoError,
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next
    )
)
```

Ejemplo (contraseña con ojito para mostrar/ocultar):

```kotlin
CampoTexto(
    valor = password,
    onValorChange = { password = it },
    placeholder = "Contraseña",
    icono = Icons.Outlined.Lock,
    visualTransformation = if (passwordVisible) VisualTransformation.None
        else PasswordVisualTransformation(),     // muestra ••••••
    iconoFinal = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
                if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = "Mostrar contraseña"
            )
        }
    }
)
```

## 2. Texto de error suelto

Para grupos de opciones que no son un campo (categoría, días, términos).

```kotlin
@Composable
fun TextoError(texto: String?) {
    if (texto == null) return
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

// Uso
TextoError(categoriaError)
```

## 3. Casilla con texto (Checkbox en un Row)

```
[✓]  Mantener sesión iniciada        ← se puede tocar toda la fila
```

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .toggleable(                         // toda la fila funciona como la casilla
            value = mantenerSesion,
            role = Role.Checkbox,
            onValueChange = { mantenerSesion = it }
        )
        .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Checkbox(
        checked = mantenerSesion,
        onCheckedChange = null,              // null porque ya lo maneja el Row
        colors = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary,
            uncheckedColor = MaterialTheme.colorScheme.outline
        )
    )
    Spacer(Modifier.width(8.dp))
    Text(
        text = "Mantener sesión iniciada",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
```

## 4. Selector de hora (parece un campo, pero abre un reloj)

```
╭──────────────────────────────╮
│ 🕒  07:30                    │  Quitar
╰──────────────────────────────╯
```

```kotlin
Surface(
    onClick = { mostrarReloj = true },
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier
        .weight(1f)
        .heightIn(min = 56.dp)
        .sombraSuave(RoundedCornerShape(12.dp), 2.dp)
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(12.dp))
        Text(text = horario.ifEmpty { "Sin hora" }, style = MaterialTheme.typography.bodyLarge)
    }
}
```
