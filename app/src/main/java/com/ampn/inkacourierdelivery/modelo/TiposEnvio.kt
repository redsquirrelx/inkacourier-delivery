package com.ampn.inkacourierdelivery.modelo

enum class EstadoEnvio(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    EN_RUTA("En ruta"),
    EN_CAMINO_A_ENTREGA("En camino a entrega"),
    ENTREGADO("Entregado"),
    VISITA_FALLIDA("Visita fallida"),
    NO_VISITADO("No visitado");

    /** Solo las paradas en ruta que aún no tienen resultado pueden registrarse. */
    val atendible: Boolean get() = this == EN_RUTA || this == EN_CAMINO_A_ENTREGA
}

enum class CondicionPago(val etiqueta: String) {
    PREPAGADO("Prepagado"),
    CONTRA_ENTREGA("Pago contra entrega")
}

enum class TipoDocumento(val etiqueta: String) {
    DNI("DNI"),
    CE("Carné de extranjería")
}

enum class TipoReceptor(val etiqueta: String) {
    TITULAR("Titular"),
    TERCERO_AUTORIZADO("Tercero autorizado")
}

enum class CausaVisitaFallida(val etiqueta: String) {
    DESTINATARIO_AUSENTE("Destinatario ausente"),
    DIRECCION_NO_LOCALIZADA("Dirección no localizada"),
    RECEPCION_RECHAZADA("Recepción rechazada"),
    PAGO_NO_VERIFICADO("Pago no verificado")
}

enum class TipoEvento {
    ESTADO,
    CAMBIO_OFICINA
}
