package com.ampn.inkacourierdelivery.vistamodelo

import androidx.lifecycle.SavedStateHandle
import com.ampn.inkacourierdelivery.modelo.Persona
import com.ampn.inkacourierdelivery.modelo.RegistroEntrega
import com.ampn.inkacourierdelivery.modelo.TipoDocumento
import com.ampn.inkacourierdelivery.modelo.TipoReceptor
import com.ampn.inkacourierdelivery.modelo.ValidadorRegistros

/** HU-03: registro de entrega. */
class RegistroEntregaViewModel(estadoGuardado: SavedStateHandle) : RegistroBaseViewModel(estadoGuardado) {

    fun prepararRegistro(
        tipoReceptor: TipoReceptor,
        tipoDocumento: TipoDocumento,
        numeroDocumento: String,
        nombre: String,
        firma: ByteArray?,
        omitirFoto: Boolean,
        justificacionSinFoto: String
    ) = RegistroEntrega(
        tipoReceptor = tipoReceptor,
        receptor = Persona(nombre.trim(), tipoDocumento, numeroDocumento.trim()),
        firma = firma,
        foto = if (omitirFoto) null else foto.value,
        justificacionSinFoto = if (omitirFoto) justificacionSinFoto.trim() else null,
        ubicacion = ubicacion.value,
        fechaHora = System.currentTimeMillis()
    )

    fun validar(registro: RegistroEntrega): String? =
        validarCon { ValidadorRegistros.validarEntrega(it, registro) }

    fun registrar(registro: RegistroEntrega) {
        procesarResultado(
            repositorio.registrarEntrega(codigo, registro.copy(fechaHora = System.currentTimeMillis())),
            "Entrega registrada. El envío $codigo quedó como Entregado."
        )
    }
}
