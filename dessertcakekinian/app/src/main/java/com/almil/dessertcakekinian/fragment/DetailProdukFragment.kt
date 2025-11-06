package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.HargaBeliAdapter
import com.almil.dessertcakekinian.adapter.HargaJualAdapter
import com.almil.dessertcakekinian.model.ProdukDetail
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

// Nama kunci untuk argumen
private const val ARG_PRODUK_DETAIL = "produk_detail"

class DetailProdukFragment : Fragment() {

    private var produkDetail: ProdukDetail? = null

    // View components
    private lateinit var tvNamaProduk: TextView
    private lateinit var tvBarcode: TextView
    private lateinit var tvKategori: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var rvHargaBeli: RecyclerView
    private lateinit var rvHargaJual: RecyclerView

    // 💡 INTERFACE: Untuk memberi tahu Parent Activity bahwa user ingin kembali
    interface OnDetailCloseListener {
        fun onDetailClosed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val jsonString = it.getString(ARG_PRODUK_DETAIL)
            if (jsonString != null) {
                try {
                    // 🚀 Json.decodeFromString sekarang valid karena import di atas
                    produkDetail = Json.decodeFromString(jsonString)
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
        return inflater.inflate(R.layout.fragment_detail_produk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Views
        tvNamaProduk = view.findViewById(R.id.tv_nama_produk)
        tvBarcode = view.findViewById(R.id.tv_barcode)
        tvKategori = view.findViewById(R.id.tv_kategori)
        btnBack = view.findViewById(R.id.btn_back)
        rvHargaBeli = view.findViewById(R.id.rv_harga_beli)
        rvHargaJual = view.findViewById(R.id.rv_harga_jual)

        // Tombol Kembali
        btnBack.setOnClickListener {
            (activity as? OnDetailCloseListener)?.onDetailClosed()
        }

        // Tampilkan Data
        produkDetail?.let { detail ->
            tvNamaProduk.text = detail.produk.namaproduk
            tvBarcode.text = detail.produk.barcode ?: "-"
            tvKategori.text = detail.produk.kategori ?: "-"

            // Setup RecyclerView Harga Beli
            rvHargaBeli.layoutManager = LinearLayoutManager(context)
            rvHargaBeli.adapter = HargaBeliAdapter(detail.detailStok)

            // Setup RecyclerView Harga Jual
            rvHargaJual.layoutManager = LinearLayoutManager(context)
            rvHargaJual.adapter = HargaJualAdapter(detail.hargaGrosir)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(produkDetail: ProdukDetail) =
            DetailProdukFragment().apply {
                arguments = Bundle().apply {
                    // 🚀 Gunakan Json.encodeToString untuk Kotlinx Serialization
                    val jsonString = Json.encodeToString(produkDetail)
                    putString(ARG_PRODUK_DETAIL, jsonString)
                }
            }
    }
}