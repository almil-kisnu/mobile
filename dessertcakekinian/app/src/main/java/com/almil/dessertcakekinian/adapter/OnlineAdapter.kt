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

class OnlineAdapter(
    private val onOrderClick: (OrderWithDetails) -> Unit,
    private val onBadgeCountChanged: ((Int) -> Unit)? = null
) : ListAdapter<OrderWithDetails, OnlineAdapter.OrderViewHolder>(OrderDiffCallback()) {

    companion object {
        private const val TAG = "OnlineAdapter"
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

        // Filter list berdasarkan kode outlet DAN status BUKAN aman
        val filteredList = if (!userOutletKode.isNullOrEmpty()) {
            list.filter { orderWithDetails ->
                val orderOutlet = orderWithDetails.order.kode_outlet
                val orderStatus = orderWithDetails.order.status
                Log.d(TAG, "Order ID: ${orderWithDetails.order.idorder}, Outlet: $orderOutlet, Status: $orderStatus, Match: ${orderOutlet == userOutletKode && orderStatus != "aman"}")
                orderOutlet == userOutletKode && orderStatus != "aman"
            }
        } else {
            Log.d(TAG, "User outlet kode kosong, filter hanya status bukan aman")
            list.filter { it.order.status != "aman" }
        }

        Log.d(TAG, "Total orders setelah filter: ${filteredList.size}")
        Log.d(TAG, "========================")

        // Submit ke adapter
        submitList(filteredList)
        onBadgeCountChanged?.invoke(filteredList.size)
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
            tvStatus.text = "Pending"

            val context = itemView.context

            try {
                // Background
                tvStatus.setBackgroundResource(R.drawable.bg_status_selesai)

                // Text Color
                val statusTextColor = Color.parseColor("#C68A00")
                tvStatus.setTextColor(statusTextColor)

                // Drawable Start (Icon)
                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_status_pending)
                tvStatus.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

            } catch (e: Exception) {
                // Fallback jika resource tidak ditemukan
                tvStatus.setBackgroundColor(Color.parseColor("#FFF7D6"))
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