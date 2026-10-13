package com.ampn.inkacourierdelivery

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ampn.inkacourierdelivery.databinding.ActivityMainBinding
import com.ampn.inkacourierdelivery.modelo.Rol
import com.ampn.inkacourierdelivery.modelo.Usuario
import com.ampn.inkacourierdelivery.vistamodelo.SesionViewModel
import com.ampn.inkacourierdelivery.vistas.EnviosB2BFragment
import com.ampn.inkacourierdelivery.vistas.InicioB2BFragment
import com.ampn.inkacourierdelivery.vistas.InicioRepartidorFragment
import com.ampn.inkacourierdelivery.vistas.LoginFragment
import com.ampn.inkacourierdelivery.vistas.ParadasFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Única actividad. Muestra el Login sin sesión y, con sesión, la navegación del rol:
 * Inicio + Paradas (repartidor) o Inicio + Envíos (representante B2B).
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private val sesionViewModel: SesionViewModel by viewModels()

    private var rolNavegacion: Rol? = null
    private var fragmentoPendiente: Fragment? = null

    /** Desde la raíz de una sección distinta de Inicio, "atrás" vuelve a Inicio. */
    private val volverAInicio = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.navegacionInferior.selectedItemId = R.id.seccionInicio
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.barraHerramientas)
        aplicarMargenesDeSistema()

        configurarNavegacion(sesionViewModel.usuario.value?.rol)
        binding.navegacionInferior.setOnItemSelectedListener { item ->
            mostrarSeccion(item.itemId)
            true
        }
        binding.navegacionInferior.setOnItemReselectedListener { limpiarHistorial() }
        onBackPressedDispatcher.addCallback(this, volverAInicio)
        supportFragmentManager.addOnBackStackChangedListener { actualizarNavegacionAtras() }
        addMenuProvider(MenuSesion(), this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sesionViewModel.usuario.collect { aplicarSesion(it) }
            }
        }
    }

    /** Navega hacia una pantalla interna. Sin sesión activa no se permite. */
    fun navegarA(fragmento: Fragment) {
        if (sesionViewModel.usuario.value == null) {
            Log.w(TAG, "Navegación bloqueada: no hay sesión activa")
            return
        }
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.contenedorFragmentos, fragmento)
            addToBackStack(null)
        }
    }

    fun abrirSeccion(idSeccion: Int, fragmento: Fragment) {
        fragmentoPendiente = fragmento
        if (binding.navegacionInferior.selectedItemId == idSeccion) mostrarSeccion(idSeccion)
        else binding.navegacionInferior.selectedItemId = idSeccion
    }

    /** Vista sobre la que se anclan los Snackbar para no tapar la navegación inferior. */
    fun anclaMensajes(): View? = binding.navegacionInferior.takeIf { it.isVisible }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun aplicarSesion(usuario: Usuario?) {
        val actual = supportFragmentManager.findFragmentById(R.id.contenedorFragmentos)
        invalidateOptionsMenu()
        if (usuario == null) {
            binding.barraHerramientas.isVisible = false
            binding.navegacionInferior.isVisible = false
            if (actual !is LoginFragment) {
                limpiarHistorial()
                reemplazarRaiz(LoginFragment())
            }
        } else {
            binding.barraHerramientas.isVisible = true
            binding.navegacionInferior.isVisible = true
            configurarNavegacion(usuario.rol)
            if (actual == null || actual is LoginFragment) {
                limpiarHistorial()
                // Se marca sin disparar el listener para no crear la sección dos veces.
                binding.navegacionInferior.menu.findItem(R.id.seccionInicio)?.isChecked = true
                mostrarSeccion(R.id.seccionInicio)
            }
        }
        actualizarNavegacionAtras()
    }

    private fun configurarNavegacion(rol: Rol?) {
        if (rol == null || rol == rolNavegacion) return
        rolNavegacion = rol
        binding.navegacionInferior.menu.clear()
        binding.navegacionInferior.inflateMenu(
            if (rol == Rol.REPARTIDOR) R.menu.menu_repartidor else R.menu.menu_b2b
        )
    }

    private fun mostrarSeccion(idSeccion: Int) {
        val rol = sesionViewModel.usuario.value?.rol ?: return
        val fragmento = fragmentoPendiente ?: when (idSeccion) {
            R.id.seccionParadas -> ParadasFragment()
            R.id.seccionEnvios -> EnviosB2BFragment()
            else -> if (rol == Rol.REPARTIDOR) InicioRepartidorFragment() else InicioB2BFragment()
        }
        fragmentoPendiente = null
        limpiarHistorial()
        reemplazarRaiz(fragmento)
        actualizarNavegacionAtras()
    }

    private fun reemplazarRaiz(fragmento: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.contenedorFragmentos, fragmento)
        }
    }

    private fun limpiarHistorial() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun actualizarNavegacionAtras() {
        val enRaiz = supportFragmentManager.backStackEntryCount == 0
        supportActionBar?.setDisplayHomeAsUpEnabled(!enRaiz)
        volverAInicio.isEnabled = enRaiz && sesionViewModel.usuario.value != null &&
            binding.navegacionInferior.selectedItemId != R.id.seccionInicio
    }

    private fun aplicarMargenesDeSistema() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { vista, insets ->
            val sistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            vista.setPadding(sistema.left, sistema.top, sistema.right, 0)
            binding.contenedorInferior.setPadding(0, 0, 0, maxOf(sistema.bottom, teclado.bottom))
            insets
        }
    }

    private fun confirmarCierreSesion() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.accion_cerrar_sesion)
            .setMessage(R.string.confirmar_cierre_sesion)
            .setNegativeButton(R.string.accion_cancelar, null)
            .setPositiveButton(R.string.accion_cerrar_sesion) { _, _ -> sesionViewModel.cerrarSesion() }
            .show()
    }

    private inner class MenuSesion : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_sesion, menu)
        }

        override fun onPrepareMenu(menu: Menu) {
            menu.findItem(R.id.accionCerrarSesion)?.isVisible = sesionViewModel.usuario.value != null
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId != R.id.accionCerrarSesion) return false
            confirmarCierreSesion()
            return true
        }
    }
}
