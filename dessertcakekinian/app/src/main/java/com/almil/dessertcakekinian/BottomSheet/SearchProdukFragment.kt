package com.almil.dessertcakekinian.BottomSheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.almil.dessertcakekinian.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// Interface baru untuk mengkomunikasikan hasil filter produk
interface ProductFilterAppliedListener {
    fun onProductFilterApplied(filterCategory: String?, isLowStock: Boolean, isNearExp: Boolean)
}

// Data class sederhana untuk menampung Kategori unik dari Supabase
data class Kategori(val namaKategori: String)

class SearchProdukFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SearchProdukFragment"
        private const val ARG_CATEGORY_FILTER = "category_filter"
        private const val ARG_LOW_STOCK_FILTER = "low_stock_filter"
        private const val ARG_NEAR_EXP_FILTER = "near_exp_filter"
        private const val ARG_ALL_CATEGORIES = "all_categories" // 💡 ARGUMEN BARU

        fun newInstance(
            currentCategoryFilter: String?,
            currentLowStockFilter: Boolean,
            currentNearExpFilter: Boolean,
            allUniqueCategories: List<String> // 💡 Parameter Baru
        ): SearchProdukFragment {
            return SearchProdukFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_FILTER, currentCategoryFilter)
                    putBoolean(ARG_LOW_STOCK_FILTER, currentLowStockFilter)
                    putBoolean(ARG_NEAR_EXP_FILTER, currentNearExpFilter)
                    // Menggunakan StringArrayList untuk mengirim List<String>
                    putStringArrayList(ARG_ALL_CATEGORIES, ArrayList(allUniqueCategories))
                }
            }
        }
    }

    private var filterAppliedListener: ProductFilterAppliedListener? = null

    // State filter yang diterima
    private var currentCategoryFilter: String? = null
    private var currentLowStockFilter: Boolean = false
    private var currentNearExpFilter: Boolean = false

    // Variabel View
    private lateinit var spinnerCategory: Spinner
    private lateinit var cbLowStock: CheckBox
    private lateinit var cbNearExp: CheckBox
    private lateinit var btnTerapin: Button
    private lateinit var btnRestart: Button

    // Variabel untuk data Spinner Kategori
    private var allCategories = listOf<Kategori>()
    private var selectedCategory: String? = null // Kategori yang dipilih dari spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentCategoryFilter = it.getString(ARG_CATEGORY_FILTER)
            currentLowStockFilter = it.getBoolean(ARG_LOW_STOCK_FILTER, false)
            currentNearExpFilter = it.getBoolean(ARG_NEAR_EXP_FILTER, false)

            // 💡 Baca daftar kategori dari argumen dan konversi ke List<Kategori>
            val categoriesFromArgs = it.getStringArrayList(ARG_ALL_CATEGORIES) ?: emptyList()
            allCategories = categoriesFromArgs.map { Kategori(it) }
        }

        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Menggunakan layout fragment_search_produk
        return inflater.inflate(R.layout.fragment_search_produk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi View
        spinnerCategory = view.findViewById(R.id.spinner_category)
        cbLowStock = view.findViewById(R.id.cb_stok)
        cbNearExp = view.findViewById(R.id.cb_exp)
        btnTerapin = view.findViewById(R.id.btnTerapin)
        btnRestart = view.findViewById(R.id.btnRestart)

        // 💡 TIDAK PERLU MEMANGGIL fetchCategories() lagi
        setupCategorySpinner()
        setupCheckboxListeners()
        restoreFilterState() // Restore setelah data Spinner siap
        updateRestartButtonState()


        // Logika Tombol Terapin
        btnTerapin.setOnClickListener {
            applyFilter()
            dismiss()
        }

        // Logika Tombol Restart
        btnRestart.setOnClickListener {
            // Default: Semua Kategori (null), Stok (false), EXP (false)
            filterAppliedListener?.onProductFilterApplied(null, false, false)
            dismiss()
        }
    }

    // Setter untuk Listener
    fun setProductFilterAppliedListener(listener: ProductFilterAppliedListener) {
        this.filterAppliedListener = listener
    }

    // FUNGSI fetchCategories() DIHAPUS

    // Fungsi untuk mengatur Spinner Kategori
    private fun setupCategorySpinner() {
        val categoryNames = allCategories.map { it.namaKategori }.toMutableList()
        categoryNames.add(0, "Semua Kategori") // Tambahkan opsi 'Semua' di awal

        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Set nilai awal Spinner berdasarkan currentCategoryFilter yang diterima
        val defaultIndex = categoryNames.indexOf(currentCategoryFilter)
        if (defaultIndex != -1) {
            spinnerCategory.setSelection(defaultIndex)
            selectedCategory = currentCategoryFilter
        } else {
            spinnerCategory.setSelection(0)
            selectedCategory = null
        }

        // Listener Spinner
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = if (position > 0) {
                    categoryNames[position] // Ambil nama kategori
                } else {
                    null // "Semua Kategori"
                }
                updateRestartButtonState()
            }
            override fun onNothingSelected(parent: AdapterView<*>) { selectedCategory = null }
        }
    }

    // Atur listener untuk Checkbox (tidak ada pengelompokan, bisa dicentang semua)
    private fun setupCheckboxListeners() {
        cbLowStock.setOnCheckedChangeListener { _, _ -> updateRestartButtonState() }
        cbNearExp.setOnCheckedChangeListener { _, _ -> updateRestartButtonState() }
    }

    // Mengembalikan status CheckBox dan Spinner berdasarkan filter saat ini
    private fun restoreFilterState() {
        // Checkbox
        cbLowStock.isChecked = currentLowStockFilter
        cbNearExp.isChecked = currentNearExpFilter

        // Spinner sudah diurus di setupCategorySpinner
    }

    // Menerapkan filter dan memanggil listener
    private fun applyFilter() {
        val categoryFilter: String? = selectedCategory
        val lowStockFilter: Boolean = cbLowStock.isChecked
        val nearExpFilter: Boolean = cbNearExp.isChecked

        Log.d(TAG, "Filter Diterapkan - Kategori: $categoryFilter, Stok: $lowStockFilter, EXP: $nearExpFilter")

        filterAppliedListener?.onProductFilterApplied(categoryFilter, lowStockFilter, nearExpFilter)
    }

    // Memperbarui status tombol Restart
    private fun updateRestartButtonState() {
        // Default filter: null (Semua Kategori), false (Stok), false (EXP)

        val isCategoryDefault = selectedCategory == null
        val isLowStockDefault = !cbLowStock.isChecked
        val isNearExpDefault = !cbNearExp.isChecked

        val isDefaultFilter = isCategoryDefault && isLowStockDefault && isNearExpDefault

        btnRestart.isEnabled = !isDefaultFilter
    }
}