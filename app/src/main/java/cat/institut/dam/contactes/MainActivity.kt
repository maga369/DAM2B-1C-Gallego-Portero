package cat.institut.dam.contactes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * MainActivity — la llista de contactes.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /** Codi de la petició de permís de notificacions. */
        private const val REQUEST_NOTIFICACIONS = 100
    }

    private lateinit var etCerca: EditText
    private lateinit var btnActualitza: Button
    private lateinit var pbCarga: ProgressBar
    private lateinit var txtBuit: TextView
    private lateinit var txtAvis: TextView
    private lateinit var rvContactes: RecyclerView

    private lateinit var adapter: ContacteAdapter
    private val filPrincipal = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Finestres.aplica(this, findViewById<View>(R.id.arrel))

        etCerca = findViewById(R.id.etCerca)
        btnActualitza = findViewById(R.id.btnActualitza)
        pbCarga = findViewById(R.id.pbCarga)
        txtBuit = findViewById(R.id.txtBuit)
        txtAvis = findViewById(R.id.txtAvis)
        rvContactes = findViewById(R.id.rvContactes)

        // TODO 1: crea el teu ContacteAdapter, assigna'l al RecyclerView i posa-li un LinearLayoutManager
        adapter = ContacteAdapter(
            onContacteClick = { contacte ->
                val intent = Intent(this, ContacteDetallActivity::class.java)
                intent.putExtra(ContacteDetallActivity.EXTRA_ID, contacte.id)
                startActivity(intent)
            },
            onFavoritClick = { contacte ->
                ContactesRepository.alternaFavorit(contacte.id)
            }
        )
        rvContactes.layoutManager = LinearLayoutManager(this)
        rvContactes.adapter = adapter

        // TODO 2: el cercador ha de filtrar la llista a cada tecla
        etCerca.doAfterTextChanged { text ->
            aplicaFiltre(text?.toString().orEmpty())
        }

        // TODO 3: el botó ha de tornar a carregar els contactes
        btnActualitza.setOnClickListener {
            carrega()
        }

        carrega()
    }

    override fun onResume() {
        super.onResume()
        // Refresquem la llista per si s'ha canviat el favorit dins la pantalla de detall
        if (::adapter.isInitialized) {
            aplicaFiltre(etCerca.text?.toString().orEmpty())
        }
    }

    private fun aplicaFiltre(texte: String) {
        val filtrats = ContactesRepository.cerca(texte)
        adapter.actualitza(filtrats)

        if (filtrats.isEmpty()) {
            txtBuit.visibility = View.VISIBLE
            txtBuit.text = if (texte.isBlank()) {
                getString(R.string.txt_buit)
            } else {
                getString(R.string.txt_buit_cerca)
            }
        } else {
            txtBuit.visibility = View.GONE
        }
    }

    /**
     * TODO 5: carrega els contactes en un fil secundari i actualitza la UI amb Handler.
     */
    private fun carrega() {
        pbCarga.visibility = View.VISIBLE
        btnActualitza.isEnabled = false
        txtAvis.visibility = View.GONE

        Thread {
            var dadesLocals = false
            val llistaCarregada = try {
                ContactesService.desDeXarxa()
            } catch (e: Exception) {
                dadesLocals = true
                try {
                    ContactesService.desDeRecurs(applicationContext)
                } catch (eLocal: Exception) {
                    emptyList()
                }
            }

            filPrincipal.post {
                pbCarga.visibility = View.GONE
                btnActualitza.isEnabled = true

                if (llistaCarregada.isEmpty()) {
                    Toast.makeText(this, R.string.error_carrega, Toast.LENGTH_LONG).show()
                    return@post
                }

                if (dadesLocals) {
                    txtAvis.text = getString(R.string.avis_dades_locals)
                    txtAvis.visibility = View.VISIBLE
                } else {
                    txtAvis.visibility = View.GONE
                }

                ContactesRepository.actualitza(llistaCarregada)
                aplicaFiltre(etCerca.text?.toString().orEmpty())

                // TODO 4: demanem permís després de carregar i mostrem notificació si ja el tenim
                demanaPermisNotificacions()
                Notificacions.mostraContactesCarregats(this, llistaCarregada.size)
            }
        }.start()
    }

    private fun demanaPermisNotificacions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permissio = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permissio) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        requestPermissions(arrayOf(permissio), REQUEST_NOTIFICACIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICACIONS) {
            val concedit = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (concedit) {
                Notificacions.mostraContactesCarregats(this, ContactesRepository.llista.size)
            } else {
                // TODO 6: avisa l'usuari que no es mostraran notificacions
                Toast.makeText(this, R.string.permis_denegada, Toast.LENGTH_LONG).show()
            }
        }
    }
}