package com.almil.dessertcakekinian.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.ShiftDefinitionApi
import java.util.ArrayList

class JadwalActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JadwalAdapter
    private val items = ArrayList<String>()

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
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        items.clear()
        items.add("Memuat jadwal...")
        adapter = JadwalAdapter(items)
        recyclerView.adapter = adapter

        // Load data from ShiftDefinitionApi
        loadShiftDefinitions()
    }

    private fun loadShiftDefinitions() {
        ShiftDefinitionApi().listAll(object : ShiftDefinitionApi.ShiftListCallback {
            override fun onSuccess(list: List<ShiftDefinitionApi.ShiftDefinition>) {
                val display = ArrayList<String>()
                if (list.isNotEmpty()) {
                    for (s in list) {
                        val row = s.nama_shift +
                                " (" + s.jam_mulai + " - " +
                                s.jam_selesai + ")"
                        display.add(row)
                    }
                } else {
                    display.add("Belum ada data shift")
                }
                items.clear()
                items.addAll(display)
                adapter.notifyDataSetChanged()
            }

            override fun onError(error: String) {
                items.clear()
                items.add("Gagal memuat jadwal: $error")
                adapter.notifyDataSetChanged()
                try {
                    Toast.makeText(this@JadwalActivity, "Shift: $error", Toast.LENGTH_LONG).show()
                } catch (ignored: Exception) {}
            }
        })
    }

    // Pindah JadwalAdapter ke luar class JadwalActivity
    class JadwalAdapter(private val data: List<String>) : RecyclerView.Adapter<JadwalAdapter.VH>() {

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tv: TextView = itemView as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context)
            tv.setTextColor(0xFF000000.toInt()) // Ubah ke warna hitam agar terlihat
            tv.textSize = 16f
            tv.setPadding(24, 24, 24, 24)
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.tv.text = data[position]
        }

        override fun getItemCount(): Int = data.size
    }
}