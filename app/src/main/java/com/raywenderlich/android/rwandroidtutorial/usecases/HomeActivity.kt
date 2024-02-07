package com.raywenderlich.android.rwandroidtutorial.usecases

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.raywenderlich.android.rwandroidtutorial.usecases.login.LoginFragment
import com.raywenderlich.android.runtracking.R
import com.raywenderlich.android.runtracking.databinding.ActivityHomeBinding
import com.raywenderlich.android.rwandroidtutorial.Carrera.ListaDatosUsuario
import com.raywenderlich.android.rwandroidtutorial.models.Session
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if ( savedInstanceState == null ) {
            /**La transaccion del fragmento solo se crea cuando "savedInstanceState" es null. Esto  para
             * garantizar que el fragmento se agregue  una sola vez, cuando se crea  la actividad por
             * primera vez**/
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.main_container_fragment, LoginFragment())
                addToBackStack("LoginFragment")
            }
            Log.d("HomeActivity", "Home Activity")
            Log.d("HomeActivity","${supportFragmentManager.backStackEntryCount}")
        }

        Session.readPrefs( this )
        this.setup()
//        this.sincronizar()
    }

    /** Es el evento que se dispara cuadno retrocedemos en la aplicacion (dar clic en el boton atras) **/
    override fun onBackPressed() {
        Log.d("HomeActivity", "On Back Pressed")
        Log.d("HomeActivity", "${supportFragmentManager.backStackEntryCount}")
//        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
//            binding.drawerLayout.closeDrawer(GravityCompat.START)
//        else
//            super.onBackPressed()
    }

    /** Maneja el BackStack de fragments para que no se sobrepongan los fragments **/
    private fun manageBackStack() {
        supportFragmentManager.popBackStack()
        Log.d("BackStack", "${supportFragmentManager.backStackEntryCount}")
    }

    /** Inicializa la configuracion de los componentes **/
    private fun setup() {}

//    private fun sincronizar() {
//        //variables locales
//        val sharedPreference =  getSharedPreferences("Datos", Context.MODE_PRIVATE)
//        var condicion = sharedPreference.getString("sincronizado","no")
//        var editor = sharedPreference.edit()
//
//        //obtener usuario
//        val usuario = "alex"
//
//        //Condición de si es la primera vez que se inicia en la app
//        if (condicion=="no"){
//            //Obtener datoa del usuario y sumarlos con los nuevos
//            val database = Firebase.database
//            database.getReference("users").child(usuario).child("datos").
//            get().addOnSuccessListener {
//
//                if (it.exists()){
//
//                    val bdPasosT = it.child("pasosT").getValue(Int::class.java)
//                    val bdDistancia = it.child("distanciaT").getValue(Float::class.java)
//
//                    editor.putInt("PasosTotales",bdPasosT!!)
//                    editor.putFloat("distancia",bdDistancia!!)
//                    Log.d("Datos encontrados: ","pasos T "+bdPasosT+" Distancia: "+bdDistancia)
//                    editor.putString("condicion","si")
//                    editor.commit()
//
//                }else{
//                    Log.d("Datos no encontrados: ","No existe")
//                    CrearDatos()
//                }
//
//
//            }.addOnFailureListener{
//                Log.e("firebase", "Error getting data", it)
//            }
//        }
//
//    }
//    private fun CrearDatos() {
//        //fecha hoy
//        val sdf = SimpleDateFormat("dd/M/yyyy")
//        val currentDate = sdf.format(Date())
//        //variables locales
//        val sharedPreference =  getSharedPreferences("Datos", Context.MODE_PRIVATE)
//        var pasos = sharedPreference.getInt("pasos",0)
//        var distancia = sharedPreference.getFloat("distancia",0F)
//        //obtener usuario
//        val usuario = "alex"
//        val database = Firebase.database
//        val myRef = database.getReference("users").child(usuario).child("datos")
//        val DatosUsuario = ListaDatosUsuario(pasos,distancia)
//        myRef.setValue(DatosUsuario)
//    }
}