package com.ampn.inkacourierdelivery.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepositorioSesionTest {

    private lateinit var sesion: RepositorioSesion
    private val envios = DatosSimulados.crearEnvios(1_760_000_000_000L)

    @Before
    fun preparar() {
        sesion = RepositorioSesion(DatosSimulados.CREDENCIALES_DEMO)
    }

    private fun envio(codigo: String) = envios.first { it.codigo == codigo }

    @Test
    fun iniciaSesionConUsuarioOCorreo() {
        val porUsuario = sesion.iniciarSesion("cquispe", "Ruta2026")
        assertEquals(Rol.REPARTIDOR, (porUsuario as ResultadoSesion.Exitosa).usuario.rol)

        sesion.cerrarSesion()
        val porCorreo = sesion.iniciarSesion("  OPERACIONES@comercialandina.pe ", "Andina2026")
        assertEquals(Rol.REPRESENTANTE_B2B, (porCorreo as ResultadoSesion.Exitosa).usuario.rol)
        assertEquals(DatosSimulados.USUARIO_B2B, sesion.usuarioActual.value)
    }

    @Test
    fun rechazaCredencialesIncorrectasSinIniciarSesion() {
        val resultado = sesion.iniciarSesion("cquispe", "ruta2026") as ResultadoSesion.Fallida
        assertEquals("Usuario o contraseña incorrectos", resultado.errorContrasena)
        assertNull(sesion.usuarioActual.value)

        // La contraseña de un usuario no sirve para otro.
        assertTrue(sesion.iniciarSesion("andina", "Ruta2026") is ResultadoSesion.Fallida)
    }

    @Test
    fun exigeCamposObligatorios() {
        val resultado = sesion.iniciarSesion(" ", "") as ResultadoSesion.Fallida
        assertEquals("Ingrese su usuario o correo", resultado.errorUsuario)
        assertEquals("Ingrese su contraseña", resultado.errorContrasena)
    }

    @Test
    fun cerrarSesionLimpiaElUsuario() {
        sesion.iniciarSesion("cquispe", "Ruta2026")
        sesion.cerrarSesion()
        assertNull(sesion.usuarioActual.value)
    }

    @Test
    fun repartidorSoloConsultaYRegistraSusParadas() {
        val repartidor = DatosSimulados.USUARIO_REPARTIDOR
        assertTrue(repartidor.puedeConsultar(envio("INK-24011")))
        assertTrue(repartidor.puedeRegistrar(envio("INK-24011")))
        // Parada de otro repartidor y envío fuera de ruta.
        assertFalse(repartidor.puedeConsultar(envio("INK-24017")))
        assertFalse(repartidor.puedeRegistrar(envio("INK-24017")))
        assertFalse(repartidor.puedeConsultar(envio("INK-24020")))
    }

    @Test
    fun representanteB2BSoloConsultaSuEmpresaYNoRegistra() {
        val representante = DatosSimulados.USUARIO_B2B
        assertTrue(representante.puedeConsultar(envio("INK-24011")))
        assertFalse(representante.puedeConsultar(envio("INK-24012")))
        assertFalse(representante.puedeRegistrar(envio("INK-24011")))
    }
}
