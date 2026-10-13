package com.ampn.inkacourierdelivery.vistas

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.FragmentDetalleEnvioBinding
import com.ampn.inkacourierdelivery.modelo.Envio
import com.ampn.inkacourierdelivery.modelo.EstadoEnvio
import com.ampn.inkacourierdelivery.modelo.TipoEvento
import com.ampn.inkacourierdelivery.vistamodelo.DetalleEnvioViewModel

/**
 * Detalle de un envío. Para el repartidor muestra las acciones de traslado, HU-03 y HU-05;
 * para el representante B2B (HU-07) es de solo lectura.
 */
class DetalleEnvioFragment : Fragment() {

    companion object {
        fun nuevaInstancia(codigo: String) = DetalleEnvioFragment().apply {
            arguments = bundleOf(DetalleEnvioViewModel.ARG_CODIGO to codigo)
        }
    }

    private var _binding: FragmentDetalleEnvioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetalleEnvioViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleEnvioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        establecerTitulo(
            getString(if (viewModel.esModoEmpresa) R.string.titulo_detalle_envio else R.string.titulo_detalle_parada)
        )
        recolectar(viewModel.envio) { envio ->
            binding.contenido.isVisible = envio != null
            binding.textoNoDisponible.isVisible = envio == null
            if (envio != null) mostrarEnvio(envio)
        }
        recolectar(viewModel.mensajes) { mostrarMensaje(it) }
    }

    private fun mostrarEnvio(envio: Envio) = with(binding) {
        textoCodigo.text = envio.ordenParada
            ?.takeUnless { viewModel.esModoEmpresa }
            ?.let { getString(R.string.formato_parada, it, envio.codigo) } ?: envio.codigo
        franjaEstado.setBackgroundColor(ContextCompat.getColor(requireContext(), envio.estado.colorRes()))
        textoEstado.mostrarEstado(envio.estado)
        textoEmpresa.text = envio.empresa.razonSocial
        textoUltimaActualizacion.text =
            getString(R.string.formato_ultima_actualizacion, formatearFechaHora(envio.ultimaActualizacion))

        val destinatario = envio.destinatario
        textoDestinatario.text = destinatario.nombre
        textoDocumento.text =
            getString(R.string.formato_documento, destinatario.tipoDocumento.etiqueta, destinatario.numeroDocumento)
        textoTelefono.text = envio.telefono
        textoTelefono.setOnClickListener { marcarTelefono(envio.telefono) }
        textoDireccion.text = getString(R.string.formato_direccion, envio.direccion, envio.distrito)
        textoReferencias.text = getString(R.string.formato_referencias, envio.referencias)
        textoHorario.text = getString(R.string.formato_horario, envio.horarioAtencion)
        textoPago.text = envio.descripcionPago(requireContext())
        textoInstrucciones.text = envio.instrucciones
        textoAutorizaciones.text = describirAutorizaciones(envio)
        textoHistorial.text = envio.historial.sortedByDescending { it.fechaHora }.joinToString("\n") {
            val formato = if (it.tipo == TipoEvento.CAMBIO_OFICINA) R.string.formato_evento_oficina else R.string.formato_evento
            getString(formato, formatearFechaHora(it.fechaHora), it.descripcion)
        }

        mostrarResultado(envio)

        grupoAcciones.isVisible = !viewModel.esModoEmpresa && envio.enRuta && envio.estado.atendible
        botonIniciarTraslado.isVisible = envio.estado == EstadoEnvio.EN_RUTA
        botonIniciarTraslado.setOnClickListener { viewModel.iniciarTraslado() }
        botonRegistrarEntrega.isEnabled = !envio.pagoPendiente
        botonRegistrarEntrega.setText(
            if (envio.pagoPendiente) R.string.accion_entrega_bloqueada else R.string.accion_registrar_entrega
        )
        botonRegistrarEntrega.setOnClickListener { navegarA(RegistroEntregaFragment.nuevaInstancia(envio.codigo)) }
        botonRegistrarVisitaFallida.setOnClickListener { navegarA(VisitaFallidaFragment.nuevaInstancia(envio.codigo)) }
    }

    private fun describirAutorizaciones(envio: Envio): String {
        val foto = getString(
            if (envio.zonaAltoRiesgoAutorizada) R.string.autorizacion_zona_riesgo else R.string.autorizacion_foto_obligatoria
        )
        val terceros = if (envio.autorizados.isEmpty()) {
            getString(R.string.autorizacion_sin_terceros)
        } else {
            getString(
                R.string.autorizacion_terceros,
                envio.autorizados.joinToString {
                    getString(R.string.formato_autorizado, it.nombre, it.tipoDocumento.name, it.numeroDocumento)
                }
            )
        }
        return listOf(foto, terceros).joinToString("\n")
    }

    private fun mostrarResultado(envio: Envio) = with(binding) {
        val entrega = envio.entrega
        val visita = envio.visitaFallida
        val lineas = mutableListOf<String>()
        when {
            entrega != null -> {
                lineas += getString(
                    R.string.resultado_entrega, formatearFechaHora(entrega.fechaHora), entrega.tipoReceptor.etiqueta,
                    entrega.receptor.nombre, entrega.receptor.tipoDocumento.etiqueta, entrega.receptor.numeroDocumento
                )
                entrega.ubicacion?.let { lineas += getString(R.string.resultado_gps, it.comoTexto()) }
                entrega.justificacionSinFoto?.let { lineas += getString(R.string.resultado_sin_foto_riesgo, it) }
            }
            envio.estado == EstadoEnvio.VISITA_FALLIDA && visita != null -> {
                lineas += getString(
                    R.string.resultado_visita, visita.causa?.etiqueta.orEmpty(),
                    formatearFechaHora(visita.fechaHora), visita.observaciones
                )
                visita.ubicacion?.let { lineas += getString(R.string.resultado_gps, it.comoTexto()) }
                visita.justificacionSinFoto?.let { lineas += getString(R.string.resultado_sin_foto_insegura, it) }
            }
            envio.estado == EstadoEnvio.ENTREGADO ->
                lineas += getString(R.string.resultado_entregado_simple, formatearFechaHora(envio.ultimaActualizacion))
            envio.estado == EstadoEnvio.NO_VISITADO ->
                lineas += getString(R.string.resultado_no_visitado)
        }
        grupoResultado.isVisible = lineas.isNotEmpty()
        textoResultado.text = lineas.joinToString("\n")
        tituloResultado.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (envio.estado == EstadoEnvio.ENTREGADO) R.drawable.ic_completado else R.drawable.ic_fallido, 0, 0, 0
        )
        mostrarImagen(tituloFoto, imagenFoto, entrega?.foto ?: visita?.foto)
        mostrarImagen(tituloFirma, imagenFirma, entrega?.firma)
    }

    private fun mostrarImagen(titulo: TextView, imagen: ImageView, bytes: ByteArray?) {
        val mapa = bytes?.comoBitmap()
        titulo.isVisible = mapa != null
        imagen.isVisible = mapa != null
        imagen.setImageBitmap(mapa)
    }

    private fun marcarTelefono(telefono: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, "tel:$telefono".toUri()))
        } catch (_: ActivityNotFoundException) {
            mostrarMensaje(getString(R.string.sin_aplicacion_telefono))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
