package com.almil.dessertcakekinian.adapter

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.CartItem
import com.almil.dessertcakekinian.model.CartViewModel
import com.almil.dessertcakekinian.model.ProdukDetail
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import java.util.*

class RqCartAdapter(
    cartItemsMap: Map<Int, CartItem>,
    private val cartViewModel: CartViewModel
) : RecyclerView.Adapter<RqCartAdapter.CartViewHolder>() {

    private var cartItemList: List<CartItem> = ArrayList(cartItemsMap.values)

    fun updateRqData(newCartItemsMap: Map<Int, CartItem>) {
        this.cartItemList = ArrayList(newCartItemsMap.values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dtproduk_transaksi, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = cartItemList[position]
        val produkDetail = cartItem.produkDetail
        val produk = produkDetail.produk

        // Set background transparent
        holder.itemView.setBackgroundColor(Color.TRANSPARENT)

        holder.tvNamaMenu.text = produk.namaproduk
        
        // Clear previous image first to prevent glitching
        holder.imgMenu.setImageResource(R.drawable.ic_cake)
        
        // Load gambar dengan Coil
        if (!produk.gambar.isNullOrEmpty()) {
            holder.imgMenu.load(produk.gambar) {
                crossfade(true)
                placeholder(R.drawable.ic_cake)
                error(R.drawable.ic_cake)
            }
        }
        holder.etJumlah.isFocusable = false
        holder.etJumlah.isFocusableInTouchMode = false
        holder.etJumlah.isClickable = false
        holder.etJumlah.isCursorVisible = false

        val barcode = produk.barcode
        holder.tvHarga.text = if (barcode.isNullOrEmpty()) {
            "Barcode: -"
        } else {
            "Barcode: $barcode"
        }
        holder.tvHarga.visibility = View.VISIBLE

        fun updateQuantity(newQty: Int, detail: ProdukDetail) {
            val finalQty = if (newQty < 0) 0 else newQty

            // Update UI hanya jika berbeda
            if (holder.etJumlah.text.toString() != finalQty.toString()) {
                holder.isUpdatingText = true
                holder.etJumlah.setText(finalQty.toString())
                holder.etJumlah.setSelection(holder.etJumlah.text.length)
                holder.isUpdatingText = false
            }

            holder.btnMinus.isEnabled = finalQty > 0
            holder.btnPlus.isEnabled = true // selalu enabled

            // Update cart
            holder.itemView.post {
                cartViewModel.updateCart(detail.produk.idproduk, detail, finalQty)
            }
        }

        holder.isUpdatingText = true
        holder.etJumlah.setText(cartItem.quantity.toString())
        holder.btnMinus.isEnabled = cartItem.quantity > 0
        holder.isUpdatingText = false

        holder.btnPlus.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener

            val item = cartItemList.getOrNull(currentPos) ?: return@setOnClickListener
            val latestQuantity = item.quantity

            updateQuantity(latestQuantity + 1, item.produkDetail)
        }

        holder.btnMinus.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener

            val item = cartItemList.getOrNull(currentPos) ?: return@setOnClickListener
            val latestQuantity = item.quantity

            if (latestQuantity > 0) {
                updateQuantity(latestQuantity - 1, item.produkDetail)
            }
        }
    }

    override fun getItemCount(): Int = cartItemList.size

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMenu: ImageView = itemView.findViewById(R.id.imgMenu)
        val tvNamaMenu: TextView = itemView.findViewById(R.id.tvNamaMenu)
        val tvHarga: TextView = itemView.findViewById(R.id.tvHarga)
        val btnPlus: MaterialButton = itemView.findViewById(R.id.btnPlus)
        val btnMinus: MaterialButton = itemView.findViewById(R.id.btnMinus)
        val etJumlah: EditText = itemView.findViewById(R.id.tvJumlah)
        var isUpdatingText: Boolean = false
    }
}