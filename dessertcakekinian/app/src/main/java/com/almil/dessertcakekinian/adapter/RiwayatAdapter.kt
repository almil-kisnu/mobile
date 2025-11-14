package com.almil.dessertcakekinian.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.fragment.PresensiFragment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RiwayatAdapter(private var riwayatList: List<PresensiFragment.RiwayatPresensi>) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    private var riwayatListFiltered: MutableList<PresensiFragment.RiwayatPresensi> = ArrayList(riwayatList)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_presensi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val data = riwayatListFiltered[position]

            // Format tanggal menjadi format Indonesia
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))

            val date = dateFormat.parse(data.tanggal)

            // Set data ke view sesuai dengan XML kamu
            holder.tvTanggal.text = date?.let { displayDateFormat.format(it) } ?: data.tanggal

            // Set status badge - HANYA HADIR dan IZIN
            holder.tvStatusBadge.text = data.status
            setStatusBadge(holder.tvStatusBadge, data.status)

            // Set waktu absen masuk dan pulang
            holder.tvWaktuClockIn.text = if (data.jamMasuk != "-") data.jamMasuk else "-"
            holder.tvWaktuClockOut.text = if (data.jamPulang != "-") data.jamPulang else "-"

            // Hitung total jam kerja
            val totalJam = computeWorkDuration(data.jamMasuk, data.jamPulang)
            holder.tvTotalJam.text = totalJam

            // Hitung jam terhutang (default 0 jika tidak ada hutang)
            val jamTerhutang = calculateJamTerhutang(data.jamMasuk, data.jamPulang)
            holder.tvJamTerhutang.text = jamTerhutang

            // Set warna jam terhutang - PAKAI WARNA ANDROID
            if (jamTerhutang.contains("-")) {
                holder.tvJamTerhutang.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark))
            } else {
                holder.tvJamTerhutang.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark))
            }

        } catch (e: Exception) {
            Log.e("RiwayatAdapter", "onBindViewHolder error at pos=$position", e)
        }
    }

    private fun computeWorkDuration(jamMasuk: String, jamPulang: String): String {
        return try {
            if (jamMasuk == "-" || jamPulang == "-") return "-"

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val tIn = sdf.parse(jamMasuk)
            val tOut = sdf.parse(jamPulang)

            if (tIn == null || tOut == null) return "-"

            var diff = tOut.time - tIn.time
            if (diff < 0) diff += 24 * 60 * 60 * 1000

            val hours = diff / (60 * 60 * 1000)
            val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)
            "$hours Jam $minutes Menit"
        } catch (e: Exception) {
            "-"
        }
    }

    private fun calculateJamTerhutang(jamMasuk: String, jamPulang: String): String {
        return try {
            if (jamMasuk == "-" || jamPulang == "-") return "0 Jam"

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val tIn = sdf.parse(jamMasuk)
            val tOut = sdf.parse(jamPulang)

            if (tIn == null || tOut == null) return "0 Jam"

            var diff = tOut.time - tIn.time
            if (diff < 0) diff += 24 * 60 * 60 * 1000

            val totalJam = diff / (60 * 60 * 1000)

            // Asumsi jam kerja normal adalah 8 jam
            val jamNormal = 8
            val selisih = totalJam - jamNormal

            return if (selisih < 0) {
                "$selisih Jam"
            } else if (selisih > 0) {
                "+$selisih Jam"
            } else {
                "0 Jam"
            }
        } catch (e: Exception) {
            "0 Jam"
        }
    }

    private fun setStatusBadge(tvStatusBadge: TextView, status: String) {
        when (status.lowercase(Locale.getDefault())) {
            "hadir" -> tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_hadir)
            "izin" -> tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_izin)
            // Hanya Hadir dan Izin saja, lainnya pakai default hadir
            else -> tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_hadir)
        }
    }

    override fun getItemCount(): Int {
        val size = riwayatListFiltered.size
        Log.d("RiwayatAdapter", "getItemCount=$size")
        return size
    }

    fun filterByStatus(selectedStatus: String) {
        riwayatListFiltered.clear()

        for (riwayat in riwayatList) {
            // Filter hanya untuk Hadir dan Izin
            val matchesStatus = selectedStatus == "Semua Status" ||
                    selectedStatus == "Hadir" && riwayat.status == "Hadir" ||
                    selectedStatus == "Izin" && riwayat.status == "Izin"

            if (matchesStatus) {
                riwayatListFiltered.add(riwayat)
            }
        }
        notifyDataSetChanged()
        Log.d("RiwayatAdapter", "Filtered by status: $selectedStatus, result: ${riwayatListFiltered.size} items")
    }

    fun updateData(newData: List<PresensiFragment.RiwayatPresensi>) {
        val snapshot = ArrayList(newData)

        (riwayatList as? ArrayList)?.clear()
        (riwayatList as? ArrayList)?.addAll(snapshot)
        riwayatListFiltered.clear()
        riwayatListFiltered.addAll(snapshot)
        Log.d("RiwayatAdapter", "updateData size=${snapshot.size}")
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvWaktuClockIn: TextView = itemView.findViewById(R.id.tvWaktuClockIn)
        val tvWaktuClockOut: TextView = itemView.findViewById(R.id.tvWaktuClockOut)
        val tvTotalJam: TextView = itemView.findViewById(R.id.tvTotalJam)
        val tvJamTerhutang: TextView = itemView.findViewById(R.id.tvJamTerhutang)
    }
}