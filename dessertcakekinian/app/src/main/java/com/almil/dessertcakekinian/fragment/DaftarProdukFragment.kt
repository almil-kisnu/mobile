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
import android.widget.EditText
import android.widget.ImageButton // 💡 IMPORT BARU
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.ProductAdapter
import com.almil.dessertcakekinian.adapter.OnProductItemClickListener
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.ProdukKategori
import com.almil.dessertcakekinian.model.HargaGrosir
import com.almil.dessertcakekinian.model.DetailStok
import com.almil.dessertcakekinian.model.ProdukDetail
import com.almil.dessertcakekinian.BottomSheet.SearchProdukFragment // 💡 IMPORT FRAGMENT BARU
import com.almil.dessertcakekinian.BottomSheet.ProductFilterAppliedListener // 💡 IMPORT INTERFACE BARU
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate // 💡 IMPORT UNTUK LOGIKA EXP
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Tambahkan implementasi ProductFilterAppliedListener
class DaftarProdukFragment : Fragment(), OnProductItemClickListener, ProductFilterAppliedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var searchEditText: EditText
    private lateinit var filterButton: ImageButton // 💡 Tambahkan ImageButton
    private var semuaProduk: List<ProdukDetail> = emptyList()

    private var currentOutletId: Int = -1

    private var navigationListener: OnNavigationListener? = null

    // 💡 STATE FILTER BOTTOM SHEET
    private var currentCategoryFilter: String? = null
    private var currentLowStockFilter: Boolean = false
    private var currentNearExpFilter: Boolean = false
    // 💡 END STATE FILTER

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
        filterButton = view.findViewById(R.id.button_search) // ID ImageButton dari XML

        setupSearchListener()
        setupFilterButtonListener() // 💡 Setup listener untuk tombol filter

        adapter = ProductAdapter(emptyList(), currentOutletId, this)
        recyclerView.adapter = adapter

        ambilDanTampilkanDataProduk()
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Panggil filterProduk dengan query baru
                filterProduk(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // 💡 FUNGSI BARU: Listener Tombol Filter
    private fun setupFilterButtonListener() {
        filterButton.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    // 💡 FUNGSI BARU: Tampilkan Bottom Sheet
    private fun showFilterBottomSheet() {
        // 1. Ambil Kategori Unik dari data yang sudah ada
        val uniqueCategories = semuaProduk
            .mapNotNull { it.produk.kategori }
            .distinct()
            .sorted()

        val searchFragment = SearchProdukFragment.newInstance(
            currentCategoryFilter,
            currentLowStockFilter,
            currentNearExpFilter,
            uniqueCategories.toList() // Kirim daftar kategori unik
        )

        // Atur listener agar hasil filter dikembalikan ke Fragment ini
        searchFragment.setProductFilterAppliedListener(this)
        searchFragment.show(childFragmentManager, SearchProdukFragment.TAG)
    }

    private fun ambilDanTampilkanDataProduk() {
        val client = SupabaseClientProvider.client

        lifecycleScope.launch {
            try {
                semuaProduk = withContext(Dispatchers.IO) {
                    fetchDataProduk(client)
                }

                // Terapkan semua filter (search text kosong, filter BS default)
                filterProduk(searchEditText.text.toString())
            } catch (e: Exception) {
                Log.e("DaftarProdukFragment", "Gagal mengambil data produk", e)
                // Opsi: Tampilkan UI error
            }
        }
    }

    // 💡 MODIFIKASI LOGIKA PEMFILTERAN
    private fun filterProduk(query: String) {
        val normalizedQuery = query.lowercase().trim()

        val produkFiltered = semuaProduk.filter { produk ->
            // 1. Filter Outlet (WAJIB)
            val isAvailableInOutlet = produk.detailStok.any { it.idoutlet == currentOutletId }
            if (!isAvailableInOutlet) return@filter false

            // 2. Filter Search Text (Nama, Barcode, Kategori)
            val produkMatch = produk.produk.namaproduk.lowercase().contains(normalizedQuery)
            val barcodeMatch = (produk.produk.barcode ?: "").lowercase().contains(normalizedQuery)
            val kategoriSearchMatch = (produk.produk.kategori ?: "").lowercase().contains(normalizedQuery)

            val isTextMatch = normalizedQuery.isEmpty() || produkMatch || barcodeMatch || kategoriSearchMatch
            if (!isTextMatch) return@filter false

            // 3. Filter Bottom Sheet: Kategori
            val isCategoryMatch = currentCategoryFilter == null ||
                    produk.produk.kategori.equals(currentCategoryFilter, ignoreCase = true)
            if (!isCategoryMatch) return@filter false

            // 4. Filter Bottom Sheet: Stok Menipis
            if (currentLowStockFilter) {
                val stockInfo = produk.detailStok.find { it.idoutlet == currentOutletId }
                // Asumsi: Stok menipis jika stok <= 5
                val isLowStock = stockInfo != null && stockInfo.stok <= 5
                if (!isLowStock) return@filter false
            }

            // 5. Filter Bottom Sheet: Mendekati EXP
            if (currentNearExpFilter) {
                val stockInfo = produk.detailStok.find { it.idoutlet == currentOutletId }
                val tglKadaluarsa = stockInfo?.tglKadaluarsa

                if (tglKadaluarsa == null) return@filter false // Abaikan jika tgl kadaluarsa null

                try {
                    val expDate = LocalDate.parse(tglKadaluarsa, DateTimeFormatter.ISO_LOCAL_DATE)
                    val daysUntilExp = ChronoUnit.DAYS.between(LocalDate.now(), expDate)
                    // Asumsi: Mendekati EXP jika sisa hari <= 30
                    val isNearExp = daysUntilExp <= 30 && daysUntilExp >= 0
                    if (!isNearExp) return@filter false
                } catch (e: Exception) {
                    Log.e("FilterProduk", "Error parsing date: $tglKadaluarsa", e)
                    return@filter false
                }
            }

            // Jika lolos semua filter
            true
        }

        adapter.updateData(produkFiltered)
    }

    private suspend fun fetchDataProduk(client: SupabaseClient): List<ProdukDetail> {
        val produkList = client.from("v_produk_kategori").select().decodeList<ProdukKategori>()
        val hargaList = client.from("harga_grosir").select().decodeList<HargaGrosir>()
        val stokList = client.from("detail_stock").select().decodeList<DetailStok>()

        return produkList.map { produk ->
            val hargaGrosirProduk = hargaList.filter { it.idproduk == produk.idproduk }
            val stokProduk = stokList.filter { it.idproduk == produk.idproduk }

            ProdukDetail(
                produk = produk,
                hargaGrosir = hargaGrosirProduk,
                detailStok = stokProduk
            )
        }
    }

    override fun onProductItemClicked(produkDetail: ProdukDetail) {
        Log.i("DaftarProdukFragment", "Klik produk: ${produkDetail.produk.namaproduk}")
        navigationListener?.navigateToDetail(produkDetail)
    }

    // 💡 IMPLEMENTASI DARI ProductFilterAppliedListener
    override fun onProductFilterApplied(filterCategory: String?, isLowStock: Boolean, isNearExp: Boolean) {
        // 1. Update state filter
        currentCategoryFilter = filterCategory
        currentLowStockFilter = isLowStock
        currentNearExpFilter = isNearExp

        // 2. Terapkan filter ke data yang sudah ada (termasuk search text)
        filterProduk(searchEditText.text.toString())

        // 3. Perbarui ikon tombol filter
        updateFilterIcon()
    }

    // 💡 FUNGSI BARU: Mengubah Ikon
    private fun updateFilterIcon() {
        val isFilterActive = currentCategoryFilter != null || currentLowStockFilter || currentNearExpFilter

        val iconResId = if (isFilterActive) {
            R.drawable.ic_filter_on // Asumsi Anda punya drawable ini
        } else {
            R.drawable.ic_filter_off // Asumsi Anda punya drawable ini
        }

        filterButton.setImageResource(iconResId)
    }

    companion object {
        @JvmStatic
        fun newInstance() = DaftarProdukFragment()
    }
}