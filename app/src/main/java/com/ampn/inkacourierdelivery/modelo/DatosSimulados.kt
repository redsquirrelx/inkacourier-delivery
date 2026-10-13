package com.ampn.inkacourierdelivery.modelo

/**
 * Datos de ejecución simulados. Incluye las instrucciones, cambios y autorizaciones
 * que normalmente emite el personal de oficina (no tienen interfaz propia).
 */
object DatosSimulados {

    const val ID_REPARTIDOR = "REP-001"
    private const val ID_OTRO_REPARTIDOR = "REP-002"
    const val NOMBRE_REPARTIDOR = "Carlos Quispe"

    val EMPRESA_REPRESENTADA = Empresa("EMP-001", "Comercial Andina S.A.C.")
    private val TEXTILES_DEL_SUR = Empresa("EMP-002", "Textiles del Sur S.A.")

    val USUARIO_REPARTIDOR = Usuario(
        id = ID_REPARTIDOR, usuario = "cquispe", correo = "carlos.quispe@inkacourier.pe",
        nombre = NOMBRE_REPARTIDOR, rol = Rol.REPARTIDOR
    )
    val USUARIO_B2B = Usuario(
        id = "B2B-001", usuario = "andina", correo = "operaciones@comercialandina.pe",
        nombre = "Lucía Paredes", rol = Rol.REPRESENTANTE_B2B, empresa = EMPRESA_REPRESENTADA
    )

    /** Credenciales EXCLUSIVAMENTE de demostración (usuario -> contraseña). No es autenticación segura. */
    val CREDENCIALES_DEMO: Map<Usuario, String> = mapOf(
        USUARIO_REPARTIDOR to "Ruta2026",
        USUARIO_B2B to "Andina2026"
    )

    private const val MINUTO = 60_000L
    private const val HORA = 60 * MINUTO

    fun crearEnvios(ahora: Long): List<Envio> {
        val inicioRuta = ahora - 2 * HORA
        val ayer = ahora - 24 * HORA

        fun historialRuta(extra: List<EventoEnvio> = emptyList()) = listOf(
            EventoEnvio(inicioRuta - 14 * HORA, "Envío registrado por la empresa"),
            EventoEnvio(inicioRuta, "Asignado a la ruta de $NOMBRE_REPARTIDOR")
        ) + extra

        fun cambioOficina(minutosAtras: Long, descripcion: String) =
            EventoEnvio(ahora - minutosAtras * MINUTO, "Oficina: $descripcion", TipoEvento.CAMBIO_OFICINA)

        fun historialAyer(resultado: String, horasDespues: Long) = listOf(
            EventoEnvio(ayer - 12 * HORA, "Envío registrado por la empresa"),
            EventoEnvio(ayer, "Asignado a ruta"),
            EventoEnvio(ayer + horasDespues * HORA, resultado)
        )

        return listOf(
            Envio(
                codigo = "INK-24011", empresa = EMPRESA_REPRESENTADA, ordenParada = 1, repartidorId = ID_REPARTIDOR,
                destinatario = Persona("María Fernández Rojas", TipoDocumento.DNI, "45871236"),
                telefono = "987654321",
                direccion = "Av. Arequipa 1450, Dpto. 302", distrito = "Lince",
                referencias = "Frente al parque Mariscal Castilla, edificio color crema",
                horarioAtencion = "09:00 - 13:00",
                instrucciones = "Tocar el intercomunicador 302. No dejar con el portero.",
                importe = 159.90, condicionPago = CondicionPago.PREPAGADO, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.EN_CAMINO_A_ENTREGA, ultimaActualizacion = ahora - 10 * MINUTO,
                historial = historialRuta(
                    listOf(EventoEnvio(ahora - 10 * MINUTO, "Evento operativo: traslado iniciado según hoja de ruta"))
                )
            ),
            Envio(
                codigo = "INK-24012", empresa = TEXTILES_DEL_SUR, ordenParada = 2, repartidorId = ID_REPARTIDOR,
                destinatario = Persona("Jorge Ramos Valdivia", TipoDocumento.DNI, "40125896"),
                telefono = "912345678",
                direccion = "Jr. Huiracocha 2155", distrito = "Jesús María",
                referencias = "Casa de dos pisos con portón negro",
                horarioAtencion = "10:00 - 18:00",
                instrucciones = "Pago contra entrega verificado por oficina mediante transferencia.",
                importe = 250.00, condicionPago = CondicionPago.CONTRA_ENTREGA, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false,
                autorizados = listOf(Persona("Lucía Ramos Pérez", TipoDocumento.DNI, "70451238")),
                estado = EstadoEnvio.EN_RUTA, ultimaActualizacion = inicioRuta,
                historial = historialRuta()
            ),
            Envio(
                codigo = "INK-24013", empresa = EMPRESA_REPRESENTADA, ordenParada = 3, repartidorId = ID_REPARTIDOR,
                destinatario = Persona("Rosa Huamán Quispe", TipoDocumento.DNI, "41236587"),
                telefono = "956123478",
                direccion = "Calle Los Pinos 340", distrito = "San Miguel",
                referencias = "A media cuadra de la comisaría",
                horarioAtencion = "08:00 - 12:00",
                instrucciones = "Pago contra entrega pendiente de verificación. No entregar sin verificación.",
                importe = 89.90, condicionPago = CondicionPago.CONTRA_ENTREGA, pagoVerificado = false,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.EN_RUTA, ultimaActualizacion = inicioRuta,
                historial = historialRuta()
            ),
            Envio(
                codigo = "INK-24014", empresa = EMPRESA_REPRESENTADA, ordenParada = 4, repartidorId = ID_REPARTIDOR,
                destinatario = Persona("Luis Torres Mendoza", TipoDocumento.DNI, "43659871"),
                telefono = "934567812",
                direccion = "Mz. F Lt. 12, Asoc. Las Lomas", distrito = "San Juan de Lurigancho",
                referencias = "Subiendo por la escalera azul, tercera casa",
                horarioAtencion = "14:00 - 18:00",
                instrucciones = "Zona de alto riesgo: si no es seguro fotografiar, justificar la omisión.",
                importe = 320.00, condicionPago = CondicionPago.PREPAGADO, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = true,
                autorizados = listOf(Persona("Ana Torres Mendoza", TipoDocumento.DNI, "72145863")),
                estado = EstadoEnvio.EN_RUTA, ultimaActualizacion = ahora - 40 * MINUTO,
                historial = historialRuta(
                    listOf(cambioOficina(40, "zona de alto riesgo autorizada para omitir fotografía"))
                )
            ),
            Envio(
                codigo = "INK-24015", empresa = TEXTILES_DEL_SUR, ordenParada = 5, repartidorId = ID_REPARTIDOR,
                destinatario = Persona("Carmen Díaz Salas", TipoDocumento.CE, "001234567"),
                telefono = "923456781",
                direccion = "Av. La Marina 2800, Of. 504", distrito = "San Miguel",
                referencias = "Torre B, recepción del piso 5",
                horarioAtencion = "15:00 - 19:00",
                instrucciones = "Entregar en recepción del piso 5 a la destinataria.",
                importe = 75.50, condicionPago = CondicionPago.PREPAGADO, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.EN_RUTA, ultimaActualizacion = ahora - 25 * MINUTO,
                historial = historialRuta(
                    listOf(cambioOficina(25, "horario cambiado de 09:00 - 12:00 a 15:00 - 19:00"))
                )
            ),
            Envio(
                codigo = "INK-24016", empresa = EMPRESA_REPRESENTADA, ordenParada = 6, repartidorId = ID_REPARTIDOR,
                destinatario = Persona("Pedro Salazar Ríos", TipoDocumento.DNI, "46987123"),
                telefono = "945612378",
                direccion = "Jr. Ica 455, Int. 3", distrito = "Cercado de Lima",
                referencias = "Galería comercial, puesto 3",
                horarioAtencion = "10:00 - 17:00",
                instrucciones = "Puede recibir su hermano, autorizado por oficina. Llamar antes de llegar.",
                importe = 210.00, condicionPago = CondicionPago.CONTRA_ENTREGA, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false,
                autorizados = listOf(Persona("Raúl Salazar Ríos", TipoDocumento.DNI, "47123698")),
                estado = EstadoEnvio.EN_RUTA, ultimaActualizacion = ahora - 15 * MINUTO,
                historial = historialRuta(
                    listOf(
                        cambioOficina(30, "pago contra entrega verificado"),
                        cambioOficina(15, "se autoriza a Raúl Salazar Ríos como receptor")
                    )
                )
            ),
            // Parada de otro repartidor: no debe aparecer en la ruta de Carlos Quispe.
            Envio(
                codigo = "INK-24017", empresa = EMPRESA_REPRESENTADA, ordenParada = 1, repartidorId = ID_OTRO_REPARTIDOR,
                destinatario = Persona("Gabriela Ruiz Campos", TipoDocumento.DNI, "45678123"),
                telefono = "951357246",
                direccion = "Av. Primavera 1230", distrito = "Santiago de Surco",
                referencias = "Edificio Prisma, Dpto. 1204",
                horarioAtencion = "09:00 - 18:00", instrucciones = "Dejar con la destinataria",
                importe = 145.00, condicionPago = CondicionPago.PREPAGADO, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.EN_RUTA, ultimaActualizacion = inicioRuta,
                historial = listOf(
                    EventoEnvio(inicioRuta - 14 * HORA, "Envío registrado por la empresa"),
                    EventoEnvio(inicioRuta, "Asignado a ruta")
                )
            ),
            // Envíos fuera de la ruta actual: solo aparecen en la consulta B2B.
            Envio(
                codigo = "INK-23990", empresa = EMPRESA_REPRESENTADA, ordenParada = null,
                destinatario = Persona("Sofía Castillo León", TipoDocumento.DNI, "48521367"),
                telefono = "961234587",
                direccion = "Av. Brasil 2950, Dpto. 801", distrito = "Magdalena del Mar",
                referencias = "Edificio Mirador",
                horarioAtencion = "09:00 - 18:00", instrucciones = "Ninguna",
                importe = 129.00, condicionPago = CondicionPago.PREPAGADO, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.ENTREGADO, ultimaActualizacion = ayer + 3 * HORA,
                historial = historialAyer("Entregado a Sofía Castillo León (titular)", 3)
            ),
            Envio(
                codigo = "INK-23991", empresa = EMPRESA_REPRESENTADA, ordenParada = null,
                destinatario = Persona("Miguel Paredes Soto", TipoDocumento.DNI, "44789652"),
                telefono = "978456123",
                direccion = "Calle Las Begonias 120", distrito = "San Isidro",
                referencias = "Cerca al óvalo Gutiérrez",
                horarioAtencion = "09:00 - 13:00", instrucciones = "Ninguna",
                importe = 54.00, condicionPago = CondicionPago.PREPAGADO, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.NO_VISITADO, ultimaActualizacion = ayer + 10 * HORA,
                historial = historialAyer("Parada no visitada al cierre de la ruta", 10)
            ),
            Envio(
                codigo = "INK-23992", empresa = EMPRESA_REPRESENTADA, ordenParada = null,
                destinatario = Persona("Elena Vargas Chávez", TipoDocumento.DNI, "42369785"),
                telefono = "990123456",
                direccion = "Av. Universitaria 1801", distrito = "San Martín de Porres",
                referencias = "Frente a la universidad",
                horarioAtencion = "09:00 - 18:00", instrucciones = "Ninguna",
                importe = 99.00, condicionPago = CondicionPago.PREPAGADO, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.VISITA_FALLIDA, ultimaActualizacion = ayer + 5 * HORA,
                historial = historialAyer("Visita fallida: Destinatario ausente", 5),
                visitaFallida = RegistroVisitaFallida(
                    causa = CausaVisitaFallida.DESTINATARIO_AUSENTE,
                    observaciones = "Se tocó el timbre tres veces y nadie respondió",
                    foto = null, justificacionSinFoto = null,
                    ubicacion = Ubicacion(-12.0189, -77.0832), fechaHora = ayer + 5 * HORA
                )
            ),
            Envio(
                codigo = "INK-24020", empresa = EMPRESA_REPRESENTADA, ordenParada = null,
                destinatario = Persona("Andrea Flores Núñez", TipoDocumento.DNI, "73215896"),
                telefono = "915478236",
                direccion = "Av. Javier Prado Este 4200", distrito = "Santiago de Surco",
                referencias = "Condominio Los Álamos, torre 2",
                horarioAtencion = "09:00 - 18:00", instrucciones = "Ninguna",
                importe = 189.00, condicionPago = CondicionPago.PREPAGADO, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.PENDIENTE, ultimaActualizacion = ahora - 5 * HORA,
                historial = listOf(EventoEnvio(ahora - 5 * HORA, "Envío registrado por la empresa"))
            ),
            Envio(
                codigo = "INK-24021", empresa = EMPRESA_REPRESENTADA, ordenParada = null,
                destinatario = Persona("Kevin Ortiz Lazo", TipoDocumento.CE, "002345678"),
                telefono = "927845163",
                direccion = "Calle Schell 343", distrito = "Miraflores",
                referencias = "Hotel, recepción principal",
                horarioAtencion = "24 horas", instrucciones = "Ninguna",
                importe = 64.50, condicionPago = CondicionPago.CONTRA_ENTREGA, pagoVerificado = false,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.PENDIENTE, ultimaActualizacion = ahora - 3 * HORA,
                historial = listOf(EventoEnvio(ahora - 3 * HORA, "Envío registrado por la empresa"))
            ),
            Envio(
                codigo = "INK-24030", empresa = TEXTILES_DEL_SUR, ordenParada = null,
                destinatario = Persona("Diana Rojas Pinto", TipoDocumento.DNI, "45123698"),
                telefono = "936521478",
                direccion = "Av. Angamos Este 1500", distrito = "Surquillo",
                referencias = "Al costado del mercado",
                horarioAtencion = "09:00 - 18:00", instrucciones = "Ninguna",
                importe = 110.00, condicionPago = CondicionPago.PREPAGADO, pagoVerificado = true,
                zonaAltoRiesgoAutorizada = false, autorizados = emptyList(),
                estado = EstadoEnvio.PENDIENTE, ultimaActualizacion = ahora - 4 * HORA,
                historial = listOf(EventoEnvio(ahora - 4 * HORA, "Envío registrado por la empresa"))
            )
        )
    }
}
