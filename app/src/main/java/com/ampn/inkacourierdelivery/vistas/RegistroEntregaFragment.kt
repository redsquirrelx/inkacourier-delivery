package com.ampn.inkacourierdelivery.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.FragmentRegistroEntregaBinding
import com.ampn.inkacourierdelivery.modelo.Envio
import com.ampn.inkacourierdelivery.modelo.RegistroEntrega
import com.ampn.inkacourierdelivery.modelo.TipoDocumento
import com.ampn.inkacourierdelivery.modelo.TipoReceptor
import com.ampn.inkacourierdelivery.vistamodelo.RegistroBaseViewModel
import com.ampn.inkacourierdelivery.vistamodelo.RegistroEntregaViewModel

/** HU-03: registro de entrega con receptor, firma, fotografía y GPS. */
class RegistroEntregaFragment : Fragment() {

    companion object {
        fun nuevaInstancia(codigo: String) = RegistroEntregaFragment().apply {
            arguments = bundleOf(RegistroBaseViewModel.ARG_CODIGO to codigo)
        }
    }

    private var _binding: FragmentRegistroEntregaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegistroEntregaViewModel by viewModels()
    private val evidencias = CapturaEvidencias(this) { viewModel }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegistroEntregaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        establecerTitulo(getString(R.string.titulo_registro_entrega))
        evidencias.vincular(binding.evidencias)
        binding.botonLimpiarFirma.setOnClickListener { binding.vistaFirma.limpiar() }
        binding.botonRegistrar.setOnClickListener { revisarYRegistrar() }

        recolectar(viewModel.envio) { envio -> envio?.let { mostrarEnvio(it) } }
        observarEventosRegistro(viewModel)
    }

    private fun mostrarEnvio(envio: Envio) {
        binding.textoEnvio.text = getString(R.string.formato_separado, envio.codigo, envio.destinatario.nombre)
        binding.textoAutorizados.text = if (envio.autorizados.isEmpty()) {
            getString(R.string.autorizados_ninguno)
        } else {
            getString(R.string.autorizados_lista, envio.autorizados.joinToString { it.nombre })
        }
        binding.opcionTercero.isEnabled = envio.autorizados.isNotEmpty()
    }

    private fun revisarYRegistrar() {
        val registro = viewModel.prepararRegistro(
            tipoReceptor = if (binding.opcionTercero.isChecked) TipoReceptor.TERCERO_AUTORIZADO else TipoReceptor.TITULAR,
            tipoDocumento = if (binding.opcionCe.isChecked) TipoDocumento.CE else TipoDocumento.DNI,
            numeroDocumento = binding.campoDocumento.text?.toString().orEmpty(),
            nombre = binding.campoNombre.text?.toString().orEmpty(),
            firma = binding.vistaFirma.obtenerImagen(),
            omitirFoto = evidencias.omitirFoto(binding.evidencias),
            justificacionSinFoto = evidencias.justificacion(binding.evidencias)
        )
        confirmarRegistro(
            error = viewModel.validar(registro),
            titulo = getString(R.string.confirmar_entrega),
            resumen = resumir(registro)
        ) { viewModel.registrar(registro) }
    }

    private fun resumir(registro: RegistroEntrega): String = getString(
        R.string.resumen_entrega,
        registro.receptor.nombre,
        registro.tipoReceptor.etiqueta,
        registro.receptor.tipoDocumento.etiqueta,
        registro.receptor.numeroDocumento,
        getString(if (registro.foto != null) R.string.foto_adjunta else R.string.foto_omitida_riesgo),
        registro.ubicacion?.comoTexto().orEmpty()
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
