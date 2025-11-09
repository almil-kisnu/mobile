package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc


class forgotFragment : Fragment() {

    private lateinit var masukTextView: TextView
    private lateinit var nikInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var submitButton: Button
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forgot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupNavigationToLogin()
        setupUpdateButton()
        setupPasswordToggle()
    }

    private fun initializeViews(view: View) {
        masukTextView = view.findViewById(R.id.login)
        phoneInput = view.findViewById(R.id.phone)
        nikInput = view.findViewById(R.id.nik)
        passwordInput = view.findViewById(R.id.password)
        confirmPasswordInput = view.findViewById(R.id.confirm_password)
        submitButton = view.findViewById(R.id.login_button)
    }

    private fun setupNavigationToLogin() {
        masukTextView.setOnClickListener {
            findNavController().navigate(R.id.action_forgot_to_login)
        }
    }

    private fun setupUpdateButton() {
        submitButton.setOnClickListener {
            val nik = nikInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // --- 1. Input Validation ---
            if (nik.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Semua kolom wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Password dan konfirmasi tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- 2. Update Process (Menggunakan RPC untuk Hashing Aman) ---
            submitButton.isEnabled = false
            submitButton.text = "Memeriksa..." // Pesan awal

            lifecycleScope.launch {
                try {
                    val supabase = SupabaseClientProvider.client
                    submitButton.text = "Mengirim..."
                    val response = supabase.postgrest.rpc(
                        function = "update_password_by_nik_and_phone",
                        parameters = mapOf(
                            "p_nik" to nik,
                            "p_phone" to phone,
                            "p_new_password" to password
                        )
                    ).decodeSingleOrNull<Map<String, Boolean>>()

                    val isSuccess = response?.get("success") ?: false

                    if (isSuccess) {
                        // Password berhasil diubah dan sudah di-hash oleh fungsi SQL
                        Toast.makeText(requireContext(), "Password berhasil diubah!", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_forgot_to_login)
                    } else {
                        // RPC mengembalikan FALSE, berarti NIK dan Phone tidak cocok/tidak ditemukan
                        Toast.makeText(requireContext(), "NIK dan Nomor Telepon tidak cocok atau belum terdaftar.", Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    Log.e("ForgotFragment", "Gagal mengubah password melalui RPC: ", e)
                    Toast.makeText(requireContext(), "Gagal terhubung ke server atau terjadi kesalahan.", Toast.LENGTH_LONG).show()
                } finally {
                    activity?.runOnUiThread {
                        submitButton.isEnabled = true
                        submitButton.text = "Kirim"
                    }
                }
            }
        }
    }



    companion object {
        @JvmStatic
        fun newInstance() = forgotFragment()
    }
    private fun setupPasswordToggle() {
        setupToggleForField(passwordInput, isMainPassword = true)
        setupToggleForField(confirmPasswordInput, isMainPassword = false)
    }

    private fun setupToggleForField(editText: EditText, isMainPassword: Boolean) {
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2] // drawable kanan
                if (drawableEnd != null && event.rawX >= (editText.right - drawableEnd.bounds.width())) {
                    togglePasswordVisibility(editText, isMainPassword)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility(editText: EditText, isMainPassword: Boolean) {
        val isVisible = if (isMainPassword) isPasswordVisible else isConfirmPasswordVisible

        if (isVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock),
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility),
                null
            )
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock),
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_off),
                null
            )
        }

        editText.setSelection(editText.text?.length ?: 0)

        if (isMainPassword) {
            isPasswordVisible = !isPasswordVisible
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }
    }
}