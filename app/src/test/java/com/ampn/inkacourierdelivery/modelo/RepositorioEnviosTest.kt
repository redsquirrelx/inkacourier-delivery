package com.ampn.inkacourierdelivery.modelo

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepositorioEnviosTest {

    private val ahora = 1_760_000_000_000L
    private val foto = byteArrayOf(1)
    private val firma = byteArrayOf(2)
    private val gps = Ubicacion(-12.08, -77.03)
    private lateinit var repositorio: RepositorioEnvios

    @Before
    fun preparar() {
        repositorio = RepositorioEnvios(DatosSimulados.crearEnvios(ahora))
    }

    private fun entrega(
        receptor: Persona,
        tipo: TipoReceptor = TipoReceptor.TITULAR,
        foto: ByteArray? = this.foto,
        justificacion: String? = null,
        firma: ByteArray? = this.firma,
        ubicacion: Ubicacion? = gps
    ) = RegistroEntrega(tipo, receptor, firma, foto, justificacion, ubicacion, ahora)

    private fun visita(
        causa: CausaVisitaFallida?,
        observaciones: String = "Se tocó la puerta varias veces",
        foto: ByteArray? = this.foto,
        justificacion: String? = null
    ) = RegistroVisitaFallida(causa, observaciones, foto, justificacion, gps, ahora)

    private fun titular(codigo: String) = repositorio.buscar(codigo)!!.destinatario

    private fun error(resultado: ResultadoRegistro) = (resultado as ResultadoRegistro.Fallido).mensaje

    @Test
    fun entregaDelTitularMarcaEntregadoSinAvanzarLaRuta() {
        val resultado = repositorio.registrarEntrega("INK-24011", entrega(titular("INK-24011")))

        assertEquals(ResultadoRegistro.Exitoso, resultado)
        val envio = repositorio.buscar("INK-24011")!!
        assertEquals(EstadoEnvio.ENTREGADO, envio.estado)
        assertNotNull(envio.entrega)
        assertEquals(ahora, envio.ultimaActualizacion)
        // La siguiente parada no cambia sola: el traslado se inicia intencionalmente.
        assertEquals(EstadoEnvio.EN_RUTA, repositorio.buscar("INK-24012")!!.estado)
    }

    @Test
    fun iniciarTrasladoSoloUnaParadaALaVez() {
        val conOtraEnCamino = repositorio.iniciarTraslado("INK-24012", ahora)
        assertTrue(error(conOtraEnCamino).contains("INK-24011"))

        repositorio.registrarEntrega("INK-24011", entrega(titular("INK-24011")))
        assertEquals(ResultadoRegistro.Exitoso, repositorio.iniciarTraslado("INK-24012", ahora))
        assertEquals(EstadoEnvio.EN_CAMINO_A_ENTREGA, repositorio.buscar("INK-24012")!!.estado)
    }

    @Test
    fun noSeIniciaTrasladoDeParadaAtendidaOPendiente() {
        assertTrue(error(repositorio.iniciarTraslado("INK-24020", ahora)).contains("En ruta"))
        repositorio.registrarEntrega("INK-24011", entrega(titular("INK-24011")))
        assertTrue(error(repositorio.iniciarTraslado("INK-24011", ahora)).contains("En ruta"))
    }

    @Test
    fun documentoDistintoAlTitularEsRechazado() {
        val impostor = Persona("María Fernández Rojas", TipoDocumento.DNI, "11111111")
        val resultado = repositorio.registrarEntrega("INK-24011", entrega(impostor))

        assertTrue(error(resultado).contains("titular"))
        assertEquals(EstadoEnvio.EN_CAMINO_A_ENTREGA, repositorio.buscar("INK-24011")!!.estado)
    }

    @Test
    fun terceroAutorizadoPorOficinaPuedeRecibir() {
        val hermano = Persona("Raúl Salazar Ríos", TipoDocumento.DNI, "47123698")
        val resultado = repositorio.registrarEntrega("INK-24016", entrega(hermano, TipoReceptor.TERCERO_AUTORIZADO))

        assertEquals(ResultadoRegistro.Exitoso, resultado)
    }

    @Test
    fun terceroNoAutorizadoEsRechazado() {
        val desconocido = Persona("Juan Pérez", TipoDocumento.DNI, "12345678")
        val resultado = repositorio.registrarEntrega("INK-24016", entrega(desconocido, TipoReceptor.TERCERO_AUTORIZADO))

        assertTrue(error(resultado).contains("autorizado"))
    }

    @Test
    fun pagoNoVerificadoBloqueaLaEntregaPeroPermiteVisitaFallida() {
        val resultadoEntrega = repositorio.registrarEntrega("INK-24013", entrega(titular("INK-24013")))
        assertTrue(error(resultadoEntrega).contains("pago"))

        val resultadoVisita = repositorio.registrarVisitaFallida("INK-24013", visita(CausaVisitaFallida.PAGO_NO_VERIFICADO))
        assertEquals(ResultadoRegistro.Exitoso, resultadoVisita)
        val envio = repositorio.buscar("INK-24013")!!
        assertEquals(EstadoEnvio.VISITA_FALLIDA, envio.estado)
        assertEquals(CausaVisitaFallida.PAGO_NO_VERIFICADO, envio.visitaFallida?.causa)
    }

    @Test
    fun causaPagoNoVerificadoNoAplicaAEnviosPrepagados() {
        val resultado = repositorio.registrarVisitaFallida("INK-24011", visita(CausaVisitaFallida.PAGO_NO_VERIFICADO))
        assertTrue(error(resultado).contains("prepagado"))
    }

    @Test
    fun zonaDeAltoRiesgoPermiteOmitirFotoConJustificacion() {
        val sinJustificacion = repositorio.registrarEntrega("INK-24014", entrega(titular("INK-24014"), foto = null))
        assertTrue(error(sinJustificacion).contains("Justifique"))

        val conJustificacion = repositorio.registrarEntrega(
            "INK-24014",
            entrega(titular("INK-24014"), foto = null, justificacion = "Grupo sospechoso cerca del domicilio")
        )
        assertEquals(ResultadoRegistro.Exitoso, conJustificacion)
    }

    @Test
    fun zonaNormalExigeFotografia() {
        val resultado = repositorio.registrarVisitaFallida(
            "INK-24012",
            visita(CausaVisitaFallida.DESTINATARIO_AUSENTE, foto = null, justificacion = "Justificación suficientemente larga")
        )
        assertEquals("La fotografía es obligatoria", error(resultado))
    }

    @Test
    fun exigeFirmaYUbicacion() {
        val sinFirma = repositorio.registrarEntrega("INK-24011", entrega(titular("INK-24011"), firma = null))
        assertEquals("Falta la firma del receptor", error(sinFirma))

        val sinGps = repositorio.registrarEntrega("INK-24011", entrega(titular("INK-24011"), ubicacion = null))
        assertEquals("Capture la ubicación GPS", error(sinGps))
    }

    @Test
    fun visitaFallidaExigeCausaYObservaciones() {
        assertTrue(error(repositorio.registrarVisitaFallida("INK-24012", visita(null))).contains("causa"))
        assertTrue(
            error(repositorio.registrarVisitaFallida("INK-24012", visita(CausaVisitaFallida.RECEPCION_RECHAZADA, "corta")))
                .contains("observaciones")
        )
    }

    @Test
    fun paradaAtendidaOFueraDeRutaNoAdmiteRegistros() {
        repositorio.registrarVisitaFallida("INK-24012", visita(CausaVisitaFallida.DESTINATARIO_AUSENTE))
        val segundoRegistro = repositorio.registrarEntrega("INK-24012", entrega(titular("INK-24012")))
        assertTrue(error(segundoRegistro).contains("Visita fallida"))

        val pendiente = repositorio.registrarEntrega("INK-24020", entrega(titular("INK-24020")))
        assertTrue(error(pendiente).contains("Pendiente"))
    }

    @Test
    fun validaFormatoDeDocumentos() {
        assertNotNull(ValidadorRegistros.validarDocumento(TipoDocumento.DNI, "1234567"))
        assertNotNull(ValidadorRegistros.validarDocumento(TipoDocumento.DNI, "1234567A"))
        assertEquals(null, ValidadorRegistros.validarDocumento(TipoDocumento.DNI, "12345678"))
        assertNotNull(ValidadorRegistros.validarDocumento(TipoDocumento.CE, "12345678"))
        assertEquals(null, ValidadorRegistros.validarDocumento(TipoDocumento.CE, "001234567"))
    }

    @Test
    fun consultaB2BSoloMuestraLaEmpresaYReflejaLosRegistros() = runBlocking {
        val empresaId = DatosSimulados.EMPRESA_REPRESENTADA.id
        val antes = repositorio.enviosDeEmpresa(empresaId).first()
        assertTrue(antes.all { it.empresa.id == empresaId })
        assertTrue(antes.none { it.codigo == "INK-24012" })

        repositorio.registrarEntrega("INK-24011", entrega(titular("INK-24011")))

        val despues = repositorio.enviosDeEmpresa(empresaId).first()
        assertEquals(EstadoEnvio.ENTREGADO, despues.first { it.codigo == "INK-24011" }.estado)
    }

    @Test
    fun paradasDeRutaSoloDelRepartidorYOrdenadas() = runBlocking {
        val paradas = repositorio.paradasDeRuta(DatosSimulados.ID_REPARTIDOR).first()
        assertEquals((1..6).toList(), paradas.map { it.ordenParada })
        // La parada INK-24017 pertenece a otro repartidor.
        assertTrue(paradas.none { it.codigo == "INK-24017" })
    }
}
