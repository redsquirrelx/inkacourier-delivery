package com.ampn.inkacourierdelivery.vistamodelo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampn.inkacourierdelivery.modelo.Envio
import com.ampn.inkacourierdelivery.modelo.RepositorioEnvios
import com.ampn.inkacourierdelivery.modelo.ResultadoRegistro
import com.ampn.inkacourierdelivery.modelo.Rol
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Detalle compartido por HU-01 (parada del repartidor) y HU-07 (envío B2B).
 * Solo expone el envío si el usuario en sesión puede consultarlo.
 */
class DetalleEnvioViewModel(estadoGuardado: SavedStateHandle) : ViewModel() {

    companion object {
        const val ARG_CODIGO = "codigo"
    }

    private val repositorio = RepositorioEnvios.compartido
    private val codigo: String = checkNotNull(estadoGuardado[ARG_CODIGO])
    private val usuario = usuarioEnSesion()

    val esModoEmpresa: Boolean = usuario?.rol == Rol.REPRESENTANTE_B2B

    val envio: StateFlow<Envio?> = repositorio.observarEnvio(codigo)
        .map { envio -> envio?.takeIf { usuario?.puedeConsultar(it) == true } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes: Flow<String> = _mensajes.receiveAsFlow()

    fun iniciarTraslado() {
        val envioActual = repositorio.buscar(codigo) ?: return
        if (usuario?.puedeRegistrar(envioActual) != true) return
        _mensajes.trySend(
            when (val resultado = repositorio.iniciarTraslado(codigo)) {
                ResultadoRegistro.Exitoso -> "Traslado iniciado: la parada está En camino a entrega"
                is ResultadoRegistro.Fallido -> resultado.mensaje
            }
        )
    }
}
