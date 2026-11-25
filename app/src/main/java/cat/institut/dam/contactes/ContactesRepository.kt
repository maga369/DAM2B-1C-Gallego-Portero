package cat.institut.dam.contactes

import android.net.Uri

/**
 * Repositori singleton que manté l'estat compartit en memòria
 * entre la llista principal i la pantalla de detall.
 */
object ContactesRepository {

    private val contactes = mutableListOf<Contacte>()
    private val favorits = mutableSetOf<Int>()
    private val fotos = mutableMapOf<Int, Uri>()

    val llista: List<Contacte>
        get() = contactes

    fun actualitza(novs: List<Contacte>) {
        contactes.clear()
        contactes.addAll(novs)
    }

    fun contacte(id: Int): Contacte? {
        return contactes.firstOrNull { it.id == id }
    }

    fun esFavorit(id: Int): Boolean {
        return id in favorits
    }

    fun alternaFavorit(id: Int) {
        if (id in favorits) {
            favorits.remove(id)
        } else {
            favorits.add(id)
        }
    }

    fun cerca(texte: String): List<Contacte> {
        if (texte.isBlank()) return llista
        return contactes.filter { contacte ->
            contacte.nom.contains(texte, ignoreCase = true) ||
                    contacte.ciutat.contains(texte, ignoreCase = true) ||
                    contacte.email.contains(texte, ignoreCase = true)
        }
    }

    fun desaFoto(id: Int, uri: Uri) {
        fotos[id] = uri
    }

    fun foto(id: Int): Uri? {
        return fotos[id]
    }
}