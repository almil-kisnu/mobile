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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.JadwalMingguanApi
import com.almil.dessertcakekinian.database.ShiftDefinitionApi
import com.almil.dessertcakekinian.database.SupabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class AbsenMasukActivity : AppCompatActivity() {

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val TARGET_LATITUDE = -8.157510
        const val TARGET_LONGITUDE = 113.722778
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
        getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("dev_mode", false)
            .apply()

        // Set nama karyawan dari shared preferences
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "Karyawan") ?: "Karyawan"
        tvNamaKaryawan.text = userName

        // Set tanggal dan waktu real-time
        setCurrentDateTime()

        // Setup button listeners
        setupButtonListeners()

        // Ambil data shift user - TAMBAH INI
        getUserShiftFromJadwal()

        // Request location permission
        requestLocationPermission()
    }

    // FUNGSI BARU: Ambil shift user dari jadwal
    private fun getUserShiftFromJadwal() {
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""

        if (userName.isEmpty()) {
            println("❌ Tidak bisa ambil shift: nama user kosong")
            showShiftError("Tidak bisa mengambil data shift")
            return
        }

        // Ambil data shift definitions dulu
        ShiftDefinitionApi().listAll(object : ShiftDefinitionApi.ShiftListCallback {
            override fun onSuccess(shiftList: List<ShiftDefinitionApi.ShiftDefinition>) {
                println("✅ Dapat ${shiftList.size} shift definitions")

                // Setelah dapat shift definitions, ambil jadwal user
                JadwalMingguanApi().listAllWithDetails(object : JadwalMingguanApi.JadwalListCallback {
                    override fun onSuccess(list: List<JadwalMingguanApi.JadwalMingguan>) {
                        val today = SimpleDateFormat("EEEE", Locale("id", "ID")).format(Date()).lowercase()
                        println("📅 Hari ini: $today")

                        val userJadwal = list.find { it.nama_pengguna == userName }

                        if (userJadwal != null) {
                            // Dapatkan nama shift user untuk hari ini
                            userShift = when (today) {
                                "senin" -> userJadwal.shift_senin ?: "Pagi"
                                "selasa" -> userJadwal.shift_selasa ?: "Pagi"
                                "rabu" -> userJadwal.shift_rabu ?: "Pagi"
                                "kamis" -> userJadwal.shift_kamis ?: "Pagi"
                                "jumat" -> userJadwal.shift_jumat ?: "Pagi"
                                "sabtu" -> userJadwal.shift_sabtu ?: "Pagi"
                                "minggu" -> userJadwal.shift_minggu ?: "Pagi"
                                else -> "Pagi"
                            }

                            // Cari jam shift dari shift definitions
                            val shiftDefinition = shiftList.find {
                                it.nama_shift.equals(userShift, ignoreCase = true)
                            }

                            if (shiftDefinition != null) {
                                userShiftStart = shiftDefinition.jam_mulai
                                userShiftEnd = shiftDefinition.jam_selesai
                                println("✅ Shift $userName: $userShift ($userShiftStart - $userShiftEnd)")

                                // Validasi waktu shift
                                validateShiftTime()
                            } else {
                                showShiftError("Shift $userShift tidak ditemukan")
                            }
                        } else {
                            println("❌ Jadwal tidak ditemukan untuk user: $userName")
                            showShiftError("Jadwal tidak ditemukan")
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

    // FUNGSI BARU: Validasi waktu shift
    private fun validateShiftTime() {
        val calendar = Calendar.getInstance()
        currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        currentMinute = calendar.get(Calendar.MINUTE)

        val currentTime = "$currentHour:$currentMinute"
        isShiftValid = isWithinShiftTime(currentTime, userShiftStart, userShiftEnd)

        runOnUiThread {
            if (!isShiftValid) {
                btnKonfirmasi.isEnabled = false
                btnKonfirmasi.text = "Tidak Dalam Shift"

                AlertDialog.Builder(this)
                    .setTitle("⏰ Bukan Waktu Shift Anda")
                    .setMessage("Shift $userShift Anda: $userShiftStart - $userShiftEnd\n" +
                            "Sekarang jam: ${String.format("%02d:%02d", currentHour, currentMinute)}\n\n" +
                            "Silakan absen pada jam shift Anda.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                btnKonfirmasi.isEnabled = true
                btnKonfirmasi.text = "KONFIRMASI ABSEN MASUK"
                println("✅ Validasi shift: $userShift ($userShiftStart-$userShiftEnd) - Jam $currentHour:$currentMinute - DIPERBOLEHKAN")
            }
        }
    }

    // FUNGSI BARU: Cek apakah dalam waktu shift
    private fun isWithinShiftTime(currentTime: String, shiftStart: String, shiftEnd: String): Boolean {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val current = timeFormat.parse(currentTime)
            val start = timeFormat.parse(shiftStart)
            val end = timeFormat.parse(shiftEnd)

            return current in start..end
        } catch (e: Exception) {
            e.printStackTrace()
            return true // Fallback jika parsing error
        }
    }

    // FUNGSI BARU: Tampilkan error shift
    private fun showShiftError(message: String) {
        runOnUiThread {
            btnKonfirmasi.isEnabled = false
            btnKonfirmasi.text = "Error Shift"

            AlertDialog.Builder(this)
                .setTitle("❌ Error Shift")
                .setMessage("$message\n\nTidak bisa melakukan absen.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun resetAbsenStatusIfNeeded() {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val lastAbsenDate = prefs.getString("last_absen_date", "")
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (lastAbsenDate != currentDate) {
            val editor = prefs.edit()
            editor.putString("status_absen", "BELUM_ABSEN")
            editor.putString("jam_masuk_hari_ini", "")
            editor.putString("lokasi_masuk_hari_ini", "")
            editor.putString("jam_pulang_hari_ini", "")
            editor.putString("lokasi_pulang_hari_ini", "")
            editor.apply()
            println("🔄 Status absen direset untuk hari baru - Masuk")
        }
    }

    private fun setupButtonListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnKonfirmasi.setOnClickListener {
            val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val status = prefs.getString("status_absen", "BELUM_ABSEN")

            if (status == "SUDAH_MASUK" || status == "SUDAH_PULANG") {
                showToast("❌ Anda sudah absen masuk hari ini")
                finish()
                return@setOnClickListener
            }

            // CEK SHIFT DULU SEBELUM LANJUT - TAMBAH VALIDASI INI
            if (!isShiftValid) {
                showToast("❌ Bukan waktu shift $userShift Anda ($userShiftStart-$userShiftEnd)")
                return@setOnClickListener
            }

            // CEK PERMISSION LOCATION
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showToast("❌ Izin lokasi belum diberikan")
                requestLocationPermission()
                return@setOnClickListener
            }

            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                val allowOutside = prefs.getBoolean("dev_mode", false)

                if (!isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    showLocationError()
                    if (!allowOutside) {
                        showToast("❌ Tidak bisa absen di luar lokasi TI Polije")
                        return@setOnClickListener
                    } else {
                        showToast("⚠️ Dev Mode: Absen di luar lokasi diperbolehkan")
                    }
                }

                validateNamaThen(this::simpanAbsenMasuk)
            } else {
                val allowOutside = prefs.getBoolean("dev_mode", false)
                if (allowOutside) {
                    showToast("⚠️ Dev Mode: Lokasi belum siap, lanjut simpan")
                    validateNamaThen(this::simpanAbsenMasuk)
                } else {
                    showToast("❌ Lokasi tidak tersedia, tidak bisa absen")
                    showToast("Mohon aktifkan GPS dan coba lagi")
                    getCurrentLocation()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    // ... (FUNGSI LAINNYA TETAP SAMA: setCurrentDateTime, isWithinTargetLocation, showLocationError,
    // validateNamaThen, simpanAbsenMasuk, simpanKeDatabaseOnline, requestLocationPermission,
    // onRequestPermissionsResult, getCurrentLocation, requestNewLocation, processLocation,
    // getAddressFromLocation, showToast) ...

    private fun setCurrentDateTime() {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))

            val currentTime = timeFormat.format(Date())
            val currentDate = dateFormat.format(Date())

            tvJamRealTime.text = currentTime
            tvTanggalRealTime.text = currentDate
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isWithinTargetLocation(userLat: Double, userLng: Double): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLat, userLng,
            TARGET_LATITUDE, TARGET_LONGITUDE,
            results
        )
        isWithinLocation = results[0] <= RADIUS_METERS
        return isWithinLocation
    }

    private fun showLocationError() {
        val results = FloatArray(1)
        Location.distanceBetween(
            currentLatitude, currentLongitude,
            TARGET_LATITUDE, TARGET_LONGITUDE,
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

        prefs.edit().putString("nama_pengguna", nama).apply()

        println("🔍 validateNamaThen: Langsung lanjut tanpa validasi Supabase - Nama: '$nama'")

        runOnUiThread(onValid)
    }

    private fun simpanAbsenMasuk() {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val status = prefs.getString("status_absen", "BELUM_ABSEN")

        if (status == "SUDAH_MASUK" || status == "SUDAH_PULANG") {
            showToast("❌ Anda sudah absen masuk hari ini")
            finish()
            return
        }

        // VALIDASI SHIFT LAGI SEBELUM SIMPAN
        if (!isShiftValid) {
            showToast("❌ Bukan waktu shift $userShift Anda ($userShiftStart-$userShiftEnd)")
            return
        }

        val allowOutside = getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", false)

        if (!isWithinTargetLocation(currentLatitude, currentLongitude)) {
            if (!allowOutside) {
                showToast("❌ Tidak bisa absen: Anda berada di luar lokasi TI Polije")
                return
            } else {
                showToast("⚠️ Dev Mode: Absen di luar lokasi dicatat")
            }
        }

        val currentTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        simpanKeDatabaseOnline(currentDate, currentTimeFormatted, currentAddress)

        val editor = prefs.edit()

        val absenData = "MASUK|$currentTimeFormatted|$currentDate|$currentAddress|$currentLatitude|$currentLongitude"

        val key = "riwayat_${System.currentTimeMillis()}"
        editor.putString(key, absenData)
        editor.putString("jam_masuk_hari_ini", currentTimeFormatted)
        editor.putString("lokasi_masuk_hari_ini", currentAddress)
        editor.putString("status_absen", "SUDAH_MASUK")
        editor.putString("last_absen_date", currentDate)
        val ok = editor.commit()
        println("[Masuk] saved key=$key, ok=$ok")

        try {
            if (getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", false)) {
                showToast("Masuk tersimpan lokal: $currentTimeFormatted")
            }
        } catch (ignored: Exception) {}

        getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("go_to_riwayat", true)
            .commit()

        setResult(RESULT_OK)

        val message = if (isWithinLocation) {
            "✅ Absen Masuk berhasil!\nWaktu: $currentTimeFormatted" +
                    "\nLokasi: $currentAddress" +
                    "\n📍 Lokasi: Jurusan TI Polije ✅"
        } else {
            "⚠️ Absen Masuk berhasil (Dev Mode)!\nWaktu: $currentTimeFormatted" +
                    "\nLokasi: $currentAddress" +
                    "\n❌ Lokasi: DI LUAR AREA TI Polije"
        }

        showToast(message)
        finish()
    }

    private fun simpanKeDatabaseOnline(tanggal: String, jamMasuk: String, lokasiMasuk: String) {
        try {
            val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val namaPengguna = prefs.getString("nama_pengguna", "")?.trim() ?: ""

            println("🔍 simpanKeDatabaseOnline: Nama pengguna = '$namaPengguna'")
            println("🔍 simpanKeDatabaseOnline: Data yang akan dikirim - Tanggal: $tanggal, Jam: $jamMasuk, Lokasi: $lokasiMasuk")

            if (namaPengguna.isEmpty()) {
                println("❌ simpanKeDatabaseOnline: Nama pengguna kosong")
                throw IllegalStateException("Nama pengguna kosong")
            }

            val keteranganIzin = prefs.getString("file_izin_name", "")
            val selectedStatus = prefs.getString("selected_status", "Hadir")
            val idPengguna = prefs.getInt("id_pengguna", Math.abs(namaPengguna.hashCode()))

            SupabaseHelper().simpanMasuk(tanggal, jamMasuk, lokasiMasuk, idPengguna, selectedStatus, keteranganIzin, object : SupabaseHelper.SimpanCallback {
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
            })
        } catch (e: Exception) {
            println("❌ simpanKeDatabaseOnline: EXCEPTION - ${e.message}")
            println("⚠️ Error koneksi database: ${e.message}")
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            AlertDialog.Builder(this)
                .setTitle("Izin Lokasi Diperlukan untuk Absen")
                .setMessage("Aplikasi MEMBUTUHKAN akses lokasi untuk memverifikasi bahwa Anda berada di:\n\n" +
                        "📍 Jurusan TI Polije\n" +
                        "Lat: -8.157518, Long: 113.722776\n\n" +
                        "Tanpa izin lokasi, Anda TIDAK BISA melakukan absen.\n\n" +
                        "Lokasi hanya digunakan untuk verifikasi kehadiran dan tidak disimpan secara permanen.")
                .setPositiveButton("IZINKAN LOKASI") { _, _ ->
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
                .setNegativeButton("TOLAK") { _, _ ->
                    AlertDialog.Builder(this)
                        .setTitle("Tidak Bisa Absen")
                        .setMessage("Anda tidak dapat melakukan absen tanpa izin lokasi.\n\n" +
                                "Silakan berikan izin lokasi di pengaturan aplikasi jika ingin absen.")
                        .setPositiveButton("OK") { _, _ ->
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
                .setCancelable(false)
                .show()
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Izin lokasi diberikan, mengambil lokasi...")
                getCurrentLocation()
            } else {
                tvLokasi.text = "Izin lokasi ditolak"

                AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Ditolak")
                    .setMessage("Anda menolak izin lokasi. Untuk dapat absen, Anda HARUS memberikan izin lokasi.\n\n" +
                            "Apakah Anda ingin membuka pengaturan untuk memberikan izin?")
                    .setPositiveButton("BUKA PENGATURAN") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("TUTUP APLIKASI") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                requestNewLocation()
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processLocation(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        Handler(Looper.getMainLooper()).postDelayed({
            fusedLocationClient.removeLocationUpdates(locationCallback)
            if (currentLatitude == 0.0 && currentLongitude == 0.0) {
                tvLokasi.text = "Gagal mengambil lokasi"
                showToast("Gagal mengambil lokasi. Coba nyalakan GPS atau periksa koneksi.")

                val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                if (prefs.getBoolean("dev_mode", false)) {
                    showToast("Dev mode: Lanjut tanpa lokasi")
                    currentLatitude = TARGET_LATITUDE
                    currentLongitude = TARGET_LONGITUDE
                    currentAddress = "Jurusan TI Polije (Dev Mode)"
                    tvLokasi.text = "📍 $currentAddress ✅"
                } else {
                    showToast("❌ Tidak bisa absen tanpa lokasi")
                    btnKonfirmasi.isEnabled = false
                }
            }
        }, 15000)
    }

    private fun processLocation(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude

        runOnUiThread {
            if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                tvLokasi.text = "📍 Jurusan TI Polije ✅"
                showToast("Lokasi berhasil didapatkan - Dalam area absen")
                // Hanya enable tombol jika shift juga valid
                btnKonfirmasi.isEnabled = isShiftValid
            } else {
                tvLokasi.text = "❌ Diluar area TI Polije"
                showToast("Lokasi berhasil didapatkan - Di luar area absen")

                val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                if (prefs.getBoolean("dev_mode", false)) {
                    showToast("Dev Mode: Tetap bisa absen")
                    btnKonfirmasi.isEnabled = isShiftValid
                } else {
                    showToast("❌ Tidak bisa absen di luar lokasi")
                    btnKonfirmasi.isEnabled = false
                }
            }
            getAddressFromLocation(location)
        }
    }

    private fun getAddressFromLocation(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale("id", "ID"))
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressText = StringBuilder()
                if (address.subLocality != null) addressText.append(address.subLocality).append(", ")
                if (address.locality != null) addressText.append(address.locality).append(", ")
                if (address.subAdminArea != null) addressText.append(address.subAdminArea)

                currentAddress = addressText.toString()
                if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    tvLokasi.text = "📍 $currentAddress ✅"
                } else {
                    tvLokasi.text = "❌ $currentAddress"
                }
            } else {
                currentAddress = "Lokasi: $currentLatitude, $currentLongitude"
                if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    tvLokasi.text = "📍 $currentAddress ✅"
                } else {
                    tvLokasi.text = "❌ $currentAddress"
                }
            }
        } catch (e: Exception) {
            currentAddress = "Lokasi: $currentLatitude, $currentLongitude"
            if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                tvLokasi.text = "📍 $currentAddress ✅"
            } else {
                tvLokasi.text = "❌ $currentAddress"
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}