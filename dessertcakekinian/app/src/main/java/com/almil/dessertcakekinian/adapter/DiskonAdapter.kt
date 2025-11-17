package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.EventDiskon
import java.text.SimpleDateFormat
import android.graphics.Color
import java.util.*

class DiskonAdapter(
    private val onItemClick: (EventDiskon) -> Unit
) : ListAdapter<EventDiskon, DiskonAdapter.DiskonViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiskonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diskon, parent, false)
        return DiskonViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: DiskonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiskonViewHolder(
        itemView: View,
        private val onItemClick: (EventDiskon) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvPromoTitle: TextView = itemView.findViewById(R.id.tv_promo_title)
        private val tvPromoValue: TextView = itemView.findViewById(R.id.tv_promo_value)
        private val tvPromoDate: TextView = itemView.findViewById(R.id.tv_promo_date)
        private val tvPromoStatus: TextView = itemView.findViewById(R.id.tv_promo_status)
        private val ivArrow: ImageView = itemView.findViewById(R.id.iv_arrow)

        fun bind(diskon: EventDiskon) {
            // Set title
            tvPromoTitle.text = diskon.namaDiskon

            // Set nilai diskon (format persentase)
            val nilaiDiskonFormatted = if (diskon.nilaiDiskon % 1.0 == 0.0) {
                "Diskon ${diskon.nilaiDiskon.toInt()}%"
            } else {
                "Diskon ${String.format("%.1f", diskon.nilaiDiskon)}%"
            }
            tvPromoValue.text = nilaiDiskonFormatted

            // Set tanggal (format: 15 Des - 20 Des 2023)
            val dateRange = formatDateRange(diskon.tanggalMulai, diskon.tanggalSelesai)
            tvPromoDate.text = dateRange

            // Set status (Aktif/Nonaktif)
            if (diskon.isActive) {
                tvPromoStatus.text = "Aktif"
                tvPromoStatus.setTextColor(Color.parseColor("#4CAF50")) // Hijau
                tvPromoStatus.setBackgroundResource(R.drawable.bg_status_aktif)
            } else {
                tvPromoStatus.text = "Nonaktif"
                tvPromoStatus.setTextColor(Color.parseColor("#F44336"))
                tvPromoStatus.setBackgroundResource(R.drawable.bg_status_nonaktif)
            }

            // Set click listener
            itemView.setOnClickListener {
                onItemClick(diskon)
            }
        }

        private fun formatDateRange(tanggalMulai: String, tanggalSelesai: String): String {
            return try {
                // Input format dari database (contoh: "2023-12-15" atau "2023-12-15T00:00:00")
                val inputFormat = SimpleDateFormat(
                    if (tanggalMulai.contains("T")) "yyyy-MM-dd'T'HH:mm:ss" else "yyyy-MM-dd",
                    Locale("id", "ID")
                )

                // Output format untuk tampilan
                val dayFormat = SimpleDateFormat("dd", Locale("id", "ID"))
                val monthFormat = SimpleDateFormat("MMM", Locale("id", "ID"))
                val yearFormat = SimpleDateFormat("yyyy", Locale("id", "ID"))

                val startDate = inputFormat.parse(tanggalMulai)
                val endDate = inputFormat.parse(tanggalSelesai)

                if (startDate != null && endDate != null) {
                    val startDay = dayFormat.format(startDate)
                    val startMonth = monthFormat.format(startDate)
                    val endDay = dayFormat.format(endDate)
                    val endMonth = monthFormat.format(endDate)
                    val year = yearFormat.format(endDate)

                    // Cek apakah bulan sama
                    if (startMonth == endMonth) {
                        "$startDay - $endDay $endMonth $year"
                    } else {
                        "$startDay $startMonth - $endDay $endMonth $year"
                    }
                } else {
                    "$tanggalMulai - $tanggalSelesai"
                }
            } catch (e: Exception) {
                // Fallback jika parsing gagal
                "$tanggalMulai - $tanggalSelesai"
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<EventDiskon>() {
            override fun areItemsTheSame(oldItem: EventDiskon, newItem: EventDiskon): Boolean {
                return oldItem.idDiskon == newItem.idDiskon
            }

            override fun areContentsTheSame(oldItem: EventDiskon, newItem: EventDiskon): Boolean {
                return oldItem == newItem
            }
        }
    }
}