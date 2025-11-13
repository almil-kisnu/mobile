package com.almil.dessertcakekinian.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.RiwayatAbsen
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RiwayatAdapter(private var riwayatList: List<RiwayatAbsen>) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    private var riwayatListFiltered: MutableList<RiwayatAbsen> = ArrayList(riwayatList)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val data = riwayatListFiltered[position]

            holder.tvNama.text = data.namaPengguna
            holder.tvRole.text = data.role
            holder.tvTanggal.text = data.tanggal
            holder.tvJamMasuk.text = data.jamMasuk
            holder.tvJamPulang.text = data.jamPulang ?: "-"
            holder.tvStatus.text = data.status ?: "-"

            val dur = computeWorkDuration(data.jamMasuk, data.jamPulang)
            holder.tvJamKerja.text = "Jam kerja: $dur"

            setStatusBackground(holder.tvStatus, data.status)

            if (data.keteranganIzin != null && data.keteranganIzin.isNotEmpty()) {
                holder.tvKeterangan.text = "Alasan: ${data.keteranganIzin}"
                holder.tvKeterangan.visibility = View.VISIBLE
            } else {
                holder.tvKeterangan.visibility = View.GONE
            }

            if (data.pesanOwner != null && data.pesanOwner.isNotEmpty()) {
                holder.tvPesanOwner.text = "Pesan: ${data.pesanOwner}"
                holder.tvPesanOwner.visibility = View.VISIBLE
            } else {
                holder.tvPesanOwner.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("RiwayatAdapter", "onBindViewHolder error at pos=$position", e)
        }
    }

    private fun computeWorkDuration(jamMasuk: String?, jamPulang: String?): String {
        return try {
            if (jamMasuk == null || jamPulang == null) return "-"
            if (jamMasuk == "-" || jamPulang == "-") return "-"
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val tIn = sdf.parse(jamMasuk)
            val tOut = sdf.parse(jamPulang)
            if (tIn == null || tOut == null) return "-"
            var diff = tOut.time - tIn.time
            if (diff < 0) diff += 24 * 60 * 60 * 1000
            val hours = diff / (60 * 60 * 1000)
            val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)
            "$hours jam $minutes menit"
        } catch (e: Exception) {
            "-"
        }
    }

    private fun setStatusBackground(tvStatus: TextView, status: String?) {
        if (status == null) {
            tvStatus.setBackgroundResource(R.drawable.raunded_background_drak_blue)
            return
        }
        when (status.lowercase(Locale.getDefault())) {
            "hadir" -> tvStatus.setBackgroundResource(R.drawable.button_blue)
            "izin" -> tvStatus.setBackgroundResource(R.drawable.raunded_background_drak_blue)
            "terlambat" -> tvStatus.setBackgroundResource(R.drawable.button_red)
            "ditolak" -> tvStatus.setBackgroundResource(R.drawable.button_red)
            else -> tvStatus.setBackgroundResource(R.drawable.raunded_background_drak_blue)
        }
    }

    override fun getItemCount(): Int {
        val size = riwayatListFiltered.size
        Log.d("RiwayatAdapter", "getItemCount=$size")
        return size
    }

    fun filter(searchText: String, selectedStatus: String, selectedRole: String) {
        riwayatListFiltered.clear()

        for (absen in riwayatList) {
            val matchesSearch = absen.namaPengguna.lowercase(Locale.getDefault()).contains(searchText.lowercase(Locale.getDefault()))
            val matchesStatus = selectedStatus == "All Status" || absen.status == selectedStatus
            val matchesRole = selectedRole == "All Role" || absen.role == selectedRole

            if (matchesSearch && matchesStatus && matchesRole) {
                riwayatListFiltered.add(absen)
            }
        }
        notifyDataSetChanged()
    }

    fun updateData(newData: List<RiwayatAbsen>) {
        val snapshot = ArrayList(newData)

        (riwayatList as? ArrayList)?.clear()
        (riwayatList as? ArrayList)?.addAll(snapshot)
        riwayatListFiltered.clear()
        riwayatListFiltered.addAll(snapshot)
        Log.d("RiwayatAdapter", "updateData size=${snapshot.size}")
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView = itemView.findViewById(R.id.tv_nama)
        val tvRole: TextView = itemView.findViewById(R.id.tv_role)
        val tvTanggal: TextView = itemView.findViewById(R.id.tv_tanggal)
        val tvJamMasuk: TextView = itemView.findViewById(R.id.tv_jam_masuk)
        val tvJamPulang: TextView = itemView.findViewById(R.id.tv_jam_pulang)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val tvKeterangan: TextView = itemView.findViewById(R.id.tv_keterangan)
        val tvPesanOwner: TextView = itemView.findViewById(R.id.tv_pesan_owner)
        val tvJamKerja: TextView = itemView.findViewById(R.id.tv_jam_kerja)
    }
}