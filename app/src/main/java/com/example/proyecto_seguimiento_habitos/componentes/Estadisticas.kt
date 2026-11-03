package com.example.proyecto_seguimiento_habitos.componentes

import com.example.proyecto_seguimiento_habitos.modelo.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Componentes de puntos, niveles y estadísticas

// Barra de progreso: dos cajas, el riel (fondo) y lo avanzado (encima).
// avance va de 0 (vacía) a 1 (llena).
@Composable
fun BarraProgreso(avance: Float, colorRiel: Color, colorAvance: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(colorRiel)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(avance)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(colorAvance)
        )
    }
}

// Tarjeta verde con el nivel y los puntos del usuario (perfil)
@Composable
fun TarjetaNivel(puntos: Int) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(24.dp)
    val nivel = nivelDe(puntos)
    val avance = puntosEnEsteNivel(puntos) / PUNTOS_POR_NIVEL.toFloat()

    ////////////////// Interfaz: nivel del usuario
    Surface(
        shape = forma,
        color = colores.primary,
        modifier = Modifier
            .fillMaxWidth()
            .sombraSuave(forma)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tu nivel",
                        style = MaterialTheme.typography.labelMedium,
                        color = colores.onPrimary.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Nivel $nivel",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colores.onPrimary
                    )
                }
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = colores.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${maxOf(puntos, 0)} pts",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colores.onPrimary
                )
            }
            Spacer(Modifier.height(16.dp))

            BarraProgreso(
                avance = avance,
                colorRiel = colores.onPrimary.copy(alpha = 0.25f),
                colorAvance = colores.onPrimary
            )
            Spacer(Modifier.height(10.dp))

            Text(
                text = "Te faltan ${puntosParaSubir(puntos)} puntos para el nivel ${nivel + 1}. " +
                    "Ganas $PUNTOS_POR_HABITO puntos por cada hábito completado.",
                style = MaterialTheme.typography.bodySmall,
                color = colores.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
    ////////////////////////
}

// Tarjeta pequeña con un dato: ícono, valor grande y título
@Composable
fun TarjetaEstadistica(
    icono: ImageVector,
    valor: String,
    titulo: String,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(20.dp)

    ////////////////// Interfaz: dato estadístico
    Surface(
        shape = forma,
        color = colores.surfaceContainerLowest,
        border = BorderStroke(1.dp, colores.surfaceVariant),
        modifier = modifier.sombraSuave(forma, 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = colores.tertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                color = colores.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                color = colores.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
    ////////////////////////
}

// Gráfico de barras simple: una columna por barra, con su valor arriba y su etiqueta abajo.
// La barra más alta mide 100 dp y las demás se calculan en proporción.
@Composable
fun GraficoBarras(titulo: String, barras: List<Barra>) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(24.dp)
    val alturaMaxima = 100

    // Valor más alto (mínimo 1 para no dividir entre 0)
    var maximo = 1
    for (barra in barras) {
        maximo = maxOf(maximo, barra.valor)
    }

    ////////////////// Interfaz: gráfico de barras
    Surface(
        shape = forma,
        color = colores.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .sombraSuave(forma)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = colores.onBackground
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (barra in barras) {
                    // Alto de esta barra (mínimo 4 dp para que se vea aunque sea 0)
                    val alto = maxOf(barra.valor * alturaMaxima / maximo, 4)

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${barra.valor}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colores.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(alto.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (barra.valor > 0) colores.primary else colores.surfaceVariant)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = barra.etiqueta,
                            style = MaterialTheme.typography.labelSmall,
                            color = colores.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
    ////////////////////////
}
