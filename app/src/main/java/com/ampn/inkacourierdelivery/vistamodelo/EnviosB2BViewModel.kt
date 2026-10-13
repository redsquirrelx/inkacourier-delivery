package com.ampn.inkacourierdelivery.vistamodelo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampn.inkacourierdelivery.modelo.Empresa
import com.ampn.inkacourierdelivery.modelo.Envio
import com.ampn.inkacourierdelivery.modelo.EstadoEnvio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EstadoEnviosB2B(
    val envios: List<Envio> = emptyList(),
    val totalEmpresa: Int = 0,
    val filtroEstado: EstadoEnvio? = null,
    val hayFiltros: Boolean = false
)

/** HU-07: envíos de la empresa del representante B2B en sesión. */
class EnviosB2BViewModel(estadoGuardado: SavedStateHandle) : ViewModel() {

    companion object {
        const val ARG_ESTADO = "estado"
    }

    val empresa: Empresa? = usuarioEnSesion()?.empresa

    private val consulta = MutableStateFlow("")
    private val filtroEstado = MutableStateFlow(
        estadoGuardado.get<String>(ARG_ESTADO)?.let { EstadoEnvio.valueOf(it) }
    )

    val estado: StateFlow<EstadoEnviosB2B> = combine(
        enviosDeLaEmpresa(),
        consulta,
        filtroEstado
    ) { envios, texto, estadoFiltro ->
        EstadoEnviosB2B(
            envios = envios.filter { (estadoFiltro == null || it.estado == estadoFiltro) && coincide(it, texto) },
            totalEmpresa = envios.size,
            filtroEstado = estadoFiltro,
            hayFiltros = texto.isNotBlank() || estadoFiltro != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoEnviosB2B(filtroEstado = filtroEstado.value))

    fun buscar(texto: String) {
        consulta.value = texto
    }

    fun filtrarPorEstado(estado: EstadoEnvio?) {
        filtroEstado.value = estado
    }

    private fun coincide(envio: Envio, texto: String): Boolean {
        val busqueda = texto.trim()
        if (busqueda.isEmpty()) return true
        return listOf(envio.codigo, envio.destinatario.nombre, envio.direccion, envio.distrito)
            .any { it.contains(busqueda, ignoreCase = true) }
    }
}
