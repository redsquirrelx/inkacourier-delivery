package com.ampn.inkacourierdelivery.modelo

/** Reglas de negocio de HU-03 y HU-05. Devuelve el primer error encontrado o null si es válido. */
object ValidadorRegistros {

    const val LONGITUD_MINIMA_JUSTIFICACION = 15
    const val LONGITUD_MINIMA_OBSERVACIONES = 10

    private val REGEX_DNI = Regex("""^\d{8}$""")
    private val REGEX_CE = Regex("""^[A-Za-z0-9]{9,12}$""")
    private val REGEX_NOMBRE = Regex("""^[\p{L} '.-]{3,80}$""")

    fun validarDocumento(tipo: TipoDocumento, numero: String): String? = when (tipo) {
        TipoDocumento.DNI ->
            if (REGEX_DNI.matches(numero)) null else "El DNI debe tener 8 dígitos"
        TipoDocumento.CE ->
            if (REGEX_CE.matches(numero)) null
            else "El carné de extranjería debe tener entre 9 y 12 caracteres alfanuméricos"
    }

    fun validarEntrega(envio: Envio, registro: RegistroEntrega): String? {
        validarEstado(envio)?.let { return it }
        if (envio.pagoPendiente) {
            return "El pago contra entrega no está verificado. No se puede entregar: " +
                "registre una visita fallida por pago no verificado"
        }
        val receptor = registro.receptor
        validarDocumento(receptor.tipoDocumento, receptor.numeroDocumento)?.let { return it }
        if (!REGEX_NOMBRE.matches(receptor.nombre)) {
            return "Ingrese un nombre válido (solo letras, mínimo 3 caracteres)"
        }
        when (registro.tipoReceptor) {
            TipoReceptor.TITULAR -> if (!envio.destinatario.mismoDocumento(receptor)) {
                return "El documento no coincide con el del titular"
            }
            TipoReceptor.TERCERO_AUTORIZADO -> if (envio.autorizados.none { it.mismoDocumento(receptor) }) {
                return "La persona no figura como tercero autorizado por oficina"
            }
        }
        if (registro.firma == null) return "Falta la firma del receptor"
        return validarEvidencias(envio, registro.foto, registro.justificacionSinFoto, registro.ubicacion)
    }

    fun validarVisitaFallida(envio: Envio, registro: RegistroVisitaFallida): String? {
        validarEstado(envio)?.let { return it }
        val causa = registro.causa ?: return "Seleccione la causa de la visita fallida"
        if (causa == CausaVisitaFallida.PAGO_NO_VERIFICADO && envio.condicionPago != CondicionPago.CONTRA_ENTREGA) {
            return "El envío es prepagado: la causa \"Pago no verificado\" no aplica"
        }
        if (registro.observaciones.trim().length < LONGITUD_MINIMA_OBSERVACIONES) {
            return "Las observaciones deben tener al menos $LONGITUD_MINIMA_OBSERVACIONES caracteres"
        }
        return validarEvidencias(envio, registro.foto, registro.justificacionSinFoto, registro.ubicacion)
    }

    /** Solo se inicia el traslado a una parada En ruta y de a una a la vez. */
    fun validarInicioTraslado(envio: Envio, paradaEnCamino: Envio?): String? = when {
        !envio.enRuta || envio.estado != EstadoEnvio.EN_RUTA ->
            "Solo puede iniciar el traslado de una parada En ruta (estado: ${envio.estado.etiqueta})"
        paradaEnCamino != null ->
            "Ya se dirige a la parada ${paradaEnCamino.ordenParada} (${paradaEnCamino.codigo}). " +
                "Registre su resultado antes de iniciar otro traslado"
        else -> null
    }

    private fun validarEstado(envio: Envio): String? =
        if (envio.enRuta && envio.estado.atendible) null
        else "La parada no admite registros (estado: ${envio.estado.etiqueta})"

    private fun validarEvidencias(
        envio: Envio,
        foto: ByteArray?,
        justificacionSinFoto: String?,
        ubicacion: Ubicacion?
    ): String? {
        if (foto == null) {
            if (!envio.zonaAltoRiesgoAutorizada) return "La fotografía es obligatoria"
            if ((justificacionSinFoto?.trim()?.length ?: 0) < LONGITUD_MINIMA_JUSTIFICACION) {
                return "Justifique la omisión de la fotografía (mínimo $LONGITUD_MINIMA_JUSTIFICACION caracteres)"
            }
        }
        if (ubicacion == null) return "Capture la ubicación GPS"
        return null
    }
}
