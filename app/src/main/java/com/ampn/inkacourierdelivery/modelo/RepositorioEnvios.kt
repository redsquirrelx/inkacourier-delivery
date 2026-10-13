package com.ampn.inkacourierdelivery.modelo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

sealed interface ResultadoRegistro {
    data object Exitoso : ResultadoRegistro
    data class Fallido(val mensaje: String) : ResultadoRegistro
}

/**
 * Repositorio en memoria compartido por todos los módulos (repartidor y B2B).
 * Los datos solo existen durante la ejecución de la aplicación.
 */
class RepositorioEnvios(enviosIniciales: List<Envio>) {

    companion object {
        val compartido: RepositorioEnvios by lazy {
            RepositorioEnvios(DatosSimulados.crearEnvios(System.currentTimeMillis()))
        }
    }

    private val _envios = MutableStateFlow(enviosIniciales)
    val envios: StateFlow<List<Envio>> = _envios.asStateFlow()

    fun paradasDeRuta(repartidorId: String): Flow<List<Envio>> =
        envios.map { lista ->
            lista.filter { it.enRuta && it.repartidorId == repartidorId }.sortedBy { it.ordenParada }
        }

    fun enviosDeEmpresa(empresaId: String): Flow<List<Envio>> =
        envios.map { lista ->
            lista.filter { it.empresa.id == empresaId }.sortedByDescending { it.ultimaActualizacion }
        }

    fun observarEnvio(codigo: String): Flow<Envio?> =
        envios.map { lista -> lista.find { it.codigo == codigo } }

    fun buscar(codigo: String): Envio? = _envios.value.find { it.codigo == codigo }

    fun registrarEntrega(codigo: String, registro: RegistroEntrega): ResultadoRegistro {
        val envio = buscar(codigo) ?: return ResultadoRegistro.Fallido("Envío no encontrado")
        ValidadorRegistros.validarEntrega(envio, registro)?.let { return ResultadoRegistro.Fallido(it) }
        cerrarParada(
            codigo = codigo,
            nuevoEstado = EstadoEnvio.ENTREGADO,
            descripcion = "Entregado a ${registro.receptor.nombre} (${registro.tipoReceptor.etiqueta.lowercase()})",
            fechaHora = registro.fechaHora
        ) { it.copy(entrega = registro) }
        return ResultadoRegistro.Exitoso
    }

    fun registrarVisitaFallida(codigo: String, registro: RegistroVisitaFallida): ResultadoRegistro {
        val envio = buscar(codigo) ?: return ResultadoRegistro.Fallido("Envío no encontrado")
        ValidadorRegistros.validarVisitaFallida(envio, registro)?.let { return ResultadoRegistro.Fallido(it) }
        cerrarParada(
            codigo = codigo,
            nuevoEstado = EstadoEnvio.VISITA_FALLIDA,
            descripcion = "Visita fallida: ${registro.causa?.etiqueta}",
            fechaHora = registro.fechaHora
        ) { it.copy(visitaFallida = registro) }
        return ResultadoRegistro.Exitoso
    }

    /** El repartidor inicia intencionalmente el traslado: la parada pasa a "En camino a entrega". */
    fun iniciarTraslado(codigo: String, fechaHora: Long = System.currentTimeMillis()): ResultadoRegistro {
        val envio = buscar(codigo) ?: return ResultadoRegistro.Fallido("Envío no encontrado")
        val enCamino = _envios.value.find {
            it.repartidorId == envio.repartidorId && it.enRuta && it.estado == EstadoEnvio.EN_CAMINO_A_ENTREGA
        }
        ValidadorRegistros.validarInicioTraslado(envio, enCamino)?.let { return ResultadoRegistro.Fallido(it) }
        actualizar(codigo) {
            it.conEstado(EstadoEnvio.EN_CAMINO_A_ENTREGA, "El repartidor inició el traslado a la dirección", fechaHora)
        }
        return ResultadoRegistro.Exitoso
    }

    private fun cerrarParada(
        codigo: String,
        nuevoEstado: EstadoEnvio,
        descripcion: String,
        fechaHora: Long,
        aplicarRegistro: (Envio) -> Envio
    ) {
        actualizar(codigo) { aplicarRegistro(it).conEstado(nuevoEstado, descripcion, fechaHora) }
    }

    private fun actualizar(codigo: String, cambio: (Envio) -> Envio) {
        _envios.update { lista -> lista.map { if (it.codigo == codigo) cambio(it) else it } }
    }
}
