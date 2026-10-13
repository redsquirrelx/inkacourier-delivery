package com.ampn.inkacourierdelivery.vistamodelo

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ampn.inkacourierdelivery.modelo.DatosSimulados
import com.ampn.inkacourierdelivery.modelo.RepositorioSesion
import com.ampn.inkacourierdelivery.modelo.ResultadoSesion
import com.ampn.inkacourierdelivery.modelo.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EstadoLogin(
    val errorUsuario: String? = null,
    val errorContrasena: String? = null
)

/** Inicio de sesión simulado. Nunca registra la contraseña en los logs. */
class LoginViewModel : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    /** Credenciales de demostración que se muestran en pantalla para facilitar las pruebas. */
    val credencialesDemo: List<Pair<Usuario, String>> = DatosSimulados.CREDENCIALES_DEMO.toList()

    private val _estado = MutableStateFlow(EstadoLogin())
    val estado: StateFlow<EstadoLogin> = _estado.asStateFlow()

    fun iniciarSesion(identificador: String, contrasena: String) {
        when (val resultado = RepositorioSesion.compartido.iniciarSesion(identificador, contrasena)) {
            is ResultadoSesion.Exitosa -> {
                Log.i(TAG, "Sesión iniciada: ${resultado.usuario.usuario} (${resultado.usuario.rol.etiqueta})")
                _estado.value = EstadoLogin()
            }
            is ResultadoSesion.Fallida -> {
                Log.w(TAG, "Inicio de sesión rechazado para \"${identificador.trim()}\"")
                _estado.value = EstadoLogin(resultado.errorUsuario, resultado.errorContrasena)
            }
        }
    }

    fun limpiarErrores() {
        _estado.value = EstadoLogin()
    }
}
