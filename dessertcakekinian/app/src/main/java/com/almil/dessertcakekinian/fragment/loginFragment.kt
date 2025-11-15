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
import androidx.navigation.fragment.findNavController
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.MainActivity
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.User
import com.almil.dessertcakekinian.model.Outlet
import com.almil.dessertcakekinian.model.ProductRepository
import com.almil.dessertcakekinian.model.OrderRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch

private const val TAG = "LoginFragmentDebug"

class loginFragment : Fragment() {

    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotTextView: TextView

    private var isPasswordVisible: Boolean = false
    private lateinit var sharedPreferences: SharedPreferences

    // Repositories
    private lateinit var productRepository: ProductRepository
    private lateinit var orderRepository: OrderRepository

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

        // Inisialisasi Repositories
        productRepository = ProductRepository.getInstance(requireContext())
        orderRepository = OrderRepository.getInstance(requireContext())

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
                        // Step 1: Verify user credentials
                        val response = supabase.postgrest.rpc(
                            function = "verify_password_match",
                            parameters = mapOf(
                                "p_phone" to phone,
                                "p_password" to password
                            )
                        ).decodeSingleOrNull<User>()

                        if (response != null) {
                            // Step 2: Fetch outlet data if exists
                            val outlet = if (response.idOutlet != null) {
                                try {
                                    supabase.postgrest["outlet"]
                                        .select {
                                            filter {
                                                eq("idoutlet", response.idOutlet)
                                            }
                                        }
                                        .decodeSingleOrNull<Outlet>()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to fetch outlet data: ", e)
                                    null
                                }
                            } else {
                                null
                            }

                            // Step 3: Save user session
                            saveUserSession(response, outlet)

                            // Step 4: Sync all data from Supabase
                            loginButton.text = "Syncing data..."
                            syncAllDataOnLogin()

                            // Step 5: Navigate to MainActivity
                            navigateToMainActivity()
                        } else {
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

    private suspend fun syncAllDataOnLogin() {
        try {
            Log.d(TAG, "🔄 Starting login sync...")

            // Sync product data
            val productResult = productRepository.forceSync()
            if (productResult.isFailure) {
                Log.w(TAG, "⚠️ Product sync failed: ${productResult.exceptionOrNull()?.message}")
            } else {
                Log.d(TAG, "✅ Product sync completed")
            }

            // Sync order data
            val orderResult = orderRepository.forceSync()
            if (orderResult.isFailure) {
                Log.w(TAG, "⚠️ Order sync failed: ${orderResult.exceptionOrNull()?.message}")
            } else {
                Log.d(TAG, "✅ Order sync completed")
            }

            Log.d(TAG, "✅ Login sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during login sync", e)
            // Tetap lanjutkan login meskipun sync gagal
        }
    }

    private fun saveUserSession(user: User, outlet: Outlet?) {
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

        // Simpan data outlet
        if (outlet != null) {
            editor.putString("OUTLET_KODE", outlet.kodeOutlet)
            editor.putString("OUTLET_NAMA", outlet.namaOutlet)
            editor.putString("OUTLET_ALAMAT", outlet.alamat ?: "")
            editor.putString("OUTLET_TELEPON", outlet.telepon ?: "")
            editor.putBoolean("OUTLET_IS_ACTIVE", outlet.isActive)
            Log.d(TAG, "Data Outlet Disimpan: Kode=${outlet.kodeOutlet}, Nama=${outlet.namaOutlet}")
        } else {
            // Clear outlet data jika tidak ada
            editor.remove("OUTLET_KODE")
            editor.remove("OUTLET_NAMA")
            editor.remove("OUTLET_ALAMAT")
            editor.remove("OUTLET_TELEPON")
            editor.remove("OUTLET_IS_ACTIVE")
        }

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
