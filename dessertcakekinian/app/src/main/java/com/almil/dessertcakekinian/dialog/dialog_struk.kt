package com.almil.dessertcakekinian.dialog

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.StrukAdapter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class dialog_struk : DialogFragment() {

    private lateinit var tvNamaToko: TextView
    private lateinit var tvAlamatToko: TextView
    private lateinit var tvTeleponToko: TextView
    private lateinit var tvTanggalDetail: TextView
    private lateinit var tvWaktuDetail: TextView
    private lateinit var tvKasirDetail: TextView
    private lateinit var rvDetailItem: RecyclerView
    private lateinit var tvTotalStruk: TextView
    private lateinit var tvBayarStruk: TextView
    private lateinit var tvKembalianStruk: TextView
    private lateinit var tvMetodePembayaran: TextView
    private lateinit var btnDownload: Button
    private lateinit var btnPrint: Button

    private var orderId: Int = 0
    private var cartItemsMap: Map<Int, com.almil.dessertcakekinian.model.CartItem> = emptyMap()
    private var grandTotal: String = ""
    private var bayar: String = ""
    private var kembalian: String = ""
    private var metodePembayaran: String = ""
    private var namaKasir: String = ""
    private var alamatOutlet: String = ""
    private var teleponOutlet: String = ""

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            generateAndSavePdf()
        } else {
            Toast.makeText(context, "Permission ditolak. Tidak dapat menyimpan file.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_struk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Views
        tvNamaToko = view.findViewById(R.id.tvNamaToko)
        tvAlamatToko = view.findViewById(R.id.tvAlamatToko)
        tvTeleponToko = view.findViewById(R.id.tvTeleponToko)
        tvTanggalDetail = view.findViewById(R.id.tvTanggalDetail)
        tvWaktuDetail = view.findViewById(R.id.tvWaktuDetail)
        tvKasirDetail = view.findViewById(R.id.tvKasirDetail)
        rvDetailItem = view.findViewById(R.id.rvDetailItem)
        tvTotalStruk = view.findViewById(R.id.tvTotalStruk)
        tvBayarStruk = view.findViewById(R.id.tvBayarStruk)
        tvKembalianStruk = view.findViewById(R.id.tvKembalianStruk)
        tvMetodePembayaran = view.findViewById(R.id.tvMetodePembayaran)
        btnDownload = view.findViewById(R.id.btnDownload)
        btnPrint = view.findViewById(R.id.btnPrint)

        // Ambil data dari arguments
        arguments?.let {
            orderId = it.getInt(ARG_ORDER_ID, 0)
            grandTotal = it.getString(ARG_GRAND_TOTAL, "")
            bayar = it.getString(ARG_BAYAR, "")
            kembalian = it.getString(ARG_KEMBALIAN, "")
            metodePembayaran = it.getString(ARG_METODE_PEMBAYARAN, "")
            namaKasir = it.getString(ARG_NAMA_KASIR, "")
            alamatOutlet = it.getString(ARG_ALAMAT_OUTLET, "Jl. Cendrawasih No. 45, Jakarta")
            teleponOutlet = it.getString(ARG_TELEPON_OUTLET, "0812-3456-7890")
        }

        setupStrukData()
        setupRecyclerView()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupStrukData() {
        tvAlamatToko.text = alamatOutlet
        tvTeleponToko.text = teleponOutlet

        val currentDate = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))

        tvTanggalDetail.text = dateFormat.format(currentDate)
        tvWaktuDetail.text = timeFormat.format(currentDate)
        tvKasirDetail.text = namaKasir

        tvTotalStruk.text = grandTotal
        tvBayarStruk.text = bayar
        tvKembalianStruk.text = kembalian
        tvMetodePembayaran.text = when(metodePembayaran) {
            "cash" -> "Tunai"
            "transfer" -> "Transfer/QRIS"
            else -> metodePembayaran
        }
    }

    private fun setupRecyclerView() {
        val strukAdapter = StrukAdapter(cartItemsMap)
        rvDetailItem.layoutManager = LinearLayoutManager(context)
        rvDetailItem.adapter = strukAdapter
    }

    private fun setupButtons() {
        btnDownload.setOnClickListener {
            checkPermissionAndDownload()
        }

        btnPrint.setOnClickListener {
            // TODO: Implementasi print struk
            dismiss()
            parentFragmentManager.popBackStack()
        }
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
                // Permission sudah diberikan
                generateAndSavePdf()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                // Show explanation
                Toast.makeText(
                    context,
                    "Permission dibutuhkan untuk menyimpan file PDF",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun generateAndSavePdf() {
        try {
            // Buat PDF Document
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            var yPos = 50f
            val leftMargin = 50f
            val rightMargin = 545f
            val pageWidth = 595f // Lebar total halaman A4

            // --- Header - Nama Toko (Diposisikan di Tengah) ---
            paint.textSize = 24f
            paint.color = Color.BLACK
            paint.isFakeBoldText = true

            val namaTokoText = "Sweetbake"
            val namaTokoWidth = paint.measureText(namaTokoText)
            val centerPosNamaToko = (pageWidth - namaTokoWidth) / 2
            canvas.drawText(namaTokoText, centerPosNamaToko, yPos, paint)
            yPos += 30f

            // --- Alamat dan Telepon (Diposisikan di Tengah) ---
            paint.textSize = 12f
            paint.isFakeBoldText = false
            paint.color = Color.GRAY

            // Alamat
            val alamatWidth = paint.measureText(alamatOutlet)
            val centerPosAlamat = (pageWidth - alamatWidth) / 2
            canvas.drawText(alamatOutlet, centerPosAlamat, yPos, paint)
            yPos += 20f

            // Telepon
            val teleponWidth = paint.measureText(teleponOutlet)
            val centerPosTelepon = (pageWidth - teleponWidth) / 2
            canvas.drawText(teleponOutlet, centerPosTelepon, yPos, paint)
            yPos += 30f

            // Garis pemisah
            drawDashedLine(canvas, leftMargin, yPos, rightMargin, yPos)
            yPos += 30f

            // Info Transaksi
            // ... (sisanya tetap Rata Kiri seperti sebelumnya)
            paint.color = Color.BLACK
            canvas.drawText("Tanggal: ${tvTanggalDetail.text}", leftMargin, yPos, paint)
            yPos += 20f
            canvas.drawText("Waktu: ${tvWaktuDetail.text}", leftMargin, yPos, paint)
            yPos += 20f
            canvas.drawText("Kasir: ${tvKasirDetail.text}", leftMargin, yPos, paint)
            yPos += 30f

            // Garis pemisah
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
            cartItemsMap.values.forEach { item ->
                val namaProduk = item.produkDetail.produk.namaproduk
                val itemName = if (namaProduk.length > 20) {
                    namaProduk.substring(0, 20) + "..."
                } else {
                    namaProduk
                }

                val hargaSatuan = item.hargaSatuan.toInt()
                val hargaTotal = (item.quantity * item.hargaSatuan).toInt()

                canvas.drawText(itemName, leftMargin, yPos, paint)
                canvas.drawText(item.quantity.toString(), leftMargin + 250f, yPos, paint)
                canvas.drawText(formatRupiah(hargaSatuan), leftMargin + 320f, yPos, paint)
                canvas.drawText(formatRupiah(hargaTotal), leftMargin + 420f, yPos, paint)
                yPos += 25f
            }

            yPos += 10f
            drawDashedLine(canvas, leftMargin, yPos, rightMargin, yPos)
            yPos += 30f

            // Total, Bayar, Kembalian
            paint.isFakeBoldText = true
            canvas.drawText("Total:", leftMargin, yPos, paint)

            // Posisikan nilai total (kanan)
            val totalValueWidth = paint.measureText(grandTotal)
            canvas.drawText(grandTotal, rightMargin - totalValueWidth, yPos, paint)
            yPos += 25f

            canvas.drawText("Bayar:", leftMargin, yPos, paint)
            val bayarValueWidth = paint.measureText(bayar)
            canvas.drawText(bayar, rightMargin - bayarValueWidth, yPos, paint)
            yPos += 25f

            canvas.drawText("Kembalian:", leftMargin, yPos, paint)
            val kembalianValueWidth = paint.measureText(kembalian)
            canvas.drawText(kembalian, rightMargin - kembalianValueWidth, yPos, paint)
            yPos += 30f

            // Metode Pembayaran
            paint.isFakeBoldText = false
            canvas.drawText("Metode Pembayaran:", leftMargin, yPos, paint)
            val metodeValueWidth = paint.measureText(tvMetodePembayaran.text.toString())
            canvas.drawText(tvMetodePembayaran.text.toString(), rightMargin - metodeValueWidth, yPos, paint)
            yPos += 40f

            drawDashedLine(canvas, leftMargin, yPos, rightMargin, yPos)
            yPos += 30f

            // Footer (Tetap di tengah)
            paint.textSize = 12f
            paint.color = Color.GRAY
            val footerText = "Terima kasih telah berbelanja!"
            val footerWidth = paint.measureText(footerText)
            canvas.drawText(footerText, (pageWidth - footerWidth) / 2, yPos, paint)

            pdfDocument.finishPage(page)

            // Simpan PDF
            savePdfToStorage(pdfDocument)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePdfToStorage(pdfDocument: PdfDocument) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Struk_${orderId}_${timestamp}.pdf"

            val savedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 ke atas - gunakan MediaStore
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
                // Android 9 ke bawah
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
                Toast.makeText(
                    context,
                    "✓ Struk berhasil disimpan!\nMengirim lewat WhatsApp...",
                    Toast.LENGTH_LONG
                ).show()

                android.util.Log.d("PDF_DOWNLOAD", "Saving to: $fileName")
                android.util.Log.d("PDF_DOWNLOAD", "Uri: $savedUri")

                try {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, savedUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        // Jika ingin langsung ke WhatsApp
                        setPackage("com.whatsapp")
                    }
                    context?.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membuka WhatsApp", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }

            } else {
                Toast.makeText(context, "Gagal menyimpan PDF", Toast.LENGTH_SHORT).show()
            }



            dismiss()
            parentFragmentManager.popBackStack()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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

    fun setCartItems(items: Map<Int, com.almil.dessertcakekinian.model.CartItem>) {
        cartItemsMap = items
    }

    companion object {
        private const val ARG_ORDER_ID = "order_id"
        private const val ARG_GRAND_TOTAL = "grand_total"
        private const val ARG_BAYAR = "bayar"
        private const val ARG_KEMBALIAN = "kembalian"
        private const val ARG_METODE_PEMBAYARAN = "metode_pembayaran"
        private const val ARG_NAMA_KASIR = "nama_kasir"
        private const val ARG_ALAMAT_OUTLET = "alamat_outlet"
        private const val ARG_TELEPON_OUTLET = "telepon_outlet"

        fun newInstance(
            orderId: Int,
            grandTotal: String,
            bayar: String,
            kembalian: String,
            metodePembayaran: String,
            namaKasir: String,
            alamatOutlet: String = "Jl. Cendrawasih No. 45, Jakarta",
            teleponOutlet: String = "0812-3456-7890",
            cartItems: Map<Int, com.almil.dessertcakekinian.model.CartItem>
        ): dialog_struk {
            return dialog_struk().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ORDER_ID, orderId)
                    putString(ARG_GRAND_TOTAL, grandTotal)
                    putString(ARG_BAYAR, bayar)
                    putString(ARG_KEMBALIAN, kembalian)
                    putString(ARG_METODE_PEMBAYARAN, metodePembayaran)
                    putString(ARG_NAMA_KASIR, namaKasir)
                    putString(ARG_ALAMAT_OUTLET, alamatOutlet)
                    putString(ARG_TELEPON_OUTLET, teleponOutlet)
                }
                setCartItems(cartItems)
            }
        }
    }
}