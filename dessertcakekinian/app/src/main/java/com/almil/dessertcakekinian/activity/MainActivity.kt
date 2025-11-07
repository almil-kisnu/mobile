// File: com.almil.dessertcakekinian.activity/MainActivity.kt (MODIFIKASI)

package com.almil.dessertcakekinian.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log // 💡 Import Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels // 💡 Import viewModels
import androidx.lifecycle.lifecycleScope // 💡 Import lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.fragment.HomePageFragment
import com.almil.dessertcakekinian.fragment.ProfileFragment
import com.almil.dessertcakekinian.activity.loginActivity
import com.almil.dessertcakekinian.model.ProductViewModel // 💡 Import ProductViewModel
import com.almil.dessertcakekinian.model.ProductDataState // 💡 Import ProductDataState
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch // 💡 Import launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    // 💡 DEKLARASI VIEWMODEL BARU, TERIKAT PADA ACTIVITY INI
    private val productViewModel: ProductViewModel by viewModels()

    // Fragment instances
    private val homeFragment by lazy { HomePageFragment() }
    private val profileFragment by lazy { ProfileFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()
        setupBackPress()

        // 💡 PENTING: Panggil observer untuk memicu pengambilan data (stream)
        observeProductData()

        if (savedInstanceState == null) {
            replaceFragment(homeFragment)
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    // 💡 FUNGSI UNTUK MENGAMATI DATA (HANYA UNTUK MEMASTIKAN FLOW BERJALAN)
    private fun observeProductData() {
        lifecycleScope.launch {
            // Observer ini memastikan ProductViewModel dibuat dan ProductRepository.getSharedProdukDetail()
            // mulai mengumpulkan data (membuka koneksi Realtime)
            productViewModel.allProducts.collect { state ->
                // Kita tidak perlu menampilkan apa-apa di sini, cukup logging
                when (state) {
                    is ProductDataState.Loading -> Log.d("MainActivity", "Produk Stream: Loading...")
                    is ProductDataState.Success -> Log.d("MainActivity", "Produk Stream: Berhasil dimuat & streaming aktif (${state.produkDetails.size} item)")
                    is ProductDataState.Error -> Log.e("MainActivity", "Produk Stream: Gagal: ${state.message}")
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(homeFragment)
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    private fun setupBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomNavigation.selectedItemId != R.id.nav_home) {
                    bottomNavigation.selectedItemId = R.id.nav_home
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}