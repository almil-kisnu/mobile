package com.almil.dessertcakekinian.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.RiwayatAdapter
import com.almil.dessertcakekinian.database.SupabaseHelper
import com.almil.dessertcakekinian.model.RiwayatAbsen
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class RiwayatAbsenFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: RiwayatAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_riwayat_absen, container, false)
        tvEmpty = view.findViewById(R.id.tv_empty)
        recyclerView = view.findViewById(R.id.recycler_riwayat)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = RiwayatAdapter(ArrayList())
        recyclerView.adapter = adapter

        loadRiwayatFromSupabase()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadRiwayatFromSupabase()
    }

    private fun loadRiwayatFromSupabase() {
        try {
            tvEmpty.text = "Memuat data..."
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val savedNama = prefs.getString("nama_pengguna", "Kasir") ?: "Kasir"
            val savedId = prefs.getInt("id_pengguna", 0)
            val idPengguna = if (savedId > 0) savedId else Math.abs(savedNama.hashCode())

            SupabaseHelper().getRiwayatByIdPengguna(idPengguna, object : SupabaseHelper.RiwayatCallback {
                override fun onSuccess(riwayatList: List<RiwayatAbsen>) {
                    val combinedList = ArrayList<RiwayatAbsen>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    // Process all entries (both remote and local)
                    val allEntries = ArrayList<RiwayatAbsen>()
                    allEntries.addAll(riwayatList ?: emptyList())
                    allEntries.addAll(readLocalRiwayat())

                    // Process each entry and format consistently
                    val processedEntries = ArrayList<RiwayatAbsen>()
                    allEntries.forEach { entry ->
                        try {
                            // Parse the date to a consistent format for display
                            val date = try {
                                dateFormat.parse(entry.tanggal)
                            } catch (e: Exception) {
                                try {
                                    displayFormat.parse(entry.tanggal)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            val formattedDate = if (date != null) {
                                displayFormat.format(date)
                            } else {
                                entry.tanggal // fallback to original date
                            }

                            processedEntries.add(entry.copy(tanggal = formattedDate))
                        } catch (e: Exception) {
                            Log.e("RiwayatAbsen", "Error processing entry: ${e.message}")
                            processedEntries.add(entry) // add original entry if processing fails
                        }
                    }

                    // Sort by date (newest first) and time (latest first for same date)
                    combinedList.addAll(processedEntries.sortedWith(compareByDescending<RiwayatAbsen> { entry ->
                        try {
                            displayFormat.parse(entry.tanggal) ?: Date(0)
                        } catch (e: Exception) {
                            Date(0)
                        }
                    }.thenByDescending { entry ->
                        try {
                            val time = if (entry.jamMasuk.isNotEmpty()) entry.jamMasuk else "00:00"
                            timeFormat.parse(time) ?: Date(0)
                        } catch (e: Exception) {
                            Date(0)
                        }
                    }))

                    if (combinedList.isEmpty()) {
                        tvEmpty.text = "Tidak ada data"
                        tvEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        adapter.updateData(combinedList)
                        updateUI()
                    }
                }

                override fun onError(error: String) {
                    Log.e("Riwayat", "Gagal ambil data Supabase: $error")
                    // If there's an error with the server, just show local data
                    val local = readLocalRiwayat()
                    if (local.isNotEmpty()) {
                        // Sort local data by date (newest first) and time
                        val sortedLocal = local.sortedWith(compareByDescending<RiwayatAbsen> {
                            try {
                                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                format.parse(it.tanggal) ?: Date(0)
                            } catch (e: Exception) {
                                try {
                                    val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                                    displayFormat.parse(it.tanggal) ?: Date(0)
                                } catch (e: Exception) {
                                    Date(0)
                                }
                            }
                        }.thenByDescending {
                            try {
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val time = it.jamMasuk.ifEmpty { "00:00" }
                                timeFormat.parse(time) ?: Date(0)
                            } catch (e: Exception) {
                                Date(0)
                            }
                        })

                        adapter.updateData(sortedLocal)
                        updateUI()
                    } else {
                        tvEmpty.text = "Gagal memuat data"
                        tvEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }

                    try {
                        Toast.makeText(requireContext(), "Menggunakan data lokal: $error", Toast.LENGTH_SHORT).show()
                    } catch (ignored: Exception) {}
                }
            })
        } catch (e: Exception) {
            Log.e("Riwayat", "loadRiwayatFromSupabase exception", e)
            tvEmpty.text = "Gagal memuat data"
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            // Try to show local data even if there's an error
            val local = readLocalRiwayat()
            if (local.isNotEmpty()) {
                // Sort local data by date (newest first) and time
                val sortedLocal = local.sortedWith(compareByDescending<RiwayatAbsen> {
                    try {
                        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        format.parse(it.tanggal) ?: Date(0)
                    } catch (e: Exception) {
                        try {
                            val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                            displayFormat.parse(it.tanggal) ?: Date(0)
                        } catch (e: Exception) {
                            Date(0)
                        }
                    }
                }.thenByDescending {
                    try {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val time = it.jamMasuk.ifEmpty { "00:00" }
                        timeFormat.parse(time) ?: Date(0)
                    } catch (e: Exception) {
                        Date(0)
                    }
                })

                adapter.updateData(sortedLocal)
                updateUI()
            }
        }
    }

    private fun updateUI() {
        tvEmpty.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun formatTanggalDisplay(tanggal: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            outputFormat.format(inputFormat.parse(tanggal)!!)
        } catch (e: Exception) {
            Log.e("Riwayat", "Gagal format tanggal: $tanggal", e)
            tanggal
        }
    }

    private fun readLocalRiwayat(): List<RiwayatAbsen> {
        val list = ArrayList<RiwayatAbsen>()
        try {
            val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val absenMap = HashMap<String, RiwayatAbsen>()

            val namaDefault = prefs.getString("nama_pengguna", "Kasir")

            val allEntries = prefs.all
            for ((key, value) in allEntries) {
                if (!key.startsWith("riwayat_") || key.startsWith("riwayat_supabase_")) continue
                val data = value.toString()
                val parts = data.split("|")
                if (parts.size < 3) continue

                val jenis = parts[0]
                val jam = parts[1]
                val tanggal = parts[2]
                val namaPengguna = namaDefault ?: "Kasir"

                val tanggalDisplay = formatTanggalDisplay(tanggal)
                val mapKey = "${tanggal}_$namaPengguna"

                if (!absenMap.containsKey(mapKey)) {
                    absenMap[mapKey] = RiwayatAbsen(
                        key, namaPengguna, "Karyawan", tanggalDisplay,
                        "-", "-", "Hadir", null, null, tanggal
                    )
                }

                val exist = absenMap[mapKey]!!
                if ("MASUK" == jenis) {
                    absenMap[mapKey] = RiwayatAbsen(
                        exist.id, exist.namaPengguna, exist.role, exist.tanggal,
                        jam, exist.jamPulang, exist.status, exist.keteranganIzin, exist.pesanOwner, tanggal
                    )
                } else if ("PULANG" == jenis) {
                    absenMap[mapKey] = RiwayatAbsen(
                        exist.id, exist.namaPengguna, exist.role, exist.tanggal,
                        exist.jamMasuk, jam, exist.status, exist.keteranganIzin, exist.pesanOwner, tanggal
                    )
                }
            }

            list.addAll(absenMap.values)
            Collections.sort(list) { o1, o2 -> compareByDateTimeAsc(o1, o2) }
        } catch (e: Exception) {
            Log.e("Riwayat", "Gagal membaca riwayat lokal", e)
            notifyUser("Terjadi kesalahan saat membaca data lokal.")
        }
        return list
    }

    private fun notifyUser(message: String) {
        try {
            activity?.runOnUiThread {
                try {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                } catch (ignored: Exception) {}
            }
        } catch (ignored: Exception) {}
    }

    private fun compareByDateTimeAsc(a: RiwayatAbsen, b: RiwayatAbsen): Int {
        return try {
            val d1 = a.dibuatPada ?: ""
            val d2 = b.dibuatPada ?: ""
            val cd = d1.compareTo(d2)
            if (cd != 0) cd else {
                val t1 = normalizeTime(a.jamMasuk)
                val t2 = normalizeTime(b.jamMasuk)
                t1.compareTo(t2)
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun normalizeTime(time: String?): String {
        if (time == null || time == "-" || time == "--:--" || time.trim().isEmpty()) return "99:99"
        return time
    }
}