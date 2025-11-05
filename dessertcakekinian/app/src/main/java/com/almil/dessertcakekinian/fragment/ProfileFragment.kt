package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.loginActivity
import com.almil.dessertcakekinian.dialog.editUserFragment
import com.almil.dessertcakekinian.dialog.ubahPassFragment
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

// Implementasikan interface EditUserDialogListener dari dialog
class ProfileFragment : Fragment(), editUserFragment.EditUserDialogListener {

    // View references
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvNIK: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvOutlet: TextView
    private lateinit var tvHiredDate: TextView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asumsi R.layout.fragment_profile adalah layout dari kode 2
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)

        // Bind views
        bindViews(view)

        // Load & tampilkan data dari SharedPreferences
        loadUserData()

        // Setup aksi tombol
        setupButtonActions()
    }

    private fun bindViews(view: View) {
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvRole = view.findViewById(R.id.tvRole)
        tvNIK = view.findViewById(R.id.tvNIK)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvOutlet = view.findViewById(R.id.tvOutlet)
        tvHiredDate = view.findViewById(R.id.tvHiredDate)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnLogout = view.findViewById(R.id.btnLogout)
    }

    fun loadUserData() { // Ubah akses menjadi public agar bisa dipanggil dari callback
        val userName = sharedPreferences.getString("USER_NAME", "Pengguna")
        val userRole = sharedPreferences.getString("USER_ROLE", "Karyawan")
        val userPhone = sharedPreferences.getString("USER_PHONE", "-")
        val userNIK = sharedPreferences.getString("USER_NIK", "Tidak tersedia")
        val userHiredDate = sharedPreferences.getString("USER_HIRED_DATE", null)
        val outletId = sharedPreferences.getInt("USER_OUTLET_ID", -1)

        // Tampilkan data
        tvUsername.text = userName
        tvRole.text = when (userRole?.lowercase()) {
            "admin" -> "Administrator"
            "kasir" -> "Kasir"
            "owner" -> "Pemilik"
            else -> userRole ?: "Karyawan"
        }

        tvNIK.text = userNIK
        tvPhone.text = userPhone

        // Format Outlet
        tvOutlet.text = if (outletId != -1) {
            "TK ${String.format("%02d", outletId)} - [Nama Outlet]" // Ganti dengan lookup jika ada
        } else {
            "Tidak terdaftar"
        }

        // Format Tanggal Masuk (dari "2023-06-10" → "10 Juni 2023")
        tvHiredDate.text = formatHiredDate(userHiredDate)
    }

    private fun formatHiredDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty() || dateStr == "null") return "Tidak tersedia"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateStr // fallback
        }
    }

    private fun setupButtonActions() {
        btnEditProfile.setOnClickListener {
            showEditUserDialog()
        }

        btnChangePassword.setOnClickListener {
            // Panggil showChangePasswordDialog
            showChangePasswordDialog()
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun showChangePasswordDialog() {
        // Ambil ID pengguna dari SharedPreferences
        val userId = sharedPreferences.getInt("USER_ID", 0)

        if (userId == 0) {
            Toast.makeText(requireContext(), "ID Pengguna tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Buat instance dialog dengan ID pengguna
        val dialog = ubahPassFragment.newInstance(userId)

        // Tampilkan dialog
        dialog.show(parentFragmentManager, "UbahPassDialog")
    }

    private fun showEditUserDialog() {
        // Ambil 4 data kunci dari SharedPreferences
        val userId = sharedPreferences.getInt("USER_ID", 0)
        val userName = sharedPreferences.getString("USER_NAME", "") ?: ""
        val userPhone = sharedPreferences.getString("USER_PHONE", "") ?: ""
        val userNIK = sharedPreferences.getString("USER_NIK", "") ?: ""

        // Buat instance dialog dengan data sesi
        val dialog = editUserFragment.newInstance(userId, userName, userPhone, userNIK)

        // Atur target Fragment agar listener dapat dipanggil
        dialog.setTargetFragment(this, 0)

        // Tampilkan dialog
        dialog.show(parentFragmentManager, "EditUserDialog")
    }

    // ⭐ IMPLEMENTASI CALLBACK DARI DIALOG ⭐
    override fun onUserUpdated(userId: Int, username: String, phone: String, nik: String) {

        sharedPreferences.edit().apply {
            putString("USER_NAME", username)
            putString("USER_PHONE", phone)
            putString("USER_NIK", nik)
            apply()
        }
        loadUserData()
    }

    private fun logout() {
        // Hapus semua sesi
        sharedPreferences.edit().clear().apply()

        // Pindah ke LoginActivity
        val intent = Intent(requireActivity(), loginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()

        Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show()
    }
}