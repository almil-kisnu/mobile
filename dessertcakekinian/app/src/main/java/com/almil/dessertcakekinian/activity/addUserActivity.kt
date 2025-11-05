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
import com.almil.dessertcakekinian.model.Outlet
import com.google.android.material.snackbar.Snackbar
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

class addUserActivity : AppCompatActivity() {

    private lateinit var spinnerOutlet: Spinner
    private lateinit var tvOutletTitle: TextView
    private lateinit var tvRoleTitle: TextView
    private lateinit var roleGroup: RadioGroup
    private lateinit var btnSave: Button

    private lateinit var outletAdapter: ArrayAdapter<String>
    private var allOutlets = listOf<Outlet>()
    private var selectedOutletId: Int? = null

    // Role pengguna yang sedang login (untuk pembatasan UI)
    private var loggedInUserRole: String = "karyawan"
    private var loggedInUserOutletId: Int? = null

    companion object {
        const val RESULT_USER_ADDED = 2 // Kode hasil khusus untuk Add User
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user) // Pastikan layout_name benar

        // Ambil data user login dari SharedPreferences
        val sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        loggedInUserRole = sharedPreferences.getString("USER_ROLE", "karyawan")?.lowercase(Locale.ROOT) ?: "karyawan"
        loggedInUserOutletId = sharedPreferences.getInt("USER_OUTLET_ID", -1).let {
            if (it == -1) null else it
        }

        // Inisialisasi Views
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        spinnerOutlet = findViewById(R.id.spinner_outlet)
        tvOutletTitle = findViewById(R.id.tv_outlet_title)
        roleGroup = findViewById(R.id.add_role_group)
        tvRoleTitle = findViewById(R.id.tv_role_title)
        btnSave = findViewById(R.id.btn_save)

        // Set Data Input
        val usernameInput = findViewById<EditText>(R.id.add_username)
        val nikInput = findViewById<EditText>(R.id.add_nik)
        val phoneInput = findViewById<EditText>(R.id.add_phone)
        val passwordInput = findViewById<EditText>(R.id.add_password)

        // --- Logika Visibilitas Role (Hanya Owner yang bisa melihat dan memilih Role) ---
        if (loggedInUserRole == "owner") {
            tvRoleTitle.visibility = View.VISIBLE
            roleGroup.visibility = View.VISIBLE
            // Default pilih Karyawan
            roleGroup.check(R.id.add_radio_karyawan)
        } else {
            // Admin/Karyawan: Sembunyikan dan role defaultnya akan menjadi Karyawan/Admin sesuai role login (didefinisikan di handleSave)
            tvRoleTitle.visibility = View.GONE
            roleGroup.visibility = View.GONE
            spinnerOutlet.isEnabled = false
        }
        // -------------------------------------------------------------------------------

        // Listener
        btnBack.setOnClickListener { finish() }

        // Fetch Outlets dan Setup Spinner
        fetchOutlets {
            setupOutletSpinner()
            setupRoleListener()
            updateOutletDisplay() // Panggil untuk menampilkan Spinner Outlet default
        }

        // Button Save Logic
        btnSave.setOnClickListener {
            handleSave(usernameInput, nikInput, phoneInput, passwordInput)
        }
    }

    /** Menampilkan/menyembunyikan Spinner Outlet berdasarkan role yang dipilih */
    private fun updateOutletDisplay(checkedId: Int? = null) {
        val selectedRole = if (roleGroup.visibility == View.VISIBLE) {
            when (checkedId ?: roleGroup.checkedRadioButtonId) {
                R.id.add_radio_owner -> "owner"
                R.id.add_radio_admin -> "admin"
                R.id.add_radio_karyawan -> "karyawan"
                else -> loggedInUserRole // Fallback
            }
        } else {
            // Jika RadioGroup disembunyikan (Admin/Karyawan login), gunakan role login mereka
            loggedInUserRole
        }

        // Admin dan Karyawan butuh Outlet, Owner tidak
        val needsOutlet = selectedRole.equals("admin", ignoreCase = true) || selectedRole.equals("karyawan", ignoreCase = true)

        if (needsOutlet) {
            tvOutletTitle.visibility = View.VISIBLE
            spinnerOutlet.visibility = View.VISIBLE
        } else {
            tvOutletTitle.visibility = View.GONE
            spinnerOutlet.visibility = View.GONE
        }
    }

    /** Mengatur listener untuk RadioGroup Role */
    private fun setupRoleListener() {
        roleGroup.setOnCheckedChangeListener { _, checkedId ->
            updateOutletDisplay(checkedId)

            val selectedRole = when (checkedId) {
                R.id.add_radio_owner -> "owner"
                R.id.add_radio_admin -> "admin"
                R.id.add_radio_karyawan -> "karyawan"
                else -> ""
            }

            if (selectedRole.equals("owner", ignoreCase = true)) {
                selectedOutletId = null
                spinnerOutlet.setSelection(0)
            } else {
                // Jika Admin/Karyawan: Coba set ke outlet pengguna yang login jika itu Admin/Karyawan
                if (!loggedInUserRole.equals("owner", ignoreCase = true) && loggedInUserOutletId != null) {
                    val defaultOutletIndex = allOutlets.indexOfFirst { it.idOutlet == loggedInUserOutletId }
                    if (defaultOutletIndex != -1) {
                        spinnerOutlet.setSelection(defaultOutletIndex + 1)
                        selectedOutletId = loggedInUserOutletId
                    }
                }
            }
        }
    }

    private fun fetchOutlets(onSuccess: () -> Unit) {
        lifecycleScope.launch {
            try {
                val response = SupabaseClientProvider.client.from("outlet").select()
                allOutlets = response.decodeList<Outlet>()
                onSuccess()
            } catch (e: Exception) {
                Log.e("AddUserActivity", "Gagal mengambil data outlet: ", e)
                Toast.makeText(this@addUserActivity, "Gagal memuat data toko.", Toast.LENGTH_SHORT).show()
                allOutlets = emptyList()
            }
        }
    }

    private fun setupOutletSpinner() {
        val outletNames = allOutlets.map { "${it.kodeOutlet} - ${it.namaOutlet}" }.toMutableList()
        outletNames.add(0, "Pilih Toko/Outlet")

        outletAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, outletNames)
        outletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOutlet.adapter = outletAdapter

        // Default selection logic:
        // Jika user login adalah Admin/Karyawan, set default ke outlet mereka
        if (!loggedInUserRole.equals("owner", ignoreCase = true) && loggedInUserOutletId != null) {
            val defaultIndex = allOutlets.indexOfFirst { it.idOutlet == loggedInUserOutletId }
            if (defaultIndex != -1) {
                spinnerOutlet.setSelection(defaultIndex + 1)
                selectedOutletId = loggedInUserOutletId
            }
        }

        spinnerOutlet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedOutletId = if (position > 0) {
                    allOutlets[position - 1].idOutlet
                } else {
                    null // Posisi 0 adalah hint
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { selectedOutletId = null }
        }
    }

    private fun handleSave(
        usernameInput: EditText,
        nikInput: EditText,
        phoneInput: EditText,
        passwordInput: EditText
    ) {
        val username = usernameInput.text.toString().trim()
        val nik = nikInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        // Tentukan Role yang akan disimpan
        val finalRole = if (loggedInUserRole == "owner") {
            when (roleGroup.checkedRadioButtonId) {
                R.id.add_radio_owner -> "owner"
                R.id.add_radio_admin -> "admin"
                R.id.add_radio_karyawan -> "karyawan"
                else -> "karyawan" // Default jika Owner lupa memilih
            }
        } else {
            // Jika login sebagai Admin/Karyawan, mereka hanya bisa menambah Karyawan
            "karyawan"
        }

        // --- Validasi ---
        if (username.isEmpty() || nik.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Snackbar.make(btnSave, "Semua kolom (kecuali Outlet) wajib diisi.", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (nik.length != 16) {
            Snackbar.make(btnSave, "NIK harus 16 digit.", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Snackbar.make(btnSave, "Password minimal 6 karakter.", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Validasi Outlet untuk Admin/Karyawan
        if (!finalRole.equals("owner", ignoreCase = true) && selectedOutletId == null) {
            Snackbar.make(btnSave, "${finalRole.capitalize(Locale.ROOT)} wajib memilih Toko/Outlet.", Snackbar.LENGTH_SHORT).show()
            return
        }
        // ----------------

        btnSave.isEnabled = false
        btnSave.text = "Menyimpan..."

        lifecycleScope.launch {
            try {
                // Penentuan idoutlet: NULL hanya untuk OWNER. Harus ada ID untuk ADMIN/KARYAWAN.
                val outletIdElement = if (finalRole.equals("owner", ignoreCase = true)) {
                    JsonNull
                } else {
                    selectedOutletId?.let { JsonPrimitive(it) } ?: JsonNull
                }

                val dataToInsert = buildJsonObject {
                    put("username", username)
                    put("nik", nik)
                    put("phone", phone)
                    put("password", password) // Supabase akan menghash ini
                    put("role", finalRole)
                    put(key = "idoutlet", element = outletIdElement)
                    put("is_active", true) // Pengguna baru selalu aktif
                }

                SupabaseClientProvider.client.from("pengguna").insert(dataToInsert)

                Toast.makeText(this@addUserActivity, "Pengguna ${username} berhasil ditambahkan! 🎉", Toast.LENGTH_SHORT).show()

                setResult(RESULT_USER_ADDED)
                finish()

            } catch (e: Exception) {
                Log.e("AddUserActivity", "Gagal insert data: ", e)
                // Pesan error umum
                Toast.makeText(this@addUserActivity, "Gagal: Nomor telepon/NIK mungkin sudah terdaftar.", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
                btnSave.text = "Simpan Pengguna"
            }
        }
    }
}