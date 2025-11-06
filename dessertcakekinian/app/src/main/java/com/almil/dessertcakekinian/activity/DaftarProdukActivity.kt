package com.almil.dessertcakekinian.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.fragment.DaftarProdukFragment
import com.almil.dessertcakekinian.fragment.DetailProdukFragment
import com.almil.dessertcakekinian.model.ProdukDetail

class DaftarProdukActivity : AppCompatActivity(),
    DaftarProdukFragment.OnNavigationListener,
    DetailProdukFragment.OnDetailCloseListener {

    // UBAH: Gunakan 'lateinit var' agar bisa diinisialisasi ulang
    private lateinit var daftarProdukFragment: DaftarProdukFragment
    // TIDAK PERLU disimpan, cukup gunakan tag untuk mendapatkannya kembali
    // private var detailProdukFragment: DetailProdukFragment? = null

    private val DAFTAR_PRODUK_TAG = "DAFTAR_PRODUK_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_daftar_produk)

        // 💡 PERBAIKAN UTAMA: Dapatkan Fragment yang sudah ada dari FragmentManager
        if (savedInstanceState == null) {
            // Jika pertama kali, buat instance baru dan tambahkan
            daftarProdukFragment = DaftarProdukFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, daftarProdukFragment, DAFTAR_PRODUK_TAG)
                .commit()
        } else {
            // Jika Activity dibuat ulang (mis. ganti tema), ambil Fragment lama
            // Ini akan mengembalikan Fragment yang sudah ada (termasuk yang di-hide)
            daftarProdukFragment = supportFragmentManager.findFragmentByTag(DAFTAR_PRODUK_TAG) as DaftarProdukFragment
            // Catatan: Fragment Detail (jika ada) akan otomatis dikelola oleh Back Stack.
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // 1. Logika Navigasi ke Detail
    override fun navigateToDetail(produkDetail: ProdukDetail) {
        // Buat Fragment Detail baru
        val newDetailFragment = DetailProdukFragment.newInstance(produkDetail)

        // Logika Navigasi SHOW/HIDE yang sudah benar, asalkan daftarProdukFragment sudah benar
        supportFragmentManager.beginTransaction()
            .hide(daftarProdukFragment) // Sekarang daftarProdukFragment merujuk ke instance yang benar
            .add(R.id.fragment_container, newDetailFragment, "DETAIL_PRODUK_TAG")
            .addToBackStack("DETAIL_STACK")
            .commit()
    }

    // 2. Logika Kembali
    override fun onDetailClosed() {
        supportFragmentManager.popBackStack()
    }
}