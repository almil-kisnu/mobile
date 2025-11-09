// File: com.almil.dessertcakekinian.activity/MainActivity.kt (MODIFIKASI)

package com.almil.dessertcakekinian.activity

import android.os.Bundle
import android.util.Log // 💡 Import Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels // 💡 Import viewModels
import androidx.lifecycle.lifecycleScope // 💡 Import lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.fragment.HomePageFragment
import com.almil.dessertcakekinian.fragment.ProfileFragment
import com.almil.dessertcakekinian.model.ProductViewModel // 💡 Import ProductViewModel
import com.almil.dessertcakekinian.model.ProductDataState // 💡 Import ProductDataState
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch // 💡 Import launch'
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private val productViewModel: ProductViewModel by viewModels()
    private val homeFragment by lazy { HomePageFragment() }
    private val profileFragment by lazy { ProfileFragment() }

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Izin kamera DIBERIKAN saat startup.")
            // Izin berhasil didapat, tidak perlu melakukan apa-apa lagi di sini
        } else {
            Log.e("MainActivity", "Izin kamera DITOLAK saat startup.")
            // TODO: Beri tahu pengguna bahwa fitur Scan tidak akan berfungsi
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        requestCameraPermission()
        setupBottomNavigation()
        setupBackPress()
        observeProductData()

        if (savedInstanceState == null) {
            replaceFragment(homeFragment)
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun requestCameraPermission() {
        when {
            // 1. Izin SUDAH DIBERIKAN
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("MainActivity", "Izin kamera sudah diberikan.")
            }
            // 2. Minta Izin
            else -> {
                Log.d("MainActivity", "Meminta izin kamera...")
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun observeProductData() {
        lifecycleScope.launch {
            productViewModel.allProducts.collect { state ->
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