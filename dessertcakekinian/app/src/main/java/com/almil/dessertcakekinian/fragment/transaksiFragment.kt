package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.BarcodeResult

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param1"

class transaksiFragment : Fragment() {

    // Properti untuk mengontrol scanner
    private var barcodeScannerView: DecoratedBarcodeView? = null

    // --- Life Cycle Methods ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Menggunakan layout fragment_transaksi (berdasarkan nama file kode transaksi)
        return inflater.inflate(R.layout.fragment_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dapatkan referensi View Utama
        val tabProduk = view.findViewById<LinearLayout>(R.id.tabProduk)
        val tabScan = view.findViewById<LinearLayout>(R.id.tabScan)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val scanContainer = view.findViewById<FrameLayout>(R.id.scanContainer)

        // Dapatkan ImageButton dan View indikator
        val produkIcon = view.findViewById<ImageButton>(R.id.produkIcon)
        val scanIcon = view.findViewById<ImageButton>(R.id.scanIcon)
        val indicatorProduk = view.findViewById<View>(R.id.indicatorProduk)
        val indicatorScan = view.findViewById<View>(R.id.indicatorScan)

        // --- INISIALISASI BARCODE SCANNER ---
        barcodeScannerView = view.findViewById(R.id.scanner)

        // Atur Continuous Scan Listener
        barcodeScannerView?.decodeContinuous { result: BarcodeResult? ->
            result?.text?.let { barcode ->
                // TODO: Logika saat barcode berhasil dipindai
                Log.d("BarcodeScanner", "Barcode Scanned: $barcode")

                // Jeda sebentar setelah pemindaian agar tidak memindai berulang
                barcodeScannerView?.pause()

                // Lakukan aksi (misalnya, tambahkan produk ke keranjang)
                // Setelah selesai, Anda dapat memanggil barcodeScannerView?.resume() lagi jika perlu
            }
        }

        // Tentukan status awal scanner (default ke tab Produk, jadi scanner harus berhenti)
        if (indicatorProduk.visibility == View.VISIBLE) {
            stopScanner()
        } else {
            // Jika defaultnya Scan, maka mulai
            startScanner()
        }

        // --- LOGIKA TAB SWITCHING (Termasuk ImageButton) ---

        // Listener untuk Tab Produk
        val tabProdukClickListener = View.OnClickListener {
            // 1. Ganti Content
            recyclerView.visibility = View.VISIBLE
            scanContainer.visibility = View.GONE

            // 2. Ganti Indikator Tab
            indicatorProduk.visibility = View.VISIBLE
            indicatorScan.visibility = View.GONE

            // 3. Kontrol Scanner
            stopScanner()
        }

        // Listener untuk Tab Scan
        val tabScanClickListener = View.OnClickListener {
            // 1. Ganti Content
            recyclerView.visibility = View.GONE
            scanContainer.visibility = View.VISIBLE

            // 2. Ganti Indikator Tab
            indicatorProduk.visibility = View.GONE
            indicatorScan.visibility = View.VISIBLE

            // 3. Kontrol Scanner
            startScanner()
        }

        // Terapkan listener ke semua View di tab Produk
        tabProduk.setOnClickListener(tabProdukClickListener)
        produkIcon.setOnClickListener(tabProdukClickListener)

        // Terapkan listener ke semua View di tab Scan
        tabScan.setOnClickListener(tabScanClickListener)
        scanIcon.setOnClickListener(tabScanClickListener)


        // --- LOGIKA BOTTOM SHEET (tetap sama) ---

        // Dapatkan referensi ke View Bottom Sheet permanen
        val bottomComponent = view.findViewById<LinearLayout>(R.id.permanentBottomComponent)
        val headerLayout = view.findViewById<LinearLayout>(R.id.headerLayout)
        val handleIndicator = view.findViewById<View>(R.id.handleIndicator)
        val dividerView = view.findViewById<View>(R.id.dividerView)

        // 1. Dapatkan BottomSheetBehavior
        val behavior = BottomSheetBehavior.from(bottomComponent)

        // 2. Setting permanen: TIDAK BISA DIHILANGKAN (dismissed)
        behavior.isHideable = false

        // 3. Hitung dan Atur Peek Height
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                if (handleIndicator != null && headerLayout != null && dividerView != null) {

                    val handleMarginParams = handleIndicator.layoutParams as ViewGroup.MarginLayoutParams
                    val handleHeight = handleIndicator.height + handleMarginParams.topMargin

                    val headerHeight = headerLayout.height

                    val dividerMarginParams = dividerView.layoutParams as ViewGroup.MarginLayoutParams
                    val dividerHeight = dividerView.height + dividerMarginParams.topMargin

                    val calculatedPeekHeight = handleHeight + headerHeight + dividerHeight

                    // Set peekHeight
                    val finalPeekHeight = calculatedPeekHeight + dpToPx(requireContext(), 8)
                    behavior.peekHeight = finalPeekHeight

                    // Atur padding bottom RecyclerView agar konten tidak tertutup
                    recyclerView.setPadding(
                        recyclerView.paddingLeft,
                        recyclerView.paddingTop,
                        recyclerView.paddingRight,
                        finalPeekHeight + dpToPx(requireContext(), 16) // Tambah sedikit margin
                    )

                    // Atur padding bottom Scan Container juga
                    scanContainer.setPadding(
                        scanContainer.paddingLeft,
                        scanContainer.paddingTop,
                        scanContainer.paddingRight,
                        finalPeekHeight + dpToPx(requireContext(), 16) // Tambah sedikit margin
                    )
                }

                // 4. Atur state ke COLLAPSED (peek) saat pertama kali muncul
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        })
    }

    // --- Kontrol Kamera ---

    override fun onResume() {
        super.onResume()
        // Cek apakah scanContainer sedang terlihat sebelum memulai scanner
        if (view?.findViewById<FrameLayout>(R.id.scanContainer)?.visibility == View.VISIBLE) {
            startScanner()
        }
    }

    override fun onPause() {
        super.onPause()
        stopScanner()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Lepaskan referensi
        barcodeScannerView = null
    }

    private fun startScanner() {
        // Karena izin sudah diminta di MainActivity/startup, kita bisa langsung resume()
        barcodeScannerView?.resume()
    }

    private fun stopScanner() {
        // Menghentikan pratinjau kamera dan decoding
        barcodeScannerView?.pause()
    }

    // --- Utility Function ---

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    // --- Companion Object ---

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            transaksiFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}