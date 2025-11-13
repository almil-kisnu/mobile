package com.almil.dessertcakekinian.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.PenggunaApi
import com.almil.dessertcakekinian.database.SupabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AbsenPulangActivity : AppCompatActivity() {

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1002
        const val TARGET_LATITUDE = -8.157551
        const val TARGET_LONGITUDE = 113.722800
        const val RADIUS_METERS = 100.0f
    }

    private lateinit var tvGreeting: TextView
    private lateinit var tvJamMasuk: TextView
    private lateinit var tvJamSekarang: TextView
    private lateinit var tvJamKerja: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var btnAbsenPulang: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentAddress = "Lokasi tidak diketahui"
    private var pendingPulang = false
    private var singleToast: Toast? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absen_pulang)

        tvGreeting = findViewById(R.id.tv_greeting)
        tvJamMasuk = findViewById(R.id.tv_jam_masuk)
        tvJamSekarang = findViewById(R.id.tv_jam_sekarang)
        tvJamKerja = findViewById(R.id.tv_jam_kerja)
        tvLocation = findViewById(R.id.tv_location)
        tvCoordinates = findViewById(R.id.tv_coordinates)
        btnAbsenPulang = findViewById(R.id.btn_absen_pulang)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tvGreeting.text = "Selamat Tinggal, Kasir"

        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val status = prefs.getString("status_absen", "BELUM_ABSEN")

        if (status == "BELUM_ABSEN") {
            showToast("❌ Anda harus absen masuk dulu")
            btnAbsenPulang.isEnabled = false
            return
        }

        if (status == "SUDAH_PULANG") {
            showToast("❌ Anda sudah absen pulang hari ini")
            btnAbsenPulang.isEnabled = false
            return
        }

        val jamMasuk = prefs.getString("jam_masuk_hari_ini", "08:00")
        tvJamMasuk.text = jamMasuk

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val jamSekarang = sdf.format(Date())
        tvJamSekarang.text = jamSekarang
        hitungJamKerja(jamMasuk!!, jamSekarang)

        requestLocationPermission()

        btnAbsenPulang.setOnClickListener {
            btnAbsenPulang.isEnabled = false
            btnAbsenPulang.postDelayed({ btnAbsenPulang.isEnabled = true }, 1500)

            val prefsCheck = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val statusCheck = prefsCheck.getString("status_absen", "BELUM_ABSEN")

            if (statusCheck == "BELUM_ABSEN") {
                showToast("❌ Anda harus absen masuk dulu")
                return@setOnClickListener
            }

            if (statusCheck == "SUDAH_PULANG") {
                showToast("❌ Anda sudah absen pulang hari ini")
                return@setOnClickListener
            }

            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                val allowOutside = getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", true)
                if (!isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    showLocationError()
                    if (!allowOutside) return@setOnClickListener
                }
                validateNamaThen(this::simpanAbsenPulang)
            } else {
                showToast("Mohon tunggu, sedang mengambil lokasi...")
                pendingPulang = true
                getCurrentLocation()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun validateNamaThen(onValid: () -> Unit) {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val nama = prefs.getString("nama_pengguna", "")?.trim() ?: ""

        if (nama.isEmpty()) {
            showToast("Isi nama pengguna dulu (sesuai tabel pengguna)")
            return
        }

        showToast("Memeriksa nama di Supabase...")
        PenggunaApi().getByUsername(nama, object : PenggunaApi.PenggunaCallback {
            override fun onSuccess(pengguna: PenggunaApi.Pengguna) {
                try {
                    getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("id_pengguna", pengguna.iduser)
                        .apply()
                } catch (ignored: Exception) {}
                runOnUiThread(onValid)
            }

            override fun onError(error: String) {
                runOnUiThread { showToast("Nama tidak valid: $error") }
            }
        })
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
        Location.distanceBetween(currentLatitude, currentLongitude, TARGET_LATITUDE, TARGET_LONGITUDE, results)
        val distanceInKm = results[0] / 1000

        val message = "❌ Absen hanya bisa di Jurusan TI Polije\n" +
                "Anda berjarak ${String.format(Locale.getDefault(), "%.1f", distanceInKm)} km dari lokasi."
        showToast(message)
    }

    private fun simpanAbsenPulang() {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val status = prefs.getString("status_absen", "BELUM_ABSEN")

        if (status == "BELUM_ABSEN") {
            showToast("❌ Anda harus absen masuk dulu")
            return
        }

        if (status == "SUDAH_PULANG") {
            showToast("❌ Anda sudah absen pulang hari ini")
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
        val absenData = "PULANG|$currentTime|$currentDate|$currentAddress|$currentLatitude|$currentLongitude"
        val key = "riwayat_${System.currentTimeMillis()}"
        editor.putString(key, absenData)
        editor.putString("jam_pulang_hari_ini", currentTime)
        editor.putString("lokasi_pulang_hari_ini", currentAddress)
        editor.putString("status_absen", "SUDAH_PULANG")
        editor.putString("last_absen_date", currentDate)
        val ok = editor.commit()
        println("[Pulang] saved key=$key, ok=$ok")
        try {
            if (getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", true)) {
                showToast("Pulang tersimpan lokal: $currentTime")
            }
        } catch (ignored: Exception) {}

        getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("go_to_riwayat", true)
            .commit()

        setResult(RESULT_OK)

        val message = "✅ Absen Pulang berhasil!\nWaktu: $currentTime" +
                "\nLokasi: $currentAddress" +
                "\nJam kerja: ${tvJamKerja.text}" +
                "\n📍 Lokasi: Jurusan TI Polije ✅"
        showToast(message)
        finish()
    }

    private fun simpanKeDatabaseOnline(tanggal: String, jamPulang: String, lokasiPulang: String) {
        try {
            val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val namaPengguna = prefs.getString("nama_pengguna", "")?.trim() ?: ""

            if (namaPengguna.isEmpty()) {
                throw IllegalStateException("Nama pengguna kosong")
            }

            val keteranganIzin = prefs.getString("file_izin_name", "")
            val selectedStatus = prefs.getString("selected_status", "Hadir")
            val idPengguna = prefs.getInt("id_pengguna", Math.abs(namaPengguna.hashCode()))

            SupabaseHelper().simpanPulang(tanggal, jamPulang, lokasiPulang, idPengguna, selectedStatus, keteranganIzin, object : SupabaseHelper.SimpanCallback {
                override fun onSuccess(message: String) {
                    runOnUiThread {
                        println("✅ Database Online: $message")
                        showToast("Cloud: $message")
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        println("⚠️ Database Offline: $error")
                        showToast("Cloud error: $error")
                    }
                }
            })
        } catch (e: Exception) {
            println("⚠️ Error koneksi database: ${e.message}")
        }
    }

    private fun hitungJamKerja(jamMasuk: String, jamPulang: String) {
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeMasuk = sdf.parse(jamMasuk)
            val timePulang = sdf.parse(jamPulang)

            if (timeMasuk != null && timePulang != null) {
                var diff = timePulang.time - timeMasuk.time
                if (diff < 0) diff += 24 * 60 * 60 * 1000
                val hours = diff / (60 * 60 * 1000)
                val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)
                tvJamKerja.text = "$hours jam $minutes menit"
            } else {
                tvJamKerja.text = "0 jam 0 menit"
            }
        } catch (e: Exception) {
            tvJamKerja.text = "0 jam 0 menit"
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && (
                        grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                                (grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                        )) {
                getCurrentLocation()
            } else {
                tvLocation.text = "Izin lokasi ditolak"
                tvCoordinates.text = "Tidak dapat mengakses lokasi"
                showToast("Izin lokasi diperlukan untuk absen")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        tvCoordinates.text = String.format(Locale.getDefault(),
                            "Lat: %.6f, Long: %.6f", currentLatitude, currentLongitude)

                        getAddressFromLocation(location)
                        if (pendingPulang) {
                            if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                                pendingPulang = false
                                validateNamaThen(this::simpanAbsenPulang)
                            } else {
                                pendingPulang = false
                                showLocationError()
                            }
                        }
                    } else {
                        val cts = CancellationTokenSource()
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cts.token
                        ).addOnSuccessListener { loc ->
                            if (loc != null) {
                                currentLatitude = loc.latitude
                                currentLongitude = loc.longitude
                                tvCoordinates.text = String.format(Locale.getDefault(),
                                    "Lat: %.6f, Long: %.6f", currentLatitude, currentLongitude)
                                getAddressFromLocation(loc)
                                if (pendingPulang) {
                                    if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                                        pendingPulang = false
                                        validateNamaThen(this::simpanAbsenPulang)
                                    } else {
                                        pendingPulang = false
                                        showLocationError()
                                    }
                                }
                            } else {
                                tvLocation.text = "Lokasi tidak ditemukan"
                                tvCoordinates.text = "Coba lagi..."
                            }
                        }.addOnFailureListener { e ->
                            tvLocation.text = "Gagal mengambil lokasi"
                            tvCoordinates.text = "Error: ${e.message}"
                            showToast("Gagal mengambil lokasi: ${e.message}")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    tvLocation.text = "Gagal mengambil lokasi"
                    tvCoordinates.text = "Error: ${e.message}"
                    showToast("Gagal mengambil lokasi: ${e.message}")
                }
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
                    tvLocation.text = "📍 $currentAddress ✅"
                } else {
                    tvLocation.text = "❌ $currentAddress"
                }

                if (pendingPulang && isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    pendingPulang = false
                    validateNamaThen(this::simpanAbsenPulang)
                }
            } else {
                currentAddress = "Lokasi: $currentLatitude, $currentLongitude"
                if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    tvLocation.text = "📍 $currentAddress ✅"
                } else {
                    tvLocation.text = "❌ $currentAddress"
                }
                if (pendingPulang && isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    pendingPulang = false
                    simpanAbsenPulang()
                }
            }
        } catch (e: Exception) {
            currentAddress = "Lokasi: $currentLatitude, $currentLongitude"
            if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                tvLocation.text = "📍 $currentAddress ✅"
            } else {
                tvLocation.text = "❌ $currentAddress"
            }
            if (pendingPulang && isWithinTargetLocation(currentLatitude, currentLongitude)) {
                pendingPulang = false
                validateNamaThen(this::simpanAbsenPulang)
            }
        }
    }

    private fun showToast(message: String) {
        try {
            singleToast?.cancel()
            singleToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
            singleToast?.show()
        } catch (ignored: Exception) {}
    }
}