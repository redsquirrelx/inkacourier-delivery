package com.ampn.inkacourierdelivery.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.FragmentParadasBinding
import com.ampn.inkacourierdelivery.vistamodelo.FiltroParadas
import com.ampn.inkacourierdelivery.vistamodelo.ParadasViewModel

/** HU-01: listado de paradas de la ruta del repartidor. */
class ParadasFragment : Fragment() {

    companion object {
        fun nuevaInstancia(filtro: FiltroParadas) = ParadasFragment().apply {
            arguments = bundleOf(ParadasViewModel.ARG_FILTRO to filtro.name)
        }
    }

    private var _binding: FragmentParadasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParadasViewModel by viewModels()

    private val chipsPorFiltro = mapOf(
        FiltroParadas.POR_ATENDER to R.id.chipPorAtender,
        FiltroParadas.ATENDIDAS to R.id.chipAtendidas,
        FiltroParadas.TODAS to R.id.chipTodas
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentParadasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        establecerTitulo(getString(R.string.titulo_paradas))
        val adapter = EnvioAdapter(EnvioAdapter.Modo.RUTA) { envio ->
            navegarA(DetalleEnvioFragment.nuevaInstancia(envio.codigo))
        }
        binding.listaParadas.adapter = adapter

        binding.grupoFiltros.setOnCheckedStateChangeListener { _, ids ->
            chipsPorFiltro.entries.find { it.value == ids.firstOrNull() }?.let { viewModel.cambiarFiltro(it.key) }
        }

        recolectar(viewModel.estado) { estado ->
            chipsPorFiltro[estado.filtro]?.let { if (binding.grupoFiltros.checkedChipId != it) binding.grupoFiltros.check(it) }
            binding.textoResumen.text = getString(R.string.resumen_paradas, estado.totalPorAtender, estado.totalAtendidas)
            adapter.submitList(estado.paradas)
            binding.textoVacio.isVisible = estado.paradas.isEmpty()
            binding.textoVacio.setText(
                when (estado.filtro) {
                    FiltroParadas.POR_ATENDER -> R.string.paradas_vacias_por_atender
                    FiltroParadas.ATENDIDAS -> R.string.paradas_vacias_atendidas
                    FiltroParadas.TODAS -> R.string.paradas_vacias_todas
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
