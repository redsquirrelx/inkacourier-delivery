package com.ampn.inkacourierdelivery.vistamodelo

import androidx.lifecycle.SavedStateHandle
import com.ampn.inkacourierdelivery.modelo.CausaVisitaFallida
import com.ampn.inkacourierdelivery.modelo.RegistroVisitaFallida
import com.ampn.inkacourierdelivery.modelo.ValidadorRegistros

/** HU-05: registro de visita fallida. La fecha y hora del intento se capturan al abrir el formulario. */
class VisitaFallidaViewModel(estadoGuardado: SavedStateHandle) : RegistroBaseViewModel(estadoGuardado) {

    companion object {
        private const val CLAVE_FECHA_HORA = "fechaHoraIntento"
    }

    val fechaHoraIntento: Long = estadoGuardado[CLAVE_FECHA_HORA]
        ?: System.currentTimeMillis().also { estadoGuardado[CLAVE_FECHA_HORA] = it }

    fun prepararRegistro(
        causa: CausaVisitaFallida?,
        observaciones: String,
        omitirFoto: Boolean,
        justificacionSinFoto: String
    ) = RegistroVisitaFallida(
        causa = causa,
        observaciones = observaciones.trim(),
        foto = if (omitirFoto) null else foto.value,
        justificacionSinFoto = if (omitirFoto) justificacionSinFoto.trim() else null,
        ubicacion = ubicacion.value,
        fechaHora = fechaHoraIntento
    )

    fun validar(registro: RegistroVisitaFallida): String? =
        validarCon { ValidadorRegistros.validarVisitaFallida(it, registro) }

    fun registrar(registro: RegistroVisitaFallida) {
        procesarResultado(
            repositorio.registrarVisitaFallida(codigo, registro),
            "Visita fallida registrada para el envío $codigo."
        )
    }
}
