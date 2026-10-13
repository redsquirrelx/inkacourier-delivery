package com.ampn.inkacourierdelivery.vistas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ampn.inkacourierdelivery.databinding.ItemEnvioBinding
import com.ampn.inkacourierdelivery.modelo.Envio

/** Adapter compartido por la lista de paradas (HU-01), la consulta B2B (HU-07) y el inicio B2B. */
class EnvioAdapter(
    private val modo: Modo,
    private val alSeleccionar: (Envio) -> Unit
) : ListAdapter<Envio, EnvioAdapter.EnvioViewHolder>(Comparador) {

    enum class Modo { RUTA, EMPRESA }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnvioViewHolder =
        EnvioViewHolder(ItemEnvioBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: EnvioViewHolder, position: Int) {
        holder.enlazar(getItem(position))
    }

    inner class EnvioViewHolder(private val binding: ItemEnvioBinding) : RecyclerView.ViewHolder(binding.root) {

        fun enlazar(envio: Envio) {
            binding.mostrarEnvio(envio, esRuta = modo == Modo.RUTA, alSeleccionar = alSeleccionar)
        }
    }

    private object Comparador : DiffUtil.ItemCallback<Envio>() {
        override fun areItemsTheSame(anterior: Envio, nuevo: Envio) = anterior.codigo == nuevo.codigo
        override fun areContentsTheSame(anterior: Envio, nuevo: Envio) = anterior == nuevo
    }
}
