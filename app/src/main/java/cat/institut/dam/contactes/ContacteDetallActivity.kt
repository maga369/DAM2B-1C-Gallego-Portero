package cat.institut.dam.contactes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla de detall d'un contacte amb les seves dades i els sis intents implícits.
 */
class ContacteDetallActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_contacte_id"
        private const val REQUEST_FOTO = 200
    }

    private lateinit var ivFoto: ImageView
    private lateinit var tvNom: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvTelefon: TextView
    private lateinit var tvAdreca: TextView
    private lateinit var tvWeb: TextView
    private lateinit var chkFavorit: CheckBox

    private var contacteId: Int = -1
    private val filPrincipal = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detall)
        Finestres.aplica(this, findViewById<View>(R.id.arrelDetall))

        contacteId = intent.getIntExtra(EXTRA_ID, -1)
        val contacte = ContactesRepository.contacte(contacteId)

        if (contacte == null) {
            Toast.makeText(this, R.string.error_contacte, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        ivFoto = findViewById(R.id.ivFoto)
        tvNom = findViewById(R.id.tvNom)
        tvEmail = findViewById(R.id.tvEmail)
        tvTelefon = findViewById(R.id.tvTelefon)
        tvAdreca = findViewById(R.id.tvAdreca)
        tvWeb = findViewById(R.id.tvWeb)
        chkFavorit = findViewById(R.id.chkFavorit)

        // Dades del contacte
        tvNom.text = contacte.nom
        tvEmail.text = contacte.email
        tvTelefon.text = contacte.telefon
        tvAdreca.text = contacte.adreçaCompleta
        tvWeb.text = contacte.web

        // Estat de favorit
        chkFavorit.isChecked = ContactesRepository.esFavorit(contacte.id)
        chkFavorit.setOnCheckedChangeListener { _, _ ->
            ContactesRepository.alternaFavorit(contacte.id)
        }

        // Foto: si l'usuari n'ha triat una de la galeria, la mostrem; si no, baixem l'avatar
        val fotoDesada = ContactesRepository.foto(contacte.id)
        if (fotoDesada != null) {
            ivFoto.setImageURI(fotoDesada)
        } else {
            carregaAvatar(contacte.urlAvatar)
        }

        configuraBotons(contacte)
    }

    private fun carregaAvatar(url: String) {
        Thread {
            val bitmap = ContactesService.baixaImatge(url)
            filPrincipal.post {
                if (bitmap != null) {
                    ivFoto.setImageBitmap(bitmap)
                } else {
                    ivFoto.setImageResource(R.drawable.ic_contacte)
                }
            }
        }.start()
    }

    private fun configuraBotons(contacte: Contacte) {
        // 1. Trucar
        findViewById<Button>(R.id.btnTrucar).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contacte.telefon}"))
            llancaIntentSegur(intent)
        }

        // 2. Correu
        findViewById<Button>(R.id.btnCorreu).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contacte.email}"))
            llancaIntentSegur(intent)
        }

        // 3. Mapa
        findViewById<Button>(R.id.btnMapa).setOnClickListener {
            val etiqueta = Uri.encode(contacte.nom)
            val uriMapa = Uri.parse(
                "geo:${contacte.latitud},${contacte.longitud}?q=${contacte.latitud},${contacte.longitud}($etiqueta)"
            )
            llancaIntentSegur(Intent(Intent.ACTION_VIEW, uriMapa))
        }

        // 4. Web (afegim esquema https:// si no en porta)
        findViewById<Button>(R.id.btnWeb).setOnClickListener {
            var web = contacte.web
            if (!web.startsWith("http")) {
                web = "https://$web"
            }
            llancaIntentSegur(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
        }

        // 5. Alarma (a les 9:00)
        findViewById<Button>(R.id.btnAlarma).setOnClickListener {
            val alarma = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, 9)
                putExtra(AlarmClock.EXTRA_MINUTES, 0)
                putExtra(AlarmClock.EXTRA_MESSAGE, getString(R.string.alarma_missatge, contacte.nom))
            }
            llancaIntentSegur(alarma)
        }

        // 6. Galeria
        findViewById<Button>(R.id.btnGaleria).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
            try {
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQUEST_FOTO)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, R.string.error_app_no_disponible, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun llancaIntentSegur(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_app_no_disponible, Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_FOTO || resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Alguns selectors no concedeixen permís persistent
        }

        ContactesRepository.desaFoto(contacteId, uri)
        ivFoto.setImageURI(uri)
    }
}