package com.almil.dessertcakekinian.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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

    private var currentUserOutletId: Int? = null

    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_jadwal)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_jadwal)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = JadwalAdapter(items, shiftItems)
        recyclerView.adapter = adapter

        // Tombol back (ganti dari TextView ke ImageView)
        val btnClose: ImageView = findViewById(R.id.btn_close)
        btnClose.setOnClickListener {
            finish()
        }

        // Dapatkan idoutlet dari user yang login
        getCurrentUserOutlet()
    }

    private fun getCurrentUserOutlet() {
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val username = sharedPref.getString("USER_NAME", null)
        val userId = sharedPref.getInt("USER_ID", -1)
        val outletId = sharedPref.getInt("USER_OUTLET_ID", -1)

        println("🔍 DEBUG SharedPreferences:")
        println("   - username = $username")
        println("   - userId = $userId")
        println("   - outletId = $outletId")

        if (username.isNullOrEmpty() || userId <= 0) {
            Toast.makeText(this, "User tidak ditemukan. Silakan login kembali.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (outletId > 0) {
            currentUserOutletId = outletId
            println("✅ Using outlet from SharedPreferences: $outletId")
        } else {
            currentUserOutletId = null
            println("⚠️ No outlet ID found, showing all schedules")
        }

        loadJadwalMingguan()
        loadShiftDefinitions()
    }

    private fun loadJadwalMingguan() {
        JadwalMingguanApi().listAllWithDetails(object : JadwalMingguanApi.JadwalListCallback {
            override fun onSuccess(list: List<JadwalMingguanApi.JadwalMingguan>) {
                items.clear()

                val filteredList = if (currentUserOutletId != null) {
                    list.filter { it.idoutlet == currentUserOutletId }
                } else {
                    list
                }

                println("📋 Total jadwal dari database: ${list.size}")
                println("📋 Filtered untuk outlet $currentUserOutletId: ${filteredList.size}")

                items.addAll(filteredList)
                adapter.notifyDataSetChanged()

                if (filteredList.isEmpty()) {
                    val message = if (currentUserOutletId != null) {
                        "Belum ada jadwal untuk outlet $currentUserOutletId"
                    } else {
                        "Belum ada data jadwal mingguan"
                    }
                    Toast.makeText(this@JadwalActivity, message, Toast.LENGTH_SHORT).show()
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
            val containerDataKaryawan: LinearLayout = itemView.findViewById(R.id.container_data_karyawan)
            val tvShiftInfo: TextView = itemView.findViewById(R.id.tv_shift_info)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_jadwal_mingguan, parent, false)
            return JadwalViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
            val outletInfo = if (data.isNotEmpty() && data[0].idoutlet != null) {
                " - Outlet ${data[0].idoutlet}"
            } else {
                ""
            }

            holder.tvSiklus.text = "Jadwal Mingguan Karyawan$outletInfo"
            holder.tvPengguna.text = "Total: ${data.size} Karyawan"

            holder.containerDataKaryawan.removeAllViews()

            data.forEach { jadwal ->
                val rowLayout = LinearLayout(holder.itemView.context)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                rowLayout.layoutParams = layoutParams
                rowLayout.orientation = LinearLayout.HORIZONTAL

                val tvUsername = TextView(holder.itemView.context)
                val usernameParams = LinearLayout.LayoutParams(
                    dpToPx(120, holder.itemView.context.resources),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                tvUsername.layoutParams = usernameParams
                tvUsername.text = jadwal.nama_pengguna
                tvUsername.textSize = 12f
                tvUsername.setTextColor(0xFF333333.toInt())
                tvUsername.gravity = Gravity.CENTER
                tvUsername.setPadding(
                    dpToPx(8, holder.itemView.context.resources),
                    dpToPx(8, holder.itemView.context.resources),
                    dpToPx(8, holder.itemView.context.resources),
                    dpToPx(8, holder.itemView.context.resources)
                )
                rowLayout.addView(tvUsername)

                val days = listOf(
                    jadwal.shift_senin,
                    jadwal.shift_selasa,
                    jadwal.shift_rabu,
                    jadwal.shift_kamis,
                    jadwal.shift_jumat,
                    jadwal.shift_sabtu,
                    jadwal.shift_minggu
                )

                days.forEach { shiftName ->
                    val tvShift = TextView(holder.itemView.context)
                    val shiftParams = LinearLayout.LayoutParams(
                        dpToPx(80, holder.itemView.context.resources),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    tvShift.layoutParams = shiftParams
                    tvShift.text = formatShiftName(shiftName)
                    tvShift.textSize = 12f
                    tvShift.gravity = Gravity.CENTER
                    tvShift.setPadding(
                        dpToPx(8, holder.itemView.context.resources),
                        dpToPx(8, holder.itemView.context.resources),
                        dpToPx(8, holder.itemView.context.resources),
                        dpToPx(8, holder.itemView.context.resources)
                    )

                    setShiftColor(tvShift, shiftName)
                    rowLayout.addView(tvShift)
                }

                holder.containerDataKaryawan.addView(rowLayout)
            }

            if (shiftData.isNotEmpty()) {
                val shiftText = StringBuilder()
                for (shift in shiftData) {
                    val simpleName = formatShiftName(shift.nama_shift)
                    shiftText.append("$simpleName (${shift.jam_mulai}-${shift.jam_selesai})\n")
                }
                holder.tvShiftInfo.text = shiftText.toString()
            } else {
                holder.tvShiftInfo.text = "Tidak ada data shift"
            }
        }

        private fun formatShiftName(shiftName: String?): String {
            if (shiftName == null || shiftName.isEmpty()) return "-"

            return when {
                shiftName.contains("Pagi", ignoreCase = true) -> "Pagi"
                shiftName.contains("Siang", ignoreCase = true) -> "Siang"
                shiftName.contains("Malam", ignoreCase = true) -> "Malam"
                shiftName.equals("Libur", ignoreCase = true) -> "Libur"
                else -> shiftName
            }
        }

        private fun setShiftColor(textView: TextView, shiftName: String?) {
            val simpleName = formatShiftName(shiftName)

            when {
                simpleName == "-" -> {
                    textView.setTextColor(0xFF666666.toInt())
                }
                simpleName == "Pagi" -> {
                    textView.setTextColor(0xFFE690A5.toInt())
                }
                simpleName == "Siang" -> {
                    textView.setTextColor(0xFFD81B60.toInt())
                }
                simpleName == "Malam" -> {
                    textView.setTextColor(0xFF880E4F.toInt())
                }
                simpleName == "Libur" -> {
                    textView.setTextColor(0xFF4CAF50.toInt())
                }
                else -> {
                    textView.setTextColor(0xFF333333.toInt())
                }
            }
        }

        private fun dpToPx(dp: Int, resources: Resources): Int {
            val density = resources.displayMetrics.density
            return (dp * density).toInt()
        }

        override fun getItemCount(): Int = 1
    }
}