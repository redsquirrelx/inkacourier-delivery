package com.ampn.inkacourierdelivery.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.FragmentInicioRepartidorBinding
import com.ampn.inkacourierdelivery.vistamodelo.FiltroParadas
import com.ampn.inkacourierdelivery.vistamodelo.InicioRepartidorViewModel

/** Inicio del repartidor: saludo, avance de la ruta, próxima parada y accesos del Sprint 1. */
class InicioRepartidorFragment : Fragment() {

    private var _binding: FragmentInicioRepartidorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InicioRepartidorViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioRepartidorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        establecerTitulo(getString(R.string.marca))
        binding.textoSaludo.text = getString(R.string.saludo, viewModel.usuario?.primerNombre.orEmpty())
        binding.textoSubtitulo.text =
            getString(R.string.inicio_repartidor_subtitulo, formatearFechaLarga(System.currentTimeMillis()))

        binding.accesoParadas.configurar(
            R.drawable.ic_lista, getString(R.string.acceso_paradas), getString(R.string.acceso_paradas_detalle)
        ) { abrirParadas(FiltroParadas.TODAS) }
        binding.accesoPorAtender.configurar(
            R.drawable.ic_camion, getString(R.string.acceso_por_atender), getString(R.string.acceso_por_atender_detalle)
        ) { abrirParadas(FiltroParadas.POR_ATENDER) }
        binding.accesoAtendidas.configurar(
            R.drawable.ic_completado, getString(R.string.acceso_atendidas), getString(R.string.acceso_atendidas_detalle)
        ) { abrirParadas(FiltroParadas.ATENDIDAS) }

        recolectar(viewModel.resumen) { resumen ->
            binding.textoAvance.text = getString(R.string.avance_ruta_detalle, resumen.atendidas, resumen.total)
            binding.indicadorAvance.max = resumen.total.coerceAtLeast(1)
            binding.indicadorAvance.setProgressCompat(resumen.atendidas, true)
            binding.indicadorPorAtender.mostrar(
                resumen.porAtender, getString(R.string.indicador_por_atender), R.color.estado_en_ruta
            )
            binding.indicadorEntregadas.mostrar(
                resumen.entregadas, getString(R.string.indicador_entregadas), R.color.estado_entregado
            )
            binding.indicadorFallidas.mostrar(
                resumen.fallidas, getString(R.string.indicador_fallidas), R.color.estado_visita_fallida
            )

            val proxima = resumen.proximaParada
            binding.proximaParada.root.isVisible = proxima != null
            binding.textoSinProximaParada.isVisible = proxima == null
            proxima?.let { envio ->
                binding.proximaParada.mostrarEnvio(envio, esRuta = true) {
                    navegarA(DetalleEnvioFragment.nuevaInstancia(it.codigo))
                }
            }
        }
    }

    private fun abrirParadas(filtro: FiltroParadas) {
        abrirSeccion(R.id.seccionParadas, ParadasFragment.nuevaInstancia(filtro))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
