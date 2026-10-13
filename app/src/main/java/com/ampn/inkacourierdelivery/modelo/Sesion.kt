package com.ampn.inkacourierdelivery.modelo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Rol(val etiqueta: String) {
    REPARTIDOR("Repartidor"),
    REPRESENTANTE_B2B("Representante B2B")
}

/** Usuario autenticado. No contiene la contraseña. */
data class Usuario(
    val id: String,
    val usuario: String,
    val correo: String,
    val nombre: String,
    val rol: Rol,
    val empresa: Empresa? = null
) {
    val primerNombre: String get() = nombre.substringBefore(' ')

    /** El repartidor consulta solo sus paradas; el representante B2B, solo los envíos de su empresa. */
    fun puedeConsultar(envio: Envio): Boolean = when (rol) {
        Rol.REPARTIDOR -> envio.repartidorId == id
        Rol.REPRESENTANTE_B2B -> envio.empresa.id == empresa?.id
    }

    /** Solo el repartidor asignado puede iniciar traslados y registrar resultados. */
    fun puedeRegistrar(envio: Envio): Boolean = rol == Rol.REPARTIDOR && envio.repartidorId == id
}

sealed interface ResultadoSesion {
    data class Exitosa(val usuario: Usuario) : ResultadoSesion
    data class Fallida(val errorUsuario: String? = null, val errorContrasena: String? = null) : ResultadoSesion
}

/**
 * Autenticación SIMULADA y en memoria, solo para demostración: compara contra
 * credenciales fijas y no aplica ninguna medida de seguridad de producción.
 */
class RepositorioSesion(private val credenciales: Map<Usuario, String>) {

    companion object {
        val compartido: RepositorioSesion by lazy { RepositorioSesion(DatosSimulados.CREDENCIALES_DEMO) }
    }

    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    /** Acepta el nombre de usuario o el correo (sin distinguir mayúsculas) y la contraseña exacta. */
    fun iniciarSesion(identificador: String, contrasena: String): ResultadoSesion {
        val id = identificador.trim()
        if (id.isEmpty() || contrasena.isEmpty()) {
            return ResultadoSesion.Fallida(
                errorUsuario = if (id.isEmpty()) "Ingrese su usuario o correo" else null,
                errorContrasena = if (contrasena.isEmpty()) "Ingrese su contraseña" else null
            )
        }
        val usuario = credenciales.entries.find { (candidato, clave) ->
            (candidato.usuario.equals(id, ignoreCase = true) || candidato.correo.equals(id, ignoreCase = true)) &&
                clave == contrasena
        }?.key ?: return ResultadoSesion.Fallida(errorContrasena = "Usuario o contraseña incorrectos")
        _usuarioActual.value = usuario
        return ResultadoSesion.Exitosa(usuario)
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
    }
}
