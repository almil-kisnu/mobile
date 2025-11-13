package com.almil.dessertcakekinian.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.ShiftDefinitionApi
import java.util.ArrayList

class JadwalFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JadwalAdapter
    private val items = ArrayList<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jadwal, container, false)

        recyclerView = view.findViewById(R.id.recycler_jadwal)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        items.clear()
        items.add("Memuat jadwal...")
        adapter = JadwalAdapter(items)
        recyclerView.adapter = adapter

        loadShiftDefinitions()

        return view
    }

    private fun loadShiftDefinitions() {
        ShiftDefinitionApi().listAll(object : ShiftDefinitionApi.ShiftListCallback {
            override fun onSuccess(list: List<ShiftDefinitionApi.ShiftDefinition>) {
                if (!isAdded) return
                val display = ArrayList<String>()
                if (list.isNotEmpty()) {
                    for (s in list) {
                        val row = (s.nama_shift ?: "") +
                                " (" + (s.jam_mulai ?: "") + " - " +
                                (s.jam_selesai ?: "") + ")"
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
                if (!isAdded) return
                items.clear()
                items.add("Gagal memuat jadwal: $error")
                adapter.notifyDataSetChanged()
                try {
                    Toast.makeText(requireContext(), "Shift: $error", Toast.LENGTH_LONG).show()
                } catch (ignored: Exception) {}
            }
        })
    }

    class JadwalAdapter(private val data: List<String>) : RecyclerView.Adapter<JadwalAdapter.VH>() {

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tv: TextView = itemView as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context)
            tv.setTextColor(0xFFFFFFFF.toInt())
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