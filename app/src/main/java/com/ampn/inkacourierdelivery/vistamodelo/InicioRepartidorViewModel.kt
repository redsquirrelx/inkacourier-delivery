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

data class ResumenRuta(
    val total: Int = 0,
    val atendidas: Int = 0,
    val porAtender: Int = 0,
    val entregadas: Int = 0,
    val fallidas: Int = 0,
    val proximaParada: Envio? = null
)

/** Pantalla inicial del repartidor: resumen de su ruta y próxima parada. */
class InicioRepartidorViewModel : ViewModel() {

    val usuario: Usuario? = usuarioEnSesion()

    val resumen: StateFlow<ResumenRuta> = paradasDelRepartidor()
        .map { paradas ->
            ResumenRuta(
                total = paradas.size,
                atendidas = paradas.count { !it.estado.atendible },
                porAtender = paradas.count { it.estado.atendible },
                entregadas = paradas.count { it.estado == EstadoEnvio.ENTREGADO },
                fallidas = paradas.count { it.estado == EstadoEnvio.VISITA_FALLIDA },
                proximaParada = paradas.find { it.estado == EstadoEnvio.EN_CAMINO_A_ENTREGA }
                    ?: paradas.find { it.estado.atendible }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenRuta())
}
