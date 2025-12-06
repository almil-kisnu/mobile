package com.almil.dessertcakekinian.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.loginActivity
import com.almil.dessertcakekinian.dialog.editUserFragment
import com.almil.dessertcakekinian.dialog.ubahPassFragment
import com.almil.dessertcakekinian.model.OrderRepository
import com.almil.dessertcakekinian.model.ProductRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment(), editUserFragment.EditUserDialogListener {

    // View references
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var fabEditPhoto: FloatingActionButton
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
    private val PREF_PROFILE_PHOTO = "PROFILE_PHOTO_PATH"

    // Repositories
    private lateinit var productRepository: ProductRepository
    private lateinit var orderRepository: OrderRepository

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { saveProfilePhoto(it) }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "Permission diperlukan untuk memilih foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)

        // Inisialisasi Repositories
        productRepository = ProductRepository.getInstance(requireContext())
        orderRepository = OrderRepository.getInstance(requireContext())

        // Bind views
        bindViews(view)

        // Load & tampilkan data dari SharedPreferences
        loadUserData()

        // Setup aksi tombol
        setupButtonActions()
    }

    private fun bindViews(view: View) {
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto)
        fabEditPhoto = view.findViewById(R.id.fabEditPhoto)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvRole = view.findViewById(R.id.tvRole)
        tvNIK = view.findViewById(R.id.tvNIK)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvOutlet = view.findViewById(R.id.tvOutlet)
        tvHiredDate = view.findViewById(R.id.tvHiredDate)
      //  btnEditProfile = view.findViewById(R.id.btnEditProfile)
      //  btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnLogout = view.findViewById(R.id.btnLogout)
    }

    fun loadUserData() {
        val userName = sharedPreferences.getString("USER_NAME", "Pengguna")
        val userRole = sharedPreferences.getString("USER_ROLE", "Karyawan")
        val userPhone = sharedPreferences.getString("USER_PHONE", "-")
        val userNIK = sharedPreferences.getString("USER_NIK", "Tidak tersedia")
        val userHiredDate = sharedPreferences.getString("USER_HIRED_DATE", null)
        val outletId = sharedPreferences.getInt("USER_OUTLET_ID", -1)
        val outletKode = sharedPreferences.getString("OUTLET_KODE", "")
        val outletNama = sharedPreferences.getString("OUTLET_NAMA", "")

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
            "$outletKode - $outletNama"
        } else {
            "Tidak terdaftar"
        }

        // Format Tanggal Masuk
        tvHiredDate.text = formatHiredDate(userHiredDate)
        
        // Load profile photo
        loadProfilePhoto()
    }

    private fun formatHiredDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty() || dateStr == "null") return "Tidak tersedia"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun setupButtonActions() {
     //   btnEditProfile.setOnClickListener {
     //      showEditUserDialog()
     //   }

     //   btnChangePassword.setOnClickListener {
     //       showChangePasswordDialog()
     //   }

        fabEditPhoto.setOnClickListener {
            showPhotoOptionsDialog()
        }

        btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun showChangePasswordDialog() {
        val userId = sharedPreferences.getInt("USER_ID", 0)

        if (userId == 0) {
            Toast.makeText(requireContext(), "ID Pengguna tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = ubahPassFragment.newInstance(userId)
        dialog.show(parentFragmentManager, "UbahPassDialog")
    }

    private fun showEditUserDialog() {
        val userId = sharedPreferences.getInt("USER_ID", 0)
        val userName = sharedPreferences.getString("USER_NAME", "") ?: ""
        val userPhone = sharedPreferences.getString("USER_PHONE", "") ?: ""
        val userNIK = sharedPreferences.getString("USER_NIK", "") ?: ""

        val dialog = editUserFragment.newInstance(userId, userName, userPhone, userNIK)
        dialog.setTargetFragment(this, 0)
        dialog.show(parentFragmentManager, "EditUserDialog")
    }

    override fun onUserUpdated(userId: Int, username: String, phone: String, nik: String) {
        sharedPreferences.edit().apply {
            putString("USER_NAME", username)
            putString("USER_PHONE", phone)
            putString("USER_NIK", nik)
            apply()
        }
        loadUserData()
    }

    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Edit Foto", "Hapus Foto")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Foto Profil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkPermissionAndPickImage() // Edit Foto
                    1 -> deleteProfilePhoto() // Hapus Foto
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteProfilePhoto() {
        try {
            // Delete file from storage
            val photoPath = sharedPreferences.getString(PREF_PROFILE_PHOTO, null)
            photoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            
            // Clear from SharedPreferences
            sharedPreferences.edit().remove(PREF_PROFILE_PHOTO).apply()
            
            // Reset to default icon
            ivProfilePhoto.setImageResource(R.drawable.ic_anonim)
            
            Toast.makeText(requireContext(), "Foto profil berhasil dihapus", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal menghapus foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(requireContext(), "Izin diperlukan untuk memilih foto profil", Toast.LENGTH_SHORT).show()
                permissionLauncher.launch(permission)
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun saveProfilePhoto(uri: Uri) {
        try {
            // Create directory if not exists
            val profileDir = File(requireContext().filesDir, "profile")
            if (!profileDir.exists()) {
                profileDir.mkdirs()
            }
            
            // Create file
            val photoFile = File(profileDir, "profile_photo.jpg")
            
            // Copy image to internal storage
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(photoFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Save path to SharedPreferences
            sharedPreferences.edit().putString(PREF_PROFILE_PHOTO, photoFile.absolutePath).apply()
            
            // Load and display the new photo
            loadProfilePhoto()
            
            Toast.makeText(requireContext(), "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal menyimpan foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfilePhoto() {
        val photoPath = sharedPreferences.getString(PREF_PROFILE_PHOTO, null)
        
        if (photoPath != null && File(photoPath).exists()) {
            ivProfilePhoto.load(File(photoPath)) {
                crossfade(true)
                placeholder(R.drawable.ic_anonim)
                error(R.drawable.ic_anonim)
            }
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_anonim)
        }
    }

    private fun performLogout() {
        // Disable button untuk mencegah double click
        btnLogout.isEnabled = false
        btnLogout.text = "Logging out..."

        lifecycleScope.launch {
            try {
                // Delete profile photo on logout
                val photoPath = sharedPreferences.getString(PREF_PROFILE_PHOTO, null)
                photoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }

                // Cleanup repositories
                productRepository.cleanup()
                orderRepository.cleanup()

                // Clear all local data
                productRepository.clearAllData()
                orderRepository.clearAllData()

                // Hapus semua sesi
                sharedPreferences.edit().clear().apply()

                // Pindah ke LoginActivity
                val intent = Intent(requireActivity(), loginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

                Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Jika terjadi error, tetap logout dari UI
                Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()

                sharedPreferences.edit().clear().apply()

                val intent = Intent(requireActivity(), loginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            } finally {
                btnLogout.isEnabled = true
                btnLogout.text = "Keluar"
            }
        }
    }
}