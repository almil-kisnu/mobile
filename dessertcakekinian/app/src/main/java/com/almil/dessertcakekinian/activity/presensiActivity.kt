package com.almil.dessertcakekinian.activity

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.almil.dessertcakekinian.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.almil.dessertcakekinian.fragment.JadwalFragment
import com.almil.dessertcakekinian.fragment.PresensiFragment // Asumsi Anda membuat fragment ini
import com.almil.dessertcakekinian.fragment.transaksiFragment

class presensiActivity : AppCompatActivity() { // TETAP MENGGUNAKAN NAMA presensiActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_presensi)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        // Cek apakah fragment sudah ada (misalnya, setelah rotasi layar)
        if (savedInstanceState == null) {
            // 1. Buat instance dari transaksiFragment
            val fragment = PresensiFragment()

            // 2. Gunakan Fragment Manager untuk menambahkan fragment
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment) // Ganti R.id.fragment_container dengan ID FrameLayout di activity_transaksi.xml
                .commit()
        }
    }
}