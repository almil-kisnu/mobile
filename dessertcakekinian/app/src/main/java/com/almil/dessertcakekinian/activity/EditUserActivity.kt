// File: com/almil/dessertcakekinian/activity/EditUserActivity.kt

package com.almil.dessertcakekinian.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.Pengguna
import com.almil.dessertcakekinian.model.Outlet
import com.google.android.material.snackbar.Snackbar
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.JsonPrimitive


class EditUserActivity : AppCompatActivity() {

    private lateinit var currentUser: Pengguna
    private lateinit var spinnerOutlet: Spinner
    private lateinit var tvOutletTitle: TextView
    private lateinit var tvRoleTitle: TextView
    private lateinit var roleGroup: RadioGroup

    private lateinit var outletAdapter: ArrayAdapter<String>
    private var allOutlets = listOf<Outlet>()
    private var selectedOutletId: Int? = null

    companion object {
        const val EXTRA_USER_JSON = "extra_user_json"
        const val RESULT_USER_UPDATED = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user)

        // 1. Ambil role pengguna yang saat ini login dari SharedPreferences
        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val loggedInUserRole = sharedPreferences.getString("USER_ROLE", "karyawan")?.lowercase(Locale.ROOT) ?: "karyawan"

        val userJson = intent.getStringExtra(EXTRA_USER_JSON)
        if (userJson == null) {
            Toast.makeText(this, "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUser = Json.decodeFromString(userJson)

        // Inisialisasi Views
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val usernameInput = findViewById<EditText>(R.id.edit_username)
        val nikInput = findViewById<EditText>(R.id.edit_nik)
        val phoneInput = findViewById<EditText>(R.id.edit_phone)
        val passwordInput = findViewById<EditText>(R.id.edit_password)

        spinnerOutlet = findViewById(R.id.spinner_outlet)
        tvOutletTitle = findViewById(R.id.tv_outlet_title)
        roleGroup = findViewById(R.id.edit_role_group)
        tvRoleTitle = findViewById(R.id.tv_role_title)

        val statusGroup = findViewById<RadioGroup>(R.id.edit_status_group)
        val alasanInput = findViewById<EditText>(R.id.edit_alasan_nonaktif)
        val btnUpdate = findViewById<Button>(R.id.btn_update)

        // Set Data Awal
        usernameInput.setText(currentUser.username)
        nikInput.setText(currentUser.nik)
        phoneInput.setText(currentUser.phone)

        // Set RadioButton Role
        when (currentUser.role.lowercase(Locale.ROOT)) {
            "owner" -> roleGroup.check(R.id.edit_radio_owner)
            "admin" -> roleGroup.check(R.id.edit_radio_admin)
            "karyawan" -> roleGroup.check(R.id.edit_radio_karyawan)
        }

        // 2. Terapkan Logika Visibilitas Role berdasarkan Role Pengguna Login
        if (loggedInUserRole == "owner") {
            // OWNER: BISA LIHAT SEMUA ROLE (TAMPIL)
            tvRoleTitle.visibility = View.VISIBLE
            roleGroup.visibility = View.VISIBLE
        } else {
            // ADMIN/KARYAWAN: TIDAK BISA LIHAT/EDIT ROLE (SEMBUNYI)
            tvRoleTitle.visibility = View.GONE
            roleGroup.visibility = View.GONE
        }

        if (currentUser.isActive) {
            statusGroup.check(R.id.edit_radio_aktif)
            alasanInput.visibility = View.GONE
        } else {
            statusGroup.check(R.id.edit_radio_nonaktif)
            alasanInput.visibility = View.VISIBLE
            alasanInput.setText(currentUser.deactivatedReason)
        }

        // Listener
        btnBack.setOnClickListener { finish() }

        statusGroup.setOnCheckedChangeListener { _, checkedId ->
            alasanInput.visibility = if (checkedId == R.id.edit_radio_nonaktif) View.VISIBLE else View.GONE
        }

        // 3. Fetch Outlets dan Setup Spinner
        fetchOutlets {
            setupOutletSpinner()
            setupRoleListener()
            // Panggil updateRoleDisplay untuk tampilan awal Spinner Outlet
            // Jika roleGroup disembunyikan, kita gunakan role currentUser
            val initialCheckedId = if (roleGroup.visibility == View.VISIBLE) roleGroup.checkedRadioButtonId else 0
            updateRoleDisplay(initialCheckedId)
        }

        // 4. Button Update Logic
        btnUpdate.setOnClickListener {
            handleUpdate(
                usernameInput, nikInput, phoneInput, passwordInput, roleGroup, statusGroup, alasanInput, btnUpdate
            )
        }
    }

    private fun updateRoleDisplay(checkedId: Int) {
        // Logika ini menentukan apakah Spinner Outlet ditampilkan/disembunyikan
        // OWNER tidak butuh Outlet, ADMIN/KARYAWAN butuh.

        val selectedRole = if (roleGroup.visibility == View.VISIBLE) {
            when (checkedId) {
                R.id.edit_radio_owner -> "owner"
                R.id.edit_radio_admin -> "admin"
                R.id.edit_radio_karyawan -> "karyawan"
                else -> currentUser.role
            }
        } else {
            // Jika RadioGroup disembunyikan (bukan Owner yang login), gunakan role pengguna yang diedit (currentUser)
            currentUser.role
        }

        if (selectedRole.equals("owner", ignoreCase = true)) {
            tvOutletTitle.visibility = View.GONE
            spinnerOutlet.visibility = View.GONE
        } else {
            tvOutletTitle.visibility = View.VISIBLE
            spinnerOutlet.visibility = View.VISIBLE
        }
    }

    /**
     * Mengatur logika tampilan Spinner Outlet berdasarkan pilihan Role.
     */
    private fun setupRoleListener() {
        roleGroup.setOnCheckedChangeListener { _, checkedId ->
            updateRoleDisplay(checkedId)

            val selectedRole = when (checkedId) {
                R.id.edit_radio_owner -> "owner"
                R.id.edit_radio_admin -> "admin"
                R.id.edit_radio_karyawan -> "karyawan"
                else -> ""
            }

            if (selectedRole.equals("owner", ignoreCase = true)) {
                // Owner dipilih: Kosongkan Outlet ID
                selectedOutletId = null
                spinnerOutlet.setSelection(0)
            } else if (selectedRole.equals("admin", ignoreCase = true) || selectedRole.equals("karyawan", ignoreCase = true)) {
                // Admin/Karyawan dipilih: Coba isi dengan ID Outlet lama jika ada
                if (currentUser.idOutlet != null) {
                    val currentOutletIndex = allOutlets.indexOfFirst { it.idOutlet == currentUser.idOutlet }
                    if (currentOutletIndex != -1) {
                        spinnerOutlet.setSelection(currentOutletIndex + 1)
                        selectedOutletId = currentUser.idOutlet
                    }
                }
            }
        }
    }

    private fun fetchOutlets(onSuccess: () -> Unit) {
        lifecycleScope.launch {
            try {
                // Hanya ambil kolom yang dibutuhkan: idoutlet, kode_outlet, nama_outlet
                val response = SupabaseClientProvider.client.from("outlet").select()
                allOutlets = response.decodeList<Outlet>()
                onSuccess()
            } catch (e: Exception) {
                Log.e("EditUserActivity", "Gagal mengambil data outlet: ", e)
                Toast.makeText(this@EditUserActivity, "Gagal memuat data toko.", Toast.LENGTH_SHORT).show()
                allOutlets = emptyList()
            }
        }
    }

    private fun setupOutletSpinner() {
        // Buat list nama outlet (kode dan nama) untuk Spinner
        val outletNames = allOutlets.map { "${it.kodeOutlet} - ${it.namaOutlet}" }.toMutableList()
        outletNames.add(0, "Pilih Toko/Outlet") // Tambahkan hint di posisi pertama

        outletAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, outletNames)
        outletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOutlet.adapter = outletAdapter

        // Set nilai awal Spinner berdasarkan data pengguna saat ini
        val currentOutletIndex = allOutlets.indexOfFirst { it.idOutlet == currentUser.idOutlet }
        if (currentOutletIndex != -1) {
            spinnerOutlet.setSelection(currentOutletIndex + 1) // +1 karena ada hint
            selectedOutletId = currentUser.idOutlet
        } else {
            spinnerOutlet.setSelection(0)
            selectedOutletId = null
        }

        spinnerOutlet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedOutletId = if (position > 0) {
                    // Ambil ID dari objek Outlet yang sesuai
                    allOutlets[position - 1].idOutlet
                } else {
                    null // Posisi 0 adalah hint
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { selectedOutletId = null }
        }
    }

    private fun handleUpdate(
        usernameInput: EditText,
        nikInput: EditText,
        phoneInput: EditText,
        passwordInput: EditText,
        roleGroup: RadioGroup,
        statusGroup: RadioGroup,
        alasanInput: EditText,
        btnUpdate: Button
    ) {
        val newUsername = usernameInput.text.toString().trim()
        val newNik = nikInput.text.toString().trim()
        val newPhone = phoneInput.text.toString().trim()
        val newPassword = passwordInput.text.toString().trim()

        // Ambil role baru hanya jika RadioGroup terlihat, jika tidak, gunakan role lama (currentUser.role)
        val newRole = if (roleGroup.visibility == View.VISIBLE) {
            when (roleGroup.checkedRadioButtonId) {
                R.id.edit_radio_owner -> "owner"
                R.id.edit_radio_admin -> "admin"
                R.id.edit_radio_karyawan -> "karyawan"
                else -> currentUser.role // Fallback ke role lama jika tidak ada yang terpilih
            }
        } else {
            currentUser.role // Gunakan role yang ada karena tidak bisa diubah
        }

        val newIsActive = statusGroup.checkedRadioButtonId == R.id.edit_radio_aktif
        val newAlasan = alasanInput.text.toString().trim()

        // Validasi
        if (newUsername.isEmpty() || newNik.isEmpty() || newPhone.isEmpty()) {
            Snackbar.make(btnUpdate, "Username, NIK, dan Telepon tidak boleh kosong", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (!newIsActive && newAlasan.isEmpty()) {
            Snackbar.make(btnUpdate, "Alasan non-aktif wajib diisi", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (newNik.length != 16) {
            Snackbar.make(btnUpdate, "NIK harus 16 digit", Snackbar.LENGTH_SHORT).show()
            return
        }
        // Validasi Outlet: Admin dan Karyawan wajib memilih Outlet
        if ((newRole.equals("admin", ignoreCase = true) || newRole.equals("karyawan", ignoreCase = true)) && selectedOutletId == null) {
            Snackbar.make(btnUpdate, "${newRole.capitalize(Locale.ROOT)} wajib memilih Toko/Outlet", Snackbar.LENGTH_SHORT).show()
            return
        }
        // ----------------------------------------------------------------------------------

        btnUpdate.isEnabled = false
        btnUpdate.text = "Menyimpan..."

        lifecycleScope.launch {
            try {
                // Penentuan idoutlet: NULL hanya untuk OWNER. Harus ada ID untuk ADMIN/KARYAWAN.
                val outletIdElement = if (newRole.equals("owner", ignoreCase = true)) {
                    JsonNull
                } else {
                    // Gunakan selectedOutletId (yang divalidasi tidak null untuk admin/karyawan)
                    selectedOutletId?.let { JsonPrimitive(it) } ?: JsonNull
                }

                val dataToUpdate = buildJsonObject {
                    put("username", newUsername)
                    put("nik", newNik)
                    put("phone", newPhone)
                    put("role", newRole)
                    // Kirim null jika Owner, kirim ID jika Admin/Karyawan
                    put(key = "idoutlet", element = outletIdElement)
                    put("is_active", newIsActive)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault()).format(Date()))

                    if (newPassword.isNotEmpty()) {
                        if (newPassword.length < 6) {
                            Snackbar.make(btnUpdate, "Password baru minimal 6 karakter", Snackbar.LENGTH_SHORT).show()
                            btnUpdate.isEnabled = true
                            btnUpdate.text = "Simpan Perubahan"
                            return@launch
                        }
                        put("password", newPassword)
                    }

                    if (newIsActive) {
                        put("deactivated_reason", JsonNull)
                        put("deactivated_at", JsonNull)
                    } else {
                        put("deactivated_reason", newAlasan)
                        put("deactivated_at", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                    }
                }

                SupabaseClientProvider.client.from("pengguna").update(dataToUpdate) {
                    filter {
                        eq("iduser", currentUser.idUser)
                    }
                }

                Toast.makeText(this@EditUserActivity, "Data berhasil diperbarui! 🎉", Toast.LENGTH_SHORT).show()

                setResult(RESULT_USER_UPDATED)
                finish()

            } catch (e: Exception) {
                Log.e("EditUserActivity", "Gagal update data: ", e)
                Toast.makeText(this@EditUserActivity, "Gagal: Nomor telepon/NIK mungkin sudah terdaftar", Toast.LENGTH_LONG).show()
                btnUpdate.isEnabled = true
                btnUpdate.text = "Simpan Perubahan"
            }
        }
    }
}