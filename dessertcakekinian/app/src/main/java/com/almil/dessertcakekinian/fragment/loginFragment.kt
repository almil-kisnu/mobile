// File: com/almil/dessertcakekinian/fragment/loginFragment.kt

package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.almil.dessertcakekinian.R
import androidx.navigation.fragment.findNavController
import com.almil.dessertcakekinian.activity.MainActivity
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.User
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.rpc

// TAG untuk Logcat agar mudah disaring
private const val TAG = "LoginFragmentDebug"

class loginFragment : Fragment() {

    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotTextView: TextView

    private var isPasswordVisible: Boolean = false
    private lateinit var sharedPreferences: SharedPreferences

    // Ambil client Supabase
    private val supabase = SupabaseClientProvider.client

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        checkUserSession()

        phoneEditText = view.findViewById(R.id.phone)
        passwordEditText = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.login_button)
        forgotTextView = view.findViewById(R.id.lupa_sandi)

        setupPasswordToggle()
        setupNavigationToForgot()
        setupLoginButton()
    }

    private fun checkUserSession() {
        if (sharedPreferences.getBoolean("IS_LOGGED_IN", false)) {
            navigateToMainActivity()
        }
    }

    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            val phone = phoneEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInput(phone, password)) {
                loginButton.isEnabled = false
                loginButton.text = "Loading..."

                lifecycleScope.launch {
                    try {
                        // 1. PANGGIL RPC (Remote Procedure Call)
                        // Panggil fungsi SQL verify_password_match dengan parameter phone dan password
                        val response = supabase.postgrest.rpc(
                            function = "verify_password_match",
                            parameters = mapOf(
                                "p_phone" to phone,
                                "p_password" to password
                            )
                        ).decodeSingleOrNull<User>()

                        if (response != null) {
                            // RPC berhasil mengembalikan pengguna -> Sandi dan Nomor Telepon benar
                            saveUserSession(response)
                            navigateToMainActivity()
                        } else {
                            // RPC mengembalikan null -> Sandi atau Nomor Telepon salah
                            // (Pesan generik disarankan untuk keamanan)
                            showError("Nomor telepon atau Password salah.")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Login failed via RPC: ", e)
                        showError("Terjadi kesalahan. Coba lagi nanti.")
                    } finally {
                        activity?.runOnUiThread {
                            loginButton.isEnabled = true
                            loginButton.text = "Login"
                        }
                    }
                }
            }
        }
    }

    // ... (Fungsi saveUserSession, validateInput, navigateToMainActivity, dan showError tetap sama) ...
    private fun saveUserSession(user: User) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("IS_LOGGED_IN", true)
        editor.putInt("USER_ID", user.idUser)
        editor.putString("USER_NAME", user.username)
        editor.putString("USER_PHONE", user.phone)
        editor.putString("USER_ROLE", user.role)
        val outletId = user.idOutlet ?: -1
        editor.putInt("USER_OUTLET_ID", outletId)
        editor.putString("USER_NIK", user.nik ?: "")
        editor.putString("USER_HIRED_DATE", user.hiredDate ?: "")
        Log.d(TAG, "Sesi Disimpan: User=${user.username}, Role=${user.role}, OutletID=$outletId")
        editor.apply()
    }

    private fun validateInput(phone: String, password: String): Boolean {
        if (phone.isEmpty()) {
            phoneEditText.error = "Nomor Telepon tidak boleh kosong"
            phoneEditText.requestFocus()
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
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            loginButton.isEnabled = true
            loginButton.text = "Login"
        }
    }

    // --- Fungsi lainnya (setupPasswordToggle, setupNavigationToForgot, togglePasswordVisibility) ---
    private fun setupPasswordToggle() {
        passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEndBounds = passwordEditText.compoundDrawables[2]?.bounds ?: return@setOnTouchListener false
                if (event.rawX >= (passwordEditText.right - drawableEndBounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
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
}