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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PresensiFragment : Fragment() {

    private lateinit var rvRiwayat: RecyclerView
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnKalender: ImageButton
    private lateinit var btnAjukanIzin: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvToolbarTitle: TextView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var allRiwayatList = listOf<RiwayatPresensi>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_presensi, container, false)

        initViews(view)
        setupSpinner()
        setupRecyclerView()
        setupClickListeners()

        return view
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
        val statusList = arrayOf("Semua Status", "Hadir", "Izin")
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
                // Format tanggal yang dipilih
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                // Format untuk display
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
        // Cari posisi item dengan tanggal yang dipilih
        val position = allRiwayatList.indexOfFirst { it.tanggal == selectedDate }

        if (position != -1) {
            // Scroll ke posisi tanggal yang dipilih
            linearLayoutManager.scrollToPositionWithOffset(position, 0)
            showToast("Menampilkan riwayat tanggal terpilih")

            // Optional: Highlight item yang dipilih
            highlightSelectedItem(position)
        } else {
            // Jika tidak ada riwayat di tanggal tersebut, tampilkan semua dan beri pesan
            val adapter = rvRiwayat.adapter as? RiwayatAdapter
            adapter?.updateData(allRiwayatList)
            showToast("Tidak ada riwayat absen pada tanggal terpilih")
        }
    }

    private fun highlightSelectedItem(position: Int) {
        // Beri delay sedikit agar smooth scroll selesai dulu
        rvRiwayat.postDelayed({
            val adapter = rvRiwayat.adapter as? RiwayatAdapter
            adapter?.setSelectedPosition(position)

            // Optional: Scroll lagi untuk memastikan item terlihat
            linearLayoutManager.scrollToPositionWithOffset(position, 0)
        }, 300)
    }

    private fun showAjukanIzinDialog() {
        val dialog = dialog_ajukan_izin()
        dialog.show(parentFragmentManager, "AjukanIzinDialog")
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
                        riwayatMap[tanggal] = RiwayatPresensi(
                            id = key,
                            tanggal = tanggal,
                            jamMasuk = if (jenis == "MASUK") jam else "-",
                            jamPulang = if (jenis == "PULANG") jam else "-",
                            status = "Hadir",
                            lokasi = lokasi,
                            latitude = latitude,
                            longitude = longitude,
                            jenisAbsen = jenis
                        )
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
                jamPulang = "17:00",
                status = "Hadir",
                lokasi = "Jurusan TI Polije",
                latitude = -8.157551,
                longitude = 113.722800,
                jenisAbsen = "MASUK"
            ),
            RiwayatPresensi(
                id = "2",
                tanggal = "2024-11-13",
                jamMasuk = "08:15",
                jamPulang = "16:45",
                status = "Hadir",
                lokasi = "Jurusan TI Polije",
                latitude = -8.157551,
                longitude = 113.722800,
                jenisAbsen = "MASUK"
            ),
            RiwayatPresensi(
                id = "3",
                tanggal = "2024-11-12",
                jamMasuk = "07:55",
                jamPulang = "17:10",
                status = "Hadir",
                lokasi = "Jurusan TI Polije",
                latitude = -8.157551,
                longitude = 113.722800,
                jenisAbsen = "MASUK"
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
        val status: String,
        var lokasi: String,
        val latitude: Double,
        val longitude: Double,
        val jenisAbsen: String
    )
}

private fun RiwayatAdapter?.setSelectedPosition(position: Int) {}
