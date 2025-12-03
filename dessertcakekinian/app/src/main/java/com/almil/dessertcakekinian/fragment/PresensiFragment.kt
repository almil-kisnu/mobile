package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.RiwayatAdapter
import com.almil.dessertcakekinian.database.SupabaseHelper
import com.almil.dessertcakekinian.database.ShiftDefinitionApi
import com.almil.dessertcakekinian.database.JadwalMingguanApi
import com.almil.dessertcakekinian.dialog.dialog_ajukan_izin
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class PresensiFragment : Fragment() {

    private lateinit var rvRiwayat: RecyclerView
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnKalender: ImageButton
    private lateinit var btnAjukanIzin: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvToolbarTitle: TextView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var allRiwayatList = listOf<RiwayatPresensi>()
    private var currentUsername: String = "User"
    private var currentUserId: Int = 0
    private var allPenggunaList = listOf<com.almil.dessertcakekinian.database.PenggunaApi.Pengguna>()
    private lateinit var timeoutHandler: Handler

    private var jadwalMingguanList = listOf<JadwalMingguanApi.JadwalMingguan>()
    private var shiftDefinitions = mutableMapOf<String, ShiftDefinitionApi.ShiftDefinition>()
    private var isShiftDataLoaded = false
    private var isJadwalDataLoaded = false
    private var isGeneratingAlpha = false

    private var backPressCallback: OnBackPressedCallback? = null

    companion object {
        const val STATUS_SEMUA = "Semua Status"
        const val STATUS_HADIR = "Hadir"
        const val STATUS_IZIN = "Izin"
        const val STATUS_TERLAMBAT = "Terlambat"
        const val STATUS_ALPHA = "Alpha"
        const val STATUS_UTANG_JAM = "Utang Jam"
        const val STATUS_BELUM_PULANG = "Belum Pulang"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_presensi, container, false)

        currentUsername = getUsernameFromSession()
        currentUserId = getUserIdFromSession()
        timeoutHandler = Handler(Looper.getMainLooper())

        initViews(view)
        setupSpinner()
        setupRecyclerView()
        setupClickListeners()
        setupSwipeRefresh()

        loadShiftDefinitions()

        return view
    }

    private fun initViews(view: View) {
        rvRiwayat = view.findViewById(R.id.rvRiwayat)
        spinnerStatus = view.findViewById(R.id.spinnerStatus)
        btnKalender = view.findViewById(R.id.btnKalender)
        btnAjukanIzin = view.findViewById(R.id.btnAjukanIzin)
        btnBack = view.findViewById(R.id.btnBack)
        tvToolbarTitle = view.findViewById(R.id.tvToolbarTitle)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadShiftDefinitions()
        }
    }

    private fun loadJadwalMingguan() {
        JadwalMingguanApi().listAllWithDetails(object : JadwalMingguanApi.JadwalListCallback {
            override fun onSuccess(list: List<JadwalMingguanApi.JadwalMingguan>) {
                jadwalMingguanList = list
                isJadwalDataLoaded = true
                loadDataFromDatabase()
            }

            override fun onError(error: String) {
                isJadwalDataLoaded = false
                loadDataFromDatabase()
            }
        })
    }

    private fun loadShiftDefinitions() {
        showLoading(true)

        ShiftDefinitionApi().listAll(object : ShiftDefinitionApi.ShiftListCallback {
            override fun onSuccess(list: List<ShiftDefinitionApi.ShiftDefinition>) {
                shiftDefinitions.clear()
                for (shift in list) {
                    val key = shift.nama_shift.uppercase()
                    shiftDefinitions[key] = shift
                }

                isShiftDataLoaded = true
                loadAllPengguna()
            }

            override fun onError(error: String) {
                isShiftDataLoaded = false
                loadAllPengguna()
            }
        })
    }

    private fun loadAllPengguna() {
        showLoading(true)

        com.almil.dessertcakekinian.database.PenggunaApi().listAll(object : com.almil.dessertcakekinian.database.PenggunaApi.PenggunaListCallback {
            override fun onSuccess(list: List<com.almil.dessertcakekinian.database.PenggunaApi.Pengguna>) {
                allPenggunaList = list
                loadJadwalMingguan()
            }

            override fun onError(error: String) {
                loadJadwalMingguan()
            }
        })
    }

    private fun loadDataFromDatabase() {
        val userSession = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""

        showLoading(true)

        val timeoutRunnable = Runnable {
            showLoading(false)
        }
        timeoutHandler.postDelayed(timeoutRunnable, 15000)

        SupabaseHelper().getRiwayatByUsername(userName, object : SupabaseHelper.SimpleRiwayatCallback {
            override fun onSuccess(riwayatList: List<SupabaseHelper.SimpleRiwayatData>) {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                convertToRiwayatPresensi(riwayatList) { convertedList ->
                    allRiwayatList = convertedList
                    refreshRecyclerView()
                    showLoading(false)
                }
            }

            override fun onError(error: String) {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                showToast("Gagal load database: $error")
                showLoading(false)
            }
        })
    }

    private fun convertToRiwayatPresensi(
        riwayatList: List<SupabaseHelper.SimpleRiwayatData>,
        callback: (List<RiwayatPresensi>) -> Unit
    ) {
        val result = mutableListOf<RiwayatPresensi>()
        val existingPresensi = mutableMapOf<Int, MutableSet<String>>()

        for (riwayat in riwayatList) {
            if (!existingPresensi.containsKey(riwayat.idPengguna)) {
                existingPresensi[riwayat.idPengguna] = mutableSetOf()
            }
            existingPresensi[riwayat.idPengguna]!!.add(riwayat.tanggal)

            val status = determineStatus(riwayat)
            val utangJam = calculateUtangJam(riwayat, status)
            val usernameFromAbsen = getUsernameFromPenggunaId(riwayat.idPengguna)

            val riwayatPresensi = RiwayatPresensi(
                id = "db_${riwayat.tanggal}_${riwayat.shift}_${riwayat.idPengguna}",
                tanggal = riwayat.tanggal,
                jamMasuk = riwayat.jamMasuk,
                jamPulang = riwayat.jamPulang,
                status = status,
                lokasi = riwayat.lokasi,
                latitude = 0.0,
                longitude = 0.0,
                jenisAbsen = "FULL",
                utangJam = utangJam,
                username = usernameFromAbsen,
                shift = riwayat.shift.ifEmpty { "Pagi" }
            )

            result.add(riwayatPresensi)
        }

        if (isJadwalDataLoaded && !isGeneratingAlpha) {
            isGeneratingAlpha = true
            val allDates = result.map { it.tanggal }.toSet()
            val minDate = allDates.minOrNull()
            val maxDate = allDates.maxOrNull()

            if (minDate != null && maxDate != null) {
                generateAlphaEntriesForCurrentUserOnly(result, existingPresensi, minDate, maxDate) { finalList ->
                    isGeneratingAlpha = false
                    val sortedList = finalList.sortedByDescending {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.tanggal) ?: Date()
                    }
                    callback(sortedList)
                }
                return
            }
        }

        val sortedList = result.sortedByDescending {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.tanggal) ?: Date()
        }
        callback(sortedList)
    }

    private fun calculateUtangJam(riwayat: SupabaseHelper.SimpleRiwayatData, status: String): String {
        val shiftInfo = getShiftInfo(riwayat.shift)
        val totalMenitShift = shiftInfo.totalJam * 60

        if (status == STATUS_ALPHA) {
            return "${shiftInfo.totalJam}j 0m"
        }

        if (status == STATUS_IZIN) {
            if (riwayat.jamMasuk != "-" && riwayat.jamPulang != "-") {
                val menitKerja = calculateMenitKerja(riwayat.jamMasuk, riwayat.jamPulang)
                val utangMenit = maxOf(0, totalMenitShift - menitKerja)
                val jam = utangMenit / 60
                val menit = utangMenit % 60
                return "${jam}j ${menit}m"
            } else {
                return "${shiftInfo.totalJam}j 0m"
            }
        }

        if (status == STATUS_BELUM_PULANG) {
            if (riwayat.jamMasuk != "-") {
                val menitKerja = calculateMenitKerja(riwayat.jamMasuk, shiftInfo.end)
                val utangMenit = maxOf(0, totalMenitShift - menitKerja)
                val jam = utangMenit / 60
                val menit = utangMenit % 60
                return "${jam}j ${menit}m"
            } else {
                return "${shiftInfo.totalJam}j 0m"
            }
        }

        if (status == STATUS_HADIR || status == STATUS_TERLAMBAT) {
            if (riwayat.jamMasuk != "-" && riwayat.jamPulang != "-") {
                val menitKerja = calculateMenitKerja(riwayat.jamMasuk, riwayat.jamPulang)
                val utangMenit = maxOf(0, totalMenitShift - menitKerja)
                val jam = utangMenit / 60
                val menit = utangMenit % 60
                return "${jam}j ${menit}m"
            } else {
                return "0j 0m"
            }
        }

        return "0j 0m"
    }

    private fun calculateMenitKerja(jamMasuk: String, jamPulang: String): Int {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val tIn = sdf.parse(jamMasuk)
            val tOut = sdf.parse(jamPulang)

            if (tIn == null || tOut == null) return 0

            var diff = tOut.time - tIn.time

            if (diff < 0) {
                diff += 24 * 60 * 60 * 1000
            }

            return (diff / (60 * 1000)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun getShiftInfo(shift: String): ShiftInfo {
        val shiftKey = shift.uppercase()

        if (isShiftDataLoaded && shiftDefinitions.containsKey(shiftKey)) {
            val dbShift = shiftDefinitions[shiftKey]!!
            val totalJam = calculateTotalJamFromTimes(dbShift.jam_mulai, dbShift.jam_selesai)

            return ShiftInfo(
                name = dbShift.nama_shift,
                start = dbShift.jam_mulai,
                end = dbShift.jam_selesai,
                totalJam = totalJam
            )
        }

        return when (shiftKey) {
            "PAGI" -> ShiftInfo("Pagi", "08:00", "12:00", 4)
            "SIANG" -> ShiftInfo("Siang", "13:00", "17:00", 4)
            "MALAM" -> ShiftInfo("Malam", "18:00", "22:00", 4)
            "FULL" -> ShiftInfo("Full", "08:00", "17:00", 8)
            else -> ShiftInfo("Pagi", "08:00", "12:00", 4)
        }
    }

    private fun calculateTotalJamFromTimes(jamMulai: String, jamSelesai: String): Int {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeStart = sdf.parse(jamMulai)
            val timeEnd = sdf.parse(jamSelesai)

            if (timeStart == null || timeEnd == null) return 4

            var diff = timeEnd.time - timeStart.time

            if (diff < 0) {
                diff += 24 * 60 * 60 * 1000
            }

            return (diff / (60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            4
        }
    }

    private fun determineStatus(riwayat: SupabaseHelper.SimpleRiwayatData): String {
        if (riwayat.status.contains(STATUS_IZIN, ignoreCase = true)) {
            return STATUS_IZIN
        }

        if (riwayat.status.contains(STATUS_ALPHA, ignoreCase = true) ||
            riwayat.jamMasuk == "-" ||
            riwayat.jamMasuk.isEmpty()) {
            return STATUS_ALPHA
        }

        val isLate = isLate(riwayat.jamMasuk, riwayat.shift)

        if (isLate && riwayat.status.contains(STATUS_HADIR, ignoreCase = true)) {
            updateStatusToTerlambatInDatabase(riwayat.idPengguna, riwayat.tanggal, riwayat.shift)
            return STATUS_TERLAMBAT
        }

        if (riwayat.jamPulang == "-" || riwayat.jamPulang.isEmpty()) {
            return if (isLate) STATUS_TERLAMBAT else STATUS_BELUM_PULANG
        }

        return if (isLate) STATUS_TERLAMBAT else STATUS_HADIR
    }

    private fun isLate(jamMasuk: String, shift: String): Boolean {
        try {
            val shiftInfo = getShiftInfo(shift)
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

            val timeMasuk = sdf.parse(jamMasuk)
            val timeShiftStart = sdf.parse(shiftInfo.start)

            if (timeMasuk == null || timeShiftStart == null) return false

            val toleransiMenit = 5
            val toleransiMillis = toleransiMenit * 60 * 1000

            return timeMasuk.time > (timeShiftStart.time + toleransiMillis)
        } catch (e: Exception) {
            return false
        }
    }

    private fun updateStatusToTerlambatInDatabase(userId: Int, tanggal: String, shift: String) {
        SupabaseHelper().updateStatusToTerlambat(userId, tanggal, shift, object : SupabaseHelper.SimpleCallback {
            override fun onSuccess() {}
            override fun onError(error: String) {}
        })
    }

    private fun getUsernameFromPenggunaId(idPengguna: Int): String {
        val pengguna = allPenggunaList.find { it.iduser == idPengguna }
        return pengguna?.username ?: "User $idPengguna"
    }

    private fun getUserIdFromSession(): Int {
        val userSession = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return userSession.getInt("USER_ID", 0)
    }

    private fun refreshRecyclerView() {
        val adapter = rvRiwayat.adapter as? RiwayatAdapter
        if (adapter == null) {
            val newAdapter = RiwayatAdapter(allRiwayatList)
            linearLayoutManager = LinearLayoutManager(requireContext())
            rvRiwayat.layoutManager = linearLayoutManager
            rvRiwayat.adapter = newAdapter
        } else {
            adapter.updateData(allRiwayatList)
        }
    }

    private fun showLoading(show: Boolean) {
        swipeRefresh.isRefreshing = show
    }

    private fun getUsernameFromSession(): String {
        val userSession = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return userSession.getString("USER_NAME", "User") ?: "User"
    }

    private fun setupSpinner() {
        val statusList = arrayOf(
            STATUS_SEMUA,
            STATUS_HADIR,
            STATUS_IZIN,
            STATUS_TERLAMBAT,
            STATUS_ALPHA,
            STATUS_UTANG_JAM
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter
    }

    private fun setupRecyclerView() {
        val adapter = RiwayatAdapter(emptyList())
        linearLayoutManager = LinearLayoutManager(requireContext())
        rvRiwayat.layoutManager = linearLayoutManager
        rvRiwayat.adapter = adapter
    }

    private fun setupClickListeners() {
        backPressCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback!!)

        btnBack.setOnClickListener {
            handleBackNavigation()
        }

        btnKalender.setOnClickListener {
            showDatePickerDialog()
        }

        btnAjukanIzin.setOnClickListener {
            showAjukanIzinDialog()
        }

        spinnerStatus.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterRiwayatByStatus(parent?.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun handleBackNavigation() {
        try {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
                println("✅ PresensiFragment - Kembali ke fragment sebelumnya via popBackStack")
            } else {
                requireActivity().finish()
                println("✅ PresensiFragment - Tidak ada back stack, finish activity")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ PresensiFragment - Error saat navigasi back: ${e.message}")
            try {
                requireActivity().finish()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun generateAlphaEntriesForCurrentUserOnly(
        currentList: MutableList<RiwayatPresensi>,
        existingPresensi: Map<Int, Set<String>>,
        minDate: String,
        maxDate: String,
        callback: (List<RiwayatPresensi>) -> Unit
    ) {
        val result = currentList.toMutableList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = dateFormat.parse(minDate)
        val endDate = dateFormat.parse(maxDate)

        if (startDate == null || endDate == null) {
            callback(result)
            return
        }

        val jadwalUser = jadwalMingguanList.find { it.id_pengguna == currentUserId }

        if (jadwalUser != null) {
            val calendar = Calendar.getInstance()
            calendar.time = startDate

            while (calendar.time <= endDate) {
                val currentDate = dateFormat.format(calendar.time)

                try {
                    val targetDate = dateFormat.parse(currentDate)
                    if (targetDate == null) continue

                    val dayCalendar = Calendar.getInstance()
                    dayCalendar.time = targetDate
                    val dayOfWeek = dayCalendar.get(Calendar.DAY_OF_WEEK)

                    val shiftId = when (dayOfWeek) {
                        Calendar.MONDAY -> jadwalUser.id_shift_senin
                        Calendar.TUESDAY -> jadwalUser.id_shift_selasa
                        Calendar.WEDNESDAY -> jadwalUser.id_shift_rabu
                        Calendar.THURSDAY -> jadwalUser.id_shift_kamis
                        Calendar.FRIDAY -> jadwalUser.id_shift_jumat
                        Calendar.SATURDAY -> jadwalUser.id_shift_sabtu
                        Calendar.SUNDAY -> jadwalUser.id_shift_minggu
                        else -> null
                    }

                    if (shiftId != null && shiftId != 0) {
                        val hasPresensi = existingPresensi[currentUserId]?.contains(currentDate) == true
                        if (!hasPresensi) {
                            val username = getUsernameFromPenggunaId(currentUserId)
                            val shiftName = getShiftNameForDate(currentDate, jadwalUser)
                            val utangJam = calculateUtangJamForAlpha(shiftName)

                            val alphaEntry = RiwayatPresensi(
                                id = "alpha_${currentDate}_${currentUserId}",
                                tanggal = currentDate,
                                jamMasuk = "-",
                                jamPulang = "-",
                                status = STATUS_ALPHA,
                                lokasi = "Tidak Absen",
                                latitude = 0.0,
                                longitude = 0.0,
                                jenisAbsen = "ALPHA",
                                utangJam = utangJam,
                                username = username,
                                shift = shiftName
                            )
                            result.add(alphaEntry)
                        }
                    }
                } catch (e: Exception) {
                    continue
                }

                calendar.add(Calendar.DATE, 1)
            }
        }

        isGeneratingAlpha = false
        callback(result)
    }

    private fun getShiftNameForDate(tanggal: String, jadwal: JadwalMingguanApi.JadwalMingguan): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetDate = dateFormat.parse(tanggal)
            if (targetDate == null) return "Pagi"

            val calendar = Calendar.getInstance()
            calendar.time = targetDate

            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> jadwal.shift_senin.ifEmpty { "Pagi" }
                Calendar.TUESDAY -> jadwal.shift_selasa.ifEmpty { "Pagi" }
                Calendar.WEDNESDAY -> jadwal.shift_rabu.ifEmpty { "Pagi" }
                Calendar.THURSDAY -> jadwal.shift_kamis.ifEmpty { "Pagi" }
                Calendar.FRIDAY -> jadwal.shift_jumat.ifEmpty { "Pagi" }
                Calendar.SATURDAY -> jadwal.shift_sabtu.ifEmpty { "Pagi" }
                Calendar.SUNDAY -> jadwal.shift_minggu.ifEmpty { "Pagi" }
                else -> "Pagi"
            }
        } catch (e: Exception) {
            "Pagi"
        }
    }

    private fun calculateUtangJamForAlpha(shiftName: String): String {
        val shiftInfo = getShiftInfo(shiftName)
        return "${shiftInfo.totalJam}j 0m"
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                scrollToDate(formattedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun scrollToDate(selectedDate: String) {
        val position = allRiwayatList.indexOfFirst { it.tanggal == selectedDate }

        if (position != -1) {
            linearLayoutManager.scrollToPositionWithOffset(position, 0)
            showToast("Menampilkan riwayat tanggal terpilih")
            highlightSelectedItem(position)
        } else {
            showToast("Tidak ada riwayat absen pada tanggal terpilih")
        }
    }

    private fun highlightSelectedItem(position: Int) {
        rvRiwayat.postDelayed({
            val adapter = rvRiwayat.adapter as? RiwayatAdapter
            adapter?.setSelectedPosition(position)
            linearLayoutManager.scrollToPositionWithOffset(position, 0)
        }, 300)
    }

    private fun showAjukanIzinDialog() {
        val dialog = dialog_ajukan_izin()
        dialog.show(parentFragmentManager, "AjukanIzinDialog")
    }

    private fun filterRiwayatByStatus(status: String) {
        val adapter = rvRiwayat.adapter as? RiwayatAdapter
        adapter?.updateData(allRiwayatList)
        adapter?.filterByStatus(status)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPressCallback?.remove()
        backPressCallback = null
    }

    data class RiwayatPresensi(
        val id: String,
        val tanggal: String,
        val jamMasuk: String,
        val jamPulang: String,
        val status: String,
        val lokasi: String,
        val latitude: Double,
        val longitude: Double,
        val jenisAbsen: String,
        val utangJam: String,
        val username: String,
        val shift: String
    )

    data class ShiftInfo(
        val name: String,
        val start: String,
        val end: String,
        val totalJam: Int
    )
}