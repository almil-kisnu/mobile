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
import com.almil.dessertcakekinian.database.SupabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absen_masuk)

        // Initialize views dengan ID yang sesuai dari XML
        tvNamaKaryawan = findViewById(R.id.tvNamaKaryawan)
        tvLokasi = findViewById(R.id.tvLokasi)
        tvJamRealTime = findViewById(R.id.tvJamRealTime)
        tvTanggalRealTime = findViewById(R.id.tvTanggalRealTime)
        btnKonfirmasi = findViewById(R.id.btnKonfirmasi)
        btnBack = findViewById(R.id.btnBack)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Reset status absen jika sudah hari baru
        resetAbsenStatusIfNeeded()

        // Untuk testing - enable dev mode (bisa absen di luar lokasi)
        // TAPI disable dulu untuk testing real location
        getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("dev_mode", false) // UBAH KE FALSE untuk testing real location
            .apply()

        // Set nama karyawan dari shared preferences
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "Karyawan") ?: "Karyawan"
        tvNamaKaryawan.text = userName

        // Set tanggal dan waktu real-time
        setCurrentDateTime()

        // Setup button listeners
        setupButtonListeners()

        // Request location permission
        requestLocationPermission()
    }

    private fun resetAbsenStatusIfNeeded() {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val lastAbsenDate = prefs.getString("last_absen_date", "")
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Jika tanggal terakhir absen berbeda dengan hari ini, reset status
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
        // Tombol Back
        btnBack.setOnClickListener {
            finish()
        }

        // Tombol Konfirmasi
        btnKonfirmasi.setOnClickListener {
            val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val status = prefs.getString("status_absen", "BELUM_ABSEN")

            if (status == "SUDAH_MASUK" || status == "SUDAH_PULANG") {
                showToast("❌ Anda sudah absen masuk hari ini")
                finish()
                return@setOnClickListener
            }

            // CEK DULU APAKAH PERMISSION SUDAH DIBERIKAN
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showToast("❌ Izin lokasi belum diberikan")
                requestLocationPermission()
                return@setOnClickListener
            }

            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                val allowOutside = prefs.getBoolean("dev_mode", false) // Default false

                // CEK LOKASI DULU SEBELUM LANJUT
                if (!isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    showLocationError()
                    if (!allowOutside) {
                        // JIKA TIDAK DALAM LOKASI DAN BUKAN DEV MODE, BERHENTI DI SINI
                        showToast("❌ Tidak bisa absen di luar lokasi TI Polije")
                        return@setOnClickListener
                    } else {
                        // JIKA DEV MODE, TAMPILKAN PERINGATAN TAPI LANJUT
                        showToast("⚠️ Dev Mode: Absen di luar lokasi diperbolehkan")
                    }
                }

                // JIKA DALAM LOKASI ATAU DEV MODE, LANJUTKAN ABSEN
                validateNamaThen(this::simpanAbsenMasuk)
            } else {
                val allowOutside = prefs.getBoolean("dev_mode", false)
                if (allowOutside) {
                    showToast("⚠️ Dev Mode: Lokasi belum siap, lanjut simpan")
                    validateNamaThen(this::simpanAbsenMasuk)
                } else {
                    showToast("❌ Lokasi tidak tersedia, tidak bisa absen")
                    showToast("Mohon aktifkan GPS dan coba lagi")

                    // Coba ambil lokasi lagi
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

        // Tampilkan dialog error yang lebih jelas
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

        // Ambil nama dari user_session (login) bukan dari absen_data
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val nama = userSession.getString("USER_NAME", "")?.trim() ?: ""

        println("🔍 validateNamaThen: Nama dari USER_SESSION = '$nama'")

        if (nama.isEmpty()) {
            println("❌ validateNamaThen: Nama KOSONG - tidak bisa lanjut")
            showToast("Error: Nama pengguna tidak ditemukan")
            return
        }

        // Simpan nama ke absen_data untuk digunakan nanti
        prefs.edit().putString("nama_pengguna", nama).apply()

        println("🔍 validateNamaThen: Langsung lanjut tanpa validasi Supabase - Nama: '$nama'")

        // Langsung lanjut tanpa validasi Supabase (karena sudah login)
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

        val allowOutside = getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", false)

        // VALIDASI LOKASI LAGI SEBELUM SIMPAN
        if (!isWithinTargetLocation(currentLatitude, currentLongitude)) {
            if (!allowOutside) {
                showToast("❌ Tidak bisa absen: Anda berada di luar lokasi TI Polije")
                return
            } else {
                showToast("⚠️ Dev Mode: Absen di luar lokasi dicatat")
            }
        }

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        simpanKeDatabaseOnline(currentDate, currentTime, currentAddress)

        val editor = prefs.edit()

        val absenData = "MASUK|$currentTime|$currentDate|$currentAddress|$currentLatitude|$currentLongitude"

        val key = "riwayat_${System.currentTimeMillis()}"
        editor.putString(key, absenData)
        editor.putString("jam_masuk_hari_ini", currentTime)
        editor.putString("lokasi_masuk_hari_ini", currentAddress)
        editor.putString("status_absen", "SUDAH_MASUK")
        editor.putString("last_absen_date", currentDate)
        val ok = editor.commit()
        println("[Masuk] saved key=$key, ok=$ok")

        try {
            if (getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", false)) {
                showToast("Masuk tersimpan lokal: $currentTime")
            }
        } catch (ignored: Exception) {}

        getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("go_to_riwayat", true)
            .commit()

        setResult(RESULT_OK)

        // Tampilkan pesan sukses berdasarkan lokasi
        val message = if (isWithinLocation) {
            "✅ Absen Masuk berhasil!\nWaktu: $currentTime" +
                    "\nLokasi: $currentAddress" +
                    "\n📍 Lokasi: Jurusan TI Polije ✅"
        } else {
            "⚠️ Absen Masuk berhasil (Dev Mode)!\nWaktu: $currentTime" +
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

            // Tampilkan dialog penjelasan YANG LEBIH JELAS kenapa butuh lokasi
            AlertDialog.Builder(this)
                .setTitle("Izin Lokasi Diperlukan untuk Absen")
                .setMessage("Aplikasi MEMBUTUHKAN akses lokasi untuk memverifikasi bahwa Anda berada di:\n\n" +
                        "📍 Jurusan TI Polije\n" +
                        "Lat: -8.157518, Long: 113.722776\n\n" +
                        "Tanpa izin lokasi, Anda TIDAK BISA melakukan absen.\n\n" +
                        "Lokasi hanya digunakan untuk verifikasi kehadiran dan tidak disimpan secara permanen.")
                .setPositiveButton("IZINKAN LOKASI") { _, _ ->
                    // Minta permission setelah user paham
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
                .setNegativeButton("TOLAK") { _, _ ->
                    // Jika user tetap menolak, tampilkan pesan dan tutup activity
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
                .setCancelable(false) // User harus memilih, tidak bisa tekan back
                .show()
        } else {
            // Permission sudah diberikan, ambil lokasi
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission diberikan, ambil lokasi
                showToast("Izin lokasi diberikan, mengambil lokasi...")
                getCurrentLocation()
            } else {
                // Permission ditolak
                tvLokasi.text = "Izin lokasi ditolak"

                // Tampilkan dialog penjelasan lagi
                AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Ditolak")
                    .setMessage("Anda menolak izin lokasi. Untuk dapat absen, Anda HARUS memberikan izin lokasi.\n\n" +
                            "Apakah Anda ingin membuka pengaturan untuk memberikan izin?")
                    .setPositiveButton("BUKA PENGATURAN") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                        finish() // Tutup activity setelah ke pengaturan
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

        // Coba ambil last location dulu (lebih cepat)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Berhasil dapat lokasi dari cache
                    processLocation(location)
                } else {
                    // Jika last location null, request location baru
                    requestNewLocation()
                }
            }
            .addOnFailureListener { e ->
                // Jika gagal, request location baru
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

        // Request location dengan timeout
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        // Timeout setelah 15 detik
        Handler(Looper.getMainLooper()).postDelayed({
            fusedLocationClient.removeLocationUpdates(locationCallback)
            if (currentLatitude == 0.0 && currentLongitude == 0.0) {
                tvLokasi.text = "Gagal mengambil lokasi"
                showToast("Gagal mengambil lokasi. Coba nyalakan GPS atau periksa koneksi.")

                // Untuk testing, TIDAK lanjutkan dengan dev mode (default false)
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

        // Update UI di main thread
        runOnUiThread {
            if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                tvLokasi.text = "📍 Jurusan TI Polije ✅"
                showToast("Lokasi berhasil didapatkan - Dalam area absen")
                btnKonfirmasi.isEnabled = true
            } else {
                tvLokasi.text = "❌ Diluar area TI Polije"
                showToast("Lokasi berhasil didapatkan - Di luar area absen")

                // Jika di luar lokasi, cek dev mode
                val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                if (prefs.getBoolean("dev_mode", false)) {
                    showToast("Dev Mode: Tetap bisa absen")
                    btnKonfirmasi.isEnabled = true
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

    // FUNGSI showToast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}