package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.CartItem
import java.text.DecimalFormat

class StrukrequestAdapter(
    cartItemsMap: Map<Int, CartItem>
) : RecyclerView.Adapter<StrukrequestAdapter.StrukViewHolder>() {

    private var cartItemList: List<CartItem> = ArrayList(cartItemsMap.values)
    private val decimalFormat = DecimalFormat("#,##0")

    fun updateData(newCartItemsMap: Map<Int, CartItem>) {
        cartItemList = ArrayList(newCartItemsMap.values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StrukViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_struk, parent, false)
        return StrukViewHolder(view)
    }

    override fun onBindViewHolder(holder: StrukViewHolder, position: Int) {
        val cartItem = cartItemList[position]
        val produk = cartItem.produkDetail.produk

        val quantity = cartItem.quantity
        val hargaSatuan = cartItem.hargaSatuan ?: 0.0

        // 1. Nomor Urut + Nama Produk
        holder.tvNumber.text = "${position + 1}."
        holder.tvNama.text = produk.namaproduk

        // 2. tvQty dijadikan GONE
        holder.tvQty.visibility = View.GONE

        // 3. tvHarga diganti dengan quantity
        holder.tvHarga.text = quantity.toString()
    }

    override fun getItemCount(): Int = cartItemList.size

    // ViewHolder Class
    class StrukViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNumber: TextView = itemView.findViewById(R.id.tv_number)
        val tvNama: TextView = itemView.findViewById(R.id.tv_nama)
        val tvQty: TextView = itemView.findViewById(R.id.tv_qty)
        val tvHarga: TextView = itemView.findViewById(R.id.tv_harga)
    }
}