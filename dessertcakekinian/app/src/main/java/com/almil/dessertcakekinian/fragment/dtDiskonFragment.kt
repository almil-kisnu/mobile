package com.almil.dessertcakekinian.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.ProdukDiskonAdapter
import com.almil.dessertcakekinian.adapter.ProdukDiskonItem
import com.almil.dessertcakekinian.model.DiskonViewModel
import com.almil.dessertcakekinian.model.EventDiskon
import com.almil.dessertcakekinian.model.ProductViewModel
import com.almil.dessertcakekinian.model.ProductDataState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val ARG_DISKON = "arg_diskon"

class dtDiskonFragment : Fragment() {

    private var diskon: EventDiskon? = null

    private val diskonViewModel: DiskonViewModel by viewModels()
    private val productViewModel: ProductViewModel by viewModels()

    private lateinit var produkDiskonAdapter: ProdukDiskonAdapter
    private lateinit var recyclerViewProduk: RecyclerView

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var tvPromoTitle: TextView
    private lateinit var tvPromoDescription: TextView
    private lateinit var tvStatusAktif: TextView
    private lateinit var tvMulaiBerlaku: TextView
    private lateinit var tvBerakhirPada: TextView
    private lateinit var tvNilaiDiskon: TextView
    private lateinit var tvBerlaku: TextView
    private lateinit var tvProdukBerlakuTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            diskon = it.getSerializable(ARG_DISKON) as? EventDiskon
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dt_diskon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()

        diskon?.let { eventDiskon ->
            displayDiskonDetail(eventDiskon)
            loadProdukBerlaku(eventDiskon)
        }
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        tvPromoTitle = view.findViewById(R.id.tv_promo_title_detail)
        tvPromoDescription = view.findViewById(R.id.tv_promo_description)
        tvStatusAktif = view.findViewById(R.id.tv_status_aktif)
        tvMulaiBerlaku = view.findViewById(R.id.tv_mulai_berlaku)
        tvBerakhirPada = view.findViewById(R.id.tv_berakhir_pada)
        tvNilaiDiskon = view.findViewById(R.id.tv_nilai_diskon)
        tvBerlaku = view.findViewById(R.id.tv_berlaku)
        tvProdukBerlakuTitle = view.findViewById(R.id.tv_produk_berlaku_title)
        recyclerViewProduk = view.findViewById(R.id.recycler_view_produk_berlaku)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        produkDiskonAdapter = ProdukDiskonAdapter { item ->
            // Handle click produk (opsional)
            Log.d(TAG, "Clicked: ${item.produkDetail.produk.namaproduk}")
        }

        recyclerViewProduk.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = produkDiskonAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun displayDiskonDetail(diskon: EventDiskon) {
        // Nama diskon
        tvPromoTitle.text = diskon.namaDiskon

        // Deskripsi
        tvPromoDescription.text = diskon.deskripsi ?: "Tidak ada deskripsi"

        // Status
        if (diskon.isActive) {
            tvStatusAktif.text = "Aktif"
            tvStatusAktif.setTextColor(Color.parseColor("#4CAF50"))
            tvStatusAktif.setBackgroundResource(R.drawable.bg_status_aktif)
        } else {
            tvStatusAktif.text = "Nonaktif"
            tvStatusAktif.setTextColor(Color.parseColor("#F44336"))
            tvStatusAktif.setBackgroundResource(R.drawable.bg_status_nonaktif)
        }

        // Tanggal dan jam mulai
        val tanggalMulai = formatDateTime(diskon.tanggalMulai, diskon.jamMulai)
        tvMulaiBerlaku.text = tanggalMulai

        // Tanggal dan jam selesai
        val tanggalSelesai = formatDateTime(diskon.tanggalSelesai, diskon.jamSelesai)
        tvBerakhirPada.text = tanggalSelesai

        // Nilai diskon
        val nilaiDiskonFormatted = if (diskon.nilaiDiskon % 1.0 == 0.0) {
            "${diskon.nilaiDiskon.toInt()}%"
        } else {
            "${String.format("%.1f", diskon.nilaiDiskon)}%"
        }
        tvNilaiDiskon.text = nilaiDiskonFormatted

        // Berlaku untuk
        tvBerlaku.text = diskon.berlakuUntuk
    }

    private fun loadProdukBerlaku(diskon: EventDiskon) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Collect diskon dengan produk IDs
            diskonViewModel.allDiskonWithProducts.collect { diskonWithProductsList ->
                val currentDiskonWithProducts = diskonWithProductsList.find {
                    it.diskon.idDiskon == diskon.idDiskon
                }

                if (currentDiskonWithProducts != null) {
                    val produkIds = currentDiskonWithProducts.produkIds

                    // Update title dengan jumlah produk
                    tvProdukBerlakuTitle.text = "Produk Berlaku (${produkIds.size})"

                    // ✅ PERBAIKAN: Gunakan launch terpisah untuk collect products
                    viewLifecycleOwner.lifecycleScope.launch {
                        productViewModel.allProducts.collect { state ->
                            when (state) {
                                is ProductDataState.Success -> {
                                    val produkList = state.produkDetails

                                    // Filter produk yang ada di diskon
                                    val produkBerlaku = produkList.filter { produkDetail ->
                                        produkDetail.produk.idproduk in produkIds
                                    }

                                    // Convert ke ProdukDiskonItem
                                    val produkDiskonItems = produkBerlaku.map { produkDetail ->
                                        ProdukDiskonItem(
                                            produkDetail = produkDetail,
                                            diskon = diskon
                                        )
                                    }

                                    // Submit ke adapter
                                    produkDiskonAdapter.submitList(produkDiskonItems)

                                    Log.d(TAG, "Loaded ${produkDiskonItems.size} produk for diskon")
                                }
                                is ProductDataState.Loading -> {
                                    Log.d(TAG, "Loading products...")
                                }
                                is ProductDataState.Error -> {
                                    Log.e(TAG, "Error loading products: ${state.message}")
                                }
                            }
                        }
                    }
                } else {
                    tvProdukBerlakuTitle.text = "Produk Berlaku (0)"
                    produkDiskonAdapter.submitList(emptyList())
                    Log.d(TAG, "No products found for this discount")
                }
            }
        }
    }

    private fun formatDateTime(tanggal: String, jam: String): String {
        return try {
            // Parse tanggal
            val inputDateFormat = SimpleDateFormat(
                if (tanggal.contains("T")) "yyyy-MM-dd'T'HH:mm:ss" else "yyyy-MM-dd",
                Locale("id", "ID")
            )
            val outputDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

            val date = inputDateFormat.parse(tanggal)
            val formattedDate = if (date != null) {
                outputDateFormat.format(date)
            } else {
                tanggal
            }

            // Format jam (misal: "10:00:00" -> "10:00")
            val formattedTime = jam.substring(0, 5)

            "$formattedDate, $formattedTime"
        } catch (e: Exception) {
            "$tanggal, $jam"
        }
    }

    companion object {
        private const val TAG = "dtDiskonFragment"

        @JvmStatic
        fun newInstance(diskon: EventDiskon) =
            dtDiskonFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DISKON, diskon)
                }
            }
    }
}