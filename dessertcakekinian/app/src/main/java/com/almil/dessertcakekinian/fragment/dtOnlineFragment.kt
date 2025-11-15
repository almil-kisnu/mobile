package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.OrderDetailAdapter
import com.almil.dessertcakekinian.model.OrderWithDetails
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.postgrest
import android.util.Log

class dtOnlineFragment : DialogFragment() {

    companion object {
        private const val ARG_ORDER_DATA = "order_data"
        private const val TAG = "dtOnlineFragment"

        @JvmStatic
        fun newInstance(orderWithDetails: OrderWithDetails) =
            dtOnlineFragment().apply {
                arguments = Bundle().apply {
                    val json = Json.encodeToString(orderWithDetails)
                    putString(ARG_ORDER_DATA, json)
                }
            }
    }

    private lateinit var btnBack: ImageButton
    private lateinit var rvProdukPembayaran: RecyclerView
    private lateinit var tvWaktuTransaksi: TextView
    private lateinit var tvMetodePembayaran: TextView
    private lateinit var tvTotalTagihan: TextView
    private lateinit var tvUangDibayar: TextView
    private lateinit var tvKembalian: TextView
    private lateinit var btnDownload: Button
    private lateinit var btnPrint: Button

    private lateinit var orderDetailAdapter: OrderDetailAdapter
    private var orderWithDetails: OrderWithDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ambil data dari arguments
        arguments?.let {
            val orderJson = it.getString(ARG_ORDER_DATA)
            orderJson?.let { json ->
                try {
                    orderWithDetails = Json.decodeFromString<OrderWithDetails>(json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dt_online, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        displayOrderData()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        rvProdukPembayaran = view.findViewById(R.id.rvProdukPembayaran)
        tvWaktuTransaksi = view.findViewById(R.id.tvWaktuTransaksi)
        tvMetodePembayaran = view.findViewById(R.id.tvMetodePembayaran)
        tvTotalTagihan = view.findViewById(R.id.tvTotalTagihan)
        tvUangDibayar = view.findViewById(R.id.tvUangDibayar)
        tvKembalian = view.findViewById(R.id.tvKembalian)
        btnDownload = view.findViewById(R.id.btnDownload)
        btnPrint = view.findViewById(R.id.btnPrint)
    }

    private fun setupRecyclerView() {
        orderDetailAdapter = OrderDetailAdapter()
        rvProdukPembayaran.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderDetailAdapter
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            dismiss()
        }

        btnDownload.setOnClickListener {
            // Update status ke "aman" dulu, lalu kirim ke WhatsApp
            updateStatusToAman {
                shareToWhatsApp()
            }
        }

        btnPrint.setOnClickListener {
            // Update status ke "aman" dulu, lalu print
            updateStatusToAman {
                printReceipt()
            }
        }
    }

    private fun updateStatusToAman(onSuccess: () -> Unit) {
        orderWithDetails?.let { data ->
            val orderId = data.order.idorder

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Direct Postgrest query ke Supabase
                    SupabaseClientProvider.client.postgrest["orders"]
                        .update(
                            update = {
                                set("status", "aman")
                            }
                        ) {
                            filter {
                                eq("idorder", orderId)
                            }
                        }

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Status order $orderId berhasil diubah ke 'aman'")
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Status berhasil diperbarui",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        // Update data lokal
                        orderWithDetails = data.copy(
                            order = data.order.copy(status = "aman")
                        )

                        // Jalankan callback
                        onSuccess()

                        // Tutup dialog setelah sukses
                        dismiss()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error update status: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Terjadi kesalahan: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun displayOrderData() {
        orderWithDetails?.let { data ->
            val order = data.order
            val details = data.details

            // Display detail produk di RecyclerView
            orderDetailAdapter.submitList(details)

            // Display waktu transaksi (Tanggal + Jam)
            val waktuTransaksi = formatWaktuTransaksi(order.tanggal, order.jam)
            tvWaktuTransaksi.text = waktuTransaksi

            // Display metode pembayaran
            tvMetodePembayaran.text = order.metode_pembayaran

            // Display total tagihan
            tvTotalTagihan.text = formatCurrency(order.grandtotal)

            // Display uang dibayar
            tvUangDibayar.text = formatCurrency(order.bayar)

            // Display kembalian
            tvKembalian.text = formatCurrency(order.kembalian)
        }
    }

    private fun formatWaktuTransaksi(tanggal: String, jam: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(tanggal)
            val formattedDate = date?.let { outputFormat.format(it) } ?: tanggal
            "$formattedDate, $jam"
        } catch (e: Exception) {
            "$tanggal, $jam"
        }
    }

    private fun formatCurrency(amount: Double): String {
        return "Rp ${String.format("%,.0f", amount).replace(",", ".")}"
    }

    private fun shareToWhatsApp() {
        orderWithDetails?.let { data ->
            val order = data.order
            val details = data.details

            // Build struk text
            val strukText = buildString {
                appendLine("═══════════════════════════")
                appendLine("        STRUK PEMBELIAN")
                appendLine("═══════════════════════════")
                appendLine()
                appendLine("Pelanggan: ${order.namapelanggan}")
                appendLine("Tanggal: ${order.tanggal}")
                appendLine("Jam: ${order.jam}")
                appendLine("Kasir: ${order.username ?: "Unknown"}")
                appendLine("Outlet: ${order.kode_outlet ?: "-"}")
                appendLine()
                appendLine("═══════════════════════════")
                appendLine("DETAIL PEMBELIAN:")
                appendLine("═══════════════════════════")

                details.forEachIndexed { index, detail ->
                    appendLine()
                    appendLine("${index + 1}. ${detail.namaproduk}")
                    appendLine("   ${detail.jumlah} x ${formatCurrency(detail.harga)}")
                    appendLine("   Subtotal: ${formatCurrency(detail.subtotal)}")
                }

                appendLine()
                appendLine("═══════════════════════════")
                appendLine("RINGKASAN PEMBAYARAN:")
                appendLine("═══════════════════════════")
                appendLine("Total Tagihan: ${formatCurrency(order.grandtotal)}")
                appendLine("Metode: ${order.metode_pembayaran}")
                appendLine("Bayar: ${formatCurrency(order.bayar)}")
                appendLine("Kembalian: ${formatCurrency(order.kembalian)}")
                appendLine()
                appendLine("═══════════════════════════")
                appendLine("   Terima kasih atas")
                appendLine("   kunjungan Anda!")
                appendLine("═══════════════════════════")
            }

            // Share via WhatsApp
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = "com.whatsapp"
                putExtra(android.content.Intent.EXTRA_TEXT, strukText)
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "WhatsApp tidak ditemukan",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun printReceipt() {
        android.widget.Toast.makeText(
            requireContext(),
            "Fitur print akan segera tersedia",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}