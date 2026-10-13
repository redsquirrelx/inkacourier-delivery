package com.ampn.inkacourierdelivery.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ampn.inkacourierdelivery.R
import com.ampn.inkacourierdelivery.databinding.FragmentLoginBinding
import com.ampn.inkacourierdelivery.modelo.Usuario
import com.ampn.inkacourierdelivery.vistamodelo.LoginViewModel

/** Inicio de sesión simulado. La navegación al módulo del rol la decide MainActivity. */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        establecerTitulo(getString(R.string.app_name))
        mostrarCredencialesDemo()

        binding.botonIniciarSesion.setOnClickListener { iniciarSesion() }
        binding.campoContrasena.setOnEditorActionListener { _, accion, _ ->
            (accion == EditorInfo.IME_ACTION_DONE).also { if (it) iniciarSesion() }
        }
        binding.campoUsuario.doAfterTextChanged { binding.contenedorUsuario.error = null }
        binding.campoContrasena.doAfterTextChanged { binding.contenedorContrasena.error = null }

        recolectar(viewModel.estado) { estado ->
            binding.contenedorUsuario.error = estado.errorUsuario
            binding.contenedorContrasena.error = estado.errorContrasena
        }
    }

    private fun iniciarSesion() {
        viewModel.iniciarSesion(
            binding.campoUsuario.text?.toString().orEmpty(),
            binding.campoContrasena.text?.toString().orEmpty()
        )
    }

    private fun mostrarCredencialesDemo() {
        val textos = listOf(binding.textoDemoRepartidor, binding.textoDemoB2B)
        val botones = listOf(binding.botonUsarRepartidor, binding.botonUsarB2B)
        viewModel.credencialesDemo.forEachIndexed { indice, (usuario, contrasena) ->
            textos.getOrNull(indice)?.text =
                getString(R.string.demo_credencial, usuario.rol.etiqueta, usuario.usuario, contrasena)
            botones.getOrNull(indice)?.setOnClickListener { completarCredenciales(usuario, contrasena) }
        }
    }

    private fun completarCredenciales(usuario: Usuario, contrasena: String) {
        viewModel.limpiarErrores()
        binding.campoUsuario.setText(usuario.usuario)
        binding.campoContrasena.setText(contrasena)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
