package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.DetailStok
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HargaBeliAdapter(
    private val detailStokList: List<DetailStok>
) : RecyclerView.Adapter<HargaBeliAdapter.BeliViewHolder>() {

    private val displayDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0 // Tidak menampilkan desimal
    }

    inner class BeliViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Menggunakan ID dari XML yang diberikan
        val tvHargaBeli: TextView = itemView.findViewById(R.id.tvHargaBeli)
        val tvStok: TextView = itemView.findViewById(R.id.tvStok)
        val tvExp: TextView = itemView.findViewById(R.id.tvExp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeliViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tabel, parent, false) // Asumsikan nama layout: detail_stok_card
        return BeliViewHolder(view)
    }

    override fun onBindViewHolder(holder: BeliViewHolder, position: Int) {
        val currentStok = detailStokList[position]

        // 1. Harga Beli
        holder.tvHargaBeli.text = currencyFormatter.format(currentStok.hargaBeli)

        // 2. Stok
        holder.tvStok.text = "Stok: ${currentStok.stok}"

        // 3. Tanggal Kadaluarsa (EXP)
        val expDateString = currentStok.tglKadaluarsa
        if (expDateString.isNullOrEmpty()) {
            holder.tvExp.text = "N/A"
            holder.tvExp.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.black))
        } else {
            val date = apiDateFormatter.parse(expDateString)
            if (date != null) {
                holder.tvExp.text = displayDateFormatter.format(date)

                // Logika Warna EXP (Sama dengan ProductAdapter sebelumnya)
                val diff = date.time - Date().time
                val daysDifference = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

                val expColor = when {
                    daysDifference < 0 -> R.color.status_inactive // Kadaluarsa
                    daysDifference <= 30 -> R.color.status_warning // Hampir Kadaluarsa (1 bulan)
                    else -> R.color.status_active // Aman
                }

                // Menggunakan try-catch untuk fallback warna
                val finalColorId = try {
                    expColor
                } catch (e: Exception) {
                    if (daysDifference <= 30) android.R.color.holo_orange_dark else android.R.color.holo_green_dark
                }

                holder.tvExp.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, finalColorId)
                )

            } else {
                holder.tvExp.text = "Format Error"
                holder.tvExp.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray))
            }
        }
    }

    override fun getItemCount(): Int = detailStokList.size
}