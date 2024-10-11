package com.example.firebaseloginapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : FragmentActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var editTextPurchaseAmount: EditText
    private lateinit var editTextDistance: EditText
    private lateinit var buttonCalculate: Button
    private lateinit var textViewResult: TextView

    private lateinit var currentLocation: LatLng

    // Coordenadas de la ubicación de entrega
    private val deliveryBaseLocation = LatLng(-33.60728511295522, -70.85541843026547) // Coordenadas de partida

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar los elementos de la UI
        editTextPurchaseAmount = findViewById(R.id.editTextPurchaseAmount)
        editTextDistance = findViewById(R.id.editTextDistance)
        buttonCalculate = findViewById(R.id.buttonCalculate)
        textViewResult = findViewById(R.id.textViewResult)

        // Configurar el botón para calcular el costo
        buttonCalculate.setOnClickListener {
            calculateCost()
        }

        // Inicializar currentLocation con la ubicación de entrega
        currentLocation = deliveryBaseLocation

        // Configurar el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Verificar permisos
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        // Obtener la ubicación actual
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLocation = LatLng(location.latitude, location.longitude)
                map.addMarker(MarkerOptions().position(currentLocation).title("Ubicación Actual"))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            } else {
                // Si no se puede obtener la ubicación actual, usar la ubicación de entrega
                map.addMarker(MarkerOptions().position(deliveryBaseLocation).title("Ubicación de Entrega"))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(deliveryBaseLocation, 15f))
                textViewResult.text = "No se pudo obtener la ubicación actual. Usando ubicación de entrega."
            }
        }

        // Hacer clic en el mapa para seleccionar una ubicación
        map.setOnMapClickListener { latLng ->
            map.clear() // Limpiar el mapa
            map.addMarker(MarkerOptions().position(latLng).title("Ubicación Seleccionada"))
            calculateDistance(latLng)
        }
    }

    private fun calculateCost() {
        // Obtener los valores de entrada
        val purchaseAmount = editTextPurchaseAmount.text.toString().toDoubleOrNull()

        // Validar las entradas
        if (purchaseAmount == null) {
            textViewResult.text = "Por favor, ingrese un monto válido."
            return
        }

        // Cálculo del costo de despacho
        val distance = editTextDistance.text.toString().toDoubleOrNull() ?: return
        val deliveryCost = when {
            purchaseAmount > 50000 -> 0.0 // Despacho gratis
            purchaseAmount in 25000.0..49999.0 -> distance * 150 // $150 por km
            else -> distance * 300 // $300 por km
        }

        // Mostrar el resultado
        textViewResult.text = "Costo de despacho: $${deliveryCost}"
    }

    private fun calculateDistance(selectedLocation: LatLng) {
        // Asegúrate de que currentLocation esté inicializada
        if (this::currentLocation.isInitialized) {
            val results = FloatArray(1)
            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, selectedLocation.latitude, selectedLocation.longitude, results)
            val distanceInKm = results[0] / 1000 // Convertir a kilómetros
            editTextDistance.setText(String.format("%.2f", distanceInKm)) // Mostrar distancia calculada
        }
    }
}
