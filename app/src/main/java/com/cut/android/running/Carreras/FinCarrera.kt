package com.cut.android.running.Carreras

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.cut.android.running.R
import com.cut.android.running.models.Achievement
import com.cut.android.running.models.Classification
import com.cut.android.running.usecases.home.HomeActivity
import com.cut.android.running.provider.BDsqlite
import com.cut.android.running.provider.DatosUsuario
import com.cut.android.running.provider.RetrofitInstance
import com.cut.android.running.provider.resources.AccionFallida
import com.cut.android.running.provider.resources.ManejadorAccionesFallidas
import com.cut.android.running.provider.services.AchievementService
import com.cut.android.running.provider.services.ClassificationService
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


class FinCarrera : AppCompatActivity() {
    val CHANNEL_ID = "logros"
    val CHANNEL_NAME = "logros"
    private val classificationService = RetrofitInstance.getRetrofit().create(ClassificationService::class.java)
    private lateinit var manejadorAcciones: ManejadorAccionesFallidas
    private lateinit var btnReintentarEstatus: Button
    private lateinit var txtEstatusPasos: TextView
    private lateinit var txtAlert: TextView
    private lateinit var txtcaloriasT: TextView
    private lateinit var btnHome: Button
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var contadorEjecuciones = 0
    private var contadorEjecucionesUI = 0
    private val viewModel: FinCarreraViewModel by viewModels()

    private val achievementsService: AchievementService by lazy {
        RetrofitInstance.getRetrofit().create(AchievementService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fin_carrera)
        manejadorAcciones = ManejadorAccionesFallidas(this)
        initializeUI()
        animateViews()
        ProcesarDatos()
        viewModel.accionesFallidas.observe(this) { tieneAcciones ->
            actualizarEstadoUI()
        }
        iniciarTareaRepetitiva()

    }

    private fun initializeUI() {
        //iniciar botones
        btnReintentarEstatus = findViewById(R.id.btnReintentarEstatus)
        btnHome = findViewById(R.id.btnMenu)
        txtAlert = findViewById(R.id.txtAlert)
        txtEstatusPasos = findViewById(R.id.txtEstatusPasos)
        txtcaloriasT = findViewById(R.id.txtcaloriasT)
        btnReintentarEstatus.setOnClickListener {
            txtEstatusPasos.text = "Cargando datos..."
            txtEstatusPasos.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_update, 0, 0, 0)
            txtEstatusPasos.invalidate()
            it.isEnabled = false
            reintentarAccionesFallidas()
        }
        btnHome.setOnClickListener{
            ReturnHome()
        }
    }

    private fun actualizarEstadoUI() {

        //Ya intenté de muchas maneras pero sólo funcionó asi, las primeras llamadas a actualizar la IU son despreciables
        if (contadorEjecucionesUI>1) {

            val tieneAccionesEspecificasFallidas =
                manejadorAcciones.obtenerAccionesFallidas().any { accion ->
                    accion.tipo in listOf(
                        "CrearClasificacion",
                        "CrearClasificacionNueva",
                        "consultarlogro"
                    )
                }
            Log.d("FinCarrera", "se lanzo actualizar $tieneAccionesEspecificasFallidas")
            if (tieneAccionesEspecificasFallidas) {
                // Caso: Error guardando datos
                txtEstatusPasos.text = "Error"
                txtEstatusPasos.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.icon_desconnected,
                    0,
                    0,
                    0
                )
                btnReintentarEstatus.visibility = View.VISIBLE
                txtAlert.visibility = View.VISIBLE
                btnHome.isEnabled = false
            } else {
                // Caso: Datos guardados con éxito
                txtEstatusPasos.text = "Datos guardados con éxito"
                txtEstatusPasos.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.icon_connected,
                    0,
                    0,
                    0
                )
                btnReintentarEstatus.visibility = View.GONE
                txtAlert.visibility = View.GONE
                btnHome.isEnabled = true
            }

            // Forzar la actualización de las vistas
            txtEstatusPasos.invalidate()
            btnReintentarEstatus.invalidate()
            txtAlert.invalidate()
            btnHome.invalidate()
        }
        contadorEjecucionesUI++
    }



    private fun reintentarAccionesFallidas() {
        lifecycleScope.launch { // Este es el contexto de corutina
            val accionesFallidas = manejadorAcciones.obtenerAccionesFallidas()

            for (accion in accionesFallidas) {
                when (accion.tipo) {
                    "CrearClasificacion", "CrearClasificacionNueva" -> {
                        val clasificacionData = Gson().fromJson(accion.payload, Map::class.java)
                        val pasosT = clasificacionData["pasosT"].toString().toDouble().roundToInt()
                        val email = clasificacionData["email"].toString()
                        val nombreUsuario = clasificacionData["nombreUsuario"].toString()

                        // Ahora .await() debería ser reconocido correctamente
                        val exito = clasificacion(pasosT, email, nombreUsuario).await()
                        if (exito) {
                            manejadorAcciones.eliminarAccionFallida(accion)
                            actualizarEstadoUI()
                        }
                    }
                    "consultarlogro" -> {
                        val logroData = Gson().fromJson(accion.payload, Map::class.java)
                        val pasos = logroData["pasos"].toString().toDouble().roundToInt()
                        val email = logroData["email"].toString()
                        // Intenta consultar logro y verifica el éxito
                        val exito = consultarlogro(pasos, email)
                        if (exito) {
                            manejadorAcciones.eliminarAccionFallida(accion)
                            actualizarEstadoUI()
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                btnReintentarEstatus.isEnabled = true
                actualizarEstadoUI()
            }
        }

    }




    private fun ProcesarDatos(){

        val txtPasos = findViewById<TextView>(R.id.txtPasos)
        val txtDistancia = findViewById<TextView>(R.id.txtDistancia)
        val txtPasosT = findViewById<TextView>(R.id.txtpasosT)

        // Obtener nombre de usuario
        val email = DatosUsuario.getEmail(this)

        //Obtener datos de sqlite
        val db = BDsqlite(this)
        val PasosHoy = db.getIntData(email,BDsqlite.COLUMN_PASOS_HOY)
        val PasosTotales = db.getIntData(email,BDsqlite.COLUMN_PASOS_TOTALES)
        val Distancia = db.getFloatData(email,BDsqlite.COLUMN_DISTANCIA_HOY)
        val peso = db.getIntData(email,BDsqlite.COLUMN_PESO).toDouble()
        val nombreUsuario = DatosUsuario.getUserName(this)

        //mostrar los datos de cada consulta
        txtPasos.text = (PasosHoy.toString()+" pasos")
        txtPasosT.text = (PasosTotales.toString()+" pasos")
        txtDistancia.text = (Distancia.toString()+ " metros")
        calcularYMostrarCalorias(peso, PasosTotales)

        //Procesar clasificacion
        if (nombreUsuario != null) {
            clasificacion(PasosTotales, email, nombreUsuario)
        }

        //Procesar logros
        lifecycleScope.launch {
            val exito = consultarlogro(PasosTotales, email)
            if (exito) {
                // La operación fue exitosa
            } else {
                // La operación falló
            }
            withContext(Dispatchers.Main) {
                Log.d("FinCarrera procesar","Se actualiza la UI")
                actualizarEstadoUI()
            }
        }
    }

    private fun calcularYMostrarCalorias(peso: Double, totalPasos: Int) {

        // Verificar si el peso está disponible
        if (peso <= 0) {
            // Mostrar mensaje si el peso no está disponible
            txtcaloriasT.apply {
                text = "Debes agregar tu peso al perfil para ver este dato"
                textSize = 12f // Cambiar el tamaño del texto a 12sp
            }
        } else {
            val caloriasQuemadas = calcularCaloriasQuemadas(peso, totalPasos)
            txtcaloriasT.text = String.format("%.1f calorías", caloriasQuemadas)

        }
    }
    fun calcularCaloriasQuemadas(peso: Double, totalPasos: Int): Double {
        val pasosPorMinuto = 90
        val tiempoEnHoras = ((totalPasos) / pasosPorMinuto) / 60.0
        val MET = 3.5 // Valor promedio para caminata moderada
        val calorias = MET * peso * tiempoEnHoras
        return calorias
    }

    private fun clasificacion(pasosT: Int, email: String, nombreUsuario: String): Deferred<Boolean> {
        return lifecycleScope.async(Dispatchers.IO) {
            try {
                val response = classificationService.getClassificationById(email)
                when {
                    response.isSuccessful && response.body() != null && response.body()!!.isSuccess -> {
                        val clasificacionActual = response.body()!!.data
                        if (clasificacionActual != null) {
                            val clasificacionActualizada = clasificacionActual.copy(pasos = pasosT + 1)
                            val updateResponse = classificationService.updateClassification(clasificacionActualizada)
                            updateResponse.isSuccessful
                        } else false
                    }
                    response.code() == 404 -> {
                        val nuevaClasificacion = Classification(id = email, nombre = nombreUsuario, pasos = pasosT)
                        val createResponse = classificationService.addClassification(nuevaClasificacion)
                        if (!createResponse.isSuccessful) {
                            guardarAccionFallida("CrearClasificacionNueva", nuevaClasificacion)
                        }
                        createResponse.isSuccessful
                    }
                    else -> {
                        guardarAccionFallida("CrearClasificacion", mapOf("pasosT" to pasosT, "email" to email, "nombreUsuario" to nombreUsuario))
                        false
                    }
                }
            } catch (e: Exception) {
                guardarAccionFallida("CrearClasificacion", mapOf("pasosT" to pasosT, "email" to email, "nombreUsuario" to nombreUsuario))
                false
            }
        }

    }


    private fun guardarAccionFallida(tipo: String, datos: Any) {
        val jsonDatos = Gson().toJson(datos)
        manejadorAcciones.guardarAccionFallida(AccionFallida(tipo, jsonDatos))
    }



    // Asume que tus métodos de API ya son suspendidos
    private suspend fun getAchievementsByEmail(email: String): List<Achievement>? {
        val response = achievementsService.getAchievementsByUser(email)
        return if (response.isSuccessful) response.body()?.data else null
    }

    private suspend fun addAchievementIfNotExists(pasos: Int, email: String, achievements: List<Achievement>, userAchievements: List<Achievement>?) {
        achievements.filter { pasos > it.steps }.forEach { achievement ->
            if (userAchievements?.none { it.id == achievement.id } != false) {
                val response = achievementsService.addUserRelation(email, achievement.id!!)
                if (response.isSuccessful && response.body()?.data == true) {
                    notificacion(achievement.steps, achievement.name, achievement.id)
                } else {
                    Log.d("FC consultarlogro", "No se pudo añadir la relación usuario-logro para el logro ${achievement.name}")
                }
            } else {
                Log.d("FC consultarlogro", "El usuario ya tiene el logro ${achievement.name}")
            }
        }
    }

    private suspend fun consultarlogro(pasos: Int, email: String): Boolean {
        return try {
            val achievementsResponse = achievementsService.getAchievements()
            if (achievementsResponse.isSuccessful && achievementsResponse.body()?.data != null) {
                val achievements = achievementsResponse.body()!!.data ?: emptyList()
                val userAchievements = getAchievementsByEmail(email)
                addAchievementIfNotExists(pasos, email, achievements, userAchievements)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("FC consultarlogro", "Error al consultar logros", e)
            //Aqui se guarda la petición pendiente
            val datos = mapOf(
                "pasos" to pasos,
                "email" to email
            )
            manejadorAcciones.guardarAccionFallida(AccionFallida("consultarlogro", Gson().toJson(datos)))
            false // Excepción lanzada, operación no exitosa
        }
    }



    private fun notificacion(pasosLogro: Int, tituloLogro: String, id: Int) {
        Log.d("FC consultarlogro", "Datos a registrar: $tituloLogro, pasos: $pasosLogro")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)

            val manager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            // Crear un intent que se abrirá al hacer clic en la notificación
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("fragmentoDestino", "LogroConseguidoFragment")
                putExtra("nombreLogro", tituloLogro)
                putExtra("pasosLogro", pasosLogro)
            }

            // Crear el PendingIntent
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val notificacion = NotificationCompat.Builder(this, CHANNEL_ID).apply {
                setSmallIcon(R.drawable.logro)
                setContentTitle(tituloLogro)
                setContentText("!Felicidades! obtuviste un logro por haber realizado un total de $pasosLogro pasos")
                priority = NotificationCompat.PRIORITY_DEFAULT
                setContentIntent(pendingIntent) // Establecer el PendingIntent
                setAutoCancel(true) // La notificación se cancela automáticamente al hacer clic en ella
            }.build()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    "android.permission.POST_NOTIFICATIONS"
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            NotificationManagerCompat.from(this).notify(id, notificacion)
        }
    }




    fun ReturnHome() {

        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    private fun animateViews() {
        val viewsToAnimate = listOf(
            findViewById<TextView>(R.id.textView2),
            findViewById<TextView>(R.id.textView),
            findViewById<TextView>(R.id.txtPasos),
            findViewById<TextView>(R.id.textView3),
            findViewById<TextView>(R.id.txtDistancia),
            findViewById<TextView>(R.id.textview10),
            findViewById<TextView>(R.id.txtpasosT),
            findViewById<Button>(R.id.btnMenu),
            findViewById(R.id.textview11),
            txtcaloriasT
        )

        for ((index, view) in viewsToAnimate.withIndex()) {
            // Desvanecimiento
            view.animate().alpha(1.0f).setDuration(600)
                .setStartDelay((index + 1) * 200L)
                .start()

            // Deslizamiento desde abajo
            view.translationY = 200f
            view.animate().translationY(0f).setDuration(600)
                .setStartDelay((index + 1) * 200L)
                .start()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.verificarAccionesFallidas(manejadorAcciones)
    }


    override fun onPause() {
        super.onPause()
        detenerTareaRepetitiva()
    }

    private fun iniciarTareaRepetitiva() {
        contadorEjecuciones = 0 // Reiniciar contador
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                if (contadorEjecuciones < 1) { // Ejecutar solo 3 veces
                    actualizarEstadoUI() // Tu función que quieres ejecutar
                    contadorEjecuciones++ // Incrementar el contador
                    handler.postDelayed(this, 4000) // Re-ejecutar cada 4 segundos
                }
            }
        }
        handler.post(runnable)
    }

    private fun detenerTareaRepetitiva() {
        handler.removeCallbacks(runnable)
    }


}