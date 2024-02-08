import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cut.android.running.Carreras.FinCarrera
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.cut.android.running.R
import com.cut.android.running.provider.BDsqlite
import com.cut.android.running.provider.DatosUsuario
import com.cut.android.running.provider.services.TrackingService

class MapsFragment : Fragment(), OnMapReadyCallback, SensorEventListener {

    private lateinit var map: GoogleMap
    private var isTracking = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var viewModel: MapsViewModel
    private lateinit var sensorManager: SensorManager
    private lateinit var btnCentrar: ImageButton
    private lateinit var btnCentrarCut: ImageButton
    private var stepSensor: Sensor? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
        private const val ACTIVITY_RECOGNITION_REQUEST_CODE = 1000
    }

    // Inflates the layout for this fragment
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    // Initializes components and sets up observers and event listeners
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MapsViewModel::class.java)
        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        view.findViewById<Button>(R.id.btnIniciar).setOnClickListener { toggleTracking() }

        viewModel.totalDistance.observe(viewLifecycleOwner) { distance ->
            view.findViewById<TextView>(R.id.tvDistancia).text = getString(R.string.distance_template, distance)
        }

        viewModel.totalSteps.observe(viewLifecycleOwner) { steps ->
            view.findViewById<TextView>(R.id.tvPasos).text = getString(R.string.steps_template, steps)
        }
        //botón para centrar el mapa
        btnCentrar = view.findViewById(R.id.btnCentrar)
        btnCentrar.setOnClickListener {
            centrarMapaEnUbicacion()
        }
        btnCentrarCut = view.findViewById(R.id.btnCentrarCut)
        btnCentrarCut.setOnClickListener {
            centrarMapaEnCut()
        }
    }

    // Sets up the map once it's ready
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val initialLocation = LatLng(20.5665, -103.2273) // Guadalajara, Jalisco, México
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f))

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        }
    }

    // Toggles tracking, updates UI and starts/stops location and step counting
    private fun toggleTracking() {
        val serviceIntent = Intent(context, TrackingService::class.java)
        if (!checkPermissions()) {
            requestPermissions()
            return
        }
        if (!checkActivityRecognitionPermission()) {
            requestActivityRecognitionPermission()
            return
        }

        isTracking = !isTracking // Cambia el estado de seguimiento
        val btnIniciar = view?.findViewById<Button>(R.id.btnIniciar)

        if (isTracking) {
            // Si el seguimiento se ha iniciado
            btnIniciar?.text = getString(R.string.detener) // Actualiza el texto del botón a "Detener"
            btnIniciar?.setBackgroundColor(resources.getColor(R.color.button_started))


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context?.startForegroundService(serviceIntent)
            } else {
                context?.startService(serviceIntent)
            }

            startLocationUpdates()
            startStepCounting()
            startAnimation()
            btnCentrar.visibility = View.VISIBLE
            btnCentrarCut.visibility = View.VISIBLE
                
        } else {
            // Si el seguimiento se ha detenido
            Log.d("MapsFragment","Se ha detenido")
            GuardarEnSQLite()
            btnIniciar?.text = getString(R.string.iniciar) // Actualiza el texto del botón a "Iniciar"
            btnIniciar?.setBackgroundColor(resources.getColor(R.color.button_stopped))

            context?.stopService(serviceIntent)
            stopLocationUpdates()
            drawPolyline()
            stopStepCounting()

            //lanzar FinCarrera
            val intent = Intent(context, FinCarrera::class.java)
            startActivity(intent)

        }

        btnIniciar?.animate()?.scaleX(1.1f)?.scaleY(1.1f)?.setDuration(150)?.withEndAction {
            btnIniciar?.animate()?.scaleX(1f)?.scaleY(1f)?.duration = 150
        }
    }

    private fun startAnimation() {
        val layoutMaps = view?.findViewById<LinearLayout>(R.id.LayoutMaps)
        val params = layoutMaps?.layoutParams as? LinearLayout.LayoutParams
        val startWeight = params?.weight ?: 90f // Asume 90f como peso inicial si es nulo
        val endWeight = 80f // El peso objetivo

        // Crea un ValueAnimator que va desde el peso inicial al final
        val valueAnimator = ValueAnimator.ofFloat(startWeight, endWeight)
        valueAnimator.duration = 500 // Duración de la animación en milisegundos
        valueAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            params?.weight = animatedValue
            layoutMaps?.layoutParams = params
        }
        valueAnimator.start()
    }


    private fun GuardarEnSQLite() {
        val PasosRecorridoHoy = viewModel.totalSteps.value

        val totalDistanceTravelled = viewModel.totalDistance.value


        // Obtener el nombre del usuario de SharedPreferences

        val email = DatosUsuario.getEmail(requireActivity())

        val db = BDsqlite(requireContext())
        val PasosTotales = db.getIntData(email,BDsqlite.COLUMN_PASOS_TOTALES) + PasosRecorridoHoy!!
        db.upsertData(email, PasosRecorridoHoy!!, PasosTotales, totalDistanceTravelled!!)


        // Obtener datos para el email registrado
        val cursorPasosHoy = db.getData(BDsqlite.getColumnPasosHoy(), email)
        val cursorPasosTotales = db.getData(BDsqlite.getColumnPasosTotales(), email)
        val cursorDistancia = db.getData(BDsqlite.getColumnDistancia(), email)


        //cerrar los cursores después de usarlos

        cursorPasosHoy.close()
        cursorPasosTotales.close()
        cursorDistancia.close()

    }



    // Starts counting steps by registering the sensor event listener
    private fun startStepCounting() {
        stepSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    // Stops counting steps by unregistering the sensor event listener
    private fun stopStepCounting() {
        sensorManager.unregisterListener(this)
    }

    // Checks if location permissions are granted
    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Requests location permissions
    private fun requestPermissions() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    // Handles the result from permission request callbacks
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isTracking) {
                        startLocationUpdates()
                    }
                } else {
                    Toast.makeText(requireContext(), "Permiso de ubicación necesario para esta funcionalidad", Toast.LENGTH_LONG).show()
                }
            }
            ACTIVITY_RECOGNITION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isTracking) {
                        startStepCounting()
                    }
                } else {
                    Toast.makeText(requireContext(), "Permiso de reconocimiento de actividad necesario para contar pasos", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Starts location updates
    private fun startLocationUpdates() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    if (location.accuracy < 50) { // Solo considera ubicaciones con precisión menor a 50 metros
                        val newPoint = LatLng(location.latitude, location.longitude)
                        viewModel.addPathPoint(newPoint)
                        drawPolyline()
                    }else{
                        Log.d("MapsFragment","Ubicacion con poca precision: ${location.accuracy}")
                    }

                }
            }
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permiso de ubicación necesario para esta funcionalidad", Toast.LENGTH_LONG).show()

            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun drawPolyline() {
        val polylineOptions = PolylineOptions().apply {
            width(5f)
            color(Color.BLUE)
            addAll(viewModel.pathPoints.value!!)
        }
        map.addPolyline(polylineOptions)
    }

    // Checks if activity recognition permission is granted
    private fun checkActivityRecognitionPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    }

    // Requests activity recognition permission
    private fun requestActivityRecognitionPermission() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), ACTIVITY_RECOGNITION_REQUEST_CODE)
    }

    // Called when there is a new sensor event
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            viewModel.addStep()
        }
    }

    // Called when the accuracy of a sensor has changed
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // This can be left empty unless your application needs to respond to changes in sensor accuracy.
    }

    private fun centrarMapaEnUbicacion() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestActivityRecognitionPermission()
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            // Si se obtiene la ubicación correctamente, mueve la cámara
            location?.let {
                val userLocation = LatLng(it.latitude, it.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            }
        }
    }
    private fun centrarMapaEnCut() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestActivityRecognitionPermission()
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            // Si se obtiene la ubicación correctamente, mueve la cámara
            location?.let {
                val cutubicacion = LatLng(20.5665, -103.2273)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(cutubicacion, 15f))
            }
        }
    }




}

