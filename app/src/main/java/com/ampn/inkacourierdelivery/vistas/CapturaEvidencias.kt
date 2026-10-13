package com.ampn.inkacourierdelivery.vistas

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.SeccionEvidenciasBinding
import com.ampn.inkacourierdelivery.modelo.Ubicacion
import com.ampn.inkacourierdelivery.vistamodelo.RegistroBaseViewModel
import kotlinx.coroutines.flow.combine

/**
 * Captura de fotografía y GPS reutilizada por los formularios de entrega y visita fallida.
 * Debe crearse como propiedad del fragmento para registrar los launchers antes de onCreate.
 */
class CapturaEvidencias(
    private val fragmento: Fragment,
    private val obtenerViewModel: () -> RegistroBaseViewModel
) {

    companion object {
        private const val TAG = "CapturaEvidencias"
        private val PERMISOS_UBICACION = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private val lanzadorFoto = fragmento.registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { imagen ->
        if (imagen != null) obtenerViewModel().establecerFoto(imagen.comoBytes())
        else Log.d(TAG, "Captura de fotografía cancelada")
    }

    private val lanzadorPermisos = fragmento.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) solicitarUbicacion()
        else obtenerViewModel().ubicacionNoDisponible("Se necesita el permiso de ubicación para registrar el GPS")
    }

    /** Conecta la sección de evidencias del layout con el ViewModel. Llamar en onViewCreated. */
    fun vincular(binding: SeccionEvidenciasBinding) {
        val viewModel = obtenerViewModel()
        binding.botonTomarFoto.setOnClickListener { tomarFoto() }
        binding.botonCapturarUbicacion.setOnClickListener { capturarUbicacion() }
        binding.interruptorOmitirFoto.setOnCheckedChangeListener { _, omitir ->
            actualizarOmision(binding, omitir)
        }
        actualizarOmision(binding, binding.interruptorOmitirFoto.isChecked)

        fragmento.recolectar(viewModel.envio) { envio ->
            binding.grupoZonaRiesgo.isVisible = envio?.zonaAltoRiesgoAutorizada == true
        }
        fragmento.recolectar(viewModel.foto) { foto ->
            binding.imagenFoto.setImageBitmap(foto?.comoBitmap())
            binding.botonTomarFoto.setText(if (foto == null) R.string.accion_tomar_foto else R.string.accion_repetir_foto)
        }
        fragmento.recolectar(combine(viewModel.ubicacion, viewModel.buscandoUbicacion, ::Pair)) { (ubicacion, buscando) ->
            binding.botonCapturarUbicacion.isEnabled = !buscando
            binding.textoUbicacion.text = when {
                buscando -> fragmento.getString(R.string.gps_buscando)
                ubicacion != null -> fragmento.getString(R.string.resultado_gps, ubicacion.comoTexto())
                else -> fragmento.getString(R.string.gps_sin_capturar)
            }
        }
    }

    fun omitirFoto(binding: SeccionEvidenciasBinding): Boolean =
        binding.grupoZonaRiesgo.isVisible && binding.interruptorOmitirFoto.isChecked

    fun justificacion(binding: SeccionEvidenciasBinding): String =
        binding.campoJustificacion.text?.toString().orEmpty()

    private fun actualizarOmision(binding: SeccionEvidenciasBinding, omitir: Boolean) {
        binding.contenedorJustificacion.isVisible = omitir
        binding.botonTomarFoto.isEnabled = !omitir
        binding.imagenFoto.alpha = if (omitir) 0.3f else 1f
    }

    private fun tomarFoto() {
        try {
            lanzadorFoto.launch(null)
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "No hay aplicación de cámara", error)
            fragmento.mostrarMensaje("No hay una aplicación de cámara disponible")
        }
    }

    private fun capturarUbicacion() {
        val contexto = fragmento.requireContext()
        val concedido = PERMISOS_UBICACION.any {
            ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
        }
        if (concedido) solicitarUbicacion() else lanzadorPermisos.launch(PERMISOS_UBICACION)
    }

    @SuppressLint("MissingPermission")
    private fun solicitarUbicacion() {
        val viewModel = obtenerViewModel()
        val contexto = fragmento.requireContext()
        val gestor = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val proveedor = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { gestor.isProviderEnabled(it) }
        if (proveedor == null) {
            viewModel.ubicacionNoDisponible("Active la ubicación (GPS) del dispositivo")
            return
        }
        viewModel.iniciarBusquedaUbicacion()
        LocationManagerCompat.getCurrentLocation(
            gestor, proveedor, CancellationSignal(), ContextCompat.getMainExecutor(contexto)
        ) { ubicacion ->
            val resultado = ubicacion ?: gestor.getLastKnownLocation(proveedor)
            if (resultado != null) {
                Log.d(TAG, "Ubicación obtenida con $proveedor")
                viewModel.establecerUbicacion(Ubicacion(resultado.latitude, resultado.longitude))
            } else {
                viewModel.ubicacionNoDisponible("No se pudo obtener la ubicación. Intente nuevamente")
            }
        }
    }
}
