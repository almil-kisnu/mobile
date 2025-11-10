package com.almil.dessertcakekinian.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

interface UserAddedListener {
    fun onUserAdded()
}

class AddUserBottomSheetFragment : BottomSheetDialogFragment() {

    private var listener: UserAddedListener? = null

    /**
     * Metode untuk menyambungkan listener dari fragment lain.
     */
    fun setUserAddedListener(listener: UserAddedListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_user, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameInput = view.findViewById<EditText>(R.id.username_input)
        val phoneInput = view.findViewById<EditText>(R.id.phone_input)
        val passwordInput = view.findViewById<EditText>(R.id.password_input)
        val confirmPasswordInput = view.findViewById<EditText>(R.id.confirm_password_input)
        val roleRadioGroup = view.findViewById<RadioGroup>(R.id.role_radio_group)
        val submitButton = view.findViewById<Button>(R.id.submit_button)
        val supabase = SupabaseClientProvider.client

        submitButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            val selectedRoleId = roleRadioGroup.checkedRadioButtonId
            val selectedRole = if (selectedRoleId == R.id.radio_admin) "admin" else "karyawan"

            // Validasi Input
            if (username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Semua kolom wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Password dan konfirmasi tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitButton.isEnabled = false
            submitButton.text = "Menyimpan..."

            lifecycleScope.launch {
                try {
                    // Siapkan data untuk di-insert menggunakan Map
                    val dataUser = mapOf(
                        "username" to username,
                        "password" to password, // PERINGATAN: Password tidak di-hash
                        "role" to selectedRole,
                        "phone" to phone
                    )

                    // Insert data ke tabel 'pengguna'
                    supabase.from("pengguna").insert(dataUser)

                    Toast.makeText(requireContext(), "Data pengguna berhasil ditambahkan!", Toast.LENGTH_LONG).show()

                    // Kirim sinyal ke listener bahwa proses berhasil
                    listener?.onUserAdded()

                    // Tutup bottom sheet
                    dismiss()

                } catch (e: Exception) {
                    Log.e(TAG, "Gagal menyimpan data: ", e)
                    Toast.makeText(requireContext(), "nomor telephon sudah terdaftar", Toast.LENGTH_LONG).show()
                    submitButton.isEnabled = true
                    submitButton.text = "Daftar"
                }
            }
        }
    }

    companion object {
        const val TAG = "AddUserBottomSheet"
        fun newInstance(): AddUserBottomSheetFragment {
            return AddUserBottomSheetFragment()
        }
    }
}