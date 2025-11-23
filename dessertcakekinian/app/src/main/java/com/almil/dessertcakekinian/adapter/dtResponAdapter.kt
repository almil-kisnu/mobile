package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.DetailTransfer
import com.almil.dessertcakekinian.model.TransferViewModel
import com.almil.dessertcakekinian.model.ProductViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class dtResponAdapter(
    private val itemActionListener: OnItemActionListener,
    private val viewModel: TransferViewModel,
    private val productViewModel: ProductViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<dtResponAdapter.ViewHolder>() {

    private val dataList: MutableList<DetailTransfer> = mutableListOf()

    interface OnItemActionListener {
        fun onCatatanChanged(position: Int, newCatatan: String)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaMenu: TextView = itemView.findViewById(R.id.tvNamaMenu)
        val tvStok: TextView = itemView.findViewById(R.id.tvstok)
        val tvJumlah: EditText = itemView.findViewById(R.id.tvJumlah)
        val etCatatan: TextView = itemView.findViewById(R.id.etCatatan)

        private var currentMaxStok: Int = 0
        private var isUpdating = false



        fun bind(detailTransfer: DetailTransfer) {
            isUpdating = true


            lifecycleOwner.lifecycleScope.launch {
                productViewModel.getNamaProdukById(detailTransfer.idproduk).collect { namaProduk ->
                    tvNamaMenu.text = namaProduk ?: "Produk ID: ${detailTransfer.idproduk}"
                }
            }
            tvStok.text = "Stok: ${detailTransfer.jumlah}"
            etCatatan.text = "Catatan : ${detailTransfer.catatan ?: "-"}"
            tvJumlah.setText(detailTransfer.jumlahDiterima?.toString() ?: "")

            // ✅ TAMBAH: Disable input manual
            tvJumlah.isFocusable = false
            tvJumlah.isClickable = false
            tvJumlah.isCursorVisible = false
            isUpdating = false
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_dtrespon,
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