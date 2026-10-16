package com.example.proyecto_seguimiento_habitos

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

// Hábitos en Firestore (usuarios/{uid}/habitos)
// El historial vive dentro del documento: "completados" (fechas) y "completadoEn" (momento exacto)
object HabitosRepository {

    private const val SIN_SESION = "No hay una sesión activa."

    // Firebase Auth y Firestore
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Colección de hábitos del usuario (null si no hay sesión)
    private fun coleccionHabitos(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("usuarios").document(uid).collection("habitos")
    }

    // Escucha los hábitos en tiempo real (llamar remove() al salir)
    fun escucharHabitos(
        onCambio: (List<Habito>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val coleccion = coleccionHabitos()
        if (coleccion == null) {
            onError(SIN_SESION)
            return null
        }

        return coleccion.orderBy("fechaCreacion").addSnapshotListener { consulta, excepcion ->
            if (excepcion != null) {
                onError(excepcion.localizedMessage ?: "No se pudieron cargar los hábitos.")
                return@addSnapshotListener
            }
            val documentos = consulta?.documents ?: emptyList()
            onCambio(documentos.map { documentoAHabito(it) })
        }
    }

    // Campos editables del hábito (no incluye el historial)
    private fun camposEditables(habito: Habito): Map<String, Any> = mapOf(
        "nombre" to habito.nombre.trim(),
        "categoria" to habito.categoria,
        "frecuencia" to habito.frecuencia,
        "dias" to habito.dias,
        "vecesPorSemana" to habito.vecesPorSemana,
        "horario" to habito.horario.trim()
    )

    // Guarda un hábito nuevo
    fun crearHabito(
        habito: Habito,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        val coleccion = coleccionHabitos()
        if (coleccion == null) {
            onError(SIN_SESION)
            return
        }

        val datos = camposEditables(habito) + mapOf(
            "completados" to emptyList<String>(),
            "completadoEn" to emptyMap<String, Timestamp>(),
            "archivado" to false,
            "fechaCreacion" to Timestamp.now()
        )

        coleccion.add(datos)
            .addOnSuccessListener { onExito() }
            .addOnFailureListener { excepcion ->
                onError(excepcion.localizedMessage ?: "No se pudo guardar el hábito.")
            }
    }

    // Edita nombre, categoría, frecuencia y horario sin tocar el historial
    fun actualizarHabito(
        habito: Habito,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        val coleccion = coleccionHabitos()
        if (coleccion == null) {
            onError(SIN_SESION)
            return
        }

        // update() solo modifica estos campos; "completados" y "completadoEn" quedan intactos
        val cambios = camposEditables(habito) + ("fechaEdicion" to Timestamp.now())

        coleccion.document(habito.id)
            .update(cambios)
            .addOnSuccessListener { onExito() }
            .addOnFailureListener { excepcion ->
                onError(excepcion.localizedMessage ?: "No se pudo actualizar el hábito.")
            }
    }

    // Marca o desmarca el hábito hoy, guardando la fecha y la hora exacta
    fun marcarComoCompletado(
        habitoId: String,
        completado: Boolean,
        onError: (String) -> Unit
    ) {
        val coleccion = coleccionHabitos()
        if (coleccion == null) {
            onError(SIN_SESION)
            return
        }

        val hoy = fechaDeHoy()
        val documento = coleccion.document(habitoId)
        // Firestore aplica el cambio en local al instante (el dashboard se actualiza sin recargar)
        val tarea = if (completado) {
            documento.update(
                "completados", FieldValue.arrayUnion(hoy),
                "completadoEn.$hoy", Timestamp.now()
            )
        } else {
            documento.update(
                "completados", FieldValue.arrayRemove(hoy),
                "completadoEn.$hoy", FieldValue.delete()
            )
        }
        tarea.addOnFailureListener { excepcion ->
            onError(excepcion.localizedMessage ?: "No se pudo actualizar el hábito.")
        }
    }

    // Archiva (oculta) o restaura un hábito; el historial se conserva
    fun archivarHabito(
        habitoId: String,
        archivar: Boolean,
        onError: (String) -> Unit
    ) {
        val coleccion = coleccionHabitos()
        if (coleccion == null) {
            onError(SIN_SESION)
            return
        }

        coleccion.document(habitoId)
            .update("archivado", archivar)
            .addOnFailureListener { excepcion ->
                onError(excepcion.localizedMessage ?: "No se pudo archivar el hábito.")
            }
    }

    // Elimina el hábito y todo su historial (no se puede deshacer)
    fun eliminarHabito(
        habitoId: String,
        onError: (String) -> Unit
    ) {
        val coleccion = coleccionHabitos()
        if (coleccion == null) {
            onError(SIN_SESION)
            return
        }

        coleccion.document(habitoId)
            .delete()
            .addOnFailureListener { excepcion ->
                onError(excepcion.localizedMessage ?: "No se pudo eliminar el hábito.")
            }
    }

    // Documento de Firestore -> Habito
    private fun documentoAHabito(doc: DocumentSnapshot): Habito {
        val datos = doc.data ?: emptyMap()

        return Habito(
            id = doc.id,
            nombre = doc.getString("nombre") ?: "",
            categoria = doc.getString("categoria") ?: "",
            frecuencia = doc.getString("frecuencia") ?: Frecuencias.DIARIO,
            dias = aListaDeNumeros(datos["dias"]),
            vecesPorSemana = (doc.getLong("vecesPorSemana") ?: 0L).toInt(),
            horario = doc.getString("horario") ?: "",
            completados = aListaDeTextos(datos["completados"]),
            completadoEn = aMapaDeFechas(datos["completadoEn"]),
            archivado = doc.getBoolean("archivado") ?: false,
            // Se guarda al crear el hábito: el calendario no juzga los días anteriores
            fechaCreacion = doc.getTimestamp("fechaCreacion")?.toDate()
        )
    }

    // Campo "dias" -> List<Int> (Firestore los devuelve como Long)
    private fun aListaDeNumeros(valor: Any?): List<Int> {
        val lista = valor as? List<*> ?: return emptyList()
        return lista.mapNotNull { elemento -> (elemento as? Number)?.toInt() }
    }

    // Campo "completados" -> List<String>
    private fun aListaDeTextos(valor: Any?): List<String> {
        val lista = valor as? List<*> ?: return emptyList()
        return lista.mapNotNull { elemento -> elemento as? String }
    }

    // Campo "completadoEn" -> Map<fecha, Date>
    private fun aMapaDeFechas(valor: Any?): Map<String, Date> {
        val mapa = valor as? Map<*, *> ?: return emptyMap()
        return mapa.entries.mapNotNull { (clave, momento) ->
            val fecha = clave as? String ?: return@mapNotNull null
            val date = (momento as? Timestamp)?.toDate() ?: return@mapNotNull null
            fecha to date
        }.toMap()
    }
}
