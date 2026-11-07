package com.example.proyecto_seguimiento_habitos.modelo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

// Foto de perfil (avatar) del usuario.
// La foto que sube el usuario se hace pequeña, se comprime y se guarda como texto
// en Firestore: usuarios/{uid}.avatar
// Así no hace falta Firebase Storage y la foto se ve en cualquier teléfono con la misma cuenta.

private const val TAMANO_MAXIMO = 256   // píxeles del lado más largo
private const val CALIDAD = 70          // calidad del JPEG (0 a 100)

// Convierte la foto que eligió el usuario en texto (Base64). Devuelve null si algo falla.
fun uriAAvatar(context: Context, uri: Uri): String? {
    try {
        ////////////////// 1) Medir la foto sin cargarla entera (una foto del celular
        //////////////////    puede pesar mucho y dejar sin memoria a la app)
        val medida = BitmapFactory.Options()
        medida.inJustDecodeBounds = true
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, medida)
        }
        if (medida.outWidth <= 0) return null

        ////////////////// 2) Leerla ya reducida (de 2 en 2: 1, 2, 4, 8...)
        var reduccion = 1
        while (medida.outWidth / (reduccion * 2) >= TAMANO_MAXIMO &&
            medida.outHeight / (reduccion * 2) >= TAMANO_MAXIMO
        ) {
            reduccion *= 2
        }
        val opciones = BitmapFactory.Options()
        opciones.inSampleSize = reduccion
        val original = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opciones)
        } ?: return null

        ////////////////// 3) Dejarla del tamaño final sin deformarla
        val ladoMayor = maxOf(original.width, original.height)
        val escala = minOf(TAMANO_MAXIMO.toFloat() / ladoMayor, 1f)   // nunca se agranda
        val ancho = maxOf((original.width * escala).toInt(), 1)
        val alto = maxOf((original.height * escala).toInt(), 1)
        val pequena = Bitmap.createScaledBitmap(original, ancho, alto, true)

        ////////////////// 4) Enderezarla si la foto se tomó de lado
        val derecha = enderezar(context, uri, pequena)

        ////////////////// 5) Comprimir y pasar a texto
        val salida = ByteArrayOutputStream()
        derecha.compress(Bitmap.CompressFormat.JPEG, CALIDAD, salida)
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        return null
    }
}

// Las fotos del celular guardan en qué posición se tomaron. Si no se gira,
// el avatar sale de costado o al revés.
private fun enderezar(context: Context, uri: Uri, imagen: Bitmap): Bitmap {
    try {
        val entrada = context.contentResolver.openInputStream(uri) ?: return imagen
        val datos = entrada.use { ExifInterface(it) }

        val grados = when (datos.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (grados == 0f) return imagen

        val giro = Matrix()
        giro.postRotate(grados)
        return Bitmap.createBitmap(imagen, 0, 0, imagen.width, imagen.height, giro, true)
    } catch (e: Exception) {
        return imagen   // si no se puede leer la orientación, se deja como está
    }
}

// Convierte el texto guardado de vuelta a imagen. Devuelve null si no hay foto.
fun avatarABitmap(avatar: String): Bitmap? {
    if (avatar.isBlank()) return null
    try {
        val bytes = Base64.decode(avatar, Base64.NO_WRAP)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        return null
    }
}

// Primera letra del nombre (o del correo) para mostrar cuando el usuario no tiene foto
fun inicialDe(nombre: String?, correo: String?): String {
    val letra = nombre?.trim()?.firstOrNull() ?: correo?.trim()?.firstOrNull() ?: '?'
    return letra.uppercaseChar().toString()
}
