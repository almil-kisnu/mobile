package com.almil.dessertcakekinian.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.almil.dessertcakekinian.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import com.google.android.material.button.MaterialButton


// Definisikan kunci argumen
private const val ARG_USER_ID = "userId"
private const val ARG_USER_NAME = "userName"
private const val ARG_USER_PHONE = "userPhone"
private const val ARG_USER_NIK = "userNIK"

class editUserFragment : DialogFragment() { // Ubah dari Fragment menjadi DialogFragment

    // Definisi variabel untuk input dan tombol
    private lateinit var editUsername: EditText
    private lateinit var editNIK: EditText
    private lateinit var editPhone: EditText
    private lateinit var btnSave: MaterialButton

    // Data yang akan dimuat
    private var userId: Int = 0
    private var userName: String? = null
    private var userPhone: String? = null
    private var userNIK: String? = null

    // Interface untuk komunikasi ke Fragment pemanggil (ProfileFragment)
    interface EditUserDialogListener {
        fun onUserUpdated(userId: Int, username: String, phone: String, nik: String)
    }

    // Pastikan layout yang digunakan adalah layout XML dialog baru (contoh sebelumnya)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asumsi R.layout.dialog_edit_user adalah XML yang baru Anda buat
        return inflater.inflate(R.layout.fragment_edit_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Ambil data dari Bundle (Sesi)
        arguments?.let {
            userId = it.getInt(ARG_USER_ID)
            userName = it.getString(ARG_USER_NAME)
            userPhone = it.getString(ARG_USER_PHONE)
            userNIK = it.getString(ARG_USER_NIK)
        }

        // 2. Bind View
        editUsername = view.findViewById(R.id.edit_username)
        editNIK = view.findViewById(R.id.edit_nik)
        editPhone = view.findViewById(R.id.edit_phone)
        btnSave = view.findViewById(R.id.btnSave)

        // 3. Isi EditText dengan data Sesi
        editUsername.setText(userName)
        editNIK.setText(userNIK)
        editPhone.setText(userPhone)

        // 4. Setup aksi tombol Simpan
        // ... (Kode onViewCreated sebelum btnSave.setOnClickListener)

// 4. Setup aksi tombol Simpan
        btnSave.setOnClickListener {
            // Ambil nilai baru dari input
            val newUsername = editUsername.text.toString().trim()
            val newNIK = editNIK.text.toString().trim()
            val newPhone = editPhone.text.toString().trim()

            // Validasi dasar (opsional, tapi disarankan)
            if (newUsername.isEmpty() || newNIK.isEmpty() || newPhone.isEmpty()) {
                // Tampilkan feedback jika ada input yang kosong
                Toast.makeText(requireContext(), "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Hentikan proses jika validasi gagal
            }

            // 🚀 Lakukan Update ke Supabase
            lifecycleScope.launch {
                try {
                    // Data yang akan di-update. Nama properti harus sesuai dengan kolom tabel.
                    val updates = mapOf(
                        "username" to newUsername,
                        "nik" to newNIK,
                        "phone" to newPhone
                    )

                    SupabaseClientProvider.client.from("pengguna")
                        .update(updates) {
                            // MENGGUNAKAN SINTAKS BLOK FILTER
                            filter {
                                eq("iduser", userId) // Fungsi 'eq' kini ada di dalam scope 'filter'
                            }
                        }

                    // Jika eksekusi sukses:

                    // 1. Panggil listener untuk mengirim data baru kembali ke ProfileFragment
                    (targetFragment as? EditUserDialogListener)?.onUserUpdated(
                        userId, newUsername, newPhone, newNIK
                    )

                    // 2. Tampilkan feedback sukses
                    Toast.makeText(requireContext(), "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()

                    // 3. Tutup dialog
                    dismiss()

                } catch (e: Exception) {
                    // Tangani error (misalnya: koneksi, data tidak valid, phone sudah terpakai)
                    e.printStackTrace()
                    // Tampilkan pesan error yang lebih spesifik jika memungkinkan (misalnya: duplikat phone)
                    Toast.makeText(requireContext(), "Gagal memperbarui: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Companion Object yang diubah untuk menerima 4 data dari sesi
    companion object {
        @JvmStatic
        fun newInstance(id: Int, name: String, phone: String, nik: String) =
            editUserFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_USER_ID, id)
                    putString(ARG_USER_NAME, name)
                    putString(ARG_USER_PHONE, phone)
                    putString(ARG_USER_NIK, nik)
                }
            }
    }
}