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
import androidx.annotation.RequiresPermission
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

    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var btnAbsenMasuk: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentAddress = "Lokasi tidak diketahui"

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absen_masuk)

        tvGreeting = findViewById(R.id.tv_greeting)
        tvDate = findViewById(R.id.tv_date)
        tvLocation = findViewById(R.id.tv_location)
        tvCoordinates = findViewById(R.id.tv_coordinates)
        btnAbsenMasuk = findViewById(R.id.btn_absen_masuk)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tvGreeting.setText("Selamat Datang, Kasir")
        setCurrentDate()
        requestLocationPermission()

        btnAbsenMasuk.setOnClickListener {
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
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
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
        val namaRaw = prefs.getString("nama_pengguna", "")?.trim() ?: ""
        val nama = namaRaw.replace("\\s+".toRegex(), " ")

        println("🔍 validateNamaThen: Nama dari SharedPreferences = '$nama'")

        if (nama.isEmpty()) {
            println("❌ validateNamaThen: Nama KOSONG - tidak bisa lanjut")
            Toast.makeText(this, "Isi nama pengguna dulu (sesuai tabel pengguna)", Toast.LENGTH_SHORT).show()
            return
        }

        println("🔍 validateNamaThen: Memeriksa nama '$nama' di Supabase...")
        Toast.makeText(this, "Memeriksa nama di Supabase...", Toast.LENGTH_SHORT).show()

        PenggunaApi().getByUsername(nama, object : PenggunaApi.PenggunaCallback {
            override fun onSuccess(pengguna: PenggunaApi.Pengguna) {
                println("✅ validateNamaThen: Nama VALID - $nama")
                println("✅ validateNamaThen: Data pengguna = ${pengguna.username}, ID: ${pengguna.iduser}")
                try {
                    getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("id_pengguna", pengguna.iduser)
                        .apply()
                } catch (ignored: Exception) {}
                runOnUiThread(onValid)
            }

            override fun onError(error: String) {
                println("❌ validateNamaThen: Nama TIDAK VALID - $nama")
                println("❌ validateNamaThen: Error dari API = $error")
                runOnUiThread {
                    Toast.makeText(this@AbsenMasukActivity, "Nama tidak valid: $error", Toast.LENGTH_LONG).show()
                }
            }
        })
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

    private fun setCurrentDate() {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
        tvDate.text = sdf.format(Date())
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                tvLocation.text = "Izin lokasi ditolak"
                tvCoordinates.text = "Tidak dapat mengakses lokasi"
                Toast.makeText(this, "Izin lokasi diperlukan untuk absen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude

                        tvCoordinates.text = String.format(Locale.getDefault(),
                            "Lat: %.6f, Long: %.6f", currentLatitude, currentLongitude)

                        if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                            tvLocation.text = "📍 Lokasi: Jurusan TI Polije ✅"
                        } else {
                            tvLocation.text = "❌ Diluar area TI Polije"
                        }

                        getAddressFromLocation(location)
                    } else {
                        tvLocation.text = "Lokasi tidak ditemukan"
                        tvCoordinates.text = "Coba lagi..."
                    }
                }
                .addOnFailureListener { e ->
                    tvLocation.text = "Gagal mengambil lokasi"
                    tvCoordinates.text = "Error: ${e.message}"
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
            } else {
                currentAddress = "Lokasi: $currentLatitude, $currentLongitude"
                if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                    tvLocation.text = "📍 $currentAddress ✅"
                } else {
                    tvLocation.text = "❌ $currentAddress"
                }
            }
        } catch (e: Exception) {
            currentAddress = "Lokasi: $currentLatitude, $currentLongitude"
            if (isWithinTargetLocation(currentLatitude, currentLongitude)) {
                tvLocation.text = "📍 $currentAddress ✅"
            } else {
                tvLocation.text = "❌ $currentAddress"
            }
        }
    }
}