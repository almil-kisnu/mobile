package com.almil.dessertcakekinian.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.fragment.transferFragment
import com.almil.dessertcakekinian.fragment.RqTransferFragment
import com.almil.dessertcakekinian.fragment.DetailProdukFragment
import com.almil.dessertcakekinian.model.ProdukDetail

class TransferActivity : AppCompatActivity(),
    RqTransferFragment.OnNavigationListener,
    DetailProdukFragment.OnDetailCloseListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transfer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        // Cek apakah fragment sudah ada (misalnya, setelah rotasi layar)
        if (savedInstanceState == null) {
            // 1. Buat instance dari transferFragment (MENU UTAMA)
            val fragment = transferFragment()

            // 2. Gunakan Fragment Manager untuk menambahkan fragment
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }

    // Implement navigasi ke detail produk dari RqTransferFragment
    override fun navigateToDetail(produkDetail: ProdukDetail) {
        val newDetailFragment = DetailProdukFragment.newInstance(produkDetail)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, newDetailFragment, "DETAIL_PRODUK_TAG")
            .addToBackStack("DETAIL_STACK")
            .commit()
    }

    override fun onDetailClosed() {
        supportFragmentManager.popBackStack()
    }
}