package com.almil.dessertcakekinian.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.OrderWithDetails
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import android.graphics.Color
import android.util.Log

class OrderHistoryAdapter(
    private val onOrderClick: (OrderWithDetails) -> Unit
) : ListAdapter<OrderWithDetails, OrderHistoryAdapter.OrderViewHolder>(OrderDiffCallback()) {

    companion object {
        private const val TAG = "OrderHistoryAdapter"
    }

    /**
     * Fungsi untuk set data dengan filter berdasarkan outlet
     * Panggil fungsi ini untuk submit data ke adapter
     */
    fun submitFilteredList(list: List<OrderWithDetails>, context: Context) {
        // Ambil kode outlet dari SharedPreferences (HARUS SAMA DENGAN LOGIN!)
        val sharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userOutletKode = sharedPreferences.getString("OUTLET_KODE", null)

        Log.d(TAG, "===== FILTER DEBUG =====")
        Log.d(TAG, "User Outlet Kode: $userOutletKode")
        Log.d(TAG, "Total orders sebelum filter: ${list.size}")

        // Filter list berdasarkan kode outlet
        // Filter list berdasarkan kode outlet DAN status aman
        val filteredList = if (!userOutletKode.isNullOrEmpty()) {
            list.filter { orderWithDetails ->
                val orderOutlet = orderWithDetails.order.kode_outlet
                val orderStatus = orderWithDetails.order.status  // Tambahkan ini
                Log.d(TAG, "Order ID: ${orderWithDetails.order.idorder}, Outlet: $orderOutlet, Status: $orderStatus, Match: ${orderOutlet == userOutletKode && orderStatus == "aman"}")
                orderOutlet == userOutletKode && orderStatus == "aman"  // Ubah baris ini
            }
        } else {
            Log.d(TAG, "User outlet kode kosong, filter hanya status aman")
            list.filter { it.order.status == "aman" }  // Ubah baris ini
        }

        Log.d(TAG, "Total orders setelah filter: ${filteredList.size}")
        Log.d(TAG, "========================")

        // Submit ke adapter
        submitList(filteredList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_riwayat, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position), onOrderClick)
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPelanggan: TextView = itemView.findViewById(R.id.tvpelannggan)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)
        private val tvDetailTransaksi: TextView = itemView.findViewById(R.id.tvDetailTransaksi)
        private val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }

        fun bind(orderWithDetails: OrderWithDetails, onClick: (OrderWithDetails) -> Unit) {
            val order = orderWithDetails.order

            // Set nama pelanggan
            tvPelanggan.text = order.namapelanggan

            // Set total harga
            tvTotalPrice.text = formatCurrency(order.grandtotal)

            // Set detail transaksi (Jam | Kasir: Username)
            val detailText = "${order.jam} | Kasir: ${order.username ?: "Unknown"}"
            tvDetailTransaksi.text = detailText

            // Set tanggal dengan format lebih readable
            tvTanggal.text = formatTanggal(order.tanggal)
            tvStatus.text = "Selesai"

            val context = itemView.context

            try {
                // Background
                tvStatus.setBackgroundResource(R.drawable.bg_status_selesai)

                // Text Color
                val statusTextColor = ContextCompat.getColor(context, R.color.status_active)
                tvStatus.setTextColor(statusTextColor)

                // Drawable Start (Icon)
                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_status_selesai)
                tvStatus.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

            } catch (e: Exception) {
                // Fallback jika resource tidak ditemukan
                tvStatus.setBackgroundColor(Color.parseColor("#00695C"))
                tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }

            // Handle click
            itemView.setOnClickListener {
                onClick(orderWithDetails)
            }
        }

        private fun formatCurrency(amount: Double): String {
            return "Rp ${String.format("%,.0f", amount).replace(",", ".")}"
        }

        private fun formatTanggal(tanggal: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                val date = inputFormat.parse(tanggal)
                date?.let { outputFormat.format(it) } ?: tanggal
            } catch (e: Exception) {
                tanggal
            }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<OrderWithDetails>() {
        override fun areItemsTheSame(
            oldItem: OrderWithDetails,
            newItem: OrderWithDetails
        ): Boolean {
            return oldItem.order.idorder == newItem.order.idorder
        }

        override fun areContentsTheSame(
            oldItem: OrderWithDetails,
            newItem: OrderWithDetails
        ): Boolean {
            return oldItem == newItem
        }
    }
}