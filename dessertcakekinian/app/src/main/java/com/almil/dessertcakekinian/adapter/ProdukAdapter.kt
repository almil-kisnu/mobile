package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.ProdukDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface OnProductItemClickListener {
    fun onProductItemClicked(produkDetail: ProdukDetail)
}

class ProductAdapter(
    private var productList: List<ProdukDetail>,
    private val currentOutletId: Int,
    private val listener: OnProductItemClickListener

) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    private val displayDateFormatter = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvStockValue: TextView = itemView.findViewById(R.id.tvStockValue)
        val tvExpValue: TextView = itemView.findViewById(R.id.tvExpValue)
        val tvBarcodeValue: TextView = itemView.findViewById(R.id.tvBarcodeValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
// Menggunakan R.layout.user_card yang merupakan layout XML produk yang Anda berikan
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.daf_produk_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentDetail = productList[position]
        val produk = currentDetail.produk
        holder.ivProductImage.setImageResource(R.drawable.ic_cake)
        holder.tvProductName.text = produk.namaproduk
        holder.tvBarcodeValue.text = produk.barcode ?: "-"
        val outletStokList = currentDetail.detailStok
            .filter { it.idoutlet == currentOutletId }
        val totalStok = outletStokList.sumOf { it.stok }
        holder.tvStockValue.text = totalStok.toString()
        val nearestExpiryDate = outletStokList
            .filter { it.stok > 0 } // Tambahkan filter stok > 0
            .mapNotNull { detail -> detail.tglKadaluarsa?.let { parseDate(it) } }
            .minOrNull()
        if (nearestExpiryDate != null) {
            holder.tvExpValue.text = displayDateFormatter.format(nearestExpiryDate)
// Logika Warna EXP
            val daysDifference = (nearestExpiryDate.time - Date().time) / (1000 * 60 * 60 * 24)
            val expColor = when {
                daysDifference < 0 -> R.color.status_inactive // Sudah kadaluarsa (Merah/Ganti jika perlu)
                daysDifference <= 30 -> R.color.status_warning // Hampir kadaluarsa (Kuning/Ganti jika perlu)
                else -> R.color.status_active // Aman (Hijau/Warna default)
            }
            val finalColorId = try {
                expColor
            } catch (e: Exception) {
                if (daysDifference <= 30) R.color.status_inactive else R.color.status_active
            }
            holder.tvExpValue.setTextColor(
                ContextCompat.getColor(holder.itemView.context, finalColorId)
            )
        } else {
            holder.tvExpValue.text = "N/A"
            holder.tvExpValue.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.black)
            )
        }
        holder.itemView.setOnClickListener {
            listener.onProductItemClicked(currentDetail)
        }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            apiDateFormatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    override fun getItemCount(): Int = productList.size
    fun updateData(newList: List<ProdukDetail>) {
        productList = newList
        notifyDataSetChanged()
    }

}