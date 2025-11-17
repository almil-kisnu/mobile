package com.almil.dessertcakekinian.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.ProdukDetail
import com.almil.dessertcakekinian.model.EventDiskon
import java.text.NumberFormat
import java.util.*

data class ProdukDiskonItem(
    val produkDetail: ProdukDetail,
    val diskon: EventDiskon
)

class ProdukDiskonAdapter(
    private val onItemClick: ((ProdukDiskonItem) -> Unit)? = null
) : ListAdapter<ProdukDiskonItem, ProdukDiskonAdapter.ProdukDiskonViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdukDiskonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produk_berlaku, parent, false)
        return ProdukDiskonViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ProdukDiskonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProdukDiskonViewHolder(
        itemView: View,
        private val onItemClick: ((ProdukDiskonItem) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivProdukImage: ImageView = itemView.findViewById(R.id.iv_produk_image)
        private val tvProdukName: TextView = itemView.findViewById(R.id.tv_produk_name)
        private val tvProdukPriceOld: TextView = itemView.findViewById(R.id.tv_produk_price_old)
        private val tvProdukPriceNew: TextView = itemView.findViewById(R.id.tv_produk_price_new)

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }

        fun bind(item: ProdukDiskonItem) {
            val produk = item.produkDetail.produk
            val diskon = item.diskon

            // Set nama produk
            tvProdukName.text = produk.namaproduk

            // Set harga
            val hargaAsli = produk.harga_eceran ?: 0.0

            if (hargaAsli > 0) {
                // Harga lama (dicoret)
                tvProdukPriceOld.text = formatRupiah(hargaAsli)
                tvProdukPriceOld.paintFlags = tvProdukPriceOld.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvProdukPriceOld.visibility = View.VISIBLE

                // Hitung harga baru setelah diskon
                val persenDiskon = diskon.nilaiDiskon / 100
                val nominalDiskon = hargaAsli * persenDiskon
                val hargaBaru = hargaAsli - nominalDiskon

                // Harga baru
                tvProdukPriceNew.text = formatRupiah(hargaBaru)
                tvProdukPriceNew.visibility = View.VISIBLE
            } else {
                // Jika harga tidak tersedia
                tvProdukPriceOld.visibility = View.GONE
                tvProdukPriceNew.text = "Harga tidak tersedia"
            }

            // Set click listener
            onItemClick?.let { clickListener ->
                itemView.setOnClickListener {
                    clickListener(item)
                }
            }
        }

        private fun formatRupiah(amount: Double): String {
            return try {
                currencyFormat.format(amount)
            } catch (e: Exception) {
                "Rp ${amount.toLong()}"
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ProdukDiskonItem>() {
            override fun areItemsTheSame(
                oldItem: ProdukDiskonItem,
                newItem: ProdukDiskonItem
            ): Boolean {
                return oldItem.produkDetail.produk.idproduk == newItem.produkDetail.produk.idproduk &&
                        oldItem.diskon.idDiskon == newItem.diskon.idDiskon
            }

            override fun areContentsTheSame(
                oldItem: ProdukDiskonItem,
                newItem: ProdukDiskonItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}