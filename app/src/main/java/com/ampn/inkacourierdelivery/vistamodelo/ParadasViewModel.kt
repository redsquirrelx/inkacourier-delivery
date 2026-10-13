package com.ampn.inkacourierdelivery.vistamodelo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampn.inkacourierdelivery.modelo.Envio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class FiltroParadas { POR_ATENDER, ATENDIDAS, TODAS }

data class EstadoParadas(
    val paradas: List<Envio> = emptyList(),
    val filtro: FiltroParadas = FiltroParadas.POR_ATENDER,
    val totalPorAtender: Int = 0,
    val totalAtendidas: Int = 0
)

/** HU-01: paradas de la ruta del repartidor en sesión. */
class ParadasViewModel(estadoGuardado: SavedStateHandle) : ViewModel() {

    companion object {
        const val ARG_FILTRO = "filtro"
    }

    private val filtro = MutableStateFlow(
        estadoGuardado.get<String>(ARG_FILTRO)?.let { FiltroParadas.valueOf(it) } ?: FiltroParadas.POR_ATENDER
    )

    val estado: StateFlow<EstadoParadas> =
        combine(paradasDelRepartidor(), filtro) { paradas, filtroActual ->
            EstadoParadas(
                paradas = paradas.filter {
                    when (filtroActual) {
                        FiltroParadas.POR_ATENDER -> it.estado.atendible
                        FiltroParadas.ATENDIDAS -> !it.estado.atendible
                        FiltroParadas.TODAS -> true
                    }
                },
                filtro = filtroActual,
                totalPorAtender = paradas.count { it.estado.atendible },
                totalAtendidas = paradas.count { !it.estado.atendible }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoParadas(filtro = filtro.value))

    fun cambiarFiltro(nuevoFiltro: FiltroParadas) {
        filtro.value = nuevoFiltro
    }
}
