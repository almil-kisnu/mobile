package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.RiwayatAdapter
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
    private var allRiwayatList = listOf<RiwayatPresensi>()
    private var currentUsername: String = "User"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_presensi, container, false)

        currentUsername = getUsernameFromSession()

        initViews(view)
        setupSpinner()
        setupRecyclerView()
        setupClickListeners()

        return view
    }

    private fun getUsernameFromSession(): String {
        val userSession = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return userSession.getString("USER_NAME", "User") ?: "User"
    }

    private fun initViews(view: View) {
        rvRiwayat = view.findViewById(R.id.rvRiwayat)
        spinnerStatus = view.findViewById(R.id.spinnerStatus)
        btnKalender = view.findViewById(R.id.btnKalender)
        btnAjukanIzin = view.findViewById(R.id.btnAjukanIzin)
        btnBack = view.findViewById(R.id.btnBack)
        tvToolbarTitle = view.findViewById(R.id.tvToolbarTitle)
    }

    private fun setupSpinner() {
        val statusList = arrayOf("Semua Status", "Hadir", "Izin", "Telat", "Utang Jam")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter
    }

    private fun setupRecyclerView() {
        allRiwayatList = getRiwayatPresensi()
        val adapter = RiwayatAdapter(allRiwayatList)
        linearLayoutManager = LinearLayoutManager(requireContext())
        rvRiwayat.layoutManager = linearLayoutManager
        rvRiwayat.adapter = adapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            requireActivity().onBackPressed()
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

                val displayFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                val displayDate = displayFormat.format(selectedDate.time)

                showToast("Menuju ke tanggal: $displayDate")
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
            val adapter = rvRiwayat.adapter as? RiwayatAdapter
            adapter?.updateData(allRiwayatList)
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

    // FUNGSI BARU: Ambil shift user untuk tanggal tertentu
    private fun getShiftForDate(tanggal: String): String {
        val userSession = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""

        if (userName.isEmpty()) return "Pagi"

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(tanggal) ?: return "Pagi"

            val calendar = Calendar.getInstance()
            calendar.time = date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            return when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> "Siang"
                else -> "Pagi"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Pagi"
        }
    }

    // FUNGSI BARU: Hitung keterlambatan berdasarkan shift
    private fun calculateKeterlambatanByShift(jamMasuk: String, shift: String): Int {
        if (jamMasuk == "-") return 0

        try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val jamMasukDate = format.parse(jamMasuk)

            val jamMulaiShift = when (shift) {
                "Pagi" -> format.parse("07:00")
                "Siang" -> format.parse("13:00")
                "Malam" -> format.parse("18:00")
                else -> format.parse("07:00")
            }

            if (jamMasukDate != null && jamMulaiShift != null) {
                val selisihMenit = (jamMasukDate.time - jamMulaiShift.time) / (60 * 1000)
                return if (selisihMenit > 0) selisihMenit.toInt() else 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    // FUNGSI BARU: Hitung jam kerja aktual
    private fun calculateJamKerjaAktual(riwayat: RiwayatPresensi): String {
        if (riwayat.jamMasuk == "-" || riwayat.jamPulang == "-") return "0j 0m"

        try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val jamMasuk = format.parse(riwayat.jamMasuk)
            val jamPulang = format.parse(riwayat.jamPulang)

            if (jamMasuk != null && jamPulang != null) {
                val selisihMs = jamPulang.time - jamMasuk.time
                val totalMenit = selisihMs / (60 * 1000)

                val jam = totalMenit / 60
                val menit = totalMenit % 60

                return if (jam > 0 && menit > 0) "${jam}j ${menit}m"
                else if (jam > 0) "${jam}j"
                else "${menit}m"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "0j 0m"
    }

    // FUNGSI BARU: Tentukan status berdasarkan logika baru
    private fun determineStatusNewLogic(riwayat: RiwayatPresensi): String {
        if (riwayat.status != "Hadir") return riwayat.status

        val telatMenit = calculateKeterlambatanByShift(riwayat.jamMasuk, riwayat.shift)
        val hasIzin = hasIzinForDate(riwayat.tanggal)

        // LOGIKA BARU: Cek apakah ada izin setelah absen
        val hasIzinSetelahAbsen = hasIzin && riwayat.jamMasuk != "-"

        return when {
            hasIzinSetelahAbsen -> "Izin"  // Izin setelah sudah absen
            telatMenit > 0 -> "Telat"      // Hanya telat, tidak ada izin
            else -> "Hadir"                // Tepat waktu
        }
    }

    // FUNGSI BARU: Hitung jam terhutang berdasarkan logika baru
    private fun calculateUtangJamNewLogic(riwayat: RiwayatPresensi): String {
        // Hanya untuk status "Izin" - TELAT tidak punya jam terhutang
        if (riwayat.status != "Izin") return ""

        // Ambil data jam terhutang dari izin_data
        val jamTerhutang = getJamTerhutangFromIzinData(riwayat.tanggal)
        if (jamTerhutang.isNotEmpty()) {
            return jamTerhutang
        }

        // Fallback jika tidak ada data izin
        val shift = riwayat.shift
        return when (shift) {
            "Pagi" -> "5j"
            "Siang" -> "4j"
            "Malam" -> "4j"
            else -> "5j"
        }
    }

    // FUNGSI BARU: Ambil jam terhutang dari izin_data
    private fun getJamTerhutangFromIzinData(tanggal: String): String {
        val prefs = requireContext().getSharedPreferences("izin_data", Context.MODE_PRIVATE)
        val allEntries = prefs.all

        for ((_, value) in allEntries) {
            if (value is String) {
                val data = value.split("|")
                if (data.size >= 8) {
                    val izinTanggal = data[1]
                    try {
                        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = inputFormat.parse(izinTanggal)
                        val formattedIzinTanggal = outputFormat.format(date ?: Date())

                        if (formattedIzinTanggal == tanggal) {
                            return data[6] // jamTerhutang
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return ""
    }

    private fun hasIzinForDate(tanggal: String): Boolean {
        val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        return prefs.getBoolean("izin_$tanggal", false)
    }

    private fun getRiwayatPresensi(): List<RiwayatPresensi> {
        val riwayatList = mutableListOf<RiwayatPresensi>()
        val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)

        val allEntries = prefs.all
        val riwayatMap = mutableMapOf<String, RiwayatPresensi>()

        for ((key, value) in allEntries) {
            if (key.startsWith("riwayat_") && value is String) {
                val data = value.split("|")
                if (data.size >= 6) {
                    val jenis = data[0]
                    val jam = data[1]
                    val tanggal = data[2]
                    val lokasi = data[3]
                    val latitude = data[4].toDoubleOrNull() ?: 0.0
                    val longitude = data[5].toDoubleOrNull() ?: 0.0

                    if (riwayatMap.containsKey(tanggal)) {
                        val existingEntry = riwayatMap[tanggal]!!
                        if (jenis == "MASUK") {
                            existingEntry.jamMasuk = jam
                        } else if (jenis == "PULANG") {
                            existingEntry.jamPulang = jam
                        }
                        existingEntry.lokasi = lokasi
                    } else {
                        // Tentukan shift untuk tanggal ini
                        val shift = getShiftForDate(tanggal)

                        val newEntry = RiwayatPresensi(
                            id = key,
                            tanggal = tanggal,
                            jamMasuk = if (jenis == "MASUK") jam else "-",
                            jamPulang = if (jenis == "PULANG") jam else "-",
                            status = "Hadir",
                            lokasi = lokasi,
                            latitude = latitude,
                            longitude = longitude,
                            jenisAbsen = jenis,
                            utangJam = "",
                            username = currentUsername,
                            shift = shift,
                            jamKerjaAktual = ""
                        )

                        // PAKAI LOGIKA BARU:
                        newEntry.status = determineStatusNewLogic(newEntry)
                        newEntry.jamKerjaAktual = calculateJamKerjaAktual(newEntry)
                        newEntry.utangJam = calculateUtangJamNewLogic(newEntry)

                        riwayatMap[tanggal] = newEntry
                    }
                }
            }
        }

        riwayatList.addAll(riwayatMap.values)

        if (riwayatList.isEmpty()) {
            riwayatList.addAll(getDummyRiwayat())
        }

        return riwayatList.sortedByDescending {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.tanggal) ?: Date()
        }
    }

    private fun getDummyRiwayat(): List<RiwayatPresensi> {
        return listOf(
            RiwayatPresensi(
                id = "1",
                tanggal = "2024-11-14",
                jamMasuk = "08:00",
                jamPulang = "12:00",
                status = "Telat",
                lokasi = "Jurusan TI Polije",
                latitude = -8.157551,
                longitude = 113.722800,
                jenisAbsen = "MASUK",
                utangJam = "", // TELAT: jam terhutang KOSONG
                username = currentUsername,
                shift = "Pagi",
                jamKerjaAktual = "4j" // Kerja 4 jam (08:00-12:00)
            ),
            RiwayatPresensi(
                id = "2",
                tanggal = "2024-11-13",
                jamMasuk = "07:00",
                jamPulang = "17:00",
                status = "Izin",
                lokasi = "Jurusan TI Polije",
                latitude = -8.157551,
                longitude = 113.722800,
                jenisAbsen = "MASUK",
                utangJam = "3j", // IZIN: hutang 3 jam (09:00-12:00)
                username = currentUsername,
                shift = "Pagi",
                jamKerjaAktual = "2j" // Kerja 2 jam (07:00-09:00)
            ),
            RiwayatPresensi(
                id = "3",
                tanggal = "2024-11-12",
                jamMasuk = "07:00",
                jamPulang = "12:00",
                status = "Hadir",
                lokasi = "Jurusan TI Polije",
                latitude = -8.157551,
                longitude = 113.722800,
                jenisAbsen = "MASUK",
                utangJam = "", // HADIR: tidak ada hutang
                username = currentUsername,
                shift = "Pagi",
                jamKerjaAktual = "5j" // Kerja 5 jam penuh
            )
        )
    }

    private fun filterRiwayatByStatus(status: String) {
        val adapter = rvRiwayat.adapter as? RiwayatAdapter
        adapter?.updateData(allRiwayatList)
        adapter?.filterByStatus(status)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    data class RiwayatPresensi(
        val id: String,
        val tanggal: String,
        var jamMasuk: String,
        var jamPulang: String,
        var status: String,
        var lokasi: String,
        val latitude: Double,
        val longitude: Double,
        val jenisAbsen: String,
        var utangJam: String = "",
        val username: String = "User",
        var shift: String = "Pagi",
        var jamKerjaAktual: String = ""
    )
}

private fun RiwayatAdapter?.setSelectedPosition(position: Int) {}