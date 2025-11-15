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
import java.util.Date
import java.util.Locale

class PresensiFragment : Fragment() {

    private lateinit var rvRiwayat: RecyclerView
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnKalender: ImageButton
    private lateinit var btnAjukanIzin: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvToolbarTitle: TextView

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
        val riwayatList = getRiwayatPresensi()
        val adapter = RiwayatAdapter(riwayatList)
        rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        rvRiwayat.adapter = adapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        btnKalender.setOnClickListener {
            showToast("Fitur kalender akan segera hadir")
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
            )
        )
    }

    private fun filterRiwayatByStatus(status: String) {
        val allRiwayat = getRiwayatPresensi()
        val adapter = rvRiwayat.adapter as? RiwayatAdapter
        adapter?.updateData(allRiwayat)
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