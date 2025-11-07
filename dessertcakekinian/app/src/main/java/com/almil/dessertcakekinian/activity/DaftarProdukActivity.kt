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
    private lateinit var daftarProdukFragment: DaftarProdukFragment
    private val DAFTAR_PRODUK_TAG = "DAFTAR_PRODUK_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_daftar_produk)

        if (savedInstanceState == null) {
            daftarProdukFragment = DaftarProdukFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, daftarProdukFragment, DAFTAR_PRODUK_TAG)
                .commit()
        } else {
            daftarProdukFragment = supportFragmentManager.findFragmentByTag(DAFTAR_PRODUK_TAG) as DaftarProdukFragment
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
    }

    override fun navigateToDetail(produkDetail: ProdukDetail) {
        val newDetailFragment = DetailProdukFragment.newInstance(produkDetail)
        supportFragmentManager.beginTransaction()
            .hide(daftarProdukFragment) // Sekarang daftarProdukFragment merujuk ke instance yang benar
            .add(R.id.fragment_container, newDetailFragment, "DETAIL_PRODUK_TAG")
            .addToBackStack("DETAIL_STACK")
            .commit()
    }

    override fun onDetailClosed() {
        supportFragmentManager.popBackStack()
    }
}