package com.almil.dessertcakekinian.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.JadwalMingguanApi
import com.almil.dessertcakekinian.database.ShiftDefinitionApi

class JadwalActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JadwalAdapter
    private val items = mutableListOf<JadwalMingguanApi.JadwalMingguan>()
    private val shiftItems = mutableListOf<ShiftDefinitionApi.ShiftDefinition>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_jadwal)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_jadwal)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = JadwalAdapter(items, shiftItems)
        recyclerView.adapter = adapter

        // Tombol X (close)
        val btnClose: TextView = findViewById(R.id.btn_close)
        btnClose.setOnClickListener {
            finish()
        }

        // Load data jadwal mingguan
        loadJadwalMingguan()
        loadShiftDefinitions()
    }

    private fun loadJadwalMingguan() {
        JadwalMingguanApi().listAllWithDetails(object : JadwalMingguanApi.JadwalListCallback {
            override fun onSuccess(list: List<JadwalMingguanApi.JadwalMingguan>) {
                items.clear()
                items.addAll(list)
                adapter.notifyDataSetChanged()

                if (list.isEmpty()) {
                    Toast.makeText(this@JadwalActivity, "Belum ada data jadwal mingguan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@JadwalActivity, "Loaded ${list.size} jadwal", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(error: String) {
                Toast.makeText(this@JadwalActivity, "Gagal memuat jadwal: $error", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadShiftDefinitions() {
        ShiftDefinitionApi().listAll(object : ShiftDefinitionApi.ShiftListCallback {
            override fun onSuccess(list: List<ShiftDefinitionApi.ShiftDefinition>) {
                shiftItems.clear()
                shiftItems.addAll(list)
                adapter.notifyDataSetChanged()
            }

            override fun onError(error: String) {
                println("Error load shift definition: $error")
            }
        })
    }

    class JadwalAdapter(
        private val data: List<JadwalMingguanApi.JadwalMingguan>,
        private val shiftData: List<ShiftDefinitionApi.ShiftDefinition>
    ) : RecyclerView.Adapter<JadwalAdapter.JadwalViewHolder>() {

        class JadwalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvSiklus: TextView = itemView.findViewById(R.id.tv_siklus)
            val tvPengguna: TextView = itemView.findViewById(R.id.tv_pengguna)
            val tvSenin: TextView = itemView.findViewById(R.id.tv_senin)
            val tvSelasa: TextView = itemView.findViewById(R.id.tv_selasa)
            val tvRabu: TextView = itemView.findViewById(R.id.tv_rabu)
            val tvKamis: TextView = itemView.findViewById(R.id.tv_kamis)
            val tvJumat: TextView = itemView.findViewById(R.id.tv_jumat)
            val tvSabtu: TextView = itemView.findViewById(R.id.tv_sabtu)
            val tvMinggu: TextView = itemView.findViewById(R.id.tv_minggu)
            val tvShiftInfo: TextView = itemView.findViewById(R.id.tv_shift_info)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_jadwal_mingguan, parent, false)
            return JadwalViewHolder(view)
        }

        override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
            val jadwal = data[position]

            // KIRI: "Karyawan"
            holder.tvSiklus.text = "Karyawan"
            // KANAN: username dari database
            holder.tvPengguna.text = jadwal.nama_pengguna

            // Set data untuk setiap hari
            holder.tvSenin.text = jadwal.shift_senin
            holder.tvSelasa.text = jadwal.shift_selasa
            holder.tvRabu.text = jadwal.shift_rabu
            holder.tvKamis.text = jadwal.shift_kamis
            holder.tvJumat.text = jadwal.shift_jumat
            holder.tvSabtu.text = jadwal.shift_sabtu
            holder.tvMinggu.text = jadwal.shift_minggu

            // Set warna berdasarkan shift
            setShiftColor(holder.tvSenin, jadwal.shift_senin)
            setShiftColor(holder.tvSelasa, jadwal.shift_selasa)
            setShiftColor(holder.tvRabu, jadwal.shift_rabu)
            setShiftColor(holder.tvKamis, jadwal.shift_kamis)
            setShiftColor(holder.tvJumat, jadwal.shift_jumat)
            setShiftColor(holder.tvSabtu, jadwal.shift_sabtu)
            setShiftColor(holder.tvMinggu, jadwal.shift_minggu)

            // Tampilkan data shift definition
            if (shiftData.isNotEmpty()) {
                val shiftText = StringBuilder()
                for (shift in shiftData) {
                    shiftText.append("${shift.nama_shift} (${shift.jam_mulai}-${shift.jam_selesai})\n")
                }
                holder.tvShiftInfo.text = shiftText.toString()
            } else {
                holder.tvShiftInfo.text = "Tidak ada data shift"
            }
        }

        private fun setShiftColor(textView: TextView, shiftName: String) {
            when {
                shiftName.contains("Pagi", ignoreCase = true) -> {
                    textView.setTextColor(0xFFE690A5.toInt()) // Pink
                }
                shiftName.contains("Siang", ignoreCase = true) -> {
                    textView.setTextColor(0xFFD81B60.toInt()) // Pink tua
                }
                shiftName.contains("Malam", ignoreCase = true) -> {
                    textView.setTextColor(0xFF880E4F.toInt()) // Pink gelap
                }
                shiftName.contains("Libur", ignoreCase = true) -> {
                    textView.setTextColor(0xFFFF9800.toInt()) // Orange
                }
                else -> {
                    textView.setTextColor(0xFF333333.toInt()) // Hitam
                }
            }
        }

        override fun getItemCount(): Int = data.size
    }
}