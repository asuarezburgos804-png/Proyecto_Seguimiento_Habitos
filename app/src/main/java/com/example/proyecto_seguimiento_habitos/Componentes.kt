package com.example.proyecto_seguimiento_habitos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Componentes reutilizables con el estilo de los mockups

// Sombra suave y difusa (0 4px 20px rgba(0,0,0,0.04))
fun Modifier.sombraSuave(shape: Shape, elevacion: Dp = 6.dp): Modifier = this.shadow(
    elevation = elevacion,
    shape = shape,
    ambientColor = Color.Black.copy(alpha = 0.04f),
    spotColor = Color.Black.copy(alpha = 0.08f)
)

// Campo de texto blanco con ícono a la izquierda
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
        ////////////////// Interfaz: etiqueta (opcional)
        if (etiqueta != null) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        ////////////////// Interfaz: campo
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

// Botón principal: píldora verde con texto blanco
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
    val forma = RoundedCornerShape(50)
    ////////////////// Interfaz: botón principal
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
    ////////////////////////
}

// Botón secundario: píldora con borde verde
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    icono: ImageVector? = null
) {
    ////////////////// Interfaz: botón secundario
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
    ////////////////////////
}

// Texto de error bajo un grupo de opciones
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

// Ícono de cada categoría
fun iconoCategoria(categoria: String): ImageVector = when (categoria) {
    "Salud" -> Icons.Outlined.FavoriteBorder
    "Estudio" -> Icons.Outlined.School
    "Ejercicio" -> Icons.Outlined.FitnessCenter
    "Trabajo" -> Icons.Outlined.WorkOutline
    "Personal" -> Icons.Outlined.Person
    else -> Icons.Outlined.Spa
}
