package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.TransferStock
import com.almil.dessertcakekinian.model.Penggunakeseluruhan
import com.almil.dessertcakekinian.model.outletkeseluruhan
import java.text.SimpleDateFormat
import java.util.*
import android.content.res.ColorStateList
import android.graphics.Color

class TransferAdapter(
    private val penggunaList: List<Penggunakeseluruhan>,
    private val outletList: List<outletkeseluruhan>,
    private val onItemClick: (TransferStock) -> Unit
) : ListAdapter<TransferStock, TransferAdapter.TransferViewHolder>(TransferDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_request_stok, parent, false)
        return TransferViewHolder(view, penggunaList, outletList, onItemClick)
    }

    override fun onBindViewHolder(holder: TransferViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransferViewHolder(
        itemView: View,
        private val penggunaList: List<Penggunakeseluruhan>,
        private val outletList: List<outletkeseluruhan>,
        private val onItemClick: (TransferStock) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvPeminta: TextView = itemView.findViewById(R.id.tvPeminta)
        private val tvPengirim: TextView = itemView.findViewById(R.id.tvPengirim)
        private val tvCatatanKirim: TextView = itemView.findViewById(R.id.tvCatatanKirim)
        private val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(transfer: TransferStock) {
            // Set onClick
            itemView.setOnClickListener {
                onItemClick(transfer)
            }

            // Ambil data penerima (peminta)
            val penerima = transfer.iduserPeminta?.let { id ->
                penggunaList.find { it.iduser == id }
            }
            val outletTujuan = outletList.find { it.idoutlet == transfer.idoutletTujuan }

            // Ambil data pengirim
            val pengirim = penggunaList.find { it.iduser == transfer.iduserPengirim }
            val outletAsal = outletList.find { it.idoutlet == transfer.idoutletAsal }

            // Set Peminta (Penerima)
            val pemintaText = if (penerima != null && outletTujuan != null) {
                "${penerima.username} (${penerima.role ?: "Staff"}) - ${outletTujuan.kodeOutlet}"
            } else if (outletTujuan != null) {
                "Belum ditentukan - ${outletTujuan.kodeOutlet}"
            } else {
                "Data tidak lengkap"
            }
            tvPeminta.text = pemintaText

            // Set Pengirim
            val pengirimText = if (pengirim != null && outletAsal != null) {
                "${pengirim.username} (${pengirim.role ?: "Staff"}) - ${outletAsal.kodeOutlet}"
            } else {
                "Data tidak lengkap"
            }
            tvPengirim.text = pengirimText

            // Set Catatan
            tvCatatanKirim.text = transfer.catatan ?: "-"

            // Set Tanggal berdasarkan status
            when (transfer.status.lowercase()) {
                "diterima" -> {
                    // Tampilkan 2 tanggal (tanggal transfer - tanggal terima)
                    val tanggalTransferFormatted = formatTanggal(transfer.tanggalTransfer)
                    val tanggalTerimaFormatted = formatTanggal(transfer.tanggalTerima)
                    tvTanggal.text = "$tanggalTransferFormatted - $tanggalTerimaFormatted"
                }
                else -> {
                    // pending, dikirim, dibatalkan -> hanya tanggal transfer
                    tvTanggal.text = formatTanggal(transfer.tanggalTransfer)
                }
            }

            // Set Status dengan background dan warna
            tvStatus.text = transfer.status.capitalize(Locale.getDefault())
            setStatusAppearance(transfer.status)
        }

        private fun setStatusAppearance(status: String) {
            val context = itemView.context
            when (status.lowercase()) {
                "pending" -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_status_selesai)
                    tvStatus.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#FFC107"))
                    tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                "dikirim" -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_status_selesai)
                    tvStatus.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#2196F3"))
                    tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                "diterima" -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_status_selesai)
                    tvStatus.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                    tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                "dibatalkan" -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_status_selesai)
                    tvStatus.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#F44336"))
                    tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }
        }

        private fun formatTanggal(tanggal: String?): String {
            if (tanggal.isNullOrEmpty()) return "-"

            return try {
                // Format input dari database (ISO 8601)
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

                val date = inputFormat.parse(tanggal)
                date?.let { outputFormat.format(it) } ?: tanggal
            } catch (e: Exception) {
                // Jika format berbeda, coba format lain
                try {
                    val inputFormat2 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

                    val date = inputFormat2.parse(tanggal)
                    date?.let { outputFormat.format(it) } ?: tanggal
                } catch (e2: Exception) {
                    tanggal
                }
            }
        }
    }

    class TransferDiffCallback : DiffUtil.ItemCallback<TransferStock>() {
        override fun areItemsTheSame(oldItem: TransferStock, newItem: TransferStock): Boolean {
            return oldItem.idtransfer == newItem.idtransfer
        }

        override fun areContentsTheSame(oldItem: TransferStock, newItem: TransferStock): Boolean {
            return oldItem == newItem
        }
    }
}