package com.example.proyecto_seguimiento_habitos

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Acceso a los hábitos en Cloud Firestore.
 * Cada usuario guarda sus hábitos en: usuarios/{uid}/habitos
 *
 * Todas las funciones usan callbacks (onExito / onError) igual que el resto del
 * proyecto, para no necesitar ViewModel ni corrutinas.
 */
object HabitosRepository {

    private const val SIN_SESION = "No hay una sesión activa."

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /** Devuelve la colección de hábitos del usuario con la sesión abierta. */
    private fun coleccionHabitos(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("usuarios").document(uid).collection("habitos")
    }

    /**
     * Escucha los hábitos del usuario en tiempo real: cada vez que se crea o se
     * actualiza un hábito, se vuelve a llamar a onCambio con la lista nueva.
     * Hay que guardar el resultado y llamar a remove() al salir de la pantalla.
     */
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

    /** Guarda un hábito nuevo en Firestore. */
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

        val datos = hashMapOf(
            "nombre" to habito.nombre.trim(),
            "categoria" to habito.categoria,
            "frecuencia" to habito.frecuencia,
            "dias" to habito.dias,
            "vecesPorSemana" to habito.vecesPorSemana,
            "horario" to habito.horario.trim(),
            "completados" to emptyList<String>(),
            "fechaCreacion" to Timestamp.now()
        )

        coleccion.add(datos)
            .addOnSuccessListener { onExito() }
            .addOnFailureListener { excepcion ->
                onError(excepcion.localizedMessage ?: "No se pudo guardar el hábito.")
            }
    }

    /**
     * Marca o desmarca el hábito para el día de hoy.
     * Guarda la fecha exacta ("yyyy-MM-dd") dentro del arreglo "completados".
     */
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
        val cambio = if (completado) FieldValue.arrayUnion(hoy) else FieldValue.arrayRemove(hoy)

        coleccion.document(habitoId)
            .update("completados", cambio)
            .addOnFailureListener { excepcion ->
                onError(excepcion.localizedMessage ?: "No se pudo actualizar el hábito.")
            }
    }

    /** Convierte un documento de Firestore en un objeto Habito. */
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
            completados = aListaDeTextos(datos["completados"])
        )
    }

    /** Convierte el campo "dias" de Firestore en una lista de enteros (1 = Lunes). */
    private fun aListaDeNumeros(valor: Any?): List<Int> {
        val lista = valor as? List<*> ?: return emptyList()
        return lista.mapNotNull { elemento -> (elemento as? Number)?.toInt() }
    }

    /** Convierte el campo "completados" de Firestore en una lista de fechas. */
    private fun aListaDeTextos(valor: Any?): List<String> {
        val lista = valor as? List<*> ?: return emptyList()
        return lista.mapNotNull { elemento -> elemento as? String }
    }
}
