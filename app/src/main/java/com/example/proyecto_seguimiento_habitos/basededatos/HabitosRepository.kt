package com.example.proyecto_seguimiento_habitos.basededatos

import com.example.proyecto_seguimiento_habitos.modelo.*

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

// Todo lo que lee o escribe hábitos en Firestore.
// Ruta: usuarios/{uid}/habitos/{id}
object HabitosRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Colección de hábitos del usuario con sesión (null si no hay sesión)
    private fun coleccion(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("usuarios").document(uid).collection("habitos")
    }

    // Escucha los hábitos en tiempo real. Cada cambio en Firestore llama a onCambio.
    fun escucharHabitos(
        onCambio: (List<Habito>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val coleccion = coleccion()
        if (coleccion == null) {
            onError("No hay una sesión activa.")
            return null
        }

        return coleccion.orderBy("fechaCreacion").addSnapshotListener { consulta, error ->
            if (error != null) {
                onError("No se pudieron cargar los hábitos.")
                return@addSnapshotListener
            }
            val lista = mutableListOf<Habito>()
            for (documento in consulta!!.documents) {
                lista.add(documentoAHabito(documento))
            }
            onCambio(lista)
        }
    }

    // Lee los hábitos una sola vez (lo usan las alarmas, que no tienen pantalla abierta).
    // Si no hay sesión o falla la lectura, devuelve una lista vacía.
    fun leerHabitos(onListo: (List<Habito>) -> Unit) {
        val coleccion = coleccion()
        if (coleccion == null) {
            onListo(emptyList())
            return
        }
        coleccion.get()
            .addOnSuccessListener { consulta ->
                val lista = mutableListOf<Habito>()
                for (documento in consulta.documents) {
                    lista.add(documentoAHabito(documento))
                }
                onListo(lista)
            }
            .addOnFailureListener { onListo(emptyList()) }
    }

    ////////////////// Puntos del usuario (campo "puntos" en usuarios/{uid})

    // Documento del perfil del usuario con sesión (null si no hay sesión)
    private fun perfil(): DocumentReference? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("usuarios").document(uid)
    }

    // Escucha los puntos en tiempo real. Devuelve null si la cuenta todavía no tiene puntos.
    fun escucharPuntos(onCambio: (Int?) -> Unit): ListenerRegistration? {
        val perfil = perfil() ?: return null   // sin sesión no hace nada
        return perfil.addSnapshotListener { documento, error ->
            if (error != null || documento == null) return@addSnapshotListener
            val puntos = documento.getLong("puntos")
            if (puntos == null) onCambio(null) else onCambio(puntos.toInt())
        }
    }

    // Suma (o resta, si la cantidad es negativa) puntos al usuario
    fun sumarPuntos(cantidad: Int) {
        val perfil = perfil() ?: return   // sin sesión no hace nada
        // merge = solo toca el campo "puntos", el resto del perfil queda igual
        perfil.set(hashMapOf("puntos" to FieldValue.increment(cantidad.toLong())), SetOptions.merge())
    }

    // Guarda un valor exacto de puntos (para cuentas que todavía no tenían)
    fun guardarPuntos(puntos: Int) {
        val perfil = perfil() ?: return   // sin sesión no hace nada
        perfil.set(hashMapOf("puntos" to puntos), SetOptions.merge())
    }

    ////////////////// Hábitos

    // Guarda un hábito nuevo (con el historial vacío)
    fun crearHabito(habito: Habito, onExito: () -> Unit, onError: (String) -> Unit) {
        val coleccion = coleccion() ?: return   // sin sesión no hace nada

        val datos = hashMapOf(
            "nombre" to habito.nombre.trim(),
            "categoria" to habito.categoria,
            "frecuencia" to habito.frecuencia,
            "dias" to habito.dias,
            "vecesPorSemana" to habito.vecesPorSemana,
            "horario" to habito.horario.trim(),
            "nota" to habito.nota.trim(),
            "completados" to emptyList<String>(),
            "completadoEn" to emptyMap<String, Timestamp>(),
            "archivado" to false,
            "fechaCreacion" to Timestamp.now()
        )

        coleccion.add(datos)
            .addOnSuccessListener { onExito() }
            .addOnFailureListener { onError("No se pudo guardar el hábito.") }
    }

    // Edita un hábito sin tocar su historial (completados y completadoEn)
    fun actualizarHabito(habito: Habito, onExito: () -> Unit, onError: (String) -> Unit) {
        val coleccion = coleccion() ?: return   // sin sesión no hace nada

        val cambios = hashMapOf<String, Any>(
            "nombre" to habito.nombre.trim(),
            "categoria" to habito.categoria,
            "frecuencia" to habito.frecuencia,
            "dias" to habito.dias,
            "vecesPorSemana" to habito.vecesPorSemana,
            "horario" to habito.horario.trim(),
            "nota" to habito.nota.trim(),
            "fechaEdicion" to Timestamp.now()
        )

        coleccion.document(habito.id).update(cambios)
            .addOnSuccessListener { onExito() }
            .addOnFailureListener { onError("No se pudo actualizar el hábito.") }
    }

    // Marca o desmarca el hábito hoy (guarda la fecha y la hora exacta)
    fun marcarComoCompletado(habitoId: String, completado: Boolean, onError: (String) -> Unit) {
        val coleccion = coleccion() ?: return   // sin sesión no hace nada
        val hoy = fechaDeHoy()
        val documento = coleccion.document(habitoId)

        if (completado) {
            documento.update(
                "completados", FieldValue.arrayUnion(hoy),
                "completadoEn.$hoy", Timestamp.now()
            ).addOnFailureListener { onError("No se pudo actualizar el hábito.") }
        } else {
            documento.update(
                "completados", FieldValue.arrayRemove(hoy),
                "completadoEn.$hoy", FieldValue.delete()
            ).addOnFailureListener { onError("No se pudo actualizar el hábito.") }
        }
    }

    // Archiva (oculta) o restaura un hábito. El historial se conserva.
    fun archivarHabito(habitoId: String, archivar: Boolean, onError: (String) -> Unit) {
        val coleccion = coleccion() ?: return   // sin sesión no hace nada
        coleccion.document(habitoId).update("archivado", archivar)
            .addOnFailureListener { onError("No se pudo archivar el hábito.") }
    }

    // Borra el hábito con todo su historial (no se puede deshacer)
    fun eliminarHabito(habitoId: String, onError: (String) -> Unit) {
        val coleccion = coleccion() ?: return   // sin sesión no hace nada
        coleccion.document(habitoId).delete()
            .addOnFailureListener { onError("No se pudo eliminar el hábito.") }
    }

    // Convierte un documento de Firestore en un Habito
    @Suppress("UNCHECKED_CAST")
    private fun documentoAHabito(doc: DocumentSnapshot): Habito {
        // Firestore guarda los números como Long y las fechas como Timestamp
        val dias = doc.get("dias") as? List<Long> ?: emptyList()
        val completados = doc.get("completados") as? List<String> ?: emptyList()
        val completadoEn = doc.get("completadoEn") as? Map<String, Timestamp> ?: emptyMap()

        return Habito(
            id = doc.id,
            nombre = doc.getString("nombre") ?: "",
            categoria = doc.getString("categoria") ?: "",
            frecuencia = doc.getString("frecuencia") ?: Frecuencias.DIARIO,
            dias = dias.map { it.toInt() },
            vecesPorSemana = (doc.getLong("vecesPorSemana") ?: 0L).toInt(),
            horario = doc.getString("horario") ?: "",
            nota = doc.getString("nota") ?: "",
            completados = completados,
            completadoEn = completadoEn.mapValues { it.value.toDate() },
            archivado = doc.getBoolean("archivado") ?: false,
            fechaCreacion = doc.getTimestamp("fechaCreacion")?.toDate()
        )
    }
}
