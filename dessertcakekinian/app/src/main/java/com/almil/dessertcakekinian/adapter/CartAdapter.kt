package com.almil.dessertcakekinian.adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.CartItem
import com.almil.dessertcakekinian.model.CartViewModel
import com.almil.dessertcakekinian.model.ProdukDetail
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import java.util.*

class CartAdapter(
    private val context: Context,
    cartItemsMap: Map<Int, CartItem>,
    private val cartViewModel: CartViewModel
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private var cartItemList: List<CartItem> = ArrayList(cartItemsMap.values)
    private val decimalFormat = DecimalFormat("#,##0")

    fun updateData(newCartItemsMap: Map<Int, CartItem>) {
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

        holder.tvNamaMenu.text = produk.namaproduk
        holder.imgMenu.setImageResource(R.drawable.ic_cake)
        holder.etJumlah.isFocusable = false
        holder.etJumlah.isFocusableInTouchMode = false
        holder.etJumlah.isClickable = false
        holder.etJumlah.isCursorVisible = false

        val hargaSatuan = cartItem.hargaSatuan
        val hargaTotal = cartItem.quantity * hargaSatuan
        val formattedHargaTotal = "Rp " + decimalFormat.format(hargaTotal)
        val hargaText = if (cartItem.quantity > 1) {
            formattedHargaTotal + " (@" + decimalFormat.format(hargaSatuan) + ")"
        } else {
            formattedHargaTotal
        }
        holder.tvHarga.text = hargaText

        // TAMBAHKAN: Hitung max stok dari produkDetail
        val maxStok = produkDetail.detailStok.sumOf { it.stok }

        fun updateQuantity(newQty: Int, detail: ProdukDetail) {
            val finalQty = when {
                newQty < 0 -> 0
                newQty > maxStok -> {
                    android.widget.Toast.makeText(
                        context,
                        "Stok maksimal: $maxStok",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    maxStok
                }
                else -> newQty
            }

            // Update UI hanya jika berbeda
            if (holder.etJumlah.text.toString() != finalQty.toString()) {
                holder.isUpdatingText = true
                holder.etJumlah.setText(finalQty.toString())
                holder.etJumlah.setSelection(holder.etJumlah.text.length)
                holder.isUpdatingText = false
            }

            holder.btnMinus.isEnabled = finalQty > 0
            holder.btnPlus.isEnabled = finalQty < maxStok

            // Update cart
            holder.itemView.post {
                cartViewModel.updateCart(detail.produk.idproduk, detail, finalQty)
            }
        }

        holder.isUpdatingText = true
        holder.etJumlah.setText(cartItem.quantity.toString())
        holder.btnMinus.isEnabled = cartItem.quantity > 0
        // TAMBAHKAN: Set status btnPlus berdasarkan stok
        holder.btnPlus.isEnabled = cartItem.quantity < maxStok
        holder.isUpdatingText = false

        holder.btnPlus.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener

            val item = cartItemList.getOrNull(currentPos) ?: return@setOnClickListener
            val latestQuantity = item.quantity

            // TAMBAHKAN: Cek stok sebelum menambah
            if (latestQuantity < maxStok) {
                updateQuantity(latestQuantity + 1, item.produkDetail)
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Stok maksimal: $maxStok",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
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

//        holder.etJumlah.tag?.let {
//            holder.etJumlah.removeTextChangedListener(it as TextWatcher)
//        }
//
//        val textWatcher = object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable) {
//                if (holder.isUpdatingText) return
//
//                val currentPos = holder.bindingAdapterPosition
//                if (currentPos == RecyclerView.NO_POSITION) return
//
//                val item = cartItemList.getOrNull(currentPos) ?: return
//                val text = s.toString()
//                val newQty = if (text.isEmpty()) 0 else text.toIntOrNull() ?: 0
//
//                // TAMBAHKAN: Validasi terhadap maxStok
//                if (newQty > maxStok) {
//                    holder.isUpdatingText = true
//                    holder.etJumlah.setText(maxStok.toString())
//                    holder.etJumlah.setSelection(holder.etJumlah.text.length)
//                    holder.isUpdatingText = false
//
//                    android.widget.Toast.makeText(
//                        context,
//                        "Stok maksimal: $maxStok",
//                        android.widget.Toast.LENGTH_SHORT
//                    ).show()
//
//                    if (maxStok != item.quantity) {
//                        updateQuantity(maxStok, item.produkDetail)
//                    }
//                } else if (newQty != item.quantity) {
//                    updateQuantity(newQty, item.produkDetail)
//                }
//            }
//        }
//        holder.etJumlah.addTextChangedListener(textWatcher)
//        holder.etJumlah.tag = textWatcher
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