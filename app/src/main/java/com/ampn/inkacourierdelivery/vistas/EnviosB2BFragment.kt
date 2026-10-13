package com.ampn.inkacourierdelivery.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.FragmentEnviosB2bBinding
import com.ampn.inkacourierdelivery.modelo.EstadoEnvio
import com.ampn.inkacourierdelivery.vistamodelo.EnviosB2BViewModel
import com.google.android.material.chip.Chip

/** HU-07: listado, búsqueda y filtros de los envíos de la empresa representada. */
class EnviosB2BFragment : Fragment() {

    companion object {
        private const val ARG_ENFOCAR_BUSQUEDA = "enfocarBusqueda"

        fun nuevaInstancia(estado: EstadoEnvio?, enfocarBusqueda: Boolean = false) = EnviosB2BFragment().apply {
            arguments = bundleOf(
                EnviosB2BViewModel.ARG_ESTADO to estado?.name,
                ARG_ENFOCAR_BUSQUEDA to enfocarBusqueda
            )
        }
    }

    private var _binding: FragmentEnviosB2bBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EnviosB2BViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEnviosB2bBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        establecerTitulo(getString(R.string.titulo_envios_b2b))
        binding.textoEmpresa.text = viewModel.empresa?.razonSocial.orEmpty()

        val adapter = EnvioAdapter(EnvioAdapter.Modo.EMPRESA) { envio ->
            navegarA(DetalleEnvioFragment.nuevaInstancia(envio.codigo))
        }
        binding.listaEnvios.adapter = adapter
        binding.campoBusqueda.doAfterTextChanged { viewModel.buscar(it?.toString().orEmpty()) }
        crearChipsDeEstado()
        if (savedInstanceState == null && arguments?.getBoolean(ARG_ENFOCAR_BUSQUEDA) == true) {
            binding.campoBusqueda.requestFocus()
            WindowCompat.getInsetsController(requireActivity().window, binding.campoBusqueda)
                .show(WindowInsetsCompat.Type.ime())
        }

        recolectar(viewModel.estado) { estado ->
            binding.grupoEstados.findViewWithTag<Chip>(estado.filtroEstado ?: getString(R.string.filtro_estado_todos))
                ?.takeUnless { it.isChecked }
                ?.let { chip ->
                    chip.isChecked = true
                    // Lleva a la vista el chip seleccionado (p. ej., al llegar filtrado desde Inicio).
                    binding.desplazamientoEstados.post { binding.desplazamientoEstados.smoothScrollTo(chip.left, 0) }
                }
            adapter.submitList(estado.envios)
            binding.textoResumen.text = getString(R.string.resumen_envios_b2b, estado.envios.size, estado.totalEmpresa)
            binding.textoVacio.isVisible = estado.envios.isEmpty()
            binding.textoVacio.setText(if (estado.hayFiltros) R.string.envios_vacios_filtro else R.string.envios_vacios)
        }
    }

    /** Un chip "Todos" más uno por cada estado; la etiqueta (tag) identifica el filtro. */
    private fun crearChipsDeEstado() {
        val opciones: List<Pair<String, EstadoEnvio?>> =
            listOf(getString(R.string.filtro_estado_todos) to null) + EstadoEnvio.entries.map { it.etiqueta to it }
        opciones.forEach { (texto, estado) ->
            val chip = layoutInflater.inflate(R.layout.chip_filtro, binding.grupoEstados, false) as Chip
            chip.id = View.generateViewId()
            chip.text = texto
            chip.tag = estado ?: texto
            chip.setOnClickListener { viewModel.filtrarPorEstado(estado) }
            binding.grupoEstados.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
