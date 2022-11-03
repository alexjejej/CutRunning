/*
 * Copyright (c) 2021 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.rwandroidtutorial.MapaySteps

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.raywenderlich.android.runtracking.databinding.ActivityMainBinding
import java.util.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACTIVITY_RECOGNITION

import android.app.NotificationChannel
import android.app.NotificationManager
import android.view.View
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.raywenderlich.android.runtracking.R
import java.text.SimpleDateFormat


/**
 * Main Screen
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
  private lateinit var binding: ActivityMainBinding
  val chanelID = "logros"
  val chanelName = "logros"

  // ViewModel
  private val mapsActivityViewModel: MapsActivityViewModel by viewModels {
    MapsActivityViewModelFactory(getTrackingRepository())
  }

  // Location & Map
  private lateinit var mMap: GoogleMap
  private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
  private var polylineOptions = PolylineOptions()
  private val locationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult?) {
      super.onLocationResult(locationResult)
      locationResult ?: return
      locationResult.locations.forEach {
        val trackingEntity =
          TrackingEntity(Calendar.getInstance().timeInMillis, it.latitude, it.longitude)
        mapsActivityViewModel.insert(trackingEntity)
      }
    }
  }

  companion object {
    // SharedPreferences
    private const val KEY_SHARED_PREFERENCE = "com.rwRunTrackingApp.sharedPreferences"
    private const val KEY_IS_TRACKING = "com.rwRunTrackingApp.isTracking"

    // Permission
    private const val REQUEST_CODE_FINE_LOCATION = 1
    private const val REQUEST_CODE_ACTIVITY_RECOGNITION = 2
  }

  private var isTracking: Boolean
    get() = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE)
      .getBoolean(KEY_IS_TRACKING, false)
    set(value) = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit()
      .putBoolean(KEY_IS_TRACKING, value).apply()

  override fun onCreate(savedInstanceState: Bundle?) {
    // Switch to AppTheme for displaying the activity
    setTheme(R.style.AppTheme)
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    valoresIniciales();

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)

    // Location
    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

    // Set up button click events
    binding.startButton.setOnClickListener {
      // Clear the PolylineOptions from Google Map
      mMap.clear()

      // Update Start & End Button
      isTracking = true
      updateButtonStatus()

      // Reset the display text
      updateAllDisplayText(0, 0f)

      startTracking()
    }
    binding.endButton.setOnClickListener { endButtonClicked() }

    // Update layouts
    updateButtonStatus()

    // 1
    mapsActivityViewModel.allTrackingEntities.observe(this) { allTrackingEntities ->
      if (allTrackingEntities.isEmpty()) {
        updateAllDisplayText(0, 0f)
      }
    }

    // 2
    mapsActivityViewModel.lastTrackingEntity.observe(this) { lastTrackingEntity ->
      lastTrackingEntity ?: return@observe
      addLocationToRoute(lastTrackingEntity)
    }

    // 3
    mapsActivityViewModel.totalDistanceTravelled.observe(this) {
      it ?: return@observe
      val stepCount = mapsActivityViewModel.currentNumberOfStepCount.value ?: 0
      updateAllDisplayText(stepCount, it)
    }

    // 4
    mapsActivityViewModel.currentNumberOfStepCount.observe(this) {
      val totalDistanceTravelled = mapsActivityViewModel.totalDistanceTravelled.value ?: 0f
      updateAllDisplayText(it, totalDistanceTravelled)
    }

    // 5
    mapsActivityViewModel.allTrackingEntitiesRecord.observe(this) {
      addLocationListToRoute(it)
    }

    if (isTracking) {
      startTracking()
    }

  }


  // Repository
  private fun getTrackingApplicationInstance() = application as TrackingApplication
  private fun getTrackingRepository() = getTrackingApplicationInstance().trackingRepository

  // UI related codes
  private fun updateButtonStatus() {
    binding.startButton.isEnabled = !isTracking
    binding.endButton.isEnabled = isTracking
  }

  private fun updateAllDisplayText(stepCount: Int, totalDistanceTravelled: Float) {
    binding.numberOfStepTextView.text = String.format("Pasos: %d", stepCount)
    binding.totalDistanceTextView.text =
      String.format("Distancia total: %.2fm", totalDistanceTravelled)

    val averagePace = if (stepCount != 0) totalDistanceTravelled / stepCount.toDouble() else 0.0
    binding.averagePaceTextView.text = String.format("Ritmo promedio: %.2fm/ pasos", averagePace);

    //Variables cache
    val sharedPreference =  getSharedPreferences("Datos",Context.MODE_PRIVATE)
    var editor = sharedPreference.edit()

    if (stepCount!=0){
      editor.putInt("pasos", stepCount)
      editor.putFloat("distancia", totalDistanceTravelled)
      editor.commit()
    }

  }

  private fun endButtonClicked() {

    AlertDialog.Builder(this)
      .setTitle("¿Detener contador?")
      .setPositiveButton("Confirmar") { _, _ ->
        isTracking = false
        updateButtonStatus()
        stopTracking()
        PasosHoy()

      }.setNegativeButton("Cancelar") { _, _ ->
      }
      .create()
      .show()

  }

  private fun valoresIniciales(){
    //variables locales
    val sharedPreference =  getSharedPreferences("Datos",Context.MODE_PRIVATE)

    //fecha hoy
    val sdf = SimpleDateFormat("dd/M/yyyy")
    val currentDate = sdf.format(Date())

    //sumar datos hoy
    var pasos = sharedPreference.getInt("pasos",0) + sharedPreference.getInt("Pasos "+currentDate,0)
    var distancia = sharedPreference.getFloat("distancia",0F) + sharedPreference.getFloat("Distancia "+currentDate,0F)
    var pasosT = sharedPreference.getInt("PasosTotales",0)

    val txtPasos = findViewById<TextView>(R.id.PasosHoy)
    val txtDist = findViewById<TextView>(R.id.DistanciaHoy)
    val txtPasosT = findViewById<TextView>(R.id.PasosTotales)
    txtPasos.setVisibility(View.VISIBLE);
    txtDist.setVisibility(View.VISIBLE);
    txtPasosT.setVisibility(View.VISIBLE);
    binding.PasosHoy.text = String.format("Pasos hoy: %d", pasos)
    binding.DistanciaHoy.text = String.format("Distancia hoy: %.2fm", distancia)
    binding.PasosTotales.text = String.format("Pasos totales: %d", pasosT)
  }

  private fun PasosHoy(){

    //variables locales
    val sharedPreference =  getSharedPreferences("Datos",Context.MODE_PRIVATE)
    var editor = sharedPreference.edit()

    //fecha hoy
    val sdf = SimpleDateFormat("dd/M/yyyy")
    val currentDate = sdf.format(Date())

    //sumar datos hoy
    var pasos = sharedPreference.getInt("pasos",0) + sharedPreference.getInt("Pasos "+currentDate,0)
    var distancia = sharedPreference.getFloat("distancia",0.0F) + sharedPreference.getFloat("Distancia "+currentDate,0.0F)
    var pasosT = sharedPreference.getInt("pasos",0) + sharedPreference.getInt("PasosTotales",0)

    //guardar datos hoy
    editor.putInt("Pasos "+currentDate,pasos)
    editor.putFloat("Distancia "+currentDate,distancia)
    editor.putInt("PasosTotales",pasosT);
    editor.putInt("pasos", 0)
    editor.putFloat("distancia", 0.0F)
    editor.commit()

    val txtPasos = findViewById<TextView>(R.id.PasosHoy)
    val txtDist = findViewById<TextView>(R.id.DistanciaHoy)
    val txtPasosT = findViewById<TextView>(R.id.PasosTotales)
    txtPasos.setVisibility(View.VISIBLE);
    txtDist.setVisibility(View.VISIBLE);
    txtPasosT.setVisibility(View.VISIBLE);
    binding.PasosHoy.text = String.format("Pasos hoy: %d", pasos)
    binding.DistanciaHoy.text = String.format("Distancia hoy: %.2fm", distancia)
    binding.PasosTotales.text = String.format("Pasos totales: %d", pasosT)
    logros(pasosT);
    //consultar logros
    //var logros = logros();
    //logros.pasos(pasosT);


  }

  private fun logros(pasostotales: Int) {
    /*if (pasostotales >= 105) {
      Log.d("logro: ", "Felicidades, obtuviste la medalla de 100 pasos");

      notificacion (105)
    }
    if (pasostotales >= 120) {
      Log.d("logro: ", "Felicidades, obtuviste la medalla de 100 pasos");

      notificacion (120)
    }
    if (pasostotales >= 130) {
      Log.d("logro: ", "Felicidades, obtuviste la medalla de 100 pasos");

      notificacion (130)
    }*/ notificacion(pasostotales)
  }

  private fun notificacion(pasos: Int){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

      // construir canal
      val importance = NotificationManager.IMPORTANCE_DEFAULT // (5)
      val channel = NotificationChannel(chanelID, chanelName, importance)

      //manager de notificaciones
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)

      //configurando notificacion

      val notificacion = NotificationCompat.Builder(this, chanelID).also { noti ->
        noti.setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
        noti.setContentTitle("Logro")
        noti.setContentText("Has obtenido la medalla de: "+pasos+" pasos")
        noti.setPriority(NotificationCompat.PRIORITY_DEFAULT)
      }.build()

      val notificationManageer = NotificationManagerCompat.from(applicationContext)
      notificationManageer.notify(1,notificacion);
    }
  }

  // Tracking
  @AfterPermissionGranted(REQUEST_CODE_ACTIVITY_RECOGNITION)
  private fun startTracking() {
    val txtPasos = findViewById<TextView>(R.id.PasosHoy)
    val txtDist = findViewById<TextView>(R.id.DistanciaHoy)
    val txtPasosT = findViewById<TextView>(R.id.PasosTotales)
    txtPasos.setVisibility(View.INVISIBLE);
    txtDist.setVisibility(View.INVISIBLE);
    txtPasosT.setVisibility(View.INVISIBLE);

    val isActivityRecognitionPermissionFree = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    val isActivityRecognitionPermissionGranted = EasyPermissions.hasPermissions(
      this,
      ACTIVITY_RECOGNITION
    )
    Log.d(
      "TAG",
      "Is ACTIVITY_RECOGNITION permission granted $isActivityRecognitionPermissionGranted"
    )
    if (isActivityRecognitionPermissionFree || isActivityRecognitionPermissionGranted) {
      setupStepCounterListener()
      setupLocationChangeListener()
    } else {
      // Do not have permissions, request them now
      EasyPermissions.requestPermissions(
        host = this,
        rationale = "For showing your step counts and calculate the average pace.",
        requestCode = REQUEST_CODE_ACTIVITY_RECOGNITION,
        perms = *arrayOf(ACTIVITY_RECOGNITION)
      )
    }
  }

  private fun stopTracking() {
    polylineOptions = PolylineOptions()

    mapsActivityViewModel.deleteAllTrackingEntity()
    fusedLocationProviderClient.removeLocationUpdates(locationCallback)

    // Stop step sensor listener
    val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    sensorManager.unregisterListener(this, stepCounterSensor)
  }

  // Map related codes
  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  @SuppressLint("MissingPermission")
  override fun onMapReady(googleMap: GoogleMap) {
    mMap = googleMap

    showUserLocation()

    // Add a marker in Hong Kong and move the camera
    val latitude = 20.566396
    val longitude = -103.228250
    val CUTLatLong = LatLng(latitude, longitude)

    val zoomLevel = 9.5f
    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(CUTLatLong, zoomLevel))

    // Draw all the previous points on the map
    if (isTracking) {
      mapsActivityViewModel.getAllTrackingEntities()
    }
  }

  private fun addLocationListToRoute(trackingEntityList: List<TrackingEntity>) {
    if (!this::mMap.isInitialized) {
      return
    }
    mMap.clear()
    trackingEntityList.forEach { trackingEntity ->
      val newLatLngInstance = trackingEntity.asLatLng()
      polylineOptions.points.add(newLatLngInstance)
    }
    mMap.addPolyline(polylineOptions)
  }

  private fun addLocationToRoute(trackingEntity: TrackingEntity) {
    mMap.clear()
    val newLatLngInstance = trackingEntity.asLatLng()
    polylineOptions.points.add(newLatLngInstance)
    mMap.addPolyline(polylineOptions)
  }

  // Step Counter related codes
  private fun setupStepCounterListener() {
    val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    stepCounterSensor ?: return
    sensorManager.registerListener(
      this@MapsActivity,
      stepCounterSensor,
      SensorManager.SENSOR_DELAY_FASTEST
    )

  }


  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    Log.d("TAG", "onAccuracyChanged: Sensor: $sensor; accuracy: $accuracy")
  }

  override fun onSensorChanged(sensorEvent: SensorEvent?) {
    Log.d("TAG", "onSensorChanged")
    sensorEvent ?: return
    val firstSensorEvent = sensorEvent.values.firstOrNull() ?: return
    Log.d("TAG", "Steps count: $firstSensorEvent ")
    val isFirstStepCountRecord = mapsActivityViewModel.currentNumberOfStepCount.value == 0
    if (isFirstStepCountRecord) {
      mapsActivityViewModel.initialStepCount = firstSensorEvent.toInt()
      mapsActivityViewModel.currentNumberOfStepCount.value = 1
    } else {
      mapsActivityViewModel.currentNumberOfStepCount.value = firstSensorEvent.toInt() - mapsActivityViewModel.initialStepCount
    }
  }

  // Location
  @SuppressLint("MissingPermission")
  @AfterPermissionGranted(REQUEST_CODE_FINE_LOCATION)
  private fun showUserLocation() {
    if (EasyPermissions.hasPermissions(this, ACCESS_FINE_LOCATION)) {
      mMap.isMyLocationEnabled = true
    } else {
      // Do not have permissions, request them now
      EasyPermissions.requestPermissions(
        host = this,
        rationale = "For showing your current location on the map.",
        requestCode = REQUEST_CODE_FINE_LOCATION,
        perms = *arrayOf(ACCESS_FINE_LOCATION)
      )
    }
  }

  @SuppressLint("MissingPermission")
  @AfterPermissionGranted(REQUEST_CODE_FINE_LOCATION)
  private fun setupLocationChangeListener() {
    if (EasyPermissions.hasPermissions(this, ACCESS_FINE_LOCATION)) {
      val locationRequest = LocationRequest()
      locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
      locationRequest.interval = 5000 // 5000ms (5s)
      fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    } else {
      // Do not have permissions, request them now
      EasyPermissions.requestPermissions(
        host = this,
        rationale = "For showing your current location on the map.",
        requestCode = REQUEST_CODE_FINE_LOCATION,
        perms = *arrayOf(ACCESS_FINE_LOCATION)
      )
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                          grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    // EasyPermissions handles the request result.
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
  }
}
