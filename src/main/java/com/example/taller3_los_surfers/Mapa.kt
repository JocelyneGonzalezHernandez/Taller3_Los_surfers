package com.example.taller3_los_surfers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

data class Localizacion(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val name: String = ""
)

class Mapa : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        val toolbar: Toolbar = findViewById(R.id.barra)
        setSupportActionBar(toolbar)

        // Configurar el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Asegurarse de que el ActionBar está visible
        supportActionBar?.show()
    }

    // Inflar el menú
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)  // Asegúrate de que el archivo menu.xml esté correctamente configurado
        Log.d("Mapa", "Menú inflado correctamente")

        // Verificar si el menú se infló correctamente
        if (menu.size() == 0) {
            Log.e("Mapa", "Error al inflar el menú")
            // Manejar el error, por ejemplo, mostrar un mensaje al usuario
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Manejo de la selección de los items del menú
        return when (item.itemId) {
            R.id.menuLogOut -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                //finish()*/
                true
            }
            R.id.dispo -> {
                // Acción para abrir configuración o lo que necesites
                true
            }
            R.id.noDispo -> {
                // Acción para abrir configuración o lo que necesites
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d("Mapa", "Mapa listo. Verificando permisos...")

        // Verificar permisos de ubicación
        verificarPermisosUbicacion()
    }
    private fun verificarPermisosUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Solicitar permisos si no están otorgados
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        } else {
            // Los permisos ya están otorgados, cargar ubicación y datos del mapa
            mostrarUbicacionActual()
            loadMapData()
        }
    }

    // Método para manejar la respuesta de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permisos otorgados, mostrar ubicación y cargar datos
            mostrarUbicacionActual()
            loadMapData()
        } else {
            Log.d("Permisos", "Permisos de ubicación no otorgados.")
        }
    }

    private fun mostrarUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.addMarker(MarkerOptions().position(currentLatLng).title("Ubicación Actual"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun loadMapData() {
        Log.d("Mapa", "loadMapData() fue llamada")
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("locations")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var firstLocation: LatLng? = null
                    Log.d("Firebase", "Datos recibidos: ${snapshot.childrenCount} lugares encontrados.")

                    // Recorrer los puntos de interés y agregar marcadores
                    for (locationSnapshot in snapshot.children) {
                        val localizacion = locationSnapshot.getValue(Localizacion::class.java)

                        if (localizacion != null) {
                            val latLng = LatLng(localizacion.latitude, localizacion.longitude)
                            mMap.addMarker(
                                MarkerOptions().position(latLng).title(localizacion.name)
                            )

                            // Guardar la primera ubicación para mover la cámara
                            if (firstLocation == null) {
                                firstLocation = latLng
                            }
                        }
                    }

                    // Si tenemos al menos un marcador, mover la cámara
                    firstLocation?.let {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 12f))
                    }
                } else {
                    Log.d("Firebase", "No hay datos disponibles.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al obtener datos", error.toException())
            }
        })
    }
}
