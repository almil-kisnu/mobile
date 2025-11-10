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
import java.util.Locale

interface OnTransaksiItemClickListener {
    fun onUpdateCartItem(produkDetail: ProdukDetail, quantity: Int)
    fun onShowHargaGrosir(produkDetail: ProdukDetail)
}

class TransaksiAdapter(
    private var productList: List<ProdukDetail>,
    private val currentOutletId: Int,
    private val listener: OnTransaksiItemClickListener,
    // UBAH: cartQuantities menjadi 'var' agar bisa diupdate secara eksternal
    private var cartQuantities: Map<Int, Int> = emptyMap()
) : RecyclerView.Adapter<TransaksiAdapter.TransaksiViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    inner class TransaksiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProdukGambar)
        val tvProductName: TextView = itemView.findViewById(R.id.tvNamaProduk)
        val tvStockValue: TextView = itemView.findViewById(R.id.tvStokValue)
        val tvPriceValue: TextView = itemView.findViewById(R.id.tvHargaJualValue)
        val btnTambahAwal: MaterialButton = itemView.findViewById(R.id.btnTambahAwal)
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
        holder.tvProductName.text = produk.namaproduk

        val totalStok = currentDetail.detailStok
            .filter { it.idoutlet == currentOutletId }
            .sumOf { it.stok }
        holder.tvStockValue.text = totalStok.toString()

        holder.tvPriceValue.text = produk.harga_eceran?.takeIf { it > 0 }
            ?.let { currencyFormat.format(it) } ?: "Rp 0"

        if (initialQuantity > 0) {
            holder.btnTambahAwal.visibility = View.GONE
            holder.quantitySelector.visibility = View.VISIBLE
            holder.quantitySelector.setQuantity(initialQuantity) // Set kuantitas dari keranjang
        } else {
            holder.btnTambahAwal.visibility = View.VISIBLE
            holder.quantitySelector.visibility = View.GONE
            holder.quantitySelector.setQuantity(0)
        }

        holder.btnTambahAwal.setOnClickListener {
            holder.btnTambahAwal.visibility = View.GONE
            holder.quantitySelector.visibility = View.VISIBLE
            holder.quantitySelector.setQuantity(1)
            listener.onUpdateCartItem(currentDetail, 1) // Panggil listener saat pertama kali ditambahkan
        }
        holder.quantitySelector.setOnQuantityChangeListener(null)

        // Listener untuk perubahan kuantitas pada QuantitySelector
        holder.quantitySelector.setOnQuantityChangeListener { qty ->
            if (qty <= 0) {
                holder.btnTambahAwal.visibility = View.VISIBLE
                holder.quantitySelector.visibility = View.GONE
            }

            listener.onUpdateCartItem(currentDetail, qty)
        }

        holder.itemView.setOnClickListener {
            if (currentDetail.hargaGrosir.isNotEmpty()) {
                listener.onShowHargaGrosir(currentDetail)
            }
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