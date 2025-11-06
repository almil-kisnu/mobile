package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.HargaGrosir
import java.text.NumberFormat
import java.util.Locale

class HargaJualAdapter(
    private val hargaGrosirList: List<HargaGrosir>
) : RecyclerView.Adapter<HargaJualAdapter.JualViewHolder>() {

    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0 // Tidak menampilkan desimal
    }

    inner class JualViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Menggunakan ID yang ada untuk tujuan yang berbeda
        val tvMinQty: TextView = itemView.findViewById(R.id.tvHargaBeli) // Digunakan untuk Min Qty
        val tvHargaJual: TextView = itemView.findViewById(R.id.tvExp)   // Digunakan untuk Harga Jual
        val tvStokIgnored: TextView = itemView.findViewById(R.id.tvStok) // Diabaikan atau disembunyikan
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JualViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tabel, parent, false) // Asumsikan nama layout: detail_stok_card
        return JualViewHolder(view)
    }

    override fun onBindViewHolder(holder: JualViewHolder, position: Int) {
        val currentHarga = hargaGrosirList[position]

        // 1. Min Qty (Menggunakan tvHargaBeli)
        holder.tvMinQty.text = "Min: ${currentHarga.minQty} pcs"
        // Atur agar teks tidak terlalu menonjol seperti harga beli

        // 2. Harga Jual (Menggunakan tvExp)
        holder.tvHargaJual.text = currencyFormatter.format(currentHarga.hargaJual)
        holder.tvHargaJual.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.status_active)) // Misal: warna hijau

        // 3. Sembunyikan atau Kosongkan tvStok
        holder.tvStokIgnored.visibility = View.GONE
        holder.tvStokIgnored.text = ""
    }

    override fun getItemCount(): Int = hargaGrosirList.size
}