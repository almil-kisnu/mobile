package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.InputType
import android.text.TextUtils
import androidx.navigation.fragment.findNavController
import com.almil.dessertcakekinian.R

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class registerFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    // Views
    private lateinit var phoneEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var daftarTextView: TextView

    // Password visibility states
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

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
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        initializeViews(view)

        // Setup components
        setupPasswordToggle()
        setupNavigationToRegister()
        setupRegisterButton()
    }

    private fun initializeViews(view: View) {
        phoneEditText = view.findViewById(R.id.phone)
        usernameEditText = view.findViewById(R.id.username)
        passwordEditText = view.findViewById(R.id.password)
        confirmPasswordEditText = view.findViewById(R.id.password2)
        registerButton = view.findViewById(R.id.login_button)
        daftarTextView = view.findViewById(R.id.daftar2)
    }
    private fun setupPasswordToggle() {
        // Setup password visibility toggle
        passwordEditText.setOnTouchListener { _, event ->
            val drawableEnd = 2 // Right drawable index
            if (event.rawX >= (passwordEditText.right - passwordEditText.compoundDrawables[drawableEnd].bounds.width())) {
                togglePasswordVisibility(passwordEditText, isPasswordVisible) { isVisible ->
                    isPasswordVisible = isVisible
                }
                true
            } else {
                false
            }
        }

        // Setup confirm password visibility toggle
        confirmPasswordEditText.setOnTouchListener { _, event ->
            val drawableEnd = 2 // Right drawable index
            if (event.rawX >= (confirmPasswordEditText.right - confirmPasswordEditText.compoundDrawables[drawableEnd].bounds.width())) {
                togglePasswordVisibility(confirmPasswordEditText, isConfirmPasswordVisible) { isVisible ->
                    isConfirmPasswordVisible = isVisible
                }
                true
            } else {
                false
            }
        }
    }

    private fun togglePasswordVisibility(editText: EditText, currentVisibility: Boolean, callback: (Boolean) -> Unit) {
        if (currentVisibility) {
            // Hide password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock, 0, R.drawable.ic_visibility, 0
            )
            callback(false)
        } else {
            // Show password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_lock, 0, R.drawable.ic_visibility_off, 0
            )
            callback(true)
        }
        // Move cursor to end
        editText.setSelection(editText.text.length)
    }

    private fun setupNavigationToRegister() {
        daftarTextView.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun setupRegisterButton() {
        registerButton.setOnClickListener {
            if (validateForm()) {

            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val phone = phoneEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        // Validate phone
        if (TextUtils.isEmpty(phone)) {
            phoneEditText.error = "Nomor telepon harus diisi"
            isValid = false
        } else if (phone.length < 10 || phone.length > 13) {
            phoneEditText.error = "Nomor telepon tidak valid"
            isValid = false
        }

        // Validate username
        if (TextUtils.isEmpty(username)) {
            usernameEditText.error = "Username harus diisi"
            isValid = false
        } else if (username.length < 3) {
            usernameEditText.error = "Username minimal 3 karakter"
            isValid = false
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = "Password harus diisi"
            isValid = false
        }
        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.error = "Konfirmasi password harus diisi"
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordEditText.error = "Password tidak cocok"
            isValid = false
        }

        return isValid
    }



    private fun clearForm() {
        phoneEditText.setText("")
        usernameEditText.setText("")
        passwordEditText.setText("")
        confirmPasswordEditText.setText("")

        // Reset password visibility
        isPasswordVisible = false
        isConfirmPasswordVisible = false
        passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        confirmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_lock, 0, R.drawable.ic_visibility, 0
        )
        confirmPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_lock, 0, R.drawable.ic_visibility, 0
        )

        // Clear errors
        phoneEditText.error = null
        usernameEditText.error = null
        passwordEditText.error = null
        confirmPasswordEditText.error = null
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            registerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}