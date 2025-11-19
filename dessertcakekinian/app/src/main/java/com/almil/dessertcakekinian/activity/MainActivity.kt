// File: com.almil.dessertcakekinian.activity/MainActivity.kt (MODIFIKASI)

package com.almil.dessertcakekinian.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.fragment.HomePageFragment
import com.almil.dessertcakekinian.fragment.ProfileFragment
import com.almil.dessertcakekinian.model.ProductViewModel
import com.almil.dessertcakekinian.model.ProductDataState
// START: Import dan ViewModel untuk Order
import com.almil.dessertcakekinian.model.OrderViewModel
import com.almil.dessertcakekinian.model.OrderDataState
// END: Import dan ViewModel untuk Order
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private val productViewModel: ProductViewModel by viewModels()
    // Deklarasi OrderViewModel
    private val orderViewModel: OrderViewModel by viewModels()
    // Deklarasi DiskonViewModel

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

        // Inisialisasi DiskonViewModel

        requestCameraPermission()
        setupBottomNavigation()
        setupBackPress()

        // Reset status absen setiap hari (TAMBAHAN BARU)
        checkAndResetDailyAbsen()

        // Panggil observer data Produk
        observeProductData()
        // Panggil observer data Order (Riwayat)
        observeOrderData()

        if (savedInstanceState == null) {
            replaceFragment(homeFragment)
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    // FUNGSI BARU: Reset status absen setiap hari
    private fun checkAndResetDailyAbsen() {
        val prefs = getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val lastAbsenDate = prefs.getString("last_absen_date", "")
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Jika tanggal terakhir absen berbeda dengan hari ini, reset status
        if (lastAbsenDate != currentDate) {
            val editor = prefs.edit()
            editor.putString("status_absen", "BELUM_ABSEN")
            editor.putString("jam_masuk_hari_ini", "")
            editor.putString("lokasi_masuk_hari_ini", "")
            editor.putString("jam_pulang_hari_ini", "")
            editor.putString("lokasi_pulang_hari_ini", "")
            editor.apply()
            println("🔄 MainActivity: Status absen direset untuk hari baru")
            Log.d("MainActivity", "Status absen direset - Hari baru: $currentDate")
        } else {
            Log.d("MainActivity", "Status absen tidak perlu direset - Masih hari yang sama: $currentDate")
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
                    is ProductDataState.Loading -> {
                        Log.d("MainActivity", "Produk Stream: Loading...")
                    }
                    is ProductDataState.Success -> {
                        Log.d("MainActivity", "Produk Stream: Berhasil dimuat & streaming aktif (${state.produkDetails.size} item)")
                    }
                    is ProductDataState.Error -> {
                        Log.e("MainActivity", "Produk Stream: Gagal: ${state.message}")
                    }
                }
            }
        }
    }

    // Fungsi untuk mengamati Order Data
    private fun observeOrderData() {
        lifecycleScope.launch {
            orderViewModel.allOrders.collect { state ->
                when (state) {
                    is OrderDataState.Loading -> {
                        Log.d("MainActivity", "Order Stream: Loading...")
                    }
                    is OrderDataState.Success -> {
                        Log.d("MainActivity", "Order Stream: Berhasil dimuat & streaming aktif (${state.orders.size} item)")
                    }
                    is OrderDataState.Error -> {
                        Log.e("MainActivity", "Order Stream: Gagal: ${state.message}")
                    }
                }
            }
        }
    }

    // Fungsi Baru untuk mengamati Diskon Data
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