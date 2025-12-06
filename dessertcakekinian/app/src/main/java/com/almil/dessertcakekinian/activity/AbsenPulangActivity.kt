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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.JadwalMingguanApi
import com.almil.dessertcakekinian.database.ShiftDefinitionApi
import com.almil.dessertcakekinian.database.SupabaseHelper
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

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

    // Executor
    private val executor = Executors.newSingleThreadExecutor()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1002
        const val TARGET_LATITUDE = -8.375066
        const val TARGET_LONGITUDE = 113.603322
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

        // ✅ AUTO CHECKOUT: Cek data kemarin yang lupa pulang
        checkAndAutoCompleteYesterday()

        getUserShiftFromJadwal()
        setupButtonListeners()

        // ✅ PERBAIKAN: Langsung request location permission di onCreate
        requestLocationPermission()
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

                println("🔄 CHECK AUTO COMPLETE YESTERDAY: $yesterday, Shift: $userShift")

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

    // FUNGSI: Ambil shift user dari jadwal
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
                        println("🤔 Hari ini: $today")

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

                                // Simpan shift ke SharedPreferences
                                saveShiftToPreferences(userShift, userShiftStart, userShiftEnd)

                                // ✅ PERBAIKAN: Validasi waktu shift untuk PULANG dengan logika yang benar
                                validateShiftTimeForPulang()

                                // Check absen status setelah dapat shift
                                checkAbsenStatusBasedOnShift()
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

    // FUNGSI: Simpan shift ke SharedPreferences
    private fun saveShiftToPreferences(shift: String, start: String, end: String) {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("user_shift", shift)
        editor.putString("user_shift_start", start)
        editor.putString("user_shift_end", end)
        editor.apply()
        println("💾 Shift disimpan ke SharedPreferences: $shift ($start - $end)")
    }

    // FUNGSI: Check absen status berdasarkan shift
    private fun checkAbsenStatusBasedOnShift() {
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (userName.isEmpty()) {
            println("❌ Tidak bisa check absen: nama user kosong")
            return
        }

        // ✅ PERBAIKAN: Gunakan callback dengan 3 parameter
        SupabaseHelper().checkAbsenStatusWithShift(userName, currentDate, userShift) { sudahAbsen, sudahCheckout, status ->
            runOnUiThread {
                // Validasi shift dulu sebelum cek status absen
                if (!isShiftValid) {
                    if (btnKonfirmasi != null) {
                        btnKonfirmasi!!.isEnabled = false
                        btnKonfirmasi!!.text = "Tidak Bisa Pulang"
                    }
                    return@runOnUiThread
                }

                if (!sudahAbsen) {
                    showToast("❌ Anda harus absen masuk dulu untuk shift $userShift")
                    if (btnKonfirmasi != null) {
                        btnKonfirmasi!!.isEnabled = false
                        btnKonfirmasi!!.text = "BELUM ABSEN MASUK"
                    }
                } else if (sudahCheckout) {
                    showToast("❌ Anda sudah absen pulang untuk shift $userShift hari ini")
                    if (btnKonfirmasi != null) {
                        btnKonfirmasi!!.isEnabled = false
                        btnKonfirmasi!!.text = "SUDAH ABSEN PULANG"
                    }
                } else {
                    // Boleh absen pulang
                    if (btnKonfirmasi != null) {
                        btnKonfirmasi!!.isEnabled = true
                        btnKonfirmasi!!.text = "KONFIRMASI ABSEN PULANG"
                    }
                }
            }
        }
    }

    // ✅ PERBAIKAN BARU: Validasi waktu shift untuk PULANG dengan logika yang benar untuk shift malam
    private fun validateShiftTimeForPulang() {
        val calendar = Calendar.getInstance()
        currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        currentMinute = calendar.get(Calendar.MINUTE)

        val currentTime = "$currentHour:$currentMinute"

        // ✅ PERBAIKAN BARU: Gunakan logika yang benar untuk semua jenis shift
        isShiftValid = isWithinShiftTimeForPulangCorrect(currentTime, userShiftStart, userShiftEnd)

        runOnUiThread {
            if (!isShiftValid) {
                if (btnKonfirmasi != null) {
                    btnKonfirmasi!!.isEnabled = false
                    btnKonfirmasi!!.text = "Tidak Bisa Pulang"
                }

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val currentTimeFormatted = timeFormat.format(Date())

                AlertDialog.Builder(this)
                    .setTitle("⌛ Bukan Waktu Pulang")
                    .setMessage("Shift $userShift Anda: $userShiftStart - $userShiftEnd\n" +
                            "Sekarang jam: $currentTimeFormatted\n\n" +
                            "Absen PULANG hanya bisa dilakukan:\n" +
                            "• SELAMA shift berlangsung\n" +
                            "• HANYA sampai AKHIR SHIFT ($userShiftEnd)\n\n" +
                            "Jika lupa absen pulang, sistem akan auto checkout jam $userShiftEnd.")
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
                println("✅ Validasi shift PULANG: $userShift ($userShiftStart-$userShiftEnd) - Jam $currentHour:$currentMinute - DIPERBOLEHKAN")
            }
        }
    }

    // ✅ PERBAIKAN BARU: Fungsi cek waktu untuk absen PULANG yang benar untuk shift malam
    private fun isWithinShiftTimeForPulangCorrect(currentTime: String, shiftStart: String, shiftEnd: String): Boolean {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            // Parse waktu
            val current = timeFormat.parse(currentTime)
            val start = timeFormat.parse(shiftStart)
            val end = timeFormat.parse(shiftEnd)

            // Jika end < start (contoh: 00:00 < 18:00), berarti shift melewati tengah malam
            if (end.before(start)) {
                // Untuk absen PULANG shift malam:
                // Bisa pulang jika: start <= current < 24:00 ATAU 00:00 <= current <= end
                // Tapi untuk shift 18:00-00:00, jam 23:59 masih bisa pulang, jam 00:01 sudah tidak bisa

                // Konversi ke menit untuk perhitungan
                val currentMinutes = currentHour * 60 + currentMinute
                val startMinutes = timeFormat.parse(shiftStart).let {
                    val cal = Calendar.getInstance()
                    cal.time = it
                    cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                }
                val endMinutes = timeFormat.parse(shiftEnd).let {
                    val cal = Calendar.getInstance()
                    cal.time = it
                    cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                }

                // Untuk shift malam (contoh: 18:00-00:00):
                // Jika current >= start (>= 18:00) -> masih hari ini, bisa pulang
                // Jika current <= end (<= 00:00) -> sudah hari besok, bisa pulang
                // Tapi untuk 00:00 artinya tepat tengah malam, masih boleh
                return (currentMinutes >= startMinutes) || (currentMinutes <= endMinutes && currentMinutes >= 0)

            } else {
                // Shift normal (tidak melewati tengah malam)
                // Bisa pulang selama shift: start <= current <= end
                return current >= start && current <= end
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return true // Fallback
        }
    }

    // FUNGSI: Tampilkan error shift
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

    private fun resetAbsenStatusIfNeeded() {
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val lastAbsenDate = prefs.getString("last_absen_date_$userName", "")
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (lastAbsenDate != currentDate) {
            val editor = prefs.edit()
            editor.putString("status_absen_$userName", "BELUM_ABSEN")
            editor.putString("jam_masuk_hari_ini_$userName", "")
            editor.putString("lokasi_masuk_hari_ini_$userName", "")
            editor.putString("jam_pulang_hari_ini_$userName", "")
            editor.putString("lokasi_pulang_hari_ini_$userName", "")
            editor.apply()
            println("🔄 Status absen direset untuk hari baru - $userName")
        }
    }

    private fun setupButtonListeners() {
        btnBack?.setOnClickListener {
            finish()
        }

        btnKonfirmasi?.setOnClickListener {
            val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userName = userSession.getString("USER_NAME", "") ?: ""
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (userName.isEmpty()) {
                showToast("❌ Error: User tidak ditemukan")
                return@setOnClickListener
            }

            // ✅ PERBAIKAN: Gunakan callback dengan 3 parameter
            SupabaseHelper().checkAbsenStatusWithShift(userName, currentDate, userShift) { sudahAbsen, sudahCheckout, status ->
                runOnUiThread {
                    if (!sudahAbsen) {
                        showToast("❌ Anda harus absen masuk dulu untuk shift $userShift")
                        return@runOnUiThread
                    }

                    if (sudahCheckout) {
                        showToast("❌ Anda sudah absen pulang untuk shift $userShift hari ini")
                        return@runOnUiThread
                    }

                    // CEK SHIFT DULU SEBELUM LANJUT
                    if (!isShiftValid) {
                        showToast("❌ Bukan waktu pulang shift $userShift Anda")
                        return@runOnUiThread
                    }

                    // ✅ PERBAIKAN: Cek permission dulu sebelum ambil lokasi
                    if (!checkLocationPermission()) {
                        showToast("❌ Izin lokasi belum diberikan")
                        requestLocationPermission()
                        return@runOnUiThread
                    }

                    if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                        val allowOutside = getSharedPreferences("absen_data", Context.MODE_PRIVATE).getBoolean("dev_mode", true)
                        val withinLocation = isWithinTargetLocation(currentLatitude, currentLongitude)

                        if (!withinLocation) {
                            showLocationError()
                            if (!allowOutside) {
                                showToast("❌ Tidak bisa absen di luar lokasi TI Polije")
                                return@runOnUiThread
                            } else {
                                showToast("⚠️ Dev Mode: Absen di luar lokasi diperbolehkan")
                            }
                        }
                        validateNamaThen()
                    } else {
                        showToast("Mohon tunggu, sedang mengambil lokasi...")
                        pendingPulang = true
                        getCurrentLocation()
                    }
                }
            }
        }
    }

    // ✅ PERBAIKAN: Fungsi permission yang sama seperti AbsenMasuk
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
                        "• Memastikan absen pulang dilakukan di area kampus\n" +
                        "• Mencegah kecurangan absen dari luar\n" +
                        "• Validasi kehadiran fisik\n\n" +
                        "Tanpa izin lokasi, Anda TIDAK BISA melakukan absen pulang.")
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
                    showToast("Izin lokasi ditolak, tidak bisa absen pulang")
                    if (btnKonfirmasi != null) {
                        btnKonfirmasi!!.isEnabled = false
                    }
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
                showToast("Izin lokasi ditolak, tidak bisa absen pulang")
                if (btnKonfirmasi != null) {
                    btnKonfirmasi!!.isEnabled = false
                }

                // Tampilkan dialog untuk buka settings
                AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Ditolak")
                    .setMessage("Anda tidak bisa absen pulang tanpa izin lokasi.\n\n" +
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
        fusedLocationClient?.lastLocation
            ?.addOnSuccessListener { location ->
                if (location != null) {
                    processLocation(location)
                } else {
                    requestNewLocation()
                }
            }
            ?.addOnFailureListener { e ->
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
                    fusedLocationClient?.removeLocationUpdates(this)
                }
            }
        }

        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        Handler(Looper.getMainLooper()).postDelayed({
            fusedLocationClient?.removeLocationUpdates(locationCallback)
            if (currentLatitude == 0.0 && currentLongitude == 0.0) {
                showToast("Gagal mengambil lokasi setelah 10 detik.")
                if (tvLokasi != null) {
                    tvLokasi!!.text = "Lokasi tidak siap"
                }
            }
        }, 10000)
    }

    private fun processLocation(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude
        resolveAddress(location)

        // Update UI berdasarkan lokasi
        val within = isWithinTargetLocation(currentLatitude, currentLongitude)
        runOnUiThread {
            if (within) {
                if (tvLokasi != null) {
                    tvLokasi!!.text = "📍 $currentAddress ✅"
                }

                // ✅ PERBAIKAN: Update status button setelah dapat lokasi
                if (pendingPulang) {
                    pendingPulang = false
                    if (btnKonfirmasi != null) {
                        btnKonfirmasi!!.isEnabled = true
                    }
                    validateNamaThen()
                }
            } else {
                if (tvLokasi != null) {
                    tvLokasi!!.text = "❌ $currentAddress"
                }
                if (btnKonfirmasi != null) {
                    btnKonfirmasi!!.isEnabled = false
                }
            }
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
            userLat,
            userLng,
            TARGET_LATITUDE,
            TARGET_LONGITUDE,
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
        val userSession = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (userName.isEmpty()) {
            showToast("❌ Error: User tidak ditemukan")
            return
        }

        // ✅ PERBAIKAN: Gunakan callback dengan 3 parameter
        SupabaseHelper().checkAbsenStatusWithShift(userName, currentDate, userShift) { sudahAbsen, sudahCheckout, status ->
            runOnUiThread {
                if (!sudahAbsen) {
                    showToast("❌ Anda harus absen masuk dulu untuk shift $userShift")
                    return@runOnUiThread
                }

                if (sudahCheckout) {
                    showToast("❌ Anda sudah absen pulang untuk shift $userShift hari ini")
                    return@runOnUiThread
                }

                // VALIDASI SHIFT LAGI SEBELUM SIMPAN
                if (!isShiftValid) {
                    showToast("❌ Bukan waktu pulang shift $userShift Anda")
                    return@runOnUiThread
                }

                val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                val currentDateObj = Date()
                val tanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDateObj)
                val jamPulang = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentDateObj)
                val lokasiPulang = currentAddress

                // Simpan ke SharedPreferences dengan key user specific
                prefs.edit()
                    .putString("status_absen_$userName", "SUDAH_PULANG")
                    .putString("jam_pulang_hari_ini_$userName", jamPulang)
                    .putString("lokasi_pulang_hari_ini_$userName", lokasiPulang)
                    .apply()

                // Simpan ke Supabase (Online storage) dengan shift
                simpanKeDatabaseOnline(tanggal, jamPulang, lokasiPulang, userName)

                val message = "✅ Absen Pulang Berhasil (Shift $userShift):\nJam $jamPulang" +
                        "\n📍 Lokasi: Jurusan TI Polije ✅"
                showToast(message)
                finish()
            }
        }
    }

    // ✅ PERBAIKAN: Tambahkan parameter username dan perbaiki pemanggilan
    private fun simpanKeDatabaseOnline(tanggal: String, jamPulang: String, lokasiPulang: String, username: String) {
        try {
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

            // ✅ PERBAIKAN: Hilangkan koma berlebih dan tambahkan parameter username
            supabaseHelper.simpanPulang(
                tanggal = tanggal,
                jamPulang = jamPulang,
                lokasiPulang = lokasiPulang,
                username = username,
                shift = userShift,
                callback = callback
            )

        } catch (e: Exception) {
            println("⚠️ Error koneksi database: ${e.message}")
        }
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