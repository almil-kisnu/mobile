package com.almil.dessertcakekinian.fragment

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.MainActivity

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class loginFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var daftarTextView: TextView
    private lateinit var forgotTextView: TextView

    private var isPasswordVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Views
        usernameEditText = view.findViewById(R.id.username)
        passwordEditText = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.login_button)
        daftarTextView = view.findViewById(R.id.daftar2)
        forgotTextView = view.findViewById(R.id.lupa_sandi)

        // Setup listeners
        setupPasswordToggle()
        setupNavigationToRegister()
        setupNavigationToForgot()
        setupLoginButton()
    }

    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            usernameEditText.error = "Username tidak boleh kosong"
            usernameEditText.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password tidak boleh kosong"
            passwordEditText.requestFocus()
            return false
        }

        return true
    }



    private fun navigateToMainActivity() {
        Toast.makeText(requireContext(), "Login berhasil!", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        loginButton.isEnabled = true
        loginButton.text = "Login"
    }

    private fun setupPasswordToggle() {
        passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordEditText.right - passwordEditText.compoundDrawables[2].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun setupNavigationToRegister() {
        daftarTextView.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun setupNavigationToForgot() {
        forgotTextView.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot)
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock),
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility),
                null
            )
        } else {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock),
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_off),
                null
            )
        }

        passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
        isPasswordVisible = !isPasswordVisible
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            loginFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}