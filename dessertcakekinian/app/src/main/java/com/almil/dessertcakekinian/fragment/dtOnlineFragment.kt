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
// TAMBAHKAN di bagian import
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import android.content.Context

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
    private lateinit var tvStatusPesanan: TextView
    private lateinit var tvNoTelepon: TextView
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

        val displayMetrics = resources.displayMetrics
        val maxHeight = (displayMetrics.heightPixels * 0.7).toInt() // 80% tinggi layar

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            maxHeight
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
        tvStatusPesanan = view.findViewById(R.id.tvStatusPesanan)
        tvNoTelepon = view.findViewById(R.id.tvNoTelepon)
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

    // TAMBAHKAN setelah deklarasi variabel orderWithDetails
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            generateAndSavePdf()
        } else {
            android.widget.Toast.makeText(
                requireContext(),
                "Permission ditolak. Tidak dapat menyimpan file.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateStatusToAman(onSuccess: () -> Unit) {
        orderWithDetails?.let { data ->
            val orderId = data.order.idorder

            // Ambil user ID dari SharedPreferences
            val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val currentUserId = sharedPreferences.getInt("USER_ID", -1)

            if (currentUserId == -1) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error: User session tidak ditemukan",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Waktu sekarang dalam format timestamp
                    val currentTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date())

                    // Direct Postgrest query ke Supabase
                    SupabaseClientProvider.client.postgrest["orders"]
                        .update(
                            update = {
                                set("status", "aman")
                                set("idkasir", currentUserId)
                                set("tanggalorder", currentTimestamp)
                            }
                        ) {
                            filter {
                                eq("idorder", orderId)
                            }
                        }

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Status order $orderId berhasil diubah ke 'aman'")
                        Log.d(TAG, "Kasir diupdate: $currentUserId")
                        Log.d(TAG, "Tanggal order diupdate: $currentTimestamp")

                        android.widget.Toast.makeText(
                            requireContext(),
                            "Status berhasil diperbarui",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        // Update data lokal
                        orderWithDetails = data.copy(
                            order = data.order.copy(
                                status = "aman",
                                idkasir = currentUserId,
                                tanggalorder = currentTimestamp
                            )
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

            tvStatusPesanan.text = order.status

            // TAMBAHKAN: Set No Telepon (dengan handling jika null/kosong)
            tvNoTelepon.text = if (!order.notelp.isNullOrEmpty()) {
                order.notelp
            } else {
                "-"
            }

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

    // HAPUS method shareToWhatsApp() lama
// GANTI dengan 3 method ini:

    private fun shareToWhatsApp() {
        checkPermissionAndDownload()
    }

    private fun checkPermissionAndDownload() {
        // Android 10+ tidak perlu permission untuk MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            generateAndSavePdf()
            return
        }

        // Android 9 ke bawah perlu WRITE_EXTERNAL_STORAGE
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                generateAndSavePdf()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Permission dibutuhkan untuk menyimpan file PDF",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun generateAndSavePdf() {
        orderWithDetails?.let { data ->
            val order = data.order
            val details = data.details

            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()

                var yPos = 50f
                val leftMargin = 50f
                val rightMargin = 545f
                val pageWidth = 595f

                // Ambil data outlet dari SharedPreferences
                val sharedPreferences = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                val alamatOutlet = sharedPreferences.getString("OUTLET_ALAMAT", "Alamat tidak tersedia") ?: "Alamat tidak tersedia"
                val teleponOutlet = sharedPreferences.getString("OUTLET_TELEPON", "Telepon tidak tersedia") ?: "Telepon tidak tersedia"

                // Header - Nama Toko (Tengah)
                paint.textSize = 24f
                paint.color = Color.BLACK
                paint.isFakeBoldText = true
                val namaTokoText = "Sweetbake"
                val namaTokoWidth = paint.measureText(namaTokoText)
                canvas.drawText(namaTokoText, (pageWidth - namaTokoWidth) / 2, yPos, paint)
                yPos += 30f

                // Alamat (Tengah)
                paint.textSize = 12f
                paint.isFakeBoldText = false
                paint.color = Color.GRAY
                val alamatWidth = paint.measureText(alamatOutlet)
                canvas.drawText(alamatOutlet, (pageWidth - alamatWidth) / 2, yPos, paint)
                yPos += 20f

                // Telepon (Tengah)
                val teleponWidth = paint.measureText(teleponOutlet)
                canvas.drawText(teleponOutlet, (pageWidth - teleponWidth) / 2, yPos, paint)
                yPos += 30f

                // Garis pemisah
                drawDashedLine(canvas, leftMargin, yPos, rightMargin, yPos)
                yPos += 30f

                // Info Transaksi
                paint.color = Color.BLACK
                paint.textSize = 12f
                canvas.drawText("Tanggal: ${order.tanggal}", leftMargin, yPos, paint)
                yPos += 20f
                canvas.drawText("Waktu: ${order.jam}", leftMargin, yPos, paint)
                yPos += 20f
                canvas.drawText("Kasir: ${order.username ?: "Unknown"}", leftMargin, yPos, paint)
                yPos += 20f
                canvas.drawText("Pelanggan: ${order.namapelanggan}", leftMargin, yPos, paint)
                yPos += 30f

                drawDashedLine(canvas, leftMargin, yPos, rightMargin, yPos)
                yPos += 30f

                // Daftar Belanja
                paint.textSize = 14f
                paint.isFakeBoldText = true
                canvas.drawText("Daftar Belanja", leftMargin, yPos, paint)
                yPos += 30f

                paint.textSize = 12f
                paint.isFakeBoldText = false

                // Header tabel
                canvas.drawText("Item", leftMargin, yPos, paint)
                canvas.drawText("Qty", leftMargin + 250f, yPos, paint)
                canvas.drawText("Harga", leftMargin + 320f, yPos, paint)
                canvas.drawText("Total", leftMargin + 420f, yPos, paint)
                yPos += 25f

                // Items
                details.forEach { detail ->
                    val itemName = if (detail.namaproduk.length > 20) {
                        detail.namaproduk.substring(0, 20) + "..."
                    } else {
                        detail.namaproduk
                    }

                    canvas.drawText(itemName, leftMargin, yPos, paint)
                    canvas.drawText(detail.jumlah.toString(), leftMargin + 250f, yPos, paint)
                    canvas.drawText(formatRupiah(detail.harga.toInt()), leftMargin + 320f, yPos, paint)
                    canvas.drawText(formatRupiah(detail.subtotal.toInt()), leftMargin + 420f, yPos, paint)
                    yPos += 25f
                }

                yPos += 10f
                drawDashedLine(canvas, leftMargin, yPos, rightMargin, yPos)
                yPos += 30f

                // Total, Bayar, Kembalian
                paint.isFakeBoldText = true
                canvas.drawText("Total:", leftMargin, yPos, paint)
                val totalText = formatCurrency(order.grandtotal)
                canvas.drawText(totalText, rightMargin - paint.measureText(totalText), yPos, paint)
                yPos += 25f

                canvas.drawText("Bayar:", leftMargin, yPos, paint)
                val bayarText = formatCurrency(order.bayar)
                canvas.drawText(bayarText, rightMargin - paint.measureText(bayarText), yPos, paint)
                yPos += 25f

                canvas.drawText("Kembalian:", leftMargin, yPos, paint)
                val kembalianText = formatCurrency(order.kembalian)
                canvas.drawText(kembalianText, rightMargin - paint.measureText(kembalianText), yPos, paint)
                yPos += 30f

                // Metode Pembayaran
                paint.isFakeBoldText = false
                canvas.drawText("Metode Pembayaran:", leftMargin, yPos, paint)
                val metodeText = order.metode_pembayaran
                canvas.drawText(metodeText, rightMargin - paint.measureText(metodeText), yPos, paint)
                yPos += 40f

                drawDashedLine(canvas, leftMargin, yPos, rightMargin, yPos)
                yPos += 30f

                // Footer
                paint.color = Color.GRAY
                val footerText = "Terima kasih telah berbelanja!"
                canvas.drawText(footerText, (pageWidth - paint.measureText(footerText)) / 2, yPos, paint)

                pdfDocument.finishPage(page)

                // Simpan dan share
                savePdfToStorage(pdfDocument, order.idorder, order.notelp)

            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun savePdfToStorage(pdfDocument: PdfDocument, orderId: Int, phoneNumber: String?) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Struk_${orderId}_${timestamp}.pdf"

            val savedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                        pdfDocument.close()
                    }
                }
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                    pdfDocument.close()
                }
                Uri.fromFile(file)
            }

            if (savedUri != null) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "✓ Struk berhasil disimpan!\nMengirim lewat WhatsApp...",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                // Share ke WhatsApp
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, savedUri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setPackage("com.whatsapp")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Gagal membuka WhatsApp",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Gagal menyimpan PDF",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(
                requireContext(),
                "Error: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun drawDashedLine(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
        val paint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        var x = startX
        while (x < endX) {
            canvas.drawLine(x, startY, minOf(x + 10f, endX), startY, paint)
            x += 20f
        }
    }

    private fun formatRupiah(amount: Int): String {
        return "Rp ${String.format("%,d", amount).replace(',', '.')}"
    }

    private fun printReceipt() {
        android.widget.Toast.makeText(
            requireContext(),
            "Fitur print akan segera tersedia",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}