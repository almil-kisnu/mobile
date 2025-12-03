package com.almil.dessertcakekinian.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.JadwalMingguanApi
import com.almil.dessertcakekinian.database.PenggunaApi
import com.almil.dessertcakekinian.database.ShiftDefinitionApi
import com.almil.dessertcakekinian.database.SupabaseHelper
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class AbsenMasukActivity : AppCompatActivity() {

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val TARGET_LATITUDE = -8.157678
        const val TARGET_LONGITUDE = 113.723130
        const val RADIUS_METERS = 50.0f
    }

    private lateinit var tvNamaKaryawan: TextView
    private lateinit var tvLokasi: TextView
    private lateinit var tvJamRealTime: TextView
    private lateinit var tvTanggalRealTime: TextView
    private lateinit var btnKonfirmasi: Button
    private lateinit var btnBack: ImageButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentAddress = "Lokasi tidak diketahui"
    private var isWithinLocation = false

    // Variabel untuk shift
    private var userShift: String = "Pagi"
    private var userShiftStart: String = "07:00"
    private var userShiftEnd: String = "12:00"
    private var currentHour: Int = 0
    private var currentMinute: Int = 0
    private var isShiftValid: Boolean = false

    // Executor untuk background tasks
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absen_masuk)

        // Initialize views
        tvNamaKaryawan = findViewById(R.id.tvNamaKaryawan)
        tvLokasi = findViewById(R.id.tvLokasi)
        tvJamRealTime = findViewById(R.id.tvJamRealTime)
        tvTanggalRealTime = findViewById(R.id.tvTanggalRealTime)
        btnKonfirmasi = findViewById(R.id.btnKonfirmasi)
        btnBack = findViewById(R.id.btnBack)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Reset status absen jika sudah hari baru
        resetAbsenStatusIfNeeded()

        // Untuk testing - enable dev mode
        getSharedPreferences("absen_data", Context.MODE_PRIVATE).edit {
            putBoolean("dev_mode", false)
        }

        // ✅ PERBAIKAN: Ambil username yang benar dari database
        getCorrectUsernameFromDatabase()

        // Set tanggal dan waktu real-time
        setCurrentDateTime()

        // Setup button listeners
        setupButtonListeners()

        // ✅ AUTO CHECKOUT: Cek dan lengkapi data kemarin yang lupa pulang
        checkAndAutoCompleteYesterday()

        // ✅ PERBAIKAN: Langsung request location permission di onCreate
        requestLocationPermission()
    }

    // ✅ PERBAIKAN: Ambil username yang benar dari database
    private fun getCorrectUsernameFromDatabase() {
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val storedUsername = userSession.getString("USER_NAME", "") ?: ""

        if (storedUsername.isEmpty()) {
            println("❌ Tidak ada username di SharedPreferences")
            showShiftError("Session login tidak valid")
            return
        }

        println("🔍 Mencari username yang benar untuk: '$storedUsername'")

        // Cari username yang sebenarnya di database
        PenggunaApi().listAll(object : PenggunaApi.PenggunaListCallback {
            override fun onSuccess(list: List<PenggunaApi.Pengguna>) {
                println("🔍 Daftar semua user di database:")
                list.forEach { user ->
                    println("   - '${user.username}' (ID: ${user.iduser})")
                }

                // ✅ CARI USERNAME YANG SESUAI (case insensitive + partial match)
                val correctUser = list.find { user ->
                    user.username.equals(storedUsername, ignoreCase = true) ||
                            storedUsername.contains(user.username, ignoreCase = true) ||
                            user.username.contains(storedUsername, ignoreCase = true)
                }

                if (correctUser != null) {
                    val correctUsername = correctUser.username
                    println("✅ Username ditemukan: '$storedUsername' -> '$correctUsername'")

                    // Update SharedPreferences dengan username yang benar
                    userSession.edit().putString("USER_NAME", correctUsername).apply()
                    tvNamaKaryawan.text = correctUsername

                    // Lanjut ambil data shift
                    getUserShiftFromJadwal(correctUsername)
                } else {
                    println("❌ Username '$storedUsername' tidak ditemukan di database")
                    showShiftError("User '$storedUsername' tidak terdaftar di sistem")
                }
            }

            override fun onError(error: String) {
                println("❌ Gagal ambil data user: $error")
                // Fallback: pakai username dari SharedPreferences
                tvNamaKaryawan.text = storedUsername
                getUserShiftFromJadwal(storedUsername)
            }
        })
    }

    // ✅ PERBAIKAN: Fungsi ambil shift user dengan parameter username
    private fun getUserShiftFromJadwal(userName: String) {
        if (userName.isEmpty()) {
            println("❌ Tidak bisa ambil shift: nama user kosong")
            showShiftError("Tidak bisa mengambil data shift: Nama user tidak ditemukan")
            return
        }

        println("🔍 Mencari jadwal untuk user: '$userName'")

        // ✅ PERBAIKAN: Ambil data shift definitions dulu dengan timeout
        ShiftDefinitionApi().listAll(object : ShiftDefinitionApi.ShiftListCallback {
            override fun onSuccess(shiftList: List<ShiftDefinitionApi.ShiftDefinition>) {
                println("✅ Dapat ${shiftList.size} shift definitions")

                if (shiftList.isEmpty()) {
                    showShiftError("Data shift definitions kosong")
                    return
                }

                // Setelah dapat shift definitions, ambil jadwal user
                JadwalMingguanApi().listAllWithDetails(object : JadwalMingguanApi.JadwalListCallback {
                    override fun onSuccess(list: List<JadwalMingguanApi.JadwalMingguan>) {
                        val today = SimpleDateFormat("EEEE", Locale("id", "ID")).format(Date()).lowercase()
                        println("📅 Hari ini: $today")
                        println("🔍 Total jadwal ditemukan: ${list.size}")

                        // ✅ PERBAIKAN: Cari jadwal dengan nama pengguna yang match persis
                        val userJadwal = list.find {
                            it.nama_pengguna.equals(userName, ignoreCase = true)
                        }

                        if (userJadwal != null) {
                            println("✅ Jadwal ditemukan untuk user: '$userName'")

                            // Dapatkan nama shift user untuk hari ini
                            userShift = when (today) {
                                "senin" -> userJadwal.shift_senin.ifEmpty { "Pagi" }
                                "selasa" -> userJadwal.shift_selasa.ifEmpty { "Pagi" }
                                "rabu" -> userJadwal.shift_rabu.ifEmpty { "Pagi" }
                                "kamis" -> userJadwal.shift_kamis.ifEmpty { "Pagi" }
                                "jumat" -> userJadwal.shift_jumat.ifEmpty { "Pagi" }
                                "sabtu" -> userJadwal.shift_sabtu.ifEmpty { "Pagi" }
                                "minggu" -> userJadwal.shift_minggu.ifEmpty { "Pagi" }
                                else -> "Pagi"
                            }

                            println("🔍 Shift untuk hari $today: $userShift")

                            // ✅ PERBAIKAN: Cari jam shift dari shift definitions dengan case insensitive
                            val shiftDefinition = shiftList.find {
                                it.nama_shift.equals(userShift, ignoreCase = true)
                            }

                            if (shiftDefinition != null) {
                                userShiftStart = shiftDefinition.jam_mulai
                                userShiftEnd = shiftDefinition.jam_selesai
                                println("✅ Shift $userName: $userShift ($userShiftStart - $userShiftEnd)")

                                // Simpan shift ke SharedPreferences
                                saveShiftToPreferences(userShift, userShiftStart, userShiftEnd)

                                // ✅ PERBAIKAN BARU: Validasi waktu dengan logika yang benar untuk shift malam
                                validateShiftTimeWithDatabaseLogic()

                                // Check absen status setelah dapat shift
                                checkAbsenStatusBasedOnShift(userName)
                            } else {
                                println("❌ Shift definition tidak ditemukan untuk: $userShift")
                                showShiftError("Shift '$userShift' tidak ditemukan dalam definitions")
                            }
                        } else {
                            println("❌ Jadwal tidak ditemukan untuk user: '$userName'")
                            println("🔍 Daftar nama pengguna yang tersedia:")
                            list.forEach { jadwal ->
                                println("   - '${jadwal.nama_pengguna}'")
                            }
                            showShiftError("Jadwal tidak ditemukan untuk user: '$userName'")
                        }
                    }

                    override fun onError(error: String) {
                        println("❌ Error ambil jadwal: $error")
                        showShiftError("Gagal mengambil jadwal: $error")
                    }
                })
            }

            override fun onError(error: String) {
                println("❌ Error ambil shift definitions: $error")
                showShiftError("Gagal mengambil data shift: $error")
            }
        })
    }

    // ✅ PERBAIKAN BARU: Validasi waktu dengan logika yang benar untuk shift malam
    private fun validateShiftTimeWithDatabaseLogic() {
        val calendar = Calendar.getInstance()
        currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        currentMinute = calendar.get(Calendar.MINUTE)

        val currentTime = "$currentHour:$currentMinute"
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        try {
            val current = sdf.parse(currentTime)
            val start = sdf.parse(userShiftStart)
            val end = sdf.parse(userShiftEnd)

            // Logika untuk menentukan apakah waktu sekarang valid untuk shift
            if (end.before(start)) {
                // Shift melewati tengah malam (contoh: 18:00 - 00:00 atau 22:00 - 02:00)
                // Waktu valid jika: current >= start ATAU current <= end
                isShiftValid = current >= start || current <= end
            } else {
                // Shift normal (tidak melewati tengah malam)
                // Waktu valid jika: start <= current < end
                isShiftValid = current >= start && current < end
            }

            println("🔍 Validasi Shift: $userShift ($userShiftStart - $userShiftEnd)")
            println("🔍 Waktu Sekarang: $currentTime")
            println("🔍 Status Valid: $isShiftValid")

            runOnUiThread {
                if (!isShiftValid) {
                    btnKonfirmasi.isEnabled = false
                    btnKonfirmasi.text = "Tidak Dalam Shift"

                    val currentTimeFormatted = String.format("%02d:%02d", currentHour, currentMinute)
                    val errorMessage = "⏰ **BUKAN JADWAL SHIFT ANDA**\n\n" +
                            "Shift Anda: **$userShift** ($userShiftStart - $userShiftEnd)\n" +
                            "Sekarang jam: $currentTimeFormatted\n\n" +
                            "❌ **TIDAK BISA ABSEN MASUK**\n" +
                            "• Absen masuk hanya bisa dilakukan selama shift berlangsung\n" +
                            "• Silakan kembali pada jam shift Anda"

                    AlertDialog.Builder(this)
                        .setTitle("❌ Bukan Jadwal Shift Anda")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    btnKonfirmasi.isEnabled = true
                    btnKonfirmasi.text = "KONFIRMASI ABSEN MASUK"
                    println("✅ Validasi shift MASUK: $userShift ($userShiftStart-$userShiftEnd) - Jam $currentHour:$currentMinute - DIPERBOLEHKAN")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            isShiftValid = true // Fallback jika parsing error
            runOnUiThread {
                btnKonfirmasi.isEnabled = true
                btnKonfirmasi.text = "KONFIRMASI ABSEN MASUK"
            }
        }
    }

    // FUNGSI: Simpan shift ke SharedPreferences
    private fun saveShiftToPreferences(shift: String, start: String, end: String) {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        prefs.edit {
            putString("user_shift", shift)
            putString("user_shift_start", start)
            putString("user_shift_end", end)
        }
        println("💾 Shift disimpan ke SharedPreferences: $shift ($start - $end)")
    }

    // ✅ PERBAIKAN BESAR: Check absen status dengan parameter username - FIXED LOGIC
    private fun checkAbsenStatusBasedOnShift(userName: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (userName.isEmpty()) {
            println("❌ Tidak bisa check absen: nama user kosong")
            return
        }

        // ✅ PERBAIKAN: Tambah parameter status di lambda
        SupabaseHelper().checkAbsenStatusWithShift(userName, currentDate, userShift) { sudahAbsen, sudahCheckout, status ->
            runOnUiThread {
                println("🔍 STATUS ABSEN - sudahAbsen: $sudahAbsen, sudahCheckout: $sudahCheckout, status: $status, isShiftValid: $isShiftValid")

                // Validasi shift dulu sebelum cek status absen
                if (!isShiftValid) {
                    btnKonfirmasi.isEnabled = false
                    btnKonfirmasi.text = "Tidak Dalam Shift"
                    return@runOnUiThread
                }

                if (sudahAbsen && !sudahCheckout) {
                    // ✅ PERBAIKAN: Jika sudah absen masuk tapi belum pulang, TAMPILKAN PESAN dan NONAKTIFKAN
                    btnKonfirmasi.isEnabled = false
                    btnKonfirmasi.text = "SUDAH ABSEN MASUK"
                    showToast("✅ Anda sudah absen MASUK untuk shift $userShift hari ini (Status: $status)")

                    // ✅ PERBAIKAN: Kembali ke home setelah 3 detik
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 3000)

                } else if (sudahAbsen && sudahCheckout) {
                    btnKonfirmasi.isEnabled = false
                    btnKonfirmasi.text = "SUDAH ABSEN PULANG"
                    showToast("✅ Anda sudah absen PULANG untuk shift $userShift hari ini (Status: $status)")

                    // ✅ PERBAIKAN: Kembali ke home setelah 3 detik
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 3000)

                } else if (!isWithinLocation) {
                    btnKonfirmasi.isEnabled = false
                    btnKonfirmasi.text = "LOKASI TIDAK SESUAI"
                } else {
                    btnKonfirmasi.isEnabled = true
                    btnKonfirmasi.text = "KONFIRMASI ABSEN MASUK"
                }
            }
        }
    }

    // ✅ PERBAIKAN: Fungsi cek waktu untuk absen MASUK - UNTUK SHIFT MALAM YANG LEWAT TENGAH MALAM
    private fun isWithinShiftTimeForMasuk(currentTime: String, shiftStart: String, shiftEnd: String): Boolean {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val current = timeFormat.parse(currentTime)
            val start = timeFormat.parse(shiftStart)
            val end = timeFormat.parse(shiftEnd)

            // Jika end < start (contoh: 00:00 < 18:00), berarti shift melewati tengah malam
            if (end.before(start)) {
                // Untuk shift melewati tengah malam, ada 2 kemungkinan:
                // 1. Waktu sekarang >= start (contoh: 23:09 >= 18:00) -> masih hari ini
                // 2. Waktu sekarang <= end (contoh: 01:00 <= 00:00) -> sudah hari besok
                return current >= start || current <= end
            } else {
                // Shift normal (tidak melewati tengah malam)
                return current >= start && current < end
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return true // Fallback jika parsing error
        }
    }

    // ✅ PERBAIKAN: Fungsi tampilkan error shift yang lebih informatif
    private fun showShiftError(message: String) {
        runOnUiThread {
            btnKonfirmasi.isEnabled = false
            btnKonfirmasi.text = "Error Shift"

            AlertDialog.Builder(this)
                .setTitle("❌ Error Shift")
                .setMessage("$message\n\nTidak bisa melakukan absen.\n\n" +
                        "Silakan hubungi admin untuk:\n" +
                        "• Memastikan jadwal Anda sudah dibuat\n" +
                        "• Memastikan nama di jadwal sesuai dengan login\n" +
                        "• Memastikan shift definitions sudah ada")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setNegativeButton("COBA LAGI") { dialog, _ ->
                    dialog.dismiss()
                    getCorrectUsernameFromDatabase() // Coba lagi
                }
                .setCancelable(false)
                .show()
        }
    }

    // ✅ PERBAIKAN TANPA HARCODE: FUNGSI AUTO CHECKOUT - Pakai jam dari database
    private fun checkAndAutoCompleteYesterday() {
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""

        if (userName.isEmpty()) return

        executor.execute {
            try {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DATE, -1) // Kemarin
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                println("🔄 CHECK AUTO COMPLETE YESTERDAY: $yesterday")

                // ✅ PERBAIKAN: Tambah parameter status di lambda
                SupabaseHelper().checkAbsenStatusWithShift(userName, yesterday, userShift) { sudahAbsen, sudahCheckout, status ->
                    if (sudahAbsen && !sudahCheckout) {
                        println("🔍 Ditemukan data kemarin yang lupa absen pulang, auto checkout... Status sebelumnya: $status")

                        // ✅ PERBAIKAN TANPA HARCODE: Gunakan jam selesai shift dari database
                        val jamPulang = if (userShiftEnd.isNotEmpty() && userShiftEnd != "null") {
                            userShiftEnd // Pakai jam selesai shift dari database
                        } else {
                            // Fallback hanya jika benar-benar tidak ada data
                            "23:59"
                        }

                        println("🔍 Auto checkout untuk shift $userShift ($userShiftStart - $userShiftEnd): $jamPulang")

                        SupabaseHelper().updatePulangManual(userName, yesterday, userShift, jamPulang,
                            object : SupabaseHelper.SimpanCallback {
                                override fun onSuccess(message: String) {
                                    println("✅ Auto checkout berhasil: $message")
                                    runOnUiThread {
                                        showToast("System: Data kemarin dilengkapi - $jamPulang (Status: $status)")
                                    }
                                }
                                override fun onError(error: String) {
                                    println("❌ Auto checkout gagal: $error")
                                }
                            })
                    } else {
                        println("🔍 Data kemarin sudah lengkap atau belum absen")
                    }
                }
            } catch (e: Exception) {
                println("❌ ERROR checkAndAutoCompleteYesterday: ${e.message}")
            }
        }
    }

    private fun resetAbsenStatusIfNeeded() {
        // Reset berdasarkan user specific
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val lastAbsenDate = prefs.getString("last_absen_date_$userName", "")
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (lastAbsenDate != currentDate) {
            prefs.edit {
                putString("status_absen_$userName", "BELUM_ABSEN")
                putString("jam_masuk_hari_ini_$userName", "")
                putString("lokasi_masuk_hari_ini_$userName", "")
                putString("jam_pulang_hari_ini_$userName", "")
                putString("lokasi_pulang_hari_ini_$userName", "")
            }
            println("🔄 Status absen direset untuk hari baru - $userName")
        }
    }

    private fun setupButtonListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnKonfirmasi.setOnClickListener {
            // ✅ PERBAIKAN: Check dari database dengan shift
            val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userName = userSession.getString("USER_NAME", "") ?: ""
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (userName.isEmpty()) {
                showToast("❌ Error: User tidak ditemukan")
                return@setOnClickListener
            }

            // ✅ PERBAIKAN: Validasi shift lagi sebelum lanjut
            if (!isShiftValid) {
                showToast("❌ Bukan jadwal shift $userShift Anda")
                return@setOnClickListener
            }

            // ✅ PERBAIKAN: Tambah parameter status di lambda
            SupabaseHelper().checkAbsenStatusWithShift(userName, currentDate, userShift) { sudahAbsen, sudahCheckout, status ->
                runOnUiThread {
                    if (sudahAbsen && !sudahCheckout) {
                        showToast("❌ Anda sudah absen MASUK untuk shift $userShift hari ini (Status: $status)")
                        // ✅ PERBAIKAN: Kembali ke home
                        Handler(Looper.getMainLooper()).postDelayed({
                            finish()
                        }, 2000)
                        return@runOnUiThread
                    }

                    if (sudahAbsen && sudahCheckout) {
                        showToast("❌ Anda sudah absen PULANG untuk shift $userShift hari ini (Status: $status)")
                        finish()
                        return@runOnUiThread
                    }

                    // CEK SHIFT DULU SEBELUM LANJUT
                    if (!isShiftValid) {
                        showToast("❌ Bukan jadwal shift $userShift Anda")
                        return@runOnUiThread
                    }

                    // ✅ PERBAIKAN: Cek permission dulu sebelum ambil lokasi
                    if (!checkLocationPermission()) {
                        showToast("❌ Izin lokasi belum diberikan")
                        requestLocationPermission()
                        return@runOnUiThread
                    }

                    if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                        val allowOutside = prefs.getBoolean("dev_mode", false)

                        if (!isWithinTargetLocation(currentLatitude, currentLongitude)) {
                            showLocationError()
                            if (!allowOutside) {
                                showToast("❌ Tidak bisa absen di luar lokasi TI Polije")
                                return@runOnUiThread
                            } else {
                                showToast("⚠️ Dev Mode: Absen di luar lokasi diperbolehkan")
                            }
                        }

                        validateNamaThen(this::simpanAbsenMasuk)
                    } else {
                        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                        val allowOutside = prefs.getBoolean("dev_mode", false)
                        if (allowOutside) {
                            showToast("⚠️ Dev Mode: Lokasi belum siap, lanjut simpan")
                            validateNamaThen(this::simpanAbsenMasuk)
                        } else {
                            showToast("❌ Lokasi tidak tersedia. Coba lagi.")
                            getCurrentLocation()
                        }
                    }
                }
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    // ✅ PERBAIKAN: Request permission yang lebih baik
    private fun requestLocationPermission() {
        if (checkLocationPermission()) {
            // Permission sudah diberikan, langsung ambil lokasi
            getCurrentLocation()
            return
        }

        // Cek apakah harus tampilkan penjelasan
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Tampilkan penjelasan mengapa butuh lokasi
            AlertDialog.Builder(this)
                .setTitle("Izin Lokasi Diperlukan")
                .setMessage("Aplikasi membutuhkan akses lokasi untuk memverifikasi bahwa Anda berada di:\n\n" +
                        "📍 Jurusan TI Polije\n\n" +
                        "Lokasi digunakan UNTUK:\n" +
                        "• Memastikan absen dilakukan di area kampus\n" +
                        "• Mencegah kecurangan absen dari luar\n" +
                        "• Validasi kehadiran fisik\n\n" +
                        "Tanpa izin lokasi, Anda TIDAK BISA melakukan absen.")
                .setPositiveButton("SETUJU") { dialog, _ ->
                    // Request permission setelah user setuju
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                    dialog.dismiss()
                }
                .setNegativeButton("TOLAK") { dialog, _ ->
                    showToast("Izin lokasi ditolak, tidak bisa absen")
                    btnKonfirmasi.isEnabled = false
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        } else {
            // Langsung request permission tanpa penjelasan
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission diberikan, langsung ambil lokasi
                showToast("Izin lokasi diberikan, mengambil lokasi...")
                getCurrentLocation()
            } else {
                showToast("Izin lokasi ditolak, tidak bisa absen")
                btnKonfirmasi.isEnabled = false

                // Tampilkan dialog untuk buka settings
                AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Ditolak")
                    .setMessage("Anda tidak bisa absen tanpa izin lokasi.\n\n" +
                            "Silakan berikan izin lokasi melalui:\n" +
                            "Settings → Apps → Dessert Cake → Permissions → Location")
                    .setPositiveButton("BUKA SETTINGS") { dialog, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                        dialog.dismiss()
                    }
                    .setNegativeButton("NANTI") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!checkLocationPermission()) {
            showToast("Izin lokasi belum diberikan")
            return
        }

        showToast("Mengambil lokasi...")
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    processLocation(location)
                } else {
                    requestNewLocation()
                }
            }
            .addOnFailureListener { e ->
                showToast("Gagal mengambil lokasi: ${e.message}")
                requestNewLocation()
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        if (!checkLocationPermission()) {
            return
        }

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processLocation(location)
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Handler(Looper.getMainLooper()).postDelayed({
            fusedLocationClient.removeLocationUpdates(locationCallback)
            if (currentLatitude == 0.0 && currentLongitude == 0.0) {
                showToast("Gagal mengambil lokasi setelah 10 detik.")
                tvLokasi.text = "Lokasi tidak siap"
            }
        }, 10000)
    }

    private fun processLocation(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude
        resolveAddress(location)
        val within = isWithinTargetLocation(currentLatitude, currentLongitude)

        // Update UI berdasarkan lokasi
        runOnUiThread {
            if (within) {
                tvLokasi.text = "📍 $currentAddress ✅"
                isWithinLocation = true

                // ✅ PERBAIKAN: Update status button setelah dapat lokasi
                val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                val userName = userSession.getString("USER_NAME", "") ?: ""
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // ✅ PERBAIKAN: Tambah parameter status di lambda
                SupabaseHelper().checkAbsenStatusWithShift(userName, currentDate, userShift) { sudahAbsen, sudahCheckout, status ->
                    runOnUiThread {
                        if (!sudahAbsen && !sudahCheckout && isShiftValid) {
                            btnKonfirmasi.isEnabled = true
                            btnKonfirmasi.text = "KONFIRMASI ABSEN MASUK"
                        }
                    }
                }
            } else {
                tvLokasi.text = "❌ $currentAddress"
                isWithinLocation = false
                btnKonfirmasi.isEnabled = false
                btnKonfirmasi.text = "LOKASI TIDAK SESUAI"
            }
        }
    }

    private fun setCurrentDateTime() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))

                val currentTime = timeFormat.format(Date())
                val currentDate = dateFormat.format(Date())

                tvJamRealTime.text = currentTime
                tvTanggalRealTime.text = currentDate

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun simpanAbsenMasuk() {
        // ✅ PERBAIKAN FINAL: Validasi shift SEBELUM menyimpan
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (userName.isEmpty()) {
            showToast("❌ Error: User tidak ditemukan")
            return
        }

        // ✅ PERBAIKAN FINAL: Validasi shift lagi sebelum simpan - JIKA TIDAK VALID, TIDAK SIMPAN KE DATABASE
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTime = "$currentHour:$currentMinute"

        if (!isWithinShiftTimeForMasuk(currentTime, userShiftStart, userShiftEnd)) {
            showToast("❌ Bukan jadwal shift $userShift Anda - Data tidak disimpan")

            // ❌ TAMPILKAN DIALOG INFORMASI
            val currentTimeFormatted = String.format("%02d:%02d", currentHour, currentMinute)
            AlertDialog.Builder(this)
                .setTitle("❌ Bukan Jadwal Shift Anda")
                .setMessage("Shift $userShift: $userShiftStart - $userShiftEnd\nSekarang: $currentTimeFormatted\n\nData TIDAK disimpan ke sistem.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setCancelable(false)
                .show()
            return
        }

        // ✅ PERBAIKAN: Tambah parameter status di lambda
        SupabaseHelper().checkAbsenStatusWithShift(userName, currentDate, userShift) { sudahAbsen, sudahCheckout, status ->
            runOnUiThread {
                if (sudahAbsen && !sudahCheckout) {
                    showToast("❌ Anda sudah absen MASUK untuk shift $userShift hari ini (Status: $status)")
                    // ✅ PERBAIKAN: Kembali ke home
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 2000)
                    return@runOnUiThread
                }

                if (sudahAbsen && sudahCheckout) {
                    showToast("❌ Anda sudah absen PULANG untuk shift $userShift hari ini (Status: $status)")
                    finish()
                    return@runOnUiThread
                }

                // VALIDASI SHIFT LAGI SEBELUM SIMPAN
                if (!isShiftValid) {
                    showToast("❌ Bukan jadwal shift $userShift Anda - Data tidak disimpan")
                    return@runOnUiThread
                }

                val allowOutside = getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", false)
                if (!isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    if (!allowOutside) {
                        showToast("❌ Tidak bisa absen di luar lokasi TI Polije")
                        return@runOnUiThread
                    }
                }

                val currentDateObj = Date()
                val tanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDateObj)
                val jamMasuk = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentDateObj)
                val lokasiMasuk = currentAddress

                // Simpan ke SharedPreferences dengan key user specific
                val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                prefs.edit {
                    putString("status_absen_$userName", "SUDAH_MASUK")
                    putString("jam_masuk_hari_ini_$userName", jamMasuk)
                    putString("lokasi_masuk_hari_ini_$userName", lokasiMasuk)
                    putString("last_absen_date_$userName", tanggal)
                }

                // Simpan ke Supabase (Online storage) dengan shift
                simpanKeDatabaseOnline(tanggal, jamMasuk, lokasiMasuk, userName)

                showToast("✅ Absen Masuk Berhasil (Shift $userShift):\nJam $jamMasuk\nLokasi: $lokasiMasuk")

                // ✅ PERBAIKAN BESAR: Kembali ke home setelah berhasil absen masuk
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 2000)
            }
        }
    }

    private fun simpanKeDatabaseOnline(tanggal: String, jamMasuk: String, lokasiMasuk: String, userName: String) {
        try {
            println("🔍 simpanKeDatabaseOnline: Nama pengguna = '$userName'")
            println("🔍 simpanKeDatabaseOnline: Data yang akan dikirim - Tanggal: $tanggal, Jam: $jamMasuk, Lokasi: $lokasiMasuk, Shift: $userShift")

            if (userName.isEmpty()) {
                println("❌ simpanKeDatabaseOnline: Nama pengguna kosong")
                throw IllegalStateException("Nama pengguna kosong")
            }

            val callback = object : SupabaseHelper.SimpanCallback {
                override fun onSuccess(message: String) {
                    println("✅ simpanKeDatabaseOnline: BERHASIL - $message")
                    runOnUiThread {
                        println("✅ Database Online: $message")
                        showToast("Cloud: $message")
                    }
                }

                override fun onError(error: String) {
                    println("❌ simpanKeDatabaseOnline: ERROR - $error")
                    runOnUiThread {
                        println("⚠️ Database Offline: $error")
                        showToast("Cloud error: $error")
                    }
                }
            }

            // PERBAIKAN: Panggil fungsi simpanMasuk dari SupabaseHelper yang sudah diperbaiki
            val supabaseHelper = SupabaseHelper()
            supabaseHelper.simpanMasuk(
                tanggal = tanggal,
                jamMasuk = jamMasuk,
                lokasiMasuk = lokasiMasuk,
                username = userName,
                shift = userShift,
                callback = callback
            )

        } catch (e: Exception) {
            println("❌ simpanKeDatabaseOnline: EXCEPTION - ${e.message}")
            println("⚠️ Error koneksi database: ${e.message}")
        }
    }

    private fun isWithinTargetLocation(userLat: Double, userLng: Double): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLat,
            userLng,
            TARGET_LATITUDE,
            TARGET_LONGITUDE,
            results
        )
        isWithinLocation = results[0] <= RADIUS_METERS
        return isWithinLocation
    }

    private fun showLocationError() {
        val results = FloatArray(1)
        Location.distanceBetween(
            currentLatitude,
            currentLongitude,
            TARGET_LATITUDE,
            TARGET_LONGITUDE,
            results
        )
        val distanceInMeters = results[0]
        val distanceInKm = distanceInMeters / 1000
        val message = "❌ Absen hanya bisa dilakukan di Jurusan TI Polije\n" +
                "Anda berada ${String.format(Locale.getDefault(), "%.1f", distanceInKm)} km dari lokasi\n" +
                "Silahkan datang ke Jurusan TI Polije untuk absen"

        AlertDialog.Builder(this)
            .setTitle("Lokasi Tidak Sesuai")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun validateNamaThen(onValid: () -> Unit) {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val nama = userSession.getString("USER_NAME", "")?.trim() ?: ""
        println("🔍 validateNamaThen: Nama dari USER_SESSION = '$nama'")

        if (nama.isEmpty()) {
            println("❌ validateNamaThen: Nama KOSONG - tidak bisa lanjut")
            showToast("Error: Nama pengguna tidak ditemukan")
            return
        }
        prefs.edit {
            putString("nama_pengguna", nama)
        }
        println("🔍 validateNamaThen: Langsung lanjut tanpa validasi Supabase - Nama: '$nama'")
        runOnUiThread(onValid)
    }

    private fun resolveAddress(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            currentLatitude = location.latitude
            currentLongitude = location.longitude

            if (addresses != null && addresses.isNotEmpty() && !addresses[0].subLocality.isNullOrEmpty()) {
                val address = addresses[0]
                val addressText = StringBuilder()
                if (address.subLocality != null) addressText.append(address.subLocality).append(", ")
                if (address.locality != null) addressText.append(address.locality).append(", ")
                if (address.subAdminArea != null) addressText.append(address.subAdminArea)

                currentAddress = addressText.toString()
            } else {
                currentAddress = "Lokasi: ${String.format("%.6f", currentLatitude)}, ${String.format("%.6f", currentLongitude)}"
            }
        } catch (e: Exception) {
            currentAddress = "Lokasi: ${String.format("%.6f", currentLatitude)}, ${String.format("%.6f", currentLongitude)}"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}