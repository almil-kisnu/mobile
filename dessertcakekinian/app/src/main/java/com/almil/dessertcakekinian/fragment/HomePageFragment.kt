package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.AbsenMasukActivity
import com.almil.dessertcakekinian.activity.AbsenPulangActivity
import com.almil.dessertcakekinian.activity.DaftarProdukActivity
import com.almil.dessertcakekinian.activity.DiskonActivity
import com.almil.dessertcakekinian.activity.RiwayatActivity
import com.almil.dessertcakekinian.activity.TransaksiActivity
import com.almil.dessertcakekinian.activity.presensiActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.adapter.OnlineAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.almil.dessertcakekinian.model.OrderWithDetails

class HomePageFragment : Fragment() {

    private lateinit var tvNamaUser: TextView
    private lateinit var tvStatusUtama: TextView
    private lateinit var tvStatusTimestamp: TextView
    private lateinit var btnAksiAbsen: Button
    private lateinit var rvPesananOnline: RecyclerView
    private lateinit var onlineAdapter: OnlineAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
            // Initialize RecyclerView
            rvPesananOnline = view.findViewById(R.id.rvPesananOnline)
            setupRecyclerView()

            // Set username from user session
            val userSession = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userName = userSession.getString("USER_NAME", "") ?: ""
            tvNamaUser.text = userName

            // Setup button click listener dengan logika absen yang benar
            btnAksiAbsen.setOnClickListener {
                try {
                    val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
                    val status = prefs.getString("status_absen", "BELUM_ABSEN") ?: "BELUM_ABSEN"

                    when (status) {
                        "BELUM_ABSEN" -> startActivity(Intent(requireContext(), AbsenMasukActivity::class.java))
                        "SUDAH_MASUK" -> startActivity(Intent(requireContext(), AbsenPulangActivity::class.java))
                        "SUDAH_PULANG" -> startActivity(Intent(requireContext(), presensiActivity::class.java))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Tidak dapat membuka halaman absen", Toast.LENGTH_SHORT).show()
                }
            }

            // Setup menu click listeners
            setupMenuClickListeners(view)

            // Update UI
            updateUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } // Tambahkan kurung kurawal penutup yang hilang di sini

    private fun setupRecyclerView() {
        try {
            // Setup adapter dengan click listener
            onlineAdapter = OnlineAdapter(
                onOrderClick = { orderWithDetails ->
                    // Navigate ke dtOnlineFragment sebagai dialog
                    navigateToDetail(orderWithDetails)
                },
                onBadgeCountChanged = { count ->
                    // TAMBAHKAN: Simpan count ke SharedPreferences
                    updateBadgeCount(count)
                }
            )

            // Setup RecyclerView
            rvPesananOnline.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = onlineAdapter
            }

            // Setup observer untuk data
            setupOrderObserver()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBadgeCount(count: Int) {
        try {
            // Simpan count ke SharedPreferences untuk dibaca TransaksiFragment
            val sharedPref = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().putInt("pending_order_count", count).apply()

            // Log untuk debug
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
            view.findViewById<LinearLayout>(R.id.menuPresensi)?.setOnClickListener {
                startActivity(Intent(requireContext(), presensiActivity::class.java))
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        try {
            val prefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
            val status = prefs.getString("status_absen", "BELUM_ABSEN") ?: "BELUM_ABSEN"
            val jamMasuk = prefs.getString("jam_masuk_hari_ini", "") ?: ""
            val jamPulang = prefs.getString("jam_pulang_hari_ini", "") ?: ""

            // Tidak perlu runOnUiThread karena sudah di main thread
            when (status) {
                "SUDAH_PULANG" -> {
                    tvStatusUtama.text = "Sudah Absen"
                    tvStatusTimestamp.text = "Masuk: $jamMasuk | Pulang: $jamPulang"
                    btnAksiAbsen.text = "Lihat Riwayat"
                }
                "SUDAH_MASUK" -> {
                    tvStatusUtama.text = "Sudah Absen Masuk"
                    tvStatusTimestamp.text = "Jam Masuk: $jamMasuk"
                    btnAksiAbsen.text = "Absen Pulang"
                }
                else -> {
                    tvStatusUtama.text = "Belum Absen"
                    tvStatusTimestamp.text = "Silakan lakukan absen masuk"
                    btnAksiAbsen.text = "Absen Masuk"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Set default values
            tvStatusUtama.text = "Belum Absen"
            tvStatusTimestamp.text = "Silakan lakukan absen masuk"
            btnAksiAbsen.text = "Absen Masuk"
        }
    }
}