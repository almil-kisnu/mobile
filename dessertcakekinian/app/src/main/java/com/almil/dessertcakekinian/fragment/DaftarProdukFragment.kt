package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.activityViewModels
import com.almil.dessertcakekinian.BottomSheet.SearchProdukFragment
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.ProductAdapter
import com.almil.dessertcakekinian.adapter.OnProductItemClickListener
import com.almil.dessertcakekinian.model.ProductViewModel
import com.almil.dessertcakekinian.model.ProductDataState
import com.almil.dessertcakekinian.model.ProdukDetail
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.almil.dessertcakekinian.BottomSheet.ProductFilterAppliedListener


class DaftarProdukFragment : Fragment(), OnProductItemClickListener, ProductFilterAppliedListener {

    private val productViewModel: ProductViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var searchEditText: EditText
    private lateinit var filterButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var textViewError: TextView
    private var cachedSemuaProduk: List<ProdukDetail> = emptyList()
    private var currentOutletId: Int = -1
    private var navigationListener: OnNavigationListener? = null
    private var currentCategoryFilter: String? = null
    private var currentLowStockFilter: Boolean = false
    private var currentNearExpFilter: Boolean = false

    interface OnNavigationListener {
        fun navigateToDetail(produkDetail: ProdukDetail)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNavigationListener) {
            navigationListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daftar_produk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferences: SharedPreferences = requireActivity()
            .getSharedPreferences("user_session", Context.MODE_PRIVATE)

        currentOutletId = sharedPreferences.getInt("USER_OUTLET_ID", -1)

        if (currentOutletId == -1) {
            Log.e("DaftarProdukFragment", "ID Outlet tidak ditemukan di sesi!")
            return
        }

        recyclerView = view.findViewById(R.id.recycler_view_produk)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchEditText = view.findViewById(R.id.edit_text_search)
        filterButton = view.findViewById(R.id.button_filter)
        backButton = view.findViewById(R.id.btnBack)
        textViewError = view.findViewById(R.id.textViewError)

        setupSearchListener()
        setupFilterButtonListener()

        adapter = ProductAdapter(emptyList(), currentOutletId, this)
        recyclerView.adapter = adapter
        observeProductData()
    }

    private fun updateUiState(isError: Boolean, isLoading: Boolean = false) {
        if (isLoading) {
            recyclerView.visibility = View.GONE
            textViewError.visibility = View.GONE
        } else if (isError) {
            recyclerView.visibility = View.GONE
            textViewError.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            textViewError.visibility = View.GONE
        }
    }

    private fun observeProductData() {
        lifecycleScope.launch {
            productViewModel.allProducts.collect { state ->
                when (state) {
                    is ProductDataState.Loading -> {
                        updateUiState(isError = false, isLoading = true)
                        Log.d("DaftarProdukFragment", "Memuat data...")
                    }
                    is ProductDataState.Success -> {
                        updateUiState(isError = false)
                        cachedSemuaProduk = state.produkDetails
                        filterProduk(searchEditText.text.toString())
                    }
                    is ProductDataState.Error -> {
                        // Saat Error, kita hanya menampilkan TextView Error
                        updateUiState(isError = true)
                        Log.e("DaftarProdukFragment", "Error memuat data: ${state.message}")
                        textViewError.text = "Gagal memuat data. Periksa koneksi internet Anda."
                    }
                }
            }
        }
    }

    // setupReloadButtonListener() telah dihapus

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProduk(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
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

    private fun setupFilterButtonListener() {
        filterButton.setOnClickListener {
            showFilterBottomSheet()
        }
        backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun showFilterBottomSheet() {
        val uniqueCategories = cachedSemuaProduk
            .mapNotNull { it.produk.kategori }
            .distinct()
            .sorted()
        val searchFragment = SearchProdukFragment.newInstance(
            currentCategoryFilter,
            currentLowStockFilter,
            currentNearExpFilter,
            uniqueCategories.toList()
        )
        searchFragment.setProductFilterAppliedListener(this)
        searchFragment.show(childFragmentManager, SearchProdukFragment.TAG)
    }

    private fun filterProduk(query: String) {
        val normalizedQuery = query.lowercase().trim()

        val produkFiltered = cachedSemuaProduk.filter { produk ->
            val isAvailableInOutlet = produk.detailStok.any { it.idoutlet == currentOutletId }
            if (!isAvailableInOutlet) return@filter false
            val produkMatch = produk.produk.namaproduk.lowercase().contains(normalizedQuery)
            val barcodeMatch = (produk.produk.barcode ?: "").lowercase().contains(normalizedQuery)
            val kategoriSearchMatch = (produk.produk.kategori ?: "").lowercase().contains(normalizedQuery)
            val isTextMatch = normalizedQuery.isEmpty() || produkMatch || barcodeMatch || kategoriSearchMatch
            if (!isTextMatch) return@filter false

            val isCategoryMatch = currentCategoryFilter == null ||
                    produk.produk.kategori.equals(currentCategoryFilter, ignoreCase = true)
            if (!isCategoryMatch) return@filter false

            if (currentLowStockFilter) {
                val stockInfo = produk.detailStok.find { it.idoutlet == currentOutletId }
                // Asumsi: Stok menipis jika stok <= 5
                val isLowStock = stockInfo != null && stockInfo.stok <= 5
                if (!isLowStock) return@filter false
            }

            if (currentNearExpFilter) {
                val stockInfo = produk.detailStok.find { it.idoutlet == currentOutletId }
                val tglKadaluarsa = stockInfo?.tglKadaluarsa

                if (tglKadaluarsa == null) return@filter false

                try {
                    val expDate = LocalDate.parse(tglKadaluarsa, DateTimeFormatter.ISO_LOCAL_DATE)
                    val daysUntilExp = ChronoUnit.DAYS.between(LocalDate.now(), expDate)
                    val isNearExp = daysUntilExp <= 30 && daysUntilExp >= 0
                    if (!isNearExp) return@filter false
                } catch (e: Exception) {
                    Log.e("FilterProduk", "Error parsing date: $tglKadaluarsa", e)
                    return@filter false
                }
            }
            true
        }

        adapter.updateData(produkFiltered)
    }

    override fun onProductItemClicked(produkDetail: ProdukDetail) {
        Log.i("DaftarProdukFragment", "Klik produk: ${produkDetail.produk.namaproduk}")
        navigationListener?.navigateToDetail(produkDetail)
    }

    override fun onProductFilterApplied(filterCategory: String?, isLowStock: Boolean, isNearExp: Boolean) {
        currentCategoryFilter = filterCategory
        currentLowStockFilter = isLowStock
        currentNearExpFilter = isNearExp
        filterProduk(searchEditText.text.toString())
        updateFilterIcon()
    }

    private fun updateFilterIcon() {
        val isFilterActive = currentCategoryFilter != null || currentLowStockFilter || currentNearExpFilter
        val iconResId = if (isFilterActive) {
            R.drawable.ic_filter_on
        } else {
            R.drawable.ic_filter_off
        }
        filterButton.setImageResource(iconResId)
    }

    companion object {
        @JvmStatic
        fun newInstance() = DaftarProdukFragment()
    }
}