package com.almil.dessertcakekinian.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.JadwalMingguanApi
import com.almil.dessertcakekinian.database.ShiftDefinitionApi
import com.almil.dessertcakekinian.database.PenggunaApi
import com.almil.dessertcakekinian.database.SupabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class AbsenPulangActivity : AppCompatActivity() {

    private var tvNamaKaryawan: TextView? = null
    private var tvLokasi: TextView? = null
    private var tvJamRealTime: TextView? = null
    private var tvTanggalRealTime: TextView? = null
    private var btnKonfirmasi: Button? = null
    private var btnBack: ImageButton? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var currentAddress: String = "Lokasi tidak diketahui"
    private var pendingPulang: Boolean = false

    // Variabel untuk shift
    private var userShift: String = "Pagi"
    private var userShiftStart: String = "07:00"
    private var userShiftEnd: String = "12:00"
    private var currentHour: Int = 0
    private var currentMinute: Int = 0
    private var isShiftValid: Boolean = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1002
        private const val TARGET_LATITUDE = -8.157510
        private const val TARGET_LONGITUDE = 113.722778
        private const val RADIUS_METERS = 50.0f
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absen_pulang)

        initViews()
        initLocationClient()
        resetAbsenStatusIfNeeded()
        enableDevMode()
        setUserData()
        setCurrentDateTime()
        // Ambil data shift user SEBELUM check absen status
        getUserShiftFromJadwal()
        checkAbsenStatus()
        setupButtonListeners()
        requestLocationPermission()
    }

    private fun initViews() {
        tvNamaKaryawan = findViewById(R.id.tvNamaKaryawan)
        tvLokasi = findViewById(R.id.tvLokasi)
        tvJamRealTime = findViewById(R.id.tvJamRealTime)
        tvTanggalRealTime = findViewById(R.id.tvTanggalRealTime)
        btnKonfirmasi = findViewById(R.id.btnKonfirmasi)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun initLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun enableDevMode() {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("dev_mode", true)
        editor.apply()
    }

    private fun setUserData() {
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "Karyawan")
        if (tvNamaKaryawan != null) {
            tvNamaKaryawan!!.text = userName
        }
    }

    private fun setCurrentDateTime() {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))

            val currentTime = timeFormat.format(Date())
            val currentDate = dateFormat.format(Date())

            if (tvJamRealTime != null) {
                tvJamRealTime!!.text = currentTime
            }
            if (tvTanggalRealTime != null) {
                tvTanggalRealTime!!.text = currentDate
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                if (btnKonfirmasi != null) {
                    btnKonfirmasi!!.isEnabled = false
                    btnKonfirmasi!!.text = "Tidak Dalam Shift"
                }

                AlertDialog.Builder(this)
                    .setTitle("⏰ Bukan Waktu Shift Anda")
                    .setMessage("Shift $userShift Anda: $userShiftStart - $userShiftEnd\n" +
                            "Sekarang jam: ${String.format("%02d:%02d", currentHour, currentMinute)}\n\n" +
                            "Silakan absen pulang pada jam shift Anda.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                if (btnKonfirmasi != null) {
                    btnKonfirmasi!!.isEnabled = true
                    btnKonfirmasi!!.text = "KONFIRMASI ABSEN PULANG"
                }
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
            if (btnKonfirmasi != null) {
                btnKonfirmasi!!.isEnabled = false
                btnKonfirmasi!!.text = "Error Shift"
            }

            AlertDialog.Builder(this)
                .setTitle("❌ Error Shift")
                .setMessage("$message\n\nTidak bisa melakukan absen pulang.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun checkAbsenStatus() {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val status = prefs.getString("status_absen", "BELUM_ABSEN")

        if ("BELUM_ABSEN" == status) {
            showToast("❌ Anda harus absen masuk dulu")
            if (btnKonfirmasi != null) {
                btnKonfirmasi!!.isEnabled = false
            }
            return
        }

        if ("SUDAH_PULANG" == status) {
            showToast("❌ Anda sudah absen pulang hari ini")
            if (btnKonfirmasi != null) {
                btnKonfirmasi!!.isEnabled = false
            }
            return
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
            println("🔄 Status absen direset untuk hari baru - Pulang")
        }
    }

    private fun setupButtonListeners() {
        if (btnBack != null) {
            btnBack!!.setOnClickListener {
                finish()
            }
        }

        if (btnKonfirmasi != null) {
            btnKonfirmasi!!.setOnClickListener {
                handleKonfirmasiClick()
            }
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun handleKonfirmasiClick() {
        if (btnKonfirmasi != null) {
            btnKonfirmasi!!.isEnabled = false
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                btnKonfirmasi!!.isEnabled = true
            }, 1500)
        }

        val prefsCheck = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val statusCheck = prefsCheck.getString("status_absen", "BELUM_ABSEN")

        if ("BELUM_ABSEN" == statusCheck) {
            showToast("❌ Anda harus absen masuk dulu")
            return
        }

        if ("SUDAH_PULANG" == statusCheck) {
            showToast("❌ Anda sudah absen pulang hari ini")
            return
        }

        // CEK SHIFT DULU SEBELUM LANJUT
        if (!isShiftValid) {
            showToast("❌ Bukan waktu shift $userShift Anda ($userShiftStart-$userShiftEnd)")
            return
        }

        if (currentLatitude != 0.0 && currentLongitude != 0.0) {
            val allowOutside = getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", true)
            val withinLocation = isWithinTargetLocation(currentLatitude, currentLongitude)
            if (!withinLocation) {
                showLocationError()
                if (!allowOutside) {
                    return
                }
            }
            validateNamaThen()
        } else {
            showToast("Mohon tunggu, sedang mengambil lokasi...")
            pendingPulang = true
            requestLocationPermission()
        }
    }

    private fun validateNamaThen() {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val nama = userSession.getString("USER_NAME", "")
        var namaTrimmed = ""
        if (nama != null) {
            namaTrimmed = nama.trim()
        }

        if (namaTrimmed.isEmpty()) {
            showToast("Error: Nama pengguna tidak ditemukan")
            return
        }

        val editor = prefs.edit()
        editor.putString("nama_pengguna", namaTrimmed)
        editor.apply()
        simpanAbsenPulang()
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

        if ("BELUM_ABSEN" == status) {
            showToast("❌ Anda harus absen masuk dulu")
            return
        }

        if ("SUDAH_PULANG" == status) {
            showToast("❌ Anda sudah absen pulang hari ini")
            return
        }

        // VALIDASI SHIFT LAGI SEBELUM SIMPAN
        if (!isShiftValid) {
            showToast("❌ Bukan waktu shift $userShift Anda ($userShiftStart-$userShiftEnd)")
            return
        }

        val allowOutside = getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", true)
        val withinLocation = isWithinTargetLocation(currentLatitude, currentLongitude)
        if (!withinLocation) {
            showLocationError()
            if (!allowOutside) {
                return
            }
        }

        val currentTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        simpanKeDatabaseOnline(currentDate, currentTimeFormatted, currentAddress)

        val editor = prefs.edit()
        val absenData = "PULANG|$currentTimeFormatted|$currentDate|$currentAddress|$currentLatitude|$currentLongitude"
        val key = "riwayat_${System.currentTimeMillis()}"
        editor.putString(key, absenData)
        editor.putString("jam_pulang_hari_ini", currentTimeFormatted)
        editor.putString("lokasi_pulang_hari_ini", currentAddress)
        editor.putString("status_absen", "SUDAH_PULANG")
        editor.putString("last_absen_date", currentDate)
        val ok = editor.commit()
        println("[Pulang] saved key=$key, ok=$ok")

        try {
            val devMode = getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", true)
            if (devMode) {
                showToast("Pulang tersimpan lokal: $currentTimeFormatted")
            }
        } catch (ignored: Exception) {}

        val riwayatEditor = getSharedPreferences("absen_data", Context.MODE_PRIVATE).edit()
        riwayatEditor.putBoolean("go_to_riwayat", true)
        riwayatEditor.commit()

        setResult(RESULT_OK)

        val message = "✅ Absen Pulang berhasil!\nWaktu: $currentTimeFormatted" +
                "\nLokasi: $currentAddress" +
                "\n📍 Lokasi: Jurusan TI Polije ✅"
        showToast(message)
        finish()
    }

    private fun simpanKeDatabaseOnline(tanggal: String, jamPulang: String, lokasiPulang: String) {
        try {
            val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val namaPengguna = prefs.getString("nama_pengguna", "")
            var namaTrimmed = ""
            if (namaPengguna != null) {
                namaTrimmed = namaPengguna.trim()
            }

            if (namaTrimmed.isEmpty()) {
                throw IllegalStateException("Nama pengguna kosong")
            }

            val keteranganIzin = prefs.getString("file_izin_name", "")
            val selectedStatus = prefs.getString("selected_status", "Hadir")
            val idPengguna = prefs.getInt("id_pengguna", Math.abs(namaTrimmed.hashCode()))

            val callback = object : SupabaseHelper.SimpanCallback {
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
            }

            val supabaseHelper = SupabaseHelper()
            supabaseHelper.simpanPulang(tanggal, jamPulang, lokasiPulang, idPengguna, selectedStatus, keteranganIzin, callback)
        } catch (e: Exception) {
            println("⚠️ Error koneksi database: ${e.message}")
        }
    }

    private fun requestLocationPermission() {
        val fineLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED && coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Diperlukan")
                    .setMessage("Aplikasi membutuhkan akses lokasi untuk memastikan Anda berada di Jurusan TI Polije saat absen pulang.")
                    .setPositiveButton("OK") { dialogInterface, which ->
                        ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            LOCATION_PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("Batal") { dialogInterface, which ->
                        Toast.makeText(this, "Tidak bisa absen pulang tanpa izin lokasi", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .create()
                dialog.show()
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            getCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED && coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
            return
        }

        if (fusedLocationClient != null) {
            fusedLocationClient!!.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude

                        try {
                            val geocoder = Geocoder(this, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
                            if (addresses != null && addresses.size > 0) {
                                val address = addresses[0]
                                val addressLine = address.getAddressLine(0)
                                if (addressLine != null) {
                                    currentAddress = addressLine
                                } else {
                                    currentAddress = "Lokasi: $currentLatitude, $currentLongitude"
                                }
                            } else {
                                currentAddress = "Lokasi: $currentLatitude, $currentLongitude"
                            }
                        } catch (e: Exception) {
                            currentAddress = "Lokasi: $currentLatitude, $currentLongitude"
                            e.printStackTrace()
                        }

                        if (tvLokasi != null) {
                            tvLokasi!!.text = currentAddress
                        }

                        if (pendingPulang) {
                            pendingPulang = false
                            validateNamaThen()
                        }
                    } else {
                        showToast("Lokasi tidak tersedia, coba lagi")
                        if (pendingPulang) {
                            pendingPulang = false
                            if (btnKonfirmasi != null) {
                                btnKonfirmasi!!.isEnabled = true
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Gagal mengambil lokasi: ${e.message}")
                    if (pendingPulang) {
                        pendingPulang = false
                        if (btnKonfirmasi != null) {
                            btnKonfirmasi!!.isEnabled = true
                        }
                    }
                }
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
                getCurrentLocation()
            } else {
                showToast("Izin lokasi ditolak, tidak bisa absen pulang")
                if (pendingPulang) {
                    pendingPulang = false
                    if (btnKonfirmasi != null) {
                        btnKonfirmasi!!.isEnabled = true
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Extension function untuk showToast
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Activity.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}