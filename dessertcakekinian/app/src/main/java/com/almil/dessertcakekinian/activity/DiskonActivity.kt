package com.almil.dessertcakekinian.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.fragment.DiskonFragment
import com.almil.dessertcakekinian.fragment.PresensiFragment

class DiskonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_diskon)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        // Cek apakah fragment sudah ada (misalnya, setelah rotasi layar)
        if (savedInstanceState == null) {
            // 1. Buat instance dari transaksiFragment
            val fragment = DiskonFragment()

            // 2. Gunakan Fragment Manager untuk menambahkan fragment
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment) // Ganti R.id.fragment_container dengan ID FrameLayout di activity_transaksi.xml
                .commit()
        }
    }
}