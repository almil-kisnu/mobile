package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.DetailOrder
class OrderDetailAdapter : ListAdapter<DetailOrder, OrderDetailAdapter.DetailOrderViewHolder>(DetailOrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailOrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_struk, parent, false)
        return DetailOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetailOrderViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class DetailOrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_number)
        private val tvNama: TextView = itemView.findViewById(R.id.tv_nama)
        private val tvQty: TextView = itemView.findViewById(R.id.tv_qty)
        private val tvHarga: TextView = itemView.findViewById(R.id.tv_harga)

        fun bind(detailOrder: DetailOrder, number: Int) {
            // Set nomor urut
            tvNumber.text = "$number."

            // Set nama produk
            tvNama.text = detailOrder.namaproduk

            // Set jumlah x harga satuan
            val qtyText = "${detailOrder.jumlah} x ${formatCurrency(detailOrder.harga)}"
            tvQty.text = qtyText

            // Set subtotal (harga total untuk produk ini)
            tvHarga.text = formatCurrency(detailOrder.subtotal)
        }

        private fun formatCurrency(amount: Double): String {
            return String.format("%,.0f", amount).replace(",", ".")
        }
    }

    class DetailOrderDiffCallback : DiffUtil.ItemCallback<DetailOrder>() {
        override fun areItemsTheSame(oldItem: DetailOrder, newItem: DetailOrder): Boolean {
            return oldItem.iddetail == newItem.iddetail
        }

        override fun areContentsTheSame(oldItem: DetailOrder, newItem: DetailOrder): Boolean {
            return oldItem == newItem
        }
    }
}