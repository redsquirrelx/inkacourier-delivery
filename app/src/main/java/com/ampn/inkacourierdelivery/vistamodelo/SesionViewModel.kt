package com.ampn.inkacourierdelivery.vistamodelo

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ampn.inkacourierdelivery.modelo.RepositorioSesion
import com.ampn.inkacourierdelivery.modelo.Usuario
import kotlinx.coroutines.flow.StateFlow

/** Sesión activa, compartida por MainActivity para decidir la navegación según el rol. */
class SesionViewModel : ViewModel() {

    companion object {
        private const val TAG = "SesionViewModel"
    }

    private val repositorio = RepositorioSesion.compartido
    val usuario: StateFlow<Usuario?> = repositorio.usuarioActual

    fun cerrarSesion() {
        Log.i(TAG, "Sesión cerrada: ${usuario.value?.usuario}")
        repositorio.cerrarSesion()
    }
}
