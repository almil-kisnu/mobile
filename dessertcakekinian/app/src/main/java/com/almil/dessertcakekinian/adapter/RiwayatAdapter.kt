package com.almil.dessertcakekinian.adapter

import android.graphics.Color
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
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_presensi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val data = riwayatListFiltered[position]

            // TAMPILKAN USERNAME - menggunakan string resource
            holder.tvUsername.text = holder.itemView.context.getString(R.string.username_format, data.username)

            // Format tanggal menjadi format Indonesia
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))

            val date = dateFormat.parse(data.tanggal)

            // TANGGAL NORMAL
            holder.tvTanggal.text = date?.let { displayDateFormat.format(it) } ?: data.tanggal

            // Set status badge
            holder.tvStatusBadge.text = data.status
            setStatusBadge(holder.tvStatusBadge, data.status)

            // Set waktu absen masuk dan pulang
            holder.tvWaktuClockIn.text = if (data.jamMasuk != "-") data.jamMasuk else "-"
            holder.tvWaktuClockOut.text = if (data.jamPulang != "-") data.jamPulang else "-"

            // Hitung total jam kerja
            val totalJam = computeWorkDuration(data.jamMasuk, data.jamPulang)
            holder.tvTotalJam.text = totalJam

            // Set jam terhutang dari data yang sudah dihitung di PresensiFragment
            holder.tvJamTerhutang.text = data.utangJam

            // Set warna jam terhutang
            if (data.utangJam.isNotEmpty() && data.utangJam != "0j") {
                holder.tvJamTerhutang.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark))
            } else {
                holder.tvJamTerhutang.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark))
            }

            // Highlight item jika dipilih (untuk fitur kalender)
            if (position == selectedPosition) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.primary_light))
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
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

            if (hours > 0 && minutes > 0) {
                "${hours}j ${minutes}m"
            } else if (hours > 0) {
                "${hours}j"
            } else {
                "${minutes}m"
            }
        } catch (e: Exception) {
            "-"
        }
    }

    private fun setStatusBadge(tvStatusBadge: TextView, status: String) {
        when (status.lowercase(Locale.getDefault())) {
            "hadir" -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_hadir)
                tvStatusBadge.setTextColor(ContextCompat.getColor(tvStatusBadge.context, android.R.color.white))
            }
            "izin" -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_izin)
                tvStatusBadge.setTextColor(ContextCompat.getColor(tvStatusBadge.context, android.R.color.white))
            }
            "telat" -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_telat)
                tvStatusBadge.setTextColor(ContextCompat.getColor(tvStatusBadge.context, android.R.color.white))
            }
            else -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_hadir)
                tvStatusBadge.setTextColor(ContextCompat.getColor(tvStatusBadge.context, android.R.color.white))
            }
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
            val matchesStatus = when (selectedStatus) {
                "Semua Status" -> true
                "Hadir" -> riwayat.status == "Hadir"
                "Izin" -> riwayat.status == "Izin"
                "Telat" -> riwayat.status == "Telat"
                "Utang Jam" -> riwayat.utangJam.isNotEmpty() && riwayat.utangJam != "0j"
                else -> true
            }

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

    // FUNGSI BARU: Untuk highlight item yang dipilih dari kalender
    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        if (previousPosition != -1) notifyItemChanged(previousPosition)
        if (position != -1) notifyItemChanged(position)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvWaktuClockIn: TextView = itemView.findViewById(R.id.tvWaktuClockIn)
        val tvWaktuClockOut: TextView = itemView.findViewById(R.id.tvWaktuClockOut)
        val tvTotalJam: TextView = itemView.findViewById(R.id.tvTotalJam)
        val tvJamTerhutang: TextView = itemView.findViewById(R.id.tvJamTerhutang)
    }
}