package com.ampn.inkacourierdelivery.vistamodelo

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampn.inkacourierdelivery.modelo.Envio
import com.ampn.inkacourierdelivery.modelo.RepositorioEnvios
import com.ampn.inkacourierdelivery.modelo.ResultadoRegistro
import com.ampn.inkacourierdelivery.modelo.Ubicacion
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

sealed interface EventoRegistro {
    data class Error(val mensaje: String) : EventoRegistro
    data class Registrado(val mensaje: String) : EventoRegistro
}

/** Estado común de HU-03 y HU-05: envío, fotografía, GPS y eventos de resultado. */
abstract class RegistroBaseViewModel(estadoGuardado: SavedStateHandle) : ViewModel() {

    companion object {
        private const val TAG = "RegistroBaseViewModel"
        const val ARG_CODIGO = "codigo"
    }

    protected val repositorio = RepositorioEnvios.compartido
    protected val codigo: String = checkNotNull(estadoGuardado[ARG_CODIGO])
    private val usuario = usuarioEnSesion()

    /** Solo el repartidor asignado ve la parada en el formulario. */
    val envio: StateFlow<Envio?> = repositorio.observarEnvio(codigo)
        .map { envio -> envio?.takeIf { usuario?.puedeRegistrar(it) == true } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _foto = MutableStateFlow<ByteArray?>(null)
    val foto: StateFlow<ByteArray?> = _foto.asStateFlow()

    private val _ubicacion = MutableStateFlow<Ubicacion?>(null)
    val ubicacion: StateFlow<Ubicacion?> = _ubicacion.asStateFlow()

    private val _buscandoUbicacion = MutableStateFlow(false)
    val buscandoUbicacion: StateFlow<Boolean> = _buscandoUbicacion.asStateFlow()

    private val _eventos = Channel<EventoRegistro>(Channel.BUFFERED)
    val eventos: Flow<EventoRegistro> = _eventos.receiveAsFlow()

    fun establecerFoto(bytes: ByteArray) {
        _foto.value = bytes
    }

    fun iniciarBusquedaUbicacion() {
        _buscandoUbicacion.value = true
    }

    fun establecerUbicacion(nuevaUbicacion: Ubicacion) {
        _buscandoUbicacion.value = false
        _ubicacion.value = nuevaUbicacion
    }

    fun ubicacionNoDisponible(motivo: String) {
        _buscandoUbicacion.value = false
        notificar(EventoRegistro.Error(motivo))
    }

    /** Devuelve el mensaje de error de validación, o null si el registro es válido. */
    protected fun validarCon(validacion: (Envio) -> String?): String? {
        val envioActual = repositorio.buscar(codigo) ?: return "Envío no encontrado"
        if (usuario?.puedeRegistrar(envioActual) != true) return "No tiene permiso para registrar en esta parada"
        return validacion(envioActual)
    }

    protected fun procesarResultado(resultado: ResultadoRegistro, mensajeExito: String) {
        when (resultado) {
            ResultadoRegistro.Exitoso -> {
                Log.i(TAG, "Registro guardado para $codigo: $mensajeExito")
                notificar(EventoRegistro.Registrado(mensajeExito))
            }
            is ResultadoRegistro.Fallido -> {
                Log.w(TAG, "Registro rechazado para $codigo: ${resultado.mensaje}")
                notificar(EventoRegistro.Error(resultado.mensaje))
            }
        }
    }

    private fun notificar(evento: EventoRegistro) {
        _eventos.trySend(evento)
    }
}
