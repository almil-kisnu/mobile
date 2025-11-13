package com.almil.dessertcakekinian.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.AbsenMasukActivity
import com.almil.dessertcakekinian.activity.AbsenPulangActivity
import com.almil.dessertcakekinian.adapter.RiwayatAdapter
import com.almil.dessertcakekinian.database.PenggunaApi
import com.almil.dessertcakekinian.model.RiwayatAbsen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomePresensiFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvJamMasuk: TextView
    private lateinit var tvJamPulang: TextView
    private lateinit var tvJamKerja: TextView
    private lateinit var btnAbsenMasuk: Button
    private lateinit var btnAbsenPulang: Button
    private lateinit var recyclerViewHome: RecyclerView
    private lateinit var adapterHome: RiwayatAdapter
    private lateinit var etSearchHome: EditText
    private lateinit var spinnerStatusHome: Spinner
    private var absensiList: MutableList<RiwayatAbsen> = mutableListOf()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_presensi, container, false)

        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvDate = view.findViewById(R.id.tv_date)
        tvJamMasuk = view.findViewById(R.id.tv_jam_masuk)
        tvJamPulang = view.findViewById(R.id.tv_jam_pulang)
        tvJamKerja = view.findViewById(R.id.tv_jam_kerja)
        btnAbsenMasuk = view.findViewById(R.id.btn_absen_masuk)
        btnAbsenPulang = view.findViewById(R.id.btn_absen_pulang)
        recyclerViewHome = view.findViewById(R.id.recycler_absensi_home)
        etSearchHome = view.findViewById(R.id.et_search_home)
        spinnerStatusHome = view.findViewById(R.id.spinner_status_home)

        val prefsNama = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val namaSaved = prefsNama.getString("nama_pengguna", "")
        val idSaved = prefsNama.getInt("id_pengguna", 0)

        if (etSearchHome.text.isEmpty()) {
            if (!namaSaved.isNullOrEmpty()) {
                etSearchHome.setText(namaSaved)
            } else if (idSaved > 0) {
                etSearchHome.setText(idSaved.toString())
            }
        }

        absensiList = mutableListOf()
        adapterHome = RiwayatAdapter(absensiList)
        recyclerViewHome.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewHome.adapter = adapterHome

        setupFilterHome()
        setGreeting()
        setDate()
        loadDataReal()
        loadAbsensiHome()

        btnAbsenMasuk.setOnClickListener {
            if (sudahAbsenMasukHariIni()) {
                android.widget.Toast.makeText(requireActivity(), "Anda sudah absen masuk hari ini", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(requireActivity(), AbsenMasukActivity::class.java)
            startActivity(intent)
        }

        btnAbsenPulang.setOnClickListener {
            val intent = Intent(requireActivity(), AbsenPulangActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun setupFilterHome() {
        val statusArray = arrayOf("Hadir", "Izin", "Terlambat")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusArray)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatusHome.adapter = statusAdapter

        val savedStatus = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            .getString("selected_status", "Hadir")
        val idx = when (savedStatus) {
            "Izin" -> 1
            "Terlambat" -> 2
            else -> 0
        }
        spinnerStatusHome.setSelection(idx)

        etSearchHome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFiltersHome()
            }
            override fun afterTextChanged(s: Editable?) {
                val input = s?.toString()?.trim() ?: ""
                val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                val ed = prefs.edit()

                if (input.isEmpty()) {
                    ed.putString("nama_pengguna", "")
                    ed.putInt("id_pengguna", 0)
                    ed.apply()
                    setGreeting()
                    return
                }

                val isNumeric = input.matches("\\d+".toRegex())
                if (isNumeric) {
                    try {
                        val idVal = input.toInt()
                        ed.putInt("id_pengguna", idVal).apply()
                        PenggunaApi().getById(idVal, object : PenggunaApi.PenggunaCallback {
                            override fun onSuccess(pengguna: PenggunaApi.Pengguna) {
                                try {
                                    requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("nama_pengguna", pengguna.username)
                                        .putInt("id_pengguna", pengguna.iduser)
                                        .apply()
                                    if (etSearchHome.text.toString() == input) {
                                        etSearchHome.setText(pengguna.iduser.toString())
                                    }
                                    setGreeting()
                                } catch (e: Exception) {}
                            }
                            override fun onError(error: String) {
                                try {
                                    android.widget.Toast.makeText(requireContext(), "ID tidak ditemukan: $error", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {}
                            }
                        })
                    } catch (e: Exception) {}
                } else {
                    ed.putString("nama_pengguna", input).apply()
                    PenggunaApi().getByUsername(input, object : PenggunaApi.PenggunaCallback {
                        override fun onSuccess(pengguna: PenggunaApi.Pengguna) {
                            try {
                                requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("nama_pengguna", pengguna.username)
                                    .putInt("id_pengguna", pengguna.iduser)
                                    .apply()
                                setGreeting()
                            } catch (e: Exception) {}
                        }
                        override fun onError(error: String) {
                            // biarkan tetap tersimpan hanya nama lokal, tanpa id
                        }
                    })
                }
            }
        })

        spinnerStatusHome.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sel = parent?.getItemAtPosition(position).toString()
                requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                    .edit().putString("selected_status", sel).apply()
                applyFiltersHome()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFiltersHome() {
        val searchText = etSearchHome.text.toString()
        val selectedStatus = spinnerStatusHome.selectedItem?.toString() ?: "Hadir"
        adapterHome.filter(searchText, "All Status", "All Role")
    }

    @SuppressLint("SimpleDateFormat")
    private fun loadAbsensiHome() {
        absensiList.clear()

        val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val nama = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            .getString("nama_pengguna", "Kasir")
        val todayDisplay = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())

        val allEntries = prefs.all
        val selectedStatusLocal = prefs.getString("selected_status", "Hadir")

        for ((key, value) in allEntries) {
            if (key.startsWith("riwayat_")) {
                val data = value as String
                val parts = data.split("|")

                if (parts.size >= 3 && parts[2] == today) {
                    val jenis = parts[0] // MASUK atau PULANG
                    val jam = parts[1]
                    val status = selectedStatusLocal ?: "Hadir"

                    if (jenis == "MASUK") {
                        absensiList.add(RiwayatAbsen(
                            key, nama ?: "Kasir", "Karyawan", todayDisplay,
                            jam, "--:--", status, null, null, today
                        ))
                    } else if (jenis == "PULANG") {
                        var updated = false
                        for (i in absensiList.indices) {
                            val absen = absensiList[i]
                            if (absen.jamPulang == "--:--") {
                                val updatedAbsen = RiwayatAbsen(
                                    absen.id, absen.namaPengguna, absen.role,
                                    absen.tanggal, absen.jamMasuk, jam,
                                    absen.status, null, null, today
                                )
                                absensiList[i] = updatedAbsen
                                updated = true
                                break
                            }
                        }
                        if (!updated) {
                            absensiList.add(RiwayatAbsen(
                                key, nama ?: "Kasir", "Karyawan", todayDisplay,
                                "--:--", jam, status, null, null, today
                            ))
                        }
                    }
                }
            }
        }

        if (absensiList.isEmpty()) {
            absensiList.add(RiwayatAbsen("1", nama ?: "Kasir", "Karyawan", todayDisplay, "08:00", "--:--", "Hadir", null, null, today))
        }

        adapterHome.updateData(absensiList)
        applyFiltersHome()
    }

    private fun sudahAbsenMasukHariIni(): Boolean {
        val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val status = prefs.getString("status_absen", "BELUM_ABSEN")
        return status == "SUDAH_MASUK" || status == "SUDAH_PULANG"
    }

    private fun sudahAbsenPulangHariIni(): Boolean {
        val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val status = prefs.getString("status_absen", "BELUM_ABSEN")
        return status == "SUDAH_PULANG"
    }

    @SuppressLint("SimpleDateFormat")
    private fun loadDataReal() {
        val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        var jamMasuk = prefs.getString("jam_masuk_hari_ini", "--:--")
        var jamPulang = prefs.getString("jam_pulang_hari_ini", "--:--")
        var status = prefs.getString("status_absen", "BELUM_ABSEN")

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString("last_absen_date", "")
        if (today != lastDate) {
            val editor = prefs.edit()
            editor.putString("status_absen", "BELUM_ABSEN")
            editor.putString("jam_masuk_hari_ini", "--:--")
            editor.putString("jam_pulang_hari_ini", "--:--")
            editor.apply()

            status = "BELUM_ABSEN"
            jamMasuk = "--:--"
            jamPulang = "--:--"
        }

        tvJamMasuk.text = jamMasuk
        tvJamPulang.text = jamPulang

        if (jamMasuk != "--:--" && jamPulang != "--:--") {
            hitungDanTampilkanJamKerja(jamMasuk!!, jamPulang!!)
        } else if (jamMasuk != "--:--") {
            val jamSekarang = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            hitungDanTampilkanJamKerja(jamMasuk!!, jamSekarang)
        } else {
            tvJamKerja.text = "0 jam 0 menit"
        }

        kontrolButton(status!!)
    }

    private fun kontrolButton(status: String) {
        when (status) {
            "BELUM_ABSEN" -> {
                btnAbsenMasuk.alpha = 1.0f
                btnAbsenPulang.alpha = 0.5f
                btnAbsenMasuk.text = "Absen Masuk"
                btnAbsenPulang.text = "Absen Pulang"
                btnAbsenMasuk.isClickable = true
                btnAbsenPulang.isClickable = false
            }
            "SUDAH_MASUK" -> {
                btnAbsenMasuk.alpha = 0.5f
                btnAbsenPulang.alpha = 1.0f
                btnAbsenMasuk.text = "✓ Sudah Absen Masuk"
                btnAbsenPulang.text = "Absen Pulang"
                btnAbsenMasuk.isClickable = false
                btnAbsenPulang.isClickable = true
            }
            "SUDAH_PULANG" -> {
                btnAbsenMasuk.alpha = 0.5f
                btnAbsenPulang.alpha = 0.5f
                btnAbsenMasuk.text = "✓ Sudah Absen Masuk"
                btnAbsenPulang.text = "✓ Sudah Absen Pulang"
                btnAbsenMasuk.isClickable = false
                btnAbsenPulang.isClickable = false
            }
        }
    }

    private fun hitungDanTampilkanJamKerja(jamMasuk: String, jamPulang: String) {
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

    override fun onResume() {
        super.onResume()
        loadDataReal()
        loadAbsensiHome()
        setDate()
    }

    private fun setGreeting() {
        tvGreeting.text = "Selamat Datang"
    }

    @SuppressLint("SimpleDateFormat")
    private fun setDate() {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
        val currentDate = sdf.format(Date())
        tvDate.text = currentDate
    }
}