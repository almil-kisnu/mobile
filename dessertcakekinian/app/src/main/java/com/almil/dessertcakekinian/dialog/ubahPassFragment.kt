package com.almil.dessertcakekinian.dialog

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.Locale

// Definisikan kunci argumen
private const val ARG_USER_ID = "userId"

class ubahPassFragment : DialogFragment() {

    // Views
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var btnSave: MaterialButton
    private lateinit var newPasswordToggle: View // Untuk menangkap klik pada drawableEnd
    private lateinit var confirmPasswordToggle: View // Untuk menangkap klik pada drawableEnd

    // Data
    private var userId: Int = 0
    private var isNewPasswordVisible: Boolean = false
    private var isConfirmPasswordVisible: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asumsi R.layout.fragment_ubah_pass adalah layout XML yang Anda berikan
        return inflater.inflate(R.layout.fragment_ubah_pass, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data ID dari argument
        arguments?.let {
            userId = it.getInt(ARG_USER_ID)
        }

        // Bind Views
        newPasswordEditText = view.findViewById(R.id.new_password)
        confirmPasswordEditText = view.findViewById(R.id.confirm_password)
        btnSave = view.findViewById(R.id.btnSave)

        // Ambil area drawableEnd untuk klik toggle (PENTING!)
        newPasswordToggle = newPasswordEditText.compoundDrawables[2]?.bounds?.let {
            newPasswordEditText
        } ?: newPasswordEditText

        confirmPasswordToggle = confirmPasswordEditText.compoundDrawables[2]?.bounds?.let {
            confirmPasswordEditText
        } ?: confirmPasswordEditText

        // Setup Listener Toggle Password
        newPasswordToggle.setOnClickListener {
            togglePasswordVisibility(newPasswordEditText, ::isNewPasswordVisible.get(), ::isNewPasswordVisible::set)
        }
        confirmPasswordToggle.setOnClickListener {
            togglePasswordVisibility(confirmPasswordEditText, ::isConfirmPasswordVisible.get(), ::isConfirmPasswordVisible::set)
        }

        // Setup aksi tombol Simpan
        btnSave.setOnClickListener {
            handlePasswordUpdate()
        }
    }

    // Fungsi Toggle Password (Kode 3 yang diadaptasi)
    private fun togglePasswordVisibility(editText: EditText, isVisible: Boolean, setter: (Boolean) -> Unit) {
        if (isVisible) {
            // Sembunyikan Password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock),
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility), // Asumsi ini adalah icon Sembunyikan
                null
            )
        } else {
            // Tampilkan Password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock),
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_off), // Asumsi ini adalah icon Tampilkan
                null
            )
        }
        // Pindahkan kursor ke akhir teks
        editText.setSelection(editText.text?.length ?: 0)
        // Ubah status
        setter(!isVisible)
    }


    private fun handlePasswordUpdate() {
        val newPass = newPasswordEditText.text.toString().trim()
        val confirmPass = confirmPasswordEditText.text.toString().trim()

        // 1. Validasi
        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(requireContext(), "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPass.length < 6) {
            Toast.makeText(requireContext(), "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPass != confirmPass) {
            Toast.makeText(requireContext(), "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == 0) {
            Toast.makeText(requireContext(), "ID Pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Memperbarui..."

        // 2. Lakukan Update ke Supabase
        lifecycleScope.launch {
            try {
                // Data yang akan di-update.
                val updates = mapOf(
                    "password" to newPass
                )

                SupabaseClientProvider.client.from("pengguna")
                    .update(updates) {
                        // MENGGUNAKAN SINTAKS BLOK FILTER
                        filter {
                            eq("iduser", userId)
                        }
                    }

                // Jika eksekusi sukses:
                Toast.makeText(requireContext(), "Password berhasil diperbarui!", Toast.LENGTH_LONG).show()
                dismiss() // Tutup dialog

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memperbarui password: ${e.message}", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
                btnSave.text = "Simpan Perubahan"
            }
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(userId: Int) =
            ubahPassFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_USER_ID, userId)
                }
            }
    }
}