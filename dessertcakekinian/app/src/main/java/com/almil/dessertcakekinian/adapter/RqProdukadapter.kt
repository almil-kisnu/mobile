package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.common.QuantitySelector
import com.almil.dessertcakekinian.model.ProdukDetail
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale// TAMBAHKAN di bagian import
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import android.text.SpannableString
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.graphics.Color


interface OnRqProdukItemClickListener {
    fun onUpdateRqCartItem(produkDetail: ProdukDetail, quantity: Int)
    fun onProductItemClicked(produkDetail: ProdukDetail) // TAMBAHKAN INI
}

class RqProdukAdapter(
    private var productList: List<ProdukDetail>,
    private val currentOutletId: Int,
    private val listener: OnRqProdukItemClickListener ,
    private var cartQuantities: Map<Int, Int> = emptyMap()
) : RecyclerView.Adapter<RqProdukAdapter.TransaksiViewHolder>() {
    private val displayDateFormatter = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    inner class TransaksiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProdukGambar)
        val tvProductName: TextView = itemView.findViewById(R.id.tvNamaProduk)
        val tvStockValue: TextView = itemView.findViewById(R.id.tvStokValue)
        val btnTambahAwal: MaterialButton = itemView.findViewById(R.id.btnTambahAwal)
        val tvExpValue: TextView = itemView.findViewById(R.id.tvHargaJualValue)
        val quantitySelector: QuantitySelector =
            itemView.findViewById(R.id.quantitySelector)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaksiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.daf_produk_card_transaksi, parent, false)
        return TransaksiViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransaksiViewHolder, position: Int) {
        val currentDetail = productList[position]
        val produk = currentDetail.produk

        val initialQuantity = cartQuantities[produk.idproduk] ?: 0

        holder.ivProductImage.setImageResource(R.drawable.ic_cake)

// Klik gambar untuk navigasi ke detail
        holder.ivProductImage.setOnClickListener {
            listener.onProductItemClicked(currentDetail)
        }

// Tambahkan setelah holder.ivProductImage.setOnClickListener

// Hitung stok dan exp
        val outletStokList = currentDetail.detailStok
            .filter { it.idoutlet == currentOutletId }

// Total stok
        val totalStok = outletStokList.sumOf { it.stok }
        holder.tvStockValue.text = totalStok.toString()

// Tanggal kadaluarsa terdekat dengan stok > 0
        val nearestExpiryDate = outletStokList
            .filter { it.stok > 0 }
            .mapNotNull { detail -> detail.tglKadaluarsa?.let { parseDate(it) } }
            .minOrNull()

        if (nearestExpiryDate != null) {
            val expLabel = "EXP: "
            val dateText = displayDateFormatter.format(nearestExpiryDate)
            val fullText = expLabel + dateText

            val daysDifference = (nearestExpiryDate.time - Date().time) / (1000 * 60 * 60 * 24)
            val expColor = when {
                daysDifference < 0 -> R.color.status_inactive
                daysDifference <= 30 -> R.color.status_warning
                else -> R.color.status_active
            }
            val finalColorId = ContextCompat.getColor(holder.itemView.context, expColor)

            val spannable = SpannableString(fullText)

            // Warna hitam untuk "EXP: "
            spannable.setSpan(
                ForegroundColorSpan(Color.BLACK),
                0,
                expLabel.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Warna status untuk tanggal
            spannable.setSpan(
                ForegroundColorSpan(finalColorId),
                expLabel.length,
                fullText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            holder.tvExpValue.text = spannable

        } else {
            val expLabel = "EXP: "
            val dateText = "N/A"
            val fullText = expLabel + dateText

            val spannable = SpannableString(fullText)

            // "EXP:" warna hitam
            spannable.setSpan(
                ForegroundColorSpan(Color.BLACK),
                0,
                expLabel.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // "N/A" juga hitam
            spannable.setSpan(
                ForegroundColorSpan(Color.BLACK),
                expLabel.length,
                fullText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            holder.tvExpValue.text = spannable
        }

        holder.tvProductName.text = produk.namaproduk

        // PENTING: Set listener ke null SEBELUM setQuantity untuk mencegah crash
        holder.quantitySelector.setOnQuantityChangeListener(null)

        if (initialQuantity > 0) {
            holder.btnTambahAwal.visibility = View.GONE
            holder.quantitySelector.visibility = View.VISIBLE
            holder.quantitySelector.setQuantitySilently(initialQuantity)
        } else {
            holder.btnTambahAwal.visibility = View.VISIBLE
            holder.quantitySelector.visibility = View.GONE
            holder.quantitySelector.setQuantitySilently(0)
        }

        holder.btnTambahAwal.setOnClickListener {
            holder.btnTambahAwal.visibility = View.GONE
            holder.quantitySelector.visibility = View.VISIBLE
            holder.quantitySelector.setQuantity(1)
            listener.onUpdateRqCartItem(currentDetail, 1)
        }

        holder.quantitySelector.setOnQuantityChangeListener { qty ->
            if (qty <= 0) {
                holder.btnTambahAwal.visibility = View.VISIBLE
                holder.quantitySelector.visibility = View.GONE
            }
            listener.onUpdateRqCartItem(currentDetail, qty)
        }

    }
    // TAMBAHKAN method ini sebelum getItemCount()
    private fun parseDate(dateString: String): Date? {
        return try {
            apiDateFormatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    override fun getItemCount(): Int = productList.size
    fun updateData(newList: List<ProdukDetail>, newCartQuantities: Map<Int, Int> = emptyMap()) {
        productList = newList
        cartQuantities = newCartQuantities
        notifyDataSetChanged()
    }
    fun updateCartQuantities(newCartQuantities: Map<Int, Int>) {
        if (this.cartQuantities != newCartQuantities) {
            this.cartQuantities = newCartQuantities
            notifyDataSetChanged()
        }
    }

    fun getProductList(): List<ProdukDetail> {
        return productList
    }
}