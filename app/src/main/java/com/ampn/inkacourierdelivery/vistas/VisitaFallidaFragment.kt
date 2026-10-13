package com.ampn.inkacourierdelivery.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.FragmentVisitaFallidaBinding
import com.ampn.inkacourierdelivery.modelo.CausaVisitaFallida
import com.ampn.inkacourierdelivery.modelo.RegistroVisitaFallida
import com.ampn.inkacourierdelivery.vistamodelo.RegistroBaseViewModel
import com.ampn.inkacourierdelivery.vistamodelo.VisitaFallidaViewModel

/** HU-05: registro de visita fallida con causa, fecha/hora, GPS, fotografía y observaciones. */
class VisitaFallidaFragment : Fragment() {

    companion object {
        fun nuevaInstancia(codigo: String) = VisitaFallidaFragment().apply {
            arguments = bundleOf(RegistroBaseViewModel.ARG_CODIGO to codigo)
        }
    }

    private var _binding: FragmentVisitaFallidaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VisitaFallidaViewModel by viewModels()
    private val evidencias = CapturaEvidencias(this) { viewModel }

    private fun causasPorBoton(): Map<RadioButton, CausaVisitaFallida> = mapOf(
        binding.causaAusente to CausaVisitaFallida.DESTINATARIO_AUSENTE,
        binding.causaDireccion to CausaVisitaFallida.DIRECCION_NO_LOCALIZADA,
        binding.causaRechazo to CausaVisitaFallida.RECEPCION_RECHAZADA,
        binding.causaPago to CausaVisitaFallida.PAGO_NO_VERIFICADO
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVisitaFallidaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        establecerTitulo(getString(R.string.titulo_visita_fallida))
        causasPorBoton().forEach { (boton, causa) -> boton.text = causa.etiqueta }
        binding.textoFechaHora.text = getString(R.string.fecha_intento, formatearFechaHora(viewModel.fechaHoraIntento))
        evidencias.vincular(binding.evidencias)
        binding.botonRegistrar.setOnClickListener { revisarYRegistrar() }

        recolectar(viewModel.envio) { envio ->
            envio ?: return@recolectar
            binding.textoEnvio.text = getString(R.string.formato_separado, envio.codigo, envio.destinatario.nombre)
            // "Pago no verificado" solo aplica a envíos con pago contra entrega.
            binding.causaPago.isEnabled = envio.pagoPendiente
        }
        observarEventosRegistro(viewModel)
    }

    private fun revisarYRegistrar() {
        val registro = viewModel.prepararRegistro(
            causa = causasPorBoton().entries.find { it.key.isChecked }?.value,
            observaciones = binding.campoObservaciones.text?.toString().orEmpty(),
            omitirFoto = evidencias.omitirFoto(binding.evidencias),
            justificacionSinFoto = evidencias.justificacion(binding.evidencias)
        )
        confirmarRegistro(
            error = viewModel.validar(registro),
            titulo = getString(R.string.confirmar_visita),
            resumen = resumir(registro)
        ) { viewModel.registrar(registro) }
    }

    private fun resumir(registro: RegistroVisitaFallida): String = getString(
        R.string.resumen_visita,
        registro.causa?.etiqueta.orEmpty(),
        formatearFechaHora(registro.fechaHora),
        getString(if (registro.foto != null) R.string.foto_adjunta else R.string.foto_omitida_insegura),
        registro.ubicacion?.comoTexto().orEmpty()
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
