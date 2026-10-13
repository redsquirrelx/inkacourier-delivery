package com.ampn.inkacourierdelivery.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.FragmentInicioB2bBinding
import com.ampn.inkacourierdelivery.databinding.ItemIndicadorBinding
import com.ampn.inkacourierdelivery.modelo.EstadoEnvio
import com.ampn.inkacourierdelivery.vistamodelo.InicioB2BViewModel

/** Inicio del representante B2B: resumen por estado y últimas actualizaciones de su empresa. */
class InicioB2BFragment : Fragment() {

    private var _binding: FragmentInicioB2bBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InicioB2BViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioB2bBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        establecerTitulo(getString(R.string.marca))
        val usuario = viewModel.usuario
        binding.textoSaludo.text = getString(R.string.saludo, usuario?.primerNombre.orEmpty())
        binding.textoEmpresa.text = usuario?.empresa?.razonSocial.orEmpty()

        val adapter = EnvioAdapter(EnvioAdapter.Modo.EMPRESA) {
            navegarA(DetalleEnvioFragment.nuevaInstancia(it.codigo))
        }
        binding.listaRecientes.adapter = adapter
        binding.tarjetaBusqueda.setOnClickListener { abrirEnvios(estado = null, enfocarBusqueda = true) }
        binding.botonVerTodos.setOnClickListener { abrirEnvios(estado = null) }

        val indicadores: Map<EstadoEnvio, ItemIndicadorBinding> = mapOf(
            EstadoEnvio.PENDIENTE to binding.indicadorPendiente,
            EstadoEnvio.EN_RUTA to binding.indicadorEnRuta,
            EstadoEnvio.EN_CAMINO_A_ENTREGA to binding.indicadorEnCamino,
            EstadoEnvio.ENTREGADO to binding.indicadorEntregado,
            EstadoEnvio.VISITA_FALLIDA to binding.indicadorVisitaFallida,
            EstadoEnvio.NO_VISITADO to binding.indicadorNoVisitado
        )
        indicadores.forEach { (estado, indicador) ->
            indicador.root.setOnClickListener { abrirEnvios(estado) }
        }

        recolectar(viewModel.resumen) { resumen ->
            indicadores.forEach { (estado, indicador) ->
                indicador.mostrar(resumen.cantidadPorEstado[estado] ?: 0, estado.etiqueta, estado.colorRes())
            }
            adapter.submitList(resumen.recientes)
            binding.textoSinRecientes.isVisible = resumen.recientes.isEmpty()
        }
    }

    private fun abrirEnvios(estado: EstadoEnvio?, enfocarBusqueda: Boolean = false) {
        abrirSeccion(R.id.seccionEnvios, EnviosB2BFragment.nuevaInstancia(estado, enfocarBusqueda))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
