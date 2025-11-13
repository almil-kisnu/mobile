package com.almil.dessertcakekinian.activity

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.almil.dessertcakekinian.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.almil.dessertcakekinian.fragment.HomePresensiFragment // Asumsi Anda membuat fragment ini
import com.almil.dessertcakekinian.fragment.RiwayatAbsenFragment // Asumsi Anda membuat fragment ini
import com.almil.dessertcakekinian.fragment.JadwalFragment // Asumsi Anda membuat fragment ini

class presensiActivity : AppCompatActivity() { // TETAP MENGGUNAKAN NAMA presensiActivity

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fragmentManager: FragmentManager

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Menggunakan layout yang dimodifikasi: R.layout.activity_presensi
        setContentView(R.layout.activity_presensi)

        // Menginisialisasi komponen
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        fragmentManager = supportFragmentManager

        // Muat fragment pertama (HomeFragment)
        if (savedInstanceState == null) {
            loadFragment(HomePresensiFragment())
        }

        // Setup Bottom Navigation Listener
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment? = when (item.itemId) {
                // ID harus sesuai dengan menu/bottom_navigation_menu.xml
                R.id.nav_home -> HomePresensiFragment()
                R.id.nav_riwayat -> RiwayatAbsenFragment()
                R.id.nav_jadwal -> JadwalFragment()
                else -> null
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment)
                true // Item berhasil ditangani
            } else {
                false
            }
        }
    }

    // Fungsi untuk mengganti Fragment di FrameLayout
    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        // ID container harus R.id.fragment_container
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    // Logika onResume dari Kode 1 untuk navigasi ke Riwayat dari luar Activity
    override fun onResume() {
        super.onResume()
        // Ganti "absen_data" dengan nama Shared Preferences yang sesuai
        val prefs: SharedPreferences = getSharedPreferences("absen_data", MODE_PRIVATE)
        val goToRiwayat = prefs.getBoolean("go_to_riwayat", false)

        if (goToRiwayat && ::bottomNavigationView.isInitialized) {
            bottomNavigationView.selectedItemId = R.id.nav_riwayat
            // Hapus flag setelah digunakan
            prefs.edit().remove("go_to_riwayat").apply()
        }
    }
}