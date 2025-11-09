// File: fragment/transaksiFragment.kt
package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.OnTransaksiItemClickListener
import com.almil.dessertcakekinian.adapter.TransaksiAdapter
import com.almil.dessertcakekinian.dialog.HargaGrosirDialog
import com.almil.dessertcakekinian.model.ProductDataState
import com.almil.dessertcakekinian.model.ProductViewModel
import com.almil.dessertcakekinian.model.ProdukDetail
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class transaksiFragment : Fragment(), OnTransaksiItemClickListener {

    private val productViewModel: ProductViewModel by activityViewModels()
    private var barcodeScannerView: DecoratedBarcodeView? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var scanContainer: FrameLayout
    private lateinit var tabProduk: LinearLayout
    private lateinit var tabScan: LinearLayout
    private lateinit var produkIcon: ImageButton
    private lateinit var scanIcon: ImageButton
    private lateinit var indicatorProduk: View
    private lateinit var indicatorScan: View
    private lateinit var transaksiAdapter: TransaksiAdapter
    private var currentOutletId: Int = -1
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- INIT SharedPreferences ---
        sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        currentOutletId = sharedPreferences.getInt("USER_OUTLET_ID", -1)

        if (currentOutletId == -1) {
            Log.e("transaksiFragment", "ID Outlet tidak ditemukan!")
            return
        }

        // --- INIT VIEWS ---
        recyclerView = view.findViewById(R.id.recyclerView)
        scanContainer = view.findViewById(R.id.scanContainer)
        tabProduk = view.findViewById(R.id.tabProduk)
        tabScan = view.findViewById(R.id.tabScan)
        produkIcon = view.findViewById(R.id.produkIcon)
        scanIcon = view.findViewById(R.id.scanIcon)
        indicatorProduk = view.findViewById(R.id.indicatorProduk)
        indicatorScan = view.findViewById(R.id.indicatorScan)
        barcodeScannerView = view.findViewById(R.id.scanner)

        // --- SETUP RECYCLERVIEW (HANYA SEKALI!) ---
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        transaksiAdapter = TransaksiAdapter(emptyList(), currentOutletId, this)
        recyclerView.adapter = transaksiAdapter

        // --- OBSERVE DATA ---
        observeProductData()

        // --- SCANNER, TAB, BOTTOM SHEET ---
        setupScanner()
        setupTabSwitching()
        setupBottomSheet(view)
    }

    override fun onShowHargaGrosir(produkDetail: ProdukDetail) {
        HargaGrosirDialog.newInstance(produkDetail.hargaGrosir)
            .show(childFragmentManager, "HargaGrosirDialog")
    }

    override fun onTransaksiItemClicked(produkDetail: ProdukDetail) {
        Log.i("transaksiFragment", "Tambah: ${produkDetail.produk.namaproduk}")
    }

    private fun observeProductData() {
        lifecycleScope.launch {
            productViewModel.allProducts.collect { state ->
                when (state) {
                    is ProductDataState.Loading -> {}
                    is ProductDataState.Success -> {
                        val filtered = state.produkDetails.filter { produk ->
                            produk.detailStok.any { it.idoutlet == currentOutletId }
                        }
                        transaksiAdapter.updateData(filtered)
                    }
                    is ProductDataState.Error -> {
                        Log.e("transaksiFragment", "Error: ${state.message}")
                    }
                }
            }
        }
    }

    private fun setupScanner() {
        barcodeScannerView?.decodeContinuous { result: BarcodeResult? ->
            result?.text?.let { barcode ->
                Log.d("BarcodeScanner", "Scanned: $barcode")
                barcodeScannerView?.pause()
                barcodeScannerView?.postDelayed({ barcodeScannerView?.resume() }, 2000)
            }
        }
    }

    private fun setupTabSwitching() {
        val tabProdukClick = View.OnClickListener {
            recyclerView.visibility = View.VISIBLE
            scanContainer.visibility = View.GONE
            indicatorProduk.visibility = View.VISIBLE
            indicatorScan.visibility = View.GONE
            stopScanner()
        }
        val tabScanClick = View.OnClickListener {
            recyclerView.visibility = View.GONE
            scanContainer.visibility = View.VISIBLE
            indicatorProduk.visibility = View.GONE
            indicatorScan.visibility = View.VISIBLE
            startScanner()
        }

        tabProduk.setOnClickListener(tabProdukClick)
        produkIcon.setOnClickListener(tabProdukClick)
        tabScan.setOnClickListener(tabScanClick)
        scanIcon.setOnClickListener(tabScanClick)

        tabProdukClick.onClick(null) // default
    }

    private fun setupBottomSheet(rootView: View) {
        val bottomComponent = rootView.findViewById<LinearLayout>(R.id.permanentBottomComponent)
        val headerLayout = rootView.findViewById<LinearLayout>(R.id.headerLayout)
        val handleIndicator = rootView.findViewById<View>(R.id.handleIndicator)
        val dividerView = rootView.findViewById<View>(R.id.dividerView)

        val behavior = BottomSheetBehavior.from(bottomComponent)
        behavior.isHideable = false

        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (handleIndicator != null && headerLayout != null && dividerView != null) {
                    val handleMargin = handleIndicator.layoutParams as ViewGroup.MarginLayoutParams
                    val handleHeight = handleIndicator.height + handleMargin.topMargin
                    val headerHeight = headerLayout.height
                    val dividerMargin = dividerView.layoutParams as ViewGroup.MarginLayoutParams
                    val dividerHeight = dividerView.height + dividerMargin.topMargin
                    val peekHeight = handleHeight + headerHeight + dividerHeight + dpToPx(8)
                    behavior.peekHeight = peekHeight
                    val paddingBottom = peekHeight + dpToPx(16)
                    recyclerView.setPadding(0, 0, 0, paddingBottom)
                    scanContainer.setPadding(0, 0, 0, paddingBottom)
                }
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (scanContainer.visibility == View.VISIBLE) startScanner()
    }

    override fun onPause() {
        super.onPause()
        stopScanner()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        barcodeScannerView = null
    }

    private fun startScanner() = barcodeScannerView?.resume()
    private fun stopScanner() = barcodeScannerView?.pause()
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    companion object {
        @JvmStatic
        fun newInstance() = transaksiFragment()
    }
}