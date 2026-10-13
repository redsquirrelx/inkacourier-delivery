package com.ampn.inkacourierdelivery.modelo

import java.util.Locale

data class Empresa(val id: String, val razonSocial: String)

data class Persona(
    val nombre: String,
    val tipoDocumento: TipoDocumento,
    val numeroDocumento: String
) {
    fun mismoDocumento(otra: Persona): Boolean =
        tipoDocumento == otra.tipoDocumento &&
            numeroDocumento.equals(otra.numeroDocumento, ignoreCase = true)
}

data class Ubicacion(val latitud: Double, val longitud: Double) {
    fun comoTexto(): String = String.format(Locale.US, "%.6f, %.6f", latitud, longitud)
}

data class EventoEnvio(
    val fechaHora: Long,
    val descripcion: String,
    val tipo: TipoEvento = TipoEvento.ESTADO
)

data class RegistroEntrega(
    val tipoReceptor: TipoReceptor,
    val receptor: Persona,
    val firma: ByteArray?,
    val foto: ByteArray?,
    val justificacionSinFoto: String?,
    val ubicacion: Ubicacion?,
    val fechaHora: Long
)

data class RegistroVisitaFallida(
    val causa: CausaVisitaFallida?,
    val observaciones: String,
    val foto: ByteArray?,
    val justificacionSinFoto: String?,
    val ubicacion: Ubicacion?,
    val fechaHora: Long
)

/**
 * Un envío y, cuando está asignado a la ruta del repartidor, su parada (ordenParada).
 * Las instrucciones, autorizados y zona de alto riesgo son emitidos por oficina.
 */
data class Envio(
    val codigo: String,
    val empresa: Empresa,
    val ordenParada: Int?,
    val destinatario: Persona,
    val telefono: String,
    val direccion: String,
    val distrito: String,
    val referencias: String,
    val horarioAtencion: String,
    val instrucciones: String,
    val importe: Double,
    val condicionPago: CondicionPago,
    val pagoVerificado: Boolean,
    val zonaAltoRiesgoAutorizada: Boolean,
    val autorizados: List<Persona>,
    val estado: EstadoEnvio,
    val ultimaActualizacion: Long,
    val historial: List<EventoEnvio>,
    val entrega: RegistroEntrega? = null,
    val visitaFallida: RegistroVisitaFallida? = null,
    /** Repartidor al que oficina asignó la parada (null si el envío no está en una ruta activa). */
    val repartidorId: String? = null
) {
    val enRuta: Boolean get() = ordenParada != null

    val tieneCambiosOficina: Boolean get() = historial.any { it.tipo == TipoEvento.CAMBIO_OFICINA }

    val pagoPendiente: Boolean get() = condicionPago == CondicionPago.CONTRA_ENTREGA && !pagoVerificado

    fun conEstado(nuevoEstado: EstadoEnvio, descripcion: String, fechaHora: Long): Envio = copy(
        estado = nuevoEstado,
        ultimaActualizacion = fechaHora,
        historial = historial + EventoEnvio(fechaHora, descripcion)
    )
}
