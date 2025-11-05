package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import android.util.Log // 💡 Tambahkan Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns // 💡 Tambahkan import
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DaftarProdukFragment : Fragment(), OnProductItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    // ID Outlet yang ingin difilter (Saat ini ID 1)
    private val idOutletDefault = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daftar_produk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view_produk)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Adapter diinisialisasi dengan ID Outlet default
        adapter = ProductAdapter(emptyList(), idOutletDefault, this)
        recyclerView.adapter = adapter

        ambilDanTampilkanDataProduk()
    }

    private fun ambilDanTampilkanDataProduk() {
        // Ambil Supabase client
        val client = SupabaseClientProvider.client

        lifecycleScope.launch {
            try {
                // 1. Ambil & Gabungkan SEMUA data produk
                val semuaProduk = withContext(Dispatchers.IO) {
                    fetchDataProduk(client)
                }

                Log.d("DaftarProdukFragment", "Total produk diambil: ${semuaProduk.size}")

                // 2. Filter data: hanya ambil ProdukDetail yang memiliki DetailStok
                //    dengan idoutlet yang sesuai (idOutletDefault = 1).
                val produkFiltered = semuaProduk.filter { produk ->
                    produk.detailStok.any { it.idoutlet == idOutletDefault }
                }

                Log.d("DaftarProdukFragment", "Produk terfilter untuk Outlet $idOutletDefault: ${produkFiltered.size}")

                // 3. Kirim data yang sudah difilter ke adapter
                //    Adapter akan menerima idOutletDefault=1 dan akan menghitung stok/exp
                //    hanya dari DetailStok yang ID outletnya 1
                adapter.updateData(produkFiltered)

            } catch (e: Exception) {
                Log.e("DaftarProdukFragment", "Gagal mengambil data produk", e)
                e.printStackTrace()
                // Tampilkan pesan error ke user jika perlu
            }
        }
    }

    // Fungsi ambil semua data
    private suspend fun fetchDataProduk(client: SupabaseClient): List<ProdukDetail> {
        // Karena ada 3 kueri terpisah, gunakan withContext(Dispatchers.IO)
        // untuk memastikan eksekusi IO yang efisien.

        // Ambil data view produk + kategori
        val produkList = client.from("v_produk_kategori")
            .select()
            .decodeList<ProdukKategori>()

        // Ambil semua harga grosir
        val hargaList = client.from("harga_grosir")
            .select()
            .decodeList<HargaGrosir>()

        // Ambil semua detail stok
        val stokList = client.from("detail_stock")
            .select()
            .decodeList<DetailStok>()

        // Gabungkan semua data di memory (In-memory join)
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
        // Implementasi navigasi atau detail produk di sini
    }

    companion object {
        @JvmStatic
        fun newInstance() = DaftarProdukFragment()
    }
}