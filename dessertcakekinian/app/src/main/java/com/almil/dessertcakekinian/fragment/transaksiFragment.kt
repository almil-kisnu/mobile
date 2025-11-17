// File: transaksiFragment.kt (Kode 1 - Logic Scanner Sudah Benar)
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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.OnTransaksiItemClickListener
import com.almil.dessertcakekinian.adapter.TransaksiAdapter
import com.almil.dessertcakekinian.adapter.CartAdapter
import com.almil.dessertcakekinian.dialog.HargaGrosirDialog
import com.almil.dessertcakekinian.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import android.widget.EditText
import android.text.TextWatcher
import android.text.Editable
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

class transaksiFragment : Fragment(), OnTransaksiItemClickListener {

    private lateinit var searchEditText: EditText // BARU: Input pencarian
    private var allProductList: List<ProdukDetail> = emptyList()
    private val productViewModel: ProductViewModel by activityViewModels()
    private val cartViewModel: CartViewModel by activityViewModels()
    private var barcodeScannerView: DecoratedBarcodeView? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewCart: RecyclerView
    private lateinit var scanContainer: FrameLayout
    private lateinit var tabProduk: LinearLayout
    private lateinit var tabScan: LinearLayout
    private lateinit var produkIcon: ImageButton
    private lateinit var scanIcon: ImageButton
    private lateinit var indicatorProduk: View
    private lateinit var indicatorScan: View
    private lateinit var btnBeli: TextView
    private lateinit var backButton: ImageButton
    private lateinit var tvTotalPrice: TextView
    private lateinit var transaksiAdapter: TransaksiAdapter
    private lateinit var cartAdapter: CartAdapter
    private var currentOutletId: Int = -1
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tvBadgeNotif: TextView
    private lateinit var tvKosongkanKeranjang: TextView

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        currentOutletId = sharedPreferences.getInt("USER_OUTLET_ID", -1)

        if (currentOutletId == -1) {
            Log.e("transaksiFragment", "ID Outlet tidak ditemukan!")
            return
        }

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerViewCart = view.findViewById(R.id.recyclerViewCart)
        scanContainer = view.findViewById(R.id.scanContainer)
        tabProduk = view.findViewById(R.id.tabProduk)
        tabScan = view.findViewById(R.id.tabScan)
        produkIcon = view.findViewById(R.id.produkIcon)
        scanIcon = view.findViewById(R.id.scanIcon)
        indicatorProduk = view.findViewById(R.id.indicatorProduk)
        indicatorScan = view.findViewById(R.id.indicatorScan)
        barcodeScannerView = view.findViewById(R.id.scanner)
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice)
        btnBeli = view.findViewById(R.id.btnBeli)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        transaksiAdapter = TransaksiAdapter(emptyList(), currentOutletId, this, cartViewModel.getCartQuantitiesMap())
        recyclerView.adapter = transaksiAdapter
        backButton = view.findViewById(R.id.btnBackTransaksi)

        recyclerViewCart.layoutManager = LinearLayoutManager(requireContext())
        cartAdapter = CartAdapter(requireContext(), cartViewModel.cartItems.value ?: emptyMap(), cartViewModel)
        recyclerViewCart.adapter = cartAdapter
        tvBadgeNotif = view.findViewById(R.id.tvBadgeNotif)
        tvKosongkanKeranjang = view.findViewById(R.id.tvKosongkanKeranjang)

        updateNotificationBadge(0)


        observeProductData()
        updateTotalPriceUI()

        cartViewModel.cartItems.observe(viewLifecycleOwner) { newCartItemsMap ->
            updateTotalPriceUI()
            recyclerViewCart.post {
                cartAdapter.updateData(newCartItemsMap)
            }
            transaksiAdapter.updateCartQuantities(cartViewModel.getCartQuantitiesMap())
        }


        searchEditText = view.findViewById(R.id.searchEditText)



        setupScanner()
        setupTabSwitching()
        setupBottomSheet(view)
        setupPaymentButton()
        setupSearch()
    }


    override fun onResume() {
        super.onResume()

        // Refresh badge count setiap kali fragment muncul
        val sharedPref = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val count = sharedPref.getInt("pending_order_count", 0)
        updateNotificationBadge(count)

        Log.d("transaksiFragment", "Badge count on resume: $count")
    }


    private fun updateNotificationBadge(count: Int) {
        if (count > 0) {
            tvBadgeNotif.visibility = View.VISIBLE
            tvBadgeNotif.text = if (count > 99) "99+" else count.toString()
        } else {
            tvBadgeNotif.visibility = View.GONE
        }
    }
    private fun setupPaymentButton() {
        btnBeli.setOnClickListener {
            val total = calculateTotalPrice()
            if (total <= 0) {
                return@setOnClickListener
            }

            val totalHargaText = tvTotalPrice.text.toString()
            Log.d("transaksiFragment", "Total Harga yang dikirim: $totalHargaText")
            val pembayaranFragment = PembayaranFragment.newInstance(totalHargaText)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, pembayaranFragment)
                .addToBackStack(null)
                .commit()
        }
        backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        tvKosongkanKeranjang.setOnClickListener {
            showClearCartDialog()
        }
    }
    private fun showClearCartDialog() {
        // Cek apakah keranjang kosong
        val totalQty = calculateTotalQuantity()
        if (totalQty == 0) {
            android.widget.Toast.makeText(
                requireContext(),
                "Keranjang sudah kosong",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Tampilkan dialog konfirmasi
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Kosongkan Keranjang")
            .setMessage("Apakah Anda yakin ingin mengosongkan semua produk di keranjang?")
            .setPositiveButton("Ya") { dialog, _ ->
                clearCart()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun clearCart() {
        // Clear semua item di cart menggunakan CartViewModel
        cartViewModel.clearCart()

        // Tampilkan toast konfirmasi
        android.widget.Toast.makeText(
            requireContext(),
            "Keranjang berhasil dikosongkan",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        // Update UI
        updateTotalPriceUI()

        Log.d("transaksiFragment", "Cart cleared successfully")
    }

    override fun onUpdateCartItem(produkDetail: ProdukDetail, quantity: Int) {
        val idproduk = produkDetail.produk.idproduk
        cartViewModel.updateCart(idproduk, produkDetail, quantity)

    }

    override fun onShowHargaGrosir(produkDetail: ProdukDetail) {
        HargaGrosirDialog.newInstance(produkDetail.hargaGrosir)
            .show(childFragmentManager, "HargaGrosirDialog")
    }

    private fun calculateTotalPrice(): Double {
        var total = 0.0
        val currentCart = cartViewModel.cartItems.value ?: emptyMap()

        for (item in currentCart.values) {
            val hargaJual = item.hargaSatuan
            val quantity = item.quantity
            total += hargaJual * quantity
        }
        return total
    }

    private fun updateTotalPriceUI() {
        val total = calculateTotalPrice()
        val totalQty = calculateTotalQuantity()
        tvTotalPrice.text = currencyFormat.format(total)

        val buttonText = if (totalQty > 0) {
            "BAYAR ($totalQty)"
        } else {
            "BAYAR (0)"
        }
        btnBeli.text = buttonText
        btnBeli.isEnabled = totalQty > 0
    }

    private fun observeProductData() {
        lifecycleScope.launch {
            productViewModel.allProducts.collect { state ->
                when (state) {
                    is ProductDataState.Loading -> {}
                    is ProductDataState.Success -> {
                        // BARU: Filter berdasarkan outlet dan simpan ke allProductList
                        allProductList = state.produkDetails.filter { produk ->
                            produk.detailStok.any { it.idoutlet == currentOutletId }
                        }
                        // BARU: Tampilkan daftar produk awal (tanpa filter search)
                        updateProductDisplay(allProductList)
                    }
                    is ProductDataState.Error -> {
                        Log.e("transaksiFragment", "Error: ${state.message}")
                    }
                }
            }
        }
    }
    // Tambahkan di akhir kelas transaksiFragment

    private fun updateProductDisplay(list: List<ProdukDetail>) {
        // Fungsi pembantu untuk memperbarui RecyclerView produk
        transaksiAdapter.updateData(list, cartViewModel.getCartQuantitiesMap())
    }

    private fun filterProducts(query: String) {
        if (query.isBlank()) {
            // Jika kueri kosong, tampilkan semua produk
            updateProductDisplay(allProductList)
            return
        }

        val lowerCaseQuery = query.lowercase(Locale.ROOT)

        val filteredList = allProductList.filter { productDetail ->

            val isBarcodeMatch = productDetail.produk.barcode?.contains(lowerCaseQuery, true) == true

            val isNameMatch = productDetail.produk.namaproduk.lowercase(Locale.ROOT).contains(lowerCaseQuery)

            val isCategoryMatch = productDetail.produk.kategori?.lowercase(Locale.ROOT)?.contains(lowerCaseQuery) == true

            isBarcodeMatch || isNameMatch || isCategoryMatch
        }

        updateProductDisplay(filteredList)
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Tidak perlu melakukan apa-apa sebelum perubahan teks
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Tidak perlu melakukan apa-apa selama perubahan teks
            }

            override fun afterTextChanged(s: Editable?) {
                // Panggil filterProducts setiap kali teks berubah
                filterProducts(s.toString())
            }
        })

        searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Sembunyikan keyboard
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
                true // Return true untuk consume event
            } else {
                false
            }
        }
    }
    private fun handleBarcodeScanned(barcode: String) {
        val productToUpdate = productViewModel.allProducts.value
            .let { if (it is ProductDataState.Success) it.produkDetails else emptyList() }
            .firstOrNull { it.produk.barcode == barcode }

        productToUpdate?.let { produkDetail ->
            val idproduk = produkDetail.produk.idproduk
            // Dapatkan kuantitas saat ini
            val currentQuantity = cartViewModel.cartItems.value?.get(idproduk)?.quantity ?: 0
            val newQuantity = currentQuantity + 1

            // 1. Tambahkan ke keranjang (memanggil CartViewModel)
            onUpdateCartItem(produkDetail, newQuantity)

            // 2. Scroll ke posisi item yang di-scan di daftar produk
            val position = transaksiAdapter.getProductList().indexOfFirst { it.produk.idproduk == idproduk }
            if (position != -1) {
                recyclerView.scrollToPosition(position)
            }
        } ?: run {
            Log.w("BarcodeScanner", "Produk dengan barcode $barcode tidak ditemukan.")
        }
    }

    private fun setupScanner() {
        barcodeScannerView?.decodeContinuous { result: BarcodeResult? ->
            result?.text?.let { barcode ->
                Log.d("BarcodeScanner", "Scanned: $barcode")
                barcodeScannerView?.pause()
                handleBarcodeScanned(barcode)
                // Lanjutkan scan setelah jeda 2 detik
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

    private fun calculateTotalQuantity(): Int {
        return cartViewModel.cartItems.value?.values?.sumOf { it.quantity } ?: 0
    }
    private fun startScanner() = barcodeScannerView?.resume()
    private fun stopScanner() = barcodeScannerView?.pause()
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    companion object {
        @JvmStatic
        fun newInstance() = transaksiFragment()
    }
}

