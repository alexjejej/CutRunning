package com.cut.android.running.common.navigation

import MapsFragment
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.cut.android.running.R
import com.cut.android.running.databinding.FragmentNavBarBinding
import com.cut.android.running.models.Session
import com.cut.android.running.provider.services.navigation.NavigationObj
import com.cut.android.running.usecases.clasificacion.ClasificacionFragment
import com.cut.android.running.usecases.estadisticas.EstadisticasFragment
import com.cut.android.running.usecases.home.HomeFragment
import com.cut.android.running.usecases.login.LoginFragment
import com.cut.android.running.usecases.logros.LogrosFragment
import com.cut.android.running.usecases.profile.ProfileFragment
import com.squareup.picasso.Picasso

/**
 * A simple [Fragment] subclass.
 * Use the [NavBarFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NavBarFragment : Fragment() {
    private var _binding: FragmentNavBarBinding? = null
    private val binding get() = _binding!!
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var activity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNavBarBinding.inflate(inflater, container, false)
        Log.d("NavBarFragment", "Fragment de barra de navegacion")
        setup()

        navigateTo(
            HomeFragment(),
            getString(R.string.HomeFragment)
        )

        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = NavBarFragment()
    }

    private fun setup() {
        //Restablecer el Estado del DrawerLayout al Iniciar Sesión:
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        activity = requireActivity()

        // Configuracion de Nav Bar
        toggle = ActionBarDrawerToggle(activity, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Obtencion del nombre de ususario de shared preferences y formateo para obtener los dos primeros valores
        Session.readPrefs(activity)
        val userName: List<String>? = Session.userName.split(" ").filter { it.isNotEmpty() }

        // Verifica si userName tiene al menos un elemento
        val displayName = if (userName != null && userName.isNotEmpty()) {
            // Usa solo el primer nombre si no hay apellido
            if (userName.size > 1) "${userName[0]} ${userName[1]}" else userName[0]
        } else {
            "userName Nulo" // O alguna cadena por defecto si userName es nulo o vacío
        }

        // Accedemos a modificar el header de Nav Bar
        var header: View = binding.navigationView.getHeaderView(0)
        header.findViewById<TextView>(R.id.txtUserName).text = displayName
        Picasso.get().load(Session.userPhoto).into(header.findViewById<ImageView>(R.id.profilePhoto))
        binding.navigationView.setNavigationItemSelectedListener { item -> setNavigation(item) }

    }

    /** Controla la navegacion del menu **/
    private fun setNavigation(item: MenuItem): Boolean {
        when( item.itemId ) {
            R.id.btnProfile -> {
                return navigateTo(ProfileFragment(), getString(R.string.ProfileFragment))
            }
            R.id.btnHome -> {
                return navigateTo(HomeFragment(), getString(R.string.HomeFragment))
            }
            R.id.btnAddTraining -> {
                return navigateTo(MapsFragment(), getString(R.string.MapsFragment))
            }
            R.id.btnAchievements -> {
                return navigateTo(LogrosFragment(), getString(R.string.AchievementFragment))
            }
            R.id.btnClasificacion -> {
                return navigateTo(ClasificacionFragment(), getString(R.string.ClasificationFragment))
            }
            R.id.btnStatistics -> {
                return navigateTo(EstadisticasFragment(), getString(R.string.StatisticsFragment))
            }


                    R.id.btnLogout -> {
                return logout()
            }
            else -> return false
        }
    }

    /** Navega al fragmento dado como parametro **/
    private fun navigateTo( fragment: Fragment, tag: String ): Boolean {
        binding.drawerLayout.closeDrawer(GravityCompat.START) // Cierra el DrawerLayout (NavBar)
        return NavigationObj.navigateTo(requireActivity().supportFragmentManager, fragment, tag)

    }

    /** Acciones para el cierre de sesion **/
    private fun logout(): Boolean {
        Session.signOut()
        Log.d("NavBarFragment","Cerrando session")
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        super.onDestroy() // Destruye el NavBarFragment
        NavigationObj.navigateTo(parentFragmentManager, LoginFragment(), getString(R.string.LoginFragment))
        return true
    }

    /** Maneja el BackStack de fragments para que no se sobrepongan los fragments **/
//    private fun manageBackStack() {
//        supportFragmentManager.popBackStack()
//        Log.d("BackStack", "${supportFragmentManager.backStackEntryCount}")
//    }
}