package com.ampn.inkacourierdelivery.vistamodelo

import com.ampn.inkacourierdelivery.modelo.Envio
import com.ampn.inkacourierdelivery.modelo.RepositorioEnvios
import com.ampn.inkacourierdelivery.modelo.RepositorioSesion
import com.ampn.inkacourierdelivery.modelo.Rol
import com.ampn.inkacourierdelivery.modelo.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Accesos a datos limitados por el perfil de la sesión simulada activa. */
internal fun usuarioEnSesion(): Usuario? = RepositorioSesion.compartido.usuarioActual.value

internal fun paradasDelRepartidor(): Flow<List<Envio>> {
    val usuario = usuarioEnSesion()?.takeIf { it.rol == Rol.REPARTIDOR } ?: return flowOf(emptyList())
    return RepositorioEnvios.compartido.paradasDeRuta(usuario.id)
}

internal fun enviosDeLaEmpresa(): Flow<List<Envio>> {
    val empresa = usuarioEnSesion()?.takeIf { it.rol == Rol.REPRESENTANTE_B2B }?.empresa
        ?: return flowOf(emptyList())
    return RepositorioEnvios.compartido.enviosDeEmpresa(empresa.id)
}
