package com.ampn.inkacourierdelivery.vistas

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ampn.inkacourierdelivery.MainActivity
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.ItemAccesoBinding
import com.ampn.inkacourierdelivery.databinding.ItemEnvioBinding
import com.ampn.inkacourierdelivery.databinding.ItemIndicadorBinding
import com.ampn.inkacourierdelivery.modelo.CondicionPago
import com.ampn.inkacourierdelivery.modelo.Envio
import com.ampn.inkacourierdelivery.modelo.EstadoEnvio
import com.ampn.inkacourierdelivery.vistamodelo.EventoRegistro
import com.ampn.inkacourierdelivery.vistamodelo.RegistroBaseViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LOCALE_PERU: Locale = Locale.forLanguageTag("es-PE")
private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", LOCALE_PERU)
private val formatoFechaLarga = SimpleDateFormat("EEEE d 'de' MMMM", LOCALE_PERU)

/** Opacidad del fondo suave de insignias e indicadores (≈14 %). */
private const val ALFA_FONDO_SUAVE = 0x24

fun formatearFechaHora(milisegundos: Long): String = formatoFechaHora.format(Date(milisegundos))

fun formatearFechaLarga(milisegundos: Long): String =
    formatoFechaLarga.format(Date(milisegundos)).replaceFirstChar { it.titlecase(LOCALE_PERU) }

fun formatearImporte(importe: Double): String = String.format(Locale.US, "S/ %.2f", importe)

fun Envio.descripcionPago(contexto: Context): String {
    val importeTexto = formatearImporte(importe)
    return when {
        condicionPago == CondicionPago.PREPAGADO -> contexto.getString(R.string.pago_prepagado, importeTexto)
        pagoVerificado -> contexto.getString(R.string.pago_verificado, importeTexto)
        else -> contexto.getString(R.string.pago_no_verificado, importeTexto)
    }
}

@ColorRes
fun EstadoEnvio.colorRes(): Int = when (this) {
    EstadoEnvio.PENDIENTE -> R.color.estado_pendiente
    EstadoEnvio.EN_RUTA -> R.color.estado_en_ruta
    EstadoEnvio.EN_CAMINO_A_ENTREGA -> R.color.estado_en_camino
    EstadoEnvio.ENTREGADO -> R.color.estado_entregado
    EstadoEnvio.VISITA_FALLIDA -> R.color.estado_visita_fallida
    EstadoEnvio.NO_VISITADO -> R.color.estado_no_visitado
}

private fun colorSuave(color: Int) = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, ALFA_FONDO_SUAVE))

/** Insignia de estado: texto y punto del color del estado sobre un fondo suave del mismo tono. */
fun TextView.mostrarEstado(estado: EstadoEnvio) {
    val color = ContextCompat.getColor(context, estado.colorRes())
    text = estado.etiqueta
    setTextColor(color)
    backgroundTintList = colorSuave(color)
    setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.punto_estado, 0, 0, 0)
    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(color))
}

fun ItemIndicadorBinding.mostrar(valor: Int, etiqueta: String, @ColorRes color: Int) {
    val colorEntero = ContextCompat.getColor(root.context, color)
    textoValor.text = String.format(Locale.getDefault(), "%d", valor)
    textoValor.setTextColor(colorEntero)
    textoEtiqueta.text = etiqueta
    root.backgroundTintList = colorSuave(colorEntero)
}

fun ItemAccesoBinding.configurar(@DrawableRes icono: Int, titulo: String, detalle: String, alPulsar: () -> Unit) {
    imagenIcono.setImageResource(icono)
    textoTitulo.text = titulo
    textoDetalle.text = detalle
    root.setOnClickListener { alPulsar() }
}

/** Tarjeta de envío reutilizada por las listas (HU-01, HU-07) y por la pantalla de inicio. */
fun ItemEnvioBinding.mostrarEnvio(envio: Envio, esRuta: Boolean, alSeleccionar: (Envio) -> Unit) {
    val contexto = root.context
    franjaEstado.setBackgroundColor(ContextCompat.getColor(contexto, envio.estado.colorRes()))
    textoEstado.mostrarEstado(envio.estado)
    textoDestinatario.text = envio.destinatario.nombre
    textoDireccion.text = contexto.getString(R.string.formato_direccion, envio.direccion, envio.distrito)
    if (esRuta) {
        textoEncabezado.text = contexto.getString(R.string.formato_parada, envio.ordenParada ?: 0, envio.codigo)
        textoComplemento.text = contexto.getString(
            R.string.formato_separado, envio.horarioAtencion, envio.descripcionPago(contexto)
        )
        textoCambioOficina.isVisible = envio.tieneCambiosOficina && envio.estado.atendible
    } else {
        textoEncabezado.text = envio.codigo
        textoComplemento.text = contexto.getString(
            R.string.formato_ultima_actualizacion, formatearFechaHora(envio.ultimaActualizacion)
        )
        textoCambioOficina.isVisible = false
    }
    root.setOnClickListener { alSeleccionar(envio) }
}

fun Bitmap.comoBytes(formato: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): ByteArray =
    ByteArrayOutputStream().also { compress(formato, 85, it) }.toByteArray()

fun ByteArray.comoBitmap(): Bitmap? = BitmapFactory.decodeByteArray(this, 0, size)

/** Recolecta un Flow mientras la vista del fragmento esté al menos en STARTED. */
fun <T> Fragment.recolectar(flujo: Flow<T>, accion: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flujo.collect { accion(it) }
        }
    }
}

fun Fragment.navegarA(fragmento: Fragment) {
    (requireActivity() as MainActivity).navegarA(fragmento)
}

/** Cambia de sección en la navegación inferior mostrando un fragmento concreto (por ejemplo, con filtro). */
fun Fragment.abrirSeccion(idSeccion: Int, fragmento: Fragment) {
    (requireActivity() as MainActivity).abrirSeccion(idSeccion, fragmento)
}

fun Fragment.establecerTitulo(titulo: String) {
    requireActivity().title = titulo
}

fun Fragment.mostrarMensaje(mensaje: String) {
    val activity = requireActivity() as MainActivity
    Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG)
        .setAnchorView(activity.anclaMensajes())
        .show()
}

/** Valida, pide confirmación y registra. Usado por los formularios de HU-03 y HU-05. */
fun Fragment.confirmarRegistro(error: String?, titulo: String, resumen: String, alConfirmar: () -> Unit) {
    if (error != null) {
        mostrarMensaje(error)
        return
    }
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(titulo)
        .setMessage(resumen)
        .setNegativeButton(R.string.accion_cancelar, null)
        .setPositiveButton(R.string.accion_confirmar) { _, _ -> alConfirmar() }
        .show()
}

/** Muestra errores y, al registrar con éxito, vuelve al detalle del envío. */
fun Fragment.observarEventosRegistro(viewModel: RegistroBaseViewModel) {
    recolectar(viewModel.eventos) { evento ->
        when (evento) {
            is EventoRegistro.Error -> mostrarMensaje(evento.mensaje)
            is EventoRegistro.Registrado -> {
                Toast.makeText(requireContext(), evento.mensaje, Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            }
        }
    }
}
