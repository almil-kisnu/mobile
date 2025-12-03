package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.AbsenMasukActivity
import com.almil.dessertcakekinian.activity.AbsenPulangActivity
import com.almil.dessertcakekinian.activity.DaftarProdukActivity
import com.almil.dessertcakekinian.activity.DiskonActivity
import com.almil.dessertcakekinian.activity.RiwayatActivity
import com.almil.dessertcakekinian.activity.TransaksiActivity
import com.almil.dessertcakekinian.activity.JadwalActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.adapter.OnlineAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.almil.dessertcakekinian.activity.TransferActivity
import kotlinx.coroutines.launch
import com.almil.dessertcakekinian.model.OrderWithDetails
import com.almil.dessertcakekinian.database.PenggunaApi
import com.almil.dessertcakekinian.database.SupabaseHelper
import com.almil.dessertcakekinian.database.JadwalMingguanApi
import java.text.SimpleDateFormat
import java.util.*

class HomePageFragment : Fragment() {

    private lateinit var tvNamaUser: TextView
    private lateinit var tvStatusUtama: TextView
    private lateinit var tvStatusTimestamp: TextView
    private lateinit var btnAksiAbsen: Button
    private lateinit var rvPesananOnline: RecyclerView
    private lateinit var onlineAdapter: OnlineAdapter
    private lateinit var ivStatusIcon: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Initialize views
            tvNamaUser = view.findViewById(R.id.tvNamaUser)
            tvStatusUtama = view.findViewById(R.id.tvStatusUtama)
            tvStatusTimestamp = view.findViewById(R.id.tvStatusTimestamp)
            btnAksiAbsen = view.findViewById(R.id.btnAksiAbsen)
            ivStatusIcon = view.findViewById(R.id.ivStatusIcon)

            // Initialize RecyclerView
            rvPesananOnline = view.findViewById(R.id.rvPesananOnline)
            setupRecyclerView()

            // Ambil username dan shift hari ini
            getCorrectUsernameAndShiftFromSupabase()

            // Setup button click listener dengan logika absen yang benar
            setupAbsenButtonListener()

            // Setup menu click listeners
            setupMenuClickListeners(view)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ✅ PERBAIKAN: Setup button listener yang dinamis
    private fun setupAbsenButtonListener() {
        btnAksiAbsen.setOnClickListener {
            try {
                // Cek status REAL-TIME dari database
                checkAbsenStatusRealTime { status, sudahAbsen, sudahCheckout ->
                    when {
                        !sudahAbsen -> {
                            // Absen Masuk
                            val intent = Intent(requireContext(), AbsenMasukActivity::class.java)
                            @Suppress("DEPRECATION")
                            startActivityForResult(intent, REQUEST_CODE_ABSEN_MASUK)
                        }
                        sudahAbsen && !sudahCheckout -> {
                            // Absen Pulang
                            val intent = Intent(requireContext(), AbsenPulangActivity::class.java)
                            @Suppress("DEPRECATION")
                            startActivityForResult(intent, REQUEST_CODE_ABSEN_PULANG)
                        }
                        else -> {
                            // Button "Lihat Riwayat" -> ke PresensiFragment
                            navigateToPresensiFragment()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Tidak dapat membuka halaman absen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ PERBAIKAN BESAR: Navigasi ke PresensiFragment dengan deteksi container otomatis
    private fun navigateToPresensiFragment() {
        try {
            println("🔍 Mencari container ID untuk navigasi fragment...")

            // METODE 1: Coba ambil ID dari parent view
            val parentView = view?.parent as? ViewGroup
            val containerId = parentView?.id

            println("🔍 Parent Container ID: $containerId")
            println("🔍 Parent View Class: ${parentView?.javaClass?.simpleName}")

            // METODE 2: Coba cari container umum
            val possibleContainerIds = listOf(
                R.id.fragment_container,
                R.id.nav_host_fragment,
                R.id.main_container,
                R.id.container,
                android.R.id.content
            )

            var validContainerId: Int? = null

            // Cek container ID yang valid
            for (id in possibleContainerIds) {
                try {
                    val containerView = requireActivity().findViewById<View>(id)
                    if (containerView != null) {
                        validContainerId = id
                        println("✅ Container ditemukan dengan ID: ${getResourceName(id)}")
                        break
                    }
                } catch (_: Exception) {
                    // ID tidak ada, lanjut ke ID berikutnya
                    continue
                }
            }

            // Jika tidak ada container yang ditemukan, gunakan container parent
            if (validContainerId == null) {
                validContainerId = containerId ?: android.R.id.content
                println("⚠️ Menggunakan container fallback: ${getResourceName(validContainerId)}")
            }

            // Lakukan fragment transaction
            val fragmentManager = parentFragmentManager
            val transaction = fragmentManager.beginTransaction()

            val presensiFragment = PresensiFragment()

            // Gunakan setCustomAnimations untuk transisi yang smooth
            transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )

            transaction.replace(validContainerId, presensiFragment)
            transaction.addToBackStack("home_to_presensi")
            transaction.commit()

            println("✅ Navigasi ke PresensiFragment berhasil dengan container: ${getResourceName(validContainerId)}")

        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Error navigasi: ${e.message}")
            println("❌ Stack trace: ${e.stackTraceToString()}")
            Toast.makeText(
                context,
                "Gagal membuka halaman presensi. Silakan coba lagi.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Helper function untuk mendapatkan nama resource ID
    private fun getResourceName(resourceId: Int): String {
        return try {
            resources.getResourceEntryName(resourceId)
        } catch (e: Exception) {
            "unknown_$resourceId"
        }
    }

    // ✅ Tambahkan onActivityResult untuk menangani hasil dari aktivitas absen
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_ABSEN_MASUK -> {
                println("🔄 HomePageFragment - Kembali dari Absen Masuk, refresh status...")
                loadAbsenStatusRealTime()
            }
            REQUEST_CODE_ABSEN_PULANG -> {
                println("🔄 HomePageFragment - Kembali dari Absen Pulang, refresh status...")
                loadAbsenStatusRealTime()
            }
        }
    }

    // ✅ Ambil username DAN shift hari ini sekaligus
    private fun getCorrectUsernameAndShiftFromSupabase() {
        val userSession = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val storedUsername = userSession.getString("USER_NAME", "") ?: ""

        if (storedUsername.isEmpty()) {
            println("❌ HomePageFragment - Tidak ada username di SharedPreferences")
            tvNamaUser.text = "Karyawan"
            loadAbsenStatusRealTime()
            return
        }

        println("🔍 HomePageFragment - Mencari username dan shift untuk: '$storedUsername'")

        PenggunaApi().listAll(object : PenggunaApi.PenggunaListCallback {
            override fun onSuccess(list: List<PenggunaApi.Pengguna>) {
                println("📋 HomePageFragment - Daftar semua user di database:")
                list.forEach { user ->
                    println("   - '${user.username}' (ID: ${user.iduser})")
                }

                val correctUser = list.find { user ->
                    user.username.equals(storedUsername, ignoreCase = true) ||
                            storedUsername.contains(user.username, ignoreCase = true) ||
                            user.username.contains(storedUsername, ignoreCase = true)
                }

                if (correctUser != null) {
                    val correctUsername = correctUser.username
                    println("✅ HomePageFragment - Username ditemukan: '$storedUsername' -> '$correctUsername'")

                    userSession.edit().putString("USER_NAME", correctUsername).apply()
                    tvNamaUser.text = correctUsername

                    println("🎉 HomePageFragment - Selamat datang: $correctUsername")

                    getUserShiftToday(correctUser.iduser, correctUsername)
                } else {
                    println("❌ HomePageFragment - Username '$storedUsername' tidak ditemukan di database")
                    tvNamaUser.text = storedUsername
                    loadAbsenStatusRealTime()
                }
            }

            override fun onError(error: String) {
                println("❌ HomePageFragment - Gagal ambil data user: $error")
                tvNamaUser.text = storedUsername
                loadAbsenStatusRealTime()
            }
        })
    }

    // ✅ Ambil shift user untuk hari ini
    private fun getUserShiftToday(userId: Int, userName: String) {
        JadwalMingguanApi().listAllWithDetails(object : JadwalMingguanApi.JadwalListCallback {
            override fun onSuccess(jadwalList: List<JadwalMingguanApi.JadwalMingguan>) {
                val userJadwal = jadwalList.find { it.id_pengguna == userId }
                val shiftHariIni = getShiftForToday(userJadwal)

                println("📅 HomePageFragment - Shift hari ini untuk $userName: $shiftHariIni")

                val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                prefs.edit().putString("user_shift", shiftHariIni).apply()

                loadAbsenStatusRealTime()
            }

            override fun onError(error: String) {
                println("❌ HomePageFragment - Gagal ambil jadwal: $error")
                val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                prefs.edit().putString("user_shift", "Pagi").apply()
                loadAbsenStatusRealTime()
            }
        })
    }

    // ✅ Tentukan shift berdasarkan hari ini
    private fun getShiftForToday(userJadwal: JadwalMingguanApi.JadwalMingguan?): String {
        if (userJadwal == null) return "Pagi"

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        return when (dayOfWeek) {
            Calendar.MONDAY -> userJadwal.shift_senin.ifEmpty { "Pagi" }
            Calendar.TUESDAY -> userJadwal.shift_selasa.ifEmpty { "Pagi" }
            Calendar.WEDNESDAY -> userJadwal.shift_rabu.ifEmpty { "Pagi" }
            Calendar.THURSDAY -> userJadwal.shift_kamis.ifEmpty { "Pagi" }
            Calendar.FRIDAY -> userJadwal.shift_jumat.ifEmpty { "Pagi" }
            Calendar.SATURDAY -> userJadwal.shift_sabtu.ifEmpty { "Pagi" }
            Calendar.SUNDAY -> userJadwal.shift_minggu.ifEmpty { "Pagi" }
            else -> "Pagi"
        }.also { shift ->
            println("📅 HomePageFragment - Hari ini: ${getHariName(dayOfWeek)}, Shift: $shift")
        }
    }

    // ✅ Helper untuk nama hari
    private fun getHariName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            Calendar.SUNDAY -> "Minggu"
            else -> "Unknown"
        }
    }

    // ✅ Fungsi baca status absen REAL-TIME dari database
    private fun loadAbsenStatusRealTime() {
        val userSession = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (userName.isEmpty()) {
            println("❌ HomePageFragment - Tidak bisa load status: nama user kosong")
            updateUI("BELUM_ABSEN", "", "")
            return
        }

        val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val userShift = prefs.getString("user_shift", "Pagi") ?: "Pagi"

        println("🔍 HomePageFragment - Cek status absen REAL-TIME: $userName, $currentDate, Shift: $userShift")

        // ✅ PERBAIKAN: Menggunakan fungsi yang benar dari SupabaseHelper
        SupabaseHelper().checkAbsenStatusWithShift(userName, currentDate, userShift) { sudahAbsen, sudahCheckout, status ->
            println("🔍 HomePageFragment - Status dari database: sudahAbsen=$sudahAbsen, sudahCheckout=$sudahCheckout, status=$status")

            if (sudahAbsen) {
                SupabaseHelper().getAbsenDetail(userName, currentDate, userShift) { jamMasuk, jamPulang, _ ->
                    runOnUiThread {
                        when {
                            !sudahAbsen -> updateUI("BELUM_ABSEN", "", "")
                            sudahAbsen && !sudahCheckout -> updateUI("SUDAH_MASUK", jamMasuk, "")
                            sudahAbsen && sudahCheckout -> updateUI("SUDAH_PULANG", jamMasuk, jamPulang)
                            else -> updateUI("BELUM_ABSEN", "", "")
                        }
                    }
                }
            } else {
                runOnUiThread {
                    updateUI("BELUM_ABSEN", "", "")
                }
            }
        }
    }

    // ✅ PERBAIKAN: Fungsi cek status real-time untuk button click - FIXED PARAMETER COUNT
    private fun checkAbsenStatusRealTime(callback: (status: String, sudahAbsen: Boolean, sudahCheckout: Boolean) -> Unit) {
        val userSession = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (userName.isEmpty()) {
            callback("BELUM_ABSEN", false, false)
            return
        }

        val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val userShift = prefs.getString("user_shift", "Pagi") ?: "Pagi"

        // ✅ PERBAIKAN: Menggunakan fungsi yang benar dengan 3 parameter output
        SupabaseHelper().checkAbsenStatusWithShift(userName, currentDate, userShift) { sudahAbsen, sudahCheckout, status ->
            val finalStatus = when {
                !sudahAbsen -> "BELUM_ABSEN"
                sudahAbsen && !sudahCheckout -> "SUDAH_MASUK"
                sudahAbsen && sudahCheckout -> "SUDAH_PULANG"
                else -> "BELUM_ABSEN"
            }
            callback(finalStatus, sudahAbsen, sudahCheckout)
        }
    }

    // ✅ Fungsi update UI dengan WARNA ABU-ABU untuk belum absen
    private fun updateUI(status: String, jamMasuk: String, jamPulang: String) {
        try {
            println("🔍 HomePageFragment - Update UI: status=$status, jamMasuk=$jamMasuk, jamPulang=$jamPulang")

            val userSession = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userName = userSession.getString("USER_NAME", "") ?: "default"

            val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("status_absen_$userName", status)
            editor.putString("jam_masuk_hari_ini_$userName", jamMasuk)
            editor.putString("jam_pulang_hari_ini_$userName", jamPulang)
            editor.apply()

            when (status) {
                "SUDAH_PULANG" -> {
                    // ✅ HIJAU dengan CENTANG HIJAU
                    tvStatusUtama.text = "Sudah Absen Hari Ini"
                    tvStatusUtama.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_success))
                    tvStatusTimestamp.text = "Masuk: $jamMasuk | Pulang: $jamPulang"
                    tvStatusTimestamp.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_success))
                    btnAksiAbsen.text = "Lihat Riwayat"

                    // Icon centang HIJAU
                    ivStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green_success))
                    ivStatusIcon.visibility = View.VISIBLE
                }
                "SUDAH_MASUK" -> {
                    // ⚪ ABU-ABU (belum absen pulang)
                    tvStatusUtama.text = "Belum Absen Pulang"
                    tvStatusUtama.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_default))
                    tvStatusTimestamp.text = "Jam Masuk: $jamMasuk"
                    tvStatusTimestamp.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_default))
                    btnAksiAbsen.text = "Absen Pulang"

                    // Icon abu-abu
                    ivStatusIcon.clearColorFilter()
                    ivStatusIcon.visibility = View.VISIBLE
                }
                else -> {
                    // ⚪ ABU-ABU (belum absen masuk)
                    tvStatusUtama.text = "Belum Absen"
                    tvStatusUtama.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_default))
                    tvStatusTimestamp.text = "Silakan lakukan absen masuk"
                    tvStatusTimestamp.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_default))
                    btnAksiAbsen.text = "Absen Masuk"

                    // Icon abu-abu
                    ivStatusIcon.clearColorFilter()
                    ivStatusIcon.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Default state juga ABU-ABU
            tvStatusUtama.text = "Belum Absen"
            tvStatusUtama.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_default))
            tvStatusTimestamp.text = "Silakan lakukan absen masuk"
            tvStatusTimestamp.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_default))
            btnAksiAbsen.text = "Absen Masuk"
            ivStatusIcon.clearColorFilter()
        }
    }

    private fun runOnUiThread(action: () -> Unit) {
        if (isAdded && context != null) {
            requireActivity().runOnUiThread(action)
        }
    }

    private fun setupRecyclerView() {
        try {
            onlineAdapter = OnlineAdapter(
                onOrderClick = { orderWithDetails ->
                    navigateToDetail(orderWithDetails)
                },
                onBadgeCountChanged = { count ->
                    updateBadgeCount(count)
                }
            )

            rvPesananOnline.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = onlineAdapter
            }

            setupOrderObserver()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBadgeCount(count: Int) {
        try {
            val sharedPref = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().putInt("pending_order_count", count).apply()
            android.util.Log.d("HomePageFragment", "Badge count updated and saved: $count")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun navigateToDetail(orderWithDetails: OrderWithDetails) {
        val detailDialog = dtOnlineFragment.newInstance(orderWithDetails)
        detailDialog.show(parentFragmentManager, "DetailOnlineDialog")
    }

    private fun setupOrderObserver() {
        try {
            val viewModel: com.almil.dessertcakekinian.model.OrderViewModel by viewModels()

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.allOrders.collect { state ->
                        when (state) {
                            is com.almil.dessertcakekinian.model.OrderDataState.Success -> {
                                onlineAdapter.submitFilteredList(state.orders, requireContext())
                            }
                            is com.almil.dessertcakekinian.model.OrderDataState.Error -> {
                                if (state.cachedOrders.isNotEmpty()) {
                                    onlineAdapter.submitFilteredList(state.cachedOrders, requireContext())
                                }
                            }
                            else -> {
                                // Loading state
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupMenuClickListeners(view: View) {
        try {
            // ✅ Menu Presensi navigasi ke PresensiFragment
            view.findViewById<LinearLayout>(R.id.menuPresensi)?.setOnClickListener {
                navigateToPresensiFragment()
            }

            view.findViewById<LinearLayout>(R.id.menuProduk)?.setOnClickListener {
                startActivity(Intent(requireContext(), DaftarProdukActivity::class.java))
            }

            view.findViewById<LinearLayout>(R.id.menuTransaksi)?.setOnClickListener {
                startActivity(Intent(requireContext(), TransaksiActivity::class.java))
            }

            view.findViewById<LinearLayout>(R.id.menuRiwayat)?.setOnClickListener {
                startActivity(Intent(requireContext(), RiwayatActivity::class.java))
            }

            view.findViewById<LinearLayout>(R.id.menuDiskon)?.setOnClickListener {
                startActivity(Intent(requireContext(), DiskonActivity::class.java))
            }
            view.findViewById<LinearLayout>(R.id.menurequest)?.setOnClickListener {
                startActivity(Intent(requireContext(), TransferActivity::class.java))
            }
            view.findViewById<LinearLayout>(R.id.menujadwal)?.setOnClickListener {
                startActivity(Intent(requireContext(), JadwalActivity::class.java))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        println("🔄 HomePageFragment - onResume: Load data REAL-TIME")
        getCorrectUsernameAndShiftFromSupabase()
    }

    companion object {
        private const val REQUEST_CODE_ABSEN_MASUK = 1001
        private const val REQUEST_CODE_ABSEN_PULANG = 1002
    }
}