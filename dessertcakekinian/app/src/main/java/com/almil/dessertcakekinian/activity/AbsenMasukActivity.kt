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
import com.almil.dessertcakekinian.database.PenggunaApi
import com.almil.dessertcakekinian.database.SupabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AbsenMasukActivity : AppCompatActivity() {

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val TARGET_LATITUDE = -8.157551
        const val TARGET_LONGITUDE = 113.722800
        const val RADIUS_METERS = 100.0f
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

        // Untuk testing - enable dev mode (bisa absen di luar lokasi)
        getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("dev_mode", true)
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
                Toast.makeText(this, "❌ Anda sudah absen masuk hari ini", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                val allowOutside = prefs.getBoolean("dev_mode", true)
                if (!isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    showLocationError()
                    if (!allowOutside) return@setOnClickListener
                }
                validateNamaThen(this::simpanAbsenMasuk)
            } else {
                val allowOutside = prefs.getBoolean("dev_mode", true)
                if (allowOutside) {
                    Toast.makeText(this, "Lokasi belum siap (dev_mode): lanjut simpan", Toast.LENGTH_SHORT).show()
                    validateNamaThen(this::simpanAbsenMasuk)
                } else {
                    Toast.makeText(this, "Mohon tunggu, sedang mengambil lokasi...", Toast.LENGTH_SHORT).show()
                    requestLocationPermission()
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
        return results[0] <= RADIUS_METERS
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun validateNamaThen(onValid: () -> Unit) {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)

        // Ambil nama dari user_session (login) bukan dari absen_data
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val nama = userSession.getString("USER_NAME", "")?.trim() ?: ""

        println("🔍 validateNamaThen: Nama dari USER_SESSION = '$nama'")

        if (nama.isEmpty()) {
            println("❌ validateNamaThen: Nama KOSONG - tidak bisa lanjut")
            Toast.makeText(this, "Error: Nama pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "❌ Anda sudah absen masuk hari ini", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val allowOutside = getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", true)
        if (!isWithinTargetLocation(currentLatitude, currentLongitude)) {
            showLocationError()
            if (!allowOutside) return
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
            if (getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", true)) {
                Toast.makeText(this, "Masuk tersimpan lokal: $currentTime", Toast.LENGTH_SHORT).show()
            }
        } catch (ignored: Exception) {}

        getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("go_to_riwayat", true)
            .commit()

        setResult(RESULT_OK)

        val message = "✅ Absen Masuk berhasil!\nWaktu: $currentTime" +
                "\nLokasi: $currentAddress" +
                "\n📍 Lokasi: Jurusan TI Polije ✅"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
                        Toast.makeText(this@AbsenMasukActivity, "Cloud: $message", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(error: String) {
                    println("❌ simpanKeDatabaseOnline: ERROR - $error")
                    runOnUiThread {
                        println("⚠️ Database Offline: $error")
                        Toast.makeText(this@AbsenMasukActivity, "Cloud error: $error", Toast.LENGTH_LONG).show()
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

            // Tampilkan dialog penjelasan sebelum minta permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Jelaskan kenapa butuh lokasi
                AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Diperlukan")
                    .setMessage("Aplikasi membutuhkan akses lokasi untuk memastikan Anda berada di Jurusan TI Polije saat absen.")
                    .setPositiveButton("OK") { _, _ ->
                        // Minta permission setelah user paham
                        ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            LOCATION_PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("Batal") { _, _ ->
                        Toast.makeText(this, "Tidak bisa absen tanpa izin lokasi", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .show()
            } else {
                // Langsung minta permission
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
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
                getCurrentLocation()
            } else {
                // Permission ditolak
                tvLokasi.text = "Izin lokasi ditolak"
                Toast.makeText(this, "Tidak bisa absen tanpa izin lokasi. Silakan berikan izin lokasi di pengaturan.", Toast.LENGTH_LONG).show()

                // Beri opsi ke pengaturan
                AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Ditolak")
                    .setMessage("Untuk dapat absen, Anda perlu memberikan izin lokasi. Buka pengaturan aplikasi?")
                    .setPositiveButton("Buka Pengaturan") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Nanti") { _, _ ->
                        finish()
                    }
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

                // Untuk testing, lanjutkan dengan dev mode
                val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                if (prefs.getBoolean("dev_mode", true)) {
                    showToast("Dev mode: Lanjut tanpa lokasi")
                    currentLatitude = TARGET_LATITUDE
                    currentLongitude = TARGET_LONGITUDE
                    currentAddress = "Jurusan TI Polije (Dev Mode)"
                    tvLokasi.text = "📍 $currentAddress ✅"
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
            } else {
                tvLokasi.text = "❌ Diluar area TI Polije"
                showToast("Lokasi berhasil didapatkan - Di luar area absen")
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