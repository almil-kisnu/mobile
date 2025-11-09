package com.almil.dessertcakekinian.activity

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.almil.dessertcakekinian.R

class RiwayatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Langsung menggunakan layout Riwayat Transaksi sebagai layout utama Activity
        setContentView(R.layout.fragment_riwayat_transaksi) // Perhatikan, ini layout fragment!

        // Inisialisasi Tombol Kembali dan Judul dari layout fragment_riwayat_transaksi.xml
        val btnBack = findViewById<ImageButton>(R.id.btnBackRiwayat)
        val tvTitle = findViewById<TextView>(R.id.tvJudulRiwayat)

        tvTitle.text = "Riwayat Transaksi" // Set teks judul (opsional, sudah di XML)

        // Tambahkan listener untuk tombol kembali
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Kembali ke activity sebelumnya
        }

        // TODO: Lanjutkan inisialisasi RecyclerView, EditText, dll. di sini
        // Jika Anda memiliki logika untuk pencarian, filter, dan mengisi data
        // ke RecyclerView, itu akan ditulis di dalam kelas RiwayatActivity ini.
        // val etSearch = findViewById<EditText>(R.id.etSearchRiwayat)
        // val cardFilter = findViewById<CardView>(R.id.cardFilterRiwayat)
        // val btnFilter = findViewById<ImageButton>(R.id.btnFilterRiwayat)
        // val rvRiwayat = findViewById<RecyclerView>(R.id.rvRiwayat)
    }
}