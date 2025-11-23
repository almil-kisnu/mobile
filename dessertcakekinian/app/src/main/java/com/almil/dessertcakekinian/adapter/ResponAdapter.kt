package com.almil.dessertcakekinian.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.DetailTransfer
import com.almil.dessertcakekinian.model.TransferViewModel
import com.almil.dessertcakekinian.model.ProductViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class ResponAdapter(
    private val itemActionListener: OnItemActionListener,
    private val viewModel: TransferViewModel,  // ✅ Tambah viewModel
    private val productViewModel: ProductViewModel,  // ✅ Tambah ini
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<ResponAdapter.ViewHolder>() {

    private val dataList: MutableList<DetailTransfer> = mutableListOf()

    interface OnItemActionListener {
        fun onQuantityChanged(position: Int, newQuantity: Int)
        fun onCatatanChanged(position: Int, newCatatan: String)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaMenu: TextView = itemView.findViewById(R.id.tvNamaMenu)
        val tvStok: TextView = itemView.findViewById(R.id.tvstok)
        val tvJumlah: EditText = itemView.findViewById(R.id.tvJumlah)
        val etCatatan: EditText = itemView.findViewById(R.id.etCatatan)
        val btnMinus: MaterialButton = itemView.findViewById(R.id.btnMinus)
        val btnPlus: MaterialButton = itemView.findViewById(R.id.btnPlus)

        private var currentMaxStok: Int = 0
        private var isUpdating = false

        private val catatanTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (adapterPosition != RecyclerView.NO_POSITION && !isUpdating) {
                    itemActionListener.onCatatanChanged(adapterPosition, s.toString())
                }
            }
        }

        private val jumlahTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val input = s.toString()
                val newQuantity = input.toIntOrNull() ?: 0

                // ✅ Validasi: tidak boleh melebihi stok
                val validQuantity = when {
                    newQuantity < 0 -> 0
                    newQuantity > currentMaxStok -> currentMaxStok
                    else -> newQuantity
                }

                // Update jika berbeda
                if (newQuantity != validQuantity) {
                    isUpdating = true
                    tvJumlah.setText(validQuantity.toString())
                    tvJumlah.setSelection(tvJumlah.text.length)
                    isUpdating = false
                }

                if (adapterPosition != RecyclerView.NO_POSITION) {
                    itemActionListener.onQuantityChanged(adapterPosition, validQuantity)
                    updateButtonStates(validQuantity)
                }
            }
        }

        init {
            // ✅ Listener untuk minus
            btnMinus.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val currentQuantity = tvJumlah.text.toString().toIntOrNull() ?: 0
                    if (currentQuantity > 0) {
                        val newQuantity = currentQuantity - 1
                        isUpdating = true
                        tvJumlah.setText(newQuantity.toString())
                        isUpdating = false
                        itemActionListener.onQuantityChanged(adapterPosition, newQuantity)
                        updateButtonStates(newQuantity)
                    }
                }
            }

            // ✅ Listener untuk plus
            btnPlus.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val currentQuantity = tvJumlah.text.toString().toIntOrNull() ?: 0
                    if (currentQuantity < currentMaxStok) {
                        val newQuantity = currentQuantity + 1
                        isUpdating = true
                        tvJumlah.setText(newQuantity.toString())
                        isUpdating = false
                        itemActionListener.onQuantityChanged(adapterPosition, newQuantity)
                        updateButtonStates(newQuantity)
                    }
                }
            }

            etCatatan.addTextChangedListener(catatanTextWatcher)
        }

        fun bind(detailTransfer: DetailTransfer) {
            isUpdating = true

            currentMaxStok = detailTransfer.jumlah

            lifecycleOwner.lifecycleScope.launch {
                productViewModel.getNamaProdukById(detailTransfer.idproduk).collect { namaProduk ->
                    tvNamaMenu.text = namaProduk ?: "Produk ID: ${detailTransfer.idproduk}"
                }
            }

            tvStok.text = "Stok: ${currentMaxStok}"
            val jumlahDiterima = detailTransfer.jumlahDiterima ?: currentMaxStok
            tvJumlah.setText(jumlahDiterima.toString())

            // ✅ TAMBAH: Disable input manual
            tvJumlah.isFocusable = false
            tvJumlah.isClickable = false
            tvJumlah.isCursorVisible = false

            etCatatan.removeTextChangedListener(catatanTextWatcher)
            etCatatan.setText(detailTransfer.catatan ?: "")
            etCatatan.addTextChangedListener(catatanTextWatcher)

            updateButtonStates(jumlahDiterima)

            isUpdating = false
        }

        // ✅ Update state tombol
        private fun updateButtonStates(currentQuantity: Int) {
            btnMinus.isEnabled = currentQuantity > 0
            btnPlus.isEnabled = currentQuantity < currentMaxStok

            // Visual feedback
            btnMinus.alpha = if (currentQuantity > 0) 1.0f else 0.5f
            btnPlus.alpha = if (currentQuantity < currentMaxStok) 1.0f else 0.5f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_produk_respon,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    override fun getItemCount(): Int = dataList.size

    fun updateData(newDataList: List<DetailTransfer>) {
        dataList.clear()
        dataList.addAll(newDataList)
        notifyDataSetChanged()
    }
}