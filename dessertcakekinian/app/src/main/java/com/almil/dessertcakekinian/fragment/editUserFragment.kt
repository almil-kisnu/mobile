package com.almil.dessertcakekinian.fragment

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
import com.almil.dessertcakekinian.model.Pengguna
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
// ✅ --- TAMBAHKAN IMPORT INI ---
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
// ------------------------------------
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface UserUpdatedListener {
    fun onUserUpdated()
}

class EditUserDialogFragment : BottomSheetDialogFragment() {

    private var listener: UserUpdatedListener? = null
    private lateinit var currentUser: Pengguna

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_USER_JSON)?.let {
            currentUser = Json.decodeFromString(it)
        }

        val usernameInput = view.findViewById<EditText>(R.id.edit_username)
        val phoneInput = view.findViewById<EditText>(R.id.edit_phone)
        val passwordInput = view.findViewById<EditText>(R.id.edit_password)
        val roleGroup = view.findViewById<RadioGroup>(R.id.edit_role_group)
        val statusGroup = view.findViewById<RadioGroup>(R.id.edit_status_group)
        val alasanInput = view.findViewById<EditText>(R.id.edit_alasan_nonaktif)
        val btnUpdate = view.findViewById<Button>(R.id.btn_update)
        val supabase = SupabaseClientProvider.client

        usernameInput.setText(currentUser.username)
        phoneInput.setText(currentUser.phone)
        if (currentUser.role.equals("admin", ignoreCase = true)) {
            roleGroup.check(R.id.edit_radio_admin)
        } else {
            roleGroup.check(R.id.edit_radio_karyawan)
        }
        if (currentUser.isActive) {
            statusGroup.check(R.id.edit_radio_aktif)
            alasanInput.visibility = View.GONE
        } else {
            statusGroup.check(R.id.edit_radio_nonaktif)
            alasanInput.visibility = View.VISIBLE
            alasanInput.setText(currentUser.deactivatedReason)
        }

        statusGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.edit_radio_nonaktif) {
                alasanInput.visibility = View.VISIBLE
            } else {
                alasanInput.visibility = View.GONE
            }
        }

        btnUpdate.setOnClickListener {
            val newUsername = usernameInput.text.toString().trim()
            val newPhone = phoneInput.text.toString().trim()
            val newPassword = passwordInput.text.toString().trim()
            val newRole = if (roleGroup.checkedRadioButtonId == R.id.edit_radio_admin) "admin" else "karyawan"
            val newIsActive = statusGroup.checkedRadioButtonId == R.id.edit_radio_aktif
            val newAlasan = alasanInput.text.toString().trim()

            if (newUsername.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(requireContext(), "Username dan Telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!newIsActive && newAlasan.isEmpty()) {
                Toast.makeText(requireContext(), "Alasan non-aktif wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnUpdate.isEnabled = false
            btnUpdate.text = "Menyimpan..."

            lifecycleScope.launch {
                try {
                    // ✅ --- GANTI Map<String, Any?> DENGAN buildJsonObject ---
                    val dataToUpdate = buildJsonObject {
                        put("username", newUsername)
                        put("phone", newPhone)
                        put("role", newRole)
                        put("is_active", newIsActive)
                        put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault()).format(Date()))

                        if (newPassword.isNotEmpty()) {
                            if (newPassword.length < 6) {
                                Toast.makeText(requireContext(), "Password baru minimal 6 karakter", Toast.LENGTH_SHORT).show()
                                btnUpdate.isEnabled = true
                                btnUpdate.text = "Simpan Perubahan"
                                return@launch
                            }
                            put("password", newPassword)
                        }

                        if (newIsActive) {
                            // Gunakan JsonNull untuk mengirim nilai null secara eksplisit
                            put("deactivated_reason", JsonNull)
                            put("deactivated_at", JsonNull)
                        } else {
                            put("deactivated_reason", newAlasan)
                            put("deactivated_at", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                        }
                    }
                    // --------------------------------------------------------

                    supabase.from("pengguna").update(dataToUpdate) {
                        filter {
                            eq("iduser", currentUser.idUser)
                        }
                    }

                    Toast.makeText(requireContext(), "Data berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    listener?.onUserUpdated()
                    dismiss()

                } catch (e: Exception) {
                    Log.e(TAG, "Gagal update data: ", e)
                    Toast.makeText(requireContext(), "Gagal: Nomor telepon mungkin sudah terdaftar", Toast.LENGTH_LONG).show()
                    btnUpdate.isEnabled = true
                    btnUpdate.text = "Simpan Perubahan"
                }
            }
        }
    }

    fun setUserUpdatedListener(listener: UserUpdatedListener) {
        this.listener = listener
    }

    companion object {
        const val TAG = "EditUserDialog"
        private const val ARG_USER_JSON = "user_json"

        fun newInstance(pengguna: Pengguna): EditUserDialogFragment {
            val fragment = EditUserDialogFragment()
            val args = Bundle()
            args.putString(ARG_USER_JSON, Json.encodeToString(pengguna))
            fragment.arguments = args
            return fragment
        }
    }
}