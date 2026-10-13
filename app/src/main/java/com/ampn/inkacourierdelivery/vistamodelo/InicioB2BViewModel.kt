package com.ampn.inkacourierdelivery.vistamodelo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampn.inkacourierdelivery.modelo.Envio
import com.ampn.inkacourierdelivery.modelo.EstadoEnvio
import com.ampn.inkacourierdelivery.modelo.Usuario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ResumenEmpresa(
    val cantidadPorEstado: Map<EstadoEnvio, Int> = emptyMap(),
    val recientes: List<Envio> = emptyList()
)

/** Pantalla inicial del representante B2B: conteo por estado y últimas actualizaciones. */
class InicioB2BViewModel : ViewModel() {

    companion object {
        private const val CANTIDAD_RECIENTES = 4
    }

    val usuario: Usuario? = usuarioEnSesion()

    val resumen: StateFlow<ResumenEmpresa> = enviosDeLaEmpresa()
        .map { envios ->
            ResumenEmpresa(
                cantidadPorEstado = envios.groupingBy { it.estado }.eachCount(),
                recientes = envios.take(CANTIDAD_RECIENTES)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenEmpresa())
}
