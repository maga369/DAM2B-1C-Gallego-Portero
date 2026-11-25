package cat.institut.dam.contactes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Servei encarregat d'obtenir les dades dels contactes (des de la xarxa o
 * des del fitxer local d'assets) i de descarregar les imatges d'avatar.
 */
object ContactesService {

    private const val URL_CONTACTES = "https://jsonplaceholder.typicode.com/users"
    private const val TIMEOUT_MS = 8000

    fun desDeXarxa(): List<Contacte> {
        val connexio = URL(URL_CONTACTES).openConnection() as HttpURLConnection
        try {
            connexio.requestMethod = "GET"
            connexio.connectTimeout = TIMEOUT_MS
            connexio.readTimeout = TIMEOUT_MS

            if (connexio.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Error HTTP: ${connexio.responseCode}")
            }

            val text = connexio.inputStream.bufferedReader().use { it.readText() }
            return parseja(text)
        } finally {
            connexio.disconnect()
        }
    }

    fun desDeRecurs(context: Context): List<Contacte> {
        val contingut = context.assets.open("contactes.json").bufferedReader().use { it.readText() }
        return parseja(contingut)
    }

    fun parseja(text: String): List<Contacte> {
        val array = JSONArray(text)
        val resultat = mutableListOf<Contacte>()
        for (i in 0 until array.length()) {
            val objecte = array.getJSONObject(i)
            resultat.add(Contacte.desDeJson(objecte))
        }
        return resultat
    }

    fun baixaImatge(url: String): Bitmap? {
        var connexio: HttpURLConnection? = null
        return try {
            connexio = URL(url).openConnection() as HttpURLConnection
            connexio.connectTimeout = TIMEOUT_MS
            connexio.readTimeout = TIMEOUT_MS
            if (connexio.responseCode != HttpURLConnection.HTTP_OK) return null
            connexio.inputStream.use { entrada ->
                BitmapFactory.decodeStream(entrada)
            }
        } catch (e: Exception) {
            null
        } finally {
            connexio?.disconnect()
        }
    }
}