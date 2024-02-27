package com.cut.android.running.usecases.home

import MapsFragment
import RacesManagement
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cut.android.running.R
import com.cut.android.running.usecases.clasificacion.ClasificacionFragment
import com.cut.android.running.provider.DatosUsuario
import com.cut.android.running.provider.resources.ManejadorAccionesFallidas
import com.cut.android.running.usecases.estadisticas.EstadisticasFragment
import com.cut.android.running.usecases.logros.LogrosFragment
import com.cut.android.running.usecases.logros.admin.AdminLogrosFragment
import com.cut.android.running.usecases.profile.ProfileFragment
import kotlinx.coroutines.launch


class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var viewModel: HomeViewModel
    private lateinit var btnCarrera: Button
    private lateinit var btnLogros: Button
    private lateinit var btnClasificacion: Button
    private lateinit var btnAdminLogros: Button
    private lateinit var btnAdminCarreras: Button
    private lateinit var btnReintentarEstatus : Button
    private lateinit var btnPerfil: Button
    private lateinit var btnEstadisticas : Button

    private lateinit var manejadorAcciones: ManejadorAccionesFallidas

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI(view)
        setupViewModel()
    }

    private fun initializeUI(view: View) {
        btnCarrera = view.findViewById(R.id.btnCarrera)
        btnLogros = view.findViewById(R.id.btnLogros)
        btnClasificacion = view.findViewById(R.id.btnClasificacion)
        btnAdminLogros = view.findViewById(R.id.btnAdminLogros)
        btnAdminCarreras = view.findViewById(R.id.btnAdminCarreras)
        btnPerfil = view.findViewById(R.id.btnProfile)
        btnReintentarEstatus = view.findViewById(R.id.btnReintentarEstatus)
        btnEstadisticas = view.findViewById(R.id.btnEstadisticas)

        btnCarrera.setOnClickListener { navigateToFragment(MapsFragment()) }
        btnLogros.setOnClickListener { navigateToFragment(LogrosFragment()) }
        btnClasificacion.setOnClickListener { navigateToFragment(ClasificacionFragment()) }
        btnAdminLogros.setOnClickListener { navigateToFragment(AdminLogrosFragment()) }
        btnAdminCarreras.setOnClickListener{ navigateToFragment(RacesManagement()) }
        btnPerfil.setOnClickListener{ navigateToFragment(ProfileFragment()) }
        btnEstadisticas.setOnClickListener{ navigateToFragment(EstadisticasFragment())}
        // Instanciar ManejadorAccionesFallidas
        manejadorAcciones = ManejadorAccionesFallidas(requireContext())

        btnReintentarEstatus.setOnClickListener {
            viewModel.apiConnection.value?.let { isConnected ->
                if (isConnected) {

                    lifecycleScope.launch {
                        manejadorAcciones.reintentarAccionesFallidas(lifecycleScope)
                    }
                } else {
                    viewModel.checkApiConnection(DatosUsuario.getEmail(requireActivity()))
                }
            }
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        viewModel.usuario.observe(viewLifecycleOwner) { nombre ->
            updateWelcomeMessage(nombre)
        }
        viewModel.roleId.observe(viewLifecycleOwner) { roleId ->
            updateAdminOptionsVisibility(roleId)
        }
        // Nuevo observador para el estado de la conexión
        viewModel.apiConnection.observe(viewLifecycleOwner) { isConnected ->
            updateApiConnectionStatus(isConnected)
        }
        viewModel.checkUserRole(DatosUsuario.getEmail(requireActivity()))

    }
    private fun updateApiConnectionStatus(isConnected: Boolean) {
        val txtApiConnectionStatus = view?.findViewById<TextView>(R.id.txtApiConnectionStatus)
        val (hayPendiente, tipoAccion) = manejadorAcciones.accionPendiente()

        if (isConnected) {
            txtApiConnectionStatus?.text = "Conectado"
            txtApiConnectionStatus?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_connected, 0, 0, 0)
            btnReintentarEstatus.visibility = View.GONE
            /*
            if (hayPendiente) {
                // Hay tareas pendientes, mostrar botón y mensaje adecuado
                btnReintentarEstatus.visibility = View.VISIBLE
                btnReintentarEstatus.text = "Tarea pendiente de enviar $tipoAccion"
                // Considera cambiar el color del botón a uno que indique acción requerida
                btnReintentarEstatus.setBackgroundColor(Color.parseColor("#FFA500")) // Color naranja, por ejemplo
            } else {
                // No hay tareas pendientes, esconder botón de reintentar
                btnReintentarEstatus.visibility = View.GONE
            }*/
        } else {
            txtApiConnectionStatus?.text = "Desconectado"
            txtApiConnectionStatus?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_desconnected, 0, 0, 0)
            // Mantén el botón de reintentar visible si hay desconexion, independientemente de si hay tareas pendientes
            btnReintentarEstatus.visibility = View.VISIBLE
            btnReintentarEstatus.setBackgroundColor(Color.parseColor("#FF8D8D")) // Color rojo, por ejemplo
        }
    }

    private fun EstadoDesconectado() {
        val txtApiConnectionStatus = view?.findViewById<TextView>(R.id.txtApiConnectionStatus)
        txtApiConnectionStatus?.text = "Desconectado"
        txtApiConnectionStatus?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_desconnected, 0, 0, 0)
        // Mantén el botón de reintentar visible si hay desconexion, independientemente de si hay tareas pendientes
        btnReintentarEstatus.visibility = View.VISIBLE
        btnReintentarEstatus.setBackgroundColor(Color.parseColor("#FF8D8D")) // Color rojo, por ejemplo
    }


    private fun updateWelcomeMessage(userName: String) {
        //var userName2 = DatosUsuario.getUserName(requireContext())
        Log.d("username","$userName")
        view?.findViewById<TextView>(R.id.bienvenidaTextView)?.text = "Bienvenido, $userName"
    }

    private fun updateAdminOptionsVisibility(roleId: Int?) {
        Log.d("HF","ROLEID: $roleId")
        val isAdmin = roleId == 1
        btnAdminCarreras.visibility = if (isAdmin) View.VISIBLE else View.GONE
        btnAdminLogros.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.home_container_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }


}
