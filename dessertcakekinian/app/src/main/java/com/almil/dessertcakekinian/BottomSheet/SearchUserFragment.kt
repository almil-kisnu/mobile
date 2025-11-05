package com.almil.dessertcakekinian.BottomSheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.Outlet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

// Interface untuk mengkomunikasikan hasil filter
interface FilterAppliedListener {
    // filterStatus: null (Semua), true (Aktif), false (NonAktif)
    // filterRole: null (Semua), "Karyawan", "Admin", "Owner"
    // filterOutletId: null (Semua Outlet), Int (ID Outlet tertentu)
    fun onFilterApplied(filterStatus: Boolean?, filterRole: String?, filterOutletId: Int?)
}

class SearchUserFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SearchUserFragment"
        private const val ARG_STATUS_FILTER = "status_filter"
        private const val ARG_ROLE_FILTER = "role_filter"
        private const val ARG_OUTLET_FILTER = "outlet_filter"
        private const val ARG_IS_OWNER = "is_owner"
        private const val ARG_IS_ADMIN = "is_admin"

        fun newInstance(
            currentStatusFilter: Boolean?,
            currentRoleFilter: String?,
            currentOutletFilter: Int?, // ID Outlet saat ini (bisa null)
            isUserOwner: Boolean,
            isUserAdmin: Boolean = false
        ): SearchUserFragment {
            return SearchUserFragment().apply {
                arguments = Bundle().apply {
                    currentStatusFilter?.let { putBoolean(ARG_STATUS_FILTER, it) }
                    putString(ARG_ROLE_FILTER, currentRoleFilter)
                    // Menggunakan -1 sebagai penanda jika null agar getInt tidak mengembalikan 0
                    putInt(ARG_OUTLET_FILTER, currentOutletFilter ?: -1)
                    putBoolean(ARG_IS_OWNER, isUserOwner)
                    putBoolean(ARG_IS_ADMIN, isUserAdmin)
                }
            }
        }
    }

    private var filterAppliedListener: FilterAppliedListener? = null

    // State filter yang diterima
    private var currentStatusFilter: Boolean? = null
    private var currentRoleFilter: String? = null
    private var currentOutletFilter: Int? = null

    // State pengguna
    private var isOwner: Boolean = false
    private var isAdmin: Boolean = false

    // Variabel View
    private lateinit var tvOutletTitle: TextView
    private lateinit var spinnerOutlet: Spinner
    private lateinit var cbAktif: CheckBox
    private lateinit var cbNonAktif: CheckBox
    private lateinit var cbOwner: CheckBox
    private lateinit var cbKaryawan: CheckBox
    private lateinit var cbAdmin: CheckBox
    private lateinit var btnTerapin: Button
    private lateinit var btnRestart: Button

    // Variabel untuk data Spinner Outlet
    private var allOutlets = listOf<Outlet>()
    private var selectedOutletId: Int? = null // Akan menampung pilihan spinner JIKA isOwner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentStatusFilter = if (it.containsKey(ARG_STATUS_FILTER)) it.getBoolean(ARG_STATUS_FILTER) else null
            currentRoleFilter = it.getString(ARG_ROLE_FILTER)

            // ✅ PERBAIKAN: Membaca Outlet Filter dengan penanda -1
            val outletIdFromArgs = it.getInt(ARG_OUTLET_FILTER, -1)
            currentOutletFilter = if (outletIdFromArgs == -1) null else outletIdFromArgs

            isOwner = it.getBoolean(ARG_IS_OWNER, false)
            isAdmin = it.getBoolean(ARG_IS_ADMIN, false)
        }

        // Log untuk debug di onCreate
        Log.d(TAG, "onCreate: currentOutletFilter dibaca: $currentOutletFilter (IsOwner: $isOwner)")

        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi View
        tvOutletTitle = view.findViewById(R.id.tv_outlet_title)
        spinnerOutlet = view.findViewById(R.id.spinner_outlet)
        cbAktif = view.findViewById(R.id.cbAktif)
        cbNonAktif = view.findViewById(R.id.cbNonAktif)
        cbOwner = view.findViewById(R.id.cbOwner)
        cbKaryawan = view.findViewById(R.id.cbKaryawan)
        cbAdmin = view.findViewById(R.id.cbAdmin)
        btnTerapin = view.findViewById(R.id.btnTerapin)
        btnRestart = view.findViewById(R.id.btnRestart)

        setupViewVisibility()

        // ✅ LOGIKA KUNCI UNTUK NON-OWNER: selectedOutletId diset HANYA sekali dari currentOutletFilter
        if (isOwner) {
            fetchOutlets() // Owner menggunakan Spinner
        } else {
            // Jika bukan Owner, Spinner disembunyikan dan selectedOutletId TIDAK PERLU diubah
            // selectedOutletId secara efektif menjadi currentOutletFilter (ID pengguna)
            selectedOutletId = currentOutletFilter
            updateRestartButtonState()
        }

        setupCheckboxListeners()
        restoreFilterState()

        // Logika Tombol Terapin
        btnTerapin.setOnClickListener {
            applyFilter()
            dismiss()
        }

        // Logika Tombol Restart
        btnRestart.setOnClickListener {
            val defaultRole = if (isOwner) "Admin" else "Karyawan"
            // Outlet default: null untuk Owner, currentOutletFilter untuk non-Owner
            val defaultOutlet = if (isOwner) null else currentOutletFilter

            filterAppliedListener?.onFilterApplied(true, defaultRole, defaultOutlet)
            dismiss()
        }
    }

    // Setter untuk Listener
    fun setFilterAppliedListener(listener: FilterAppliedListener) {
        this.filterAppliedListener = listener
    }

    // Fungsi untuk mengambil data Outlet
    private fun fetchOutlets() {
        lifecycleScope.launch {
            try {
                val response = SupabaseClientProvider.client.from("outlet").select()
                allOutlets = response.decodeList<Outlet>()
                setupOutletSpinner()
                updateRestartButtonState()
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengambil data outlet: ", e)
                Toast.makeText(requireContext(), "Gagal memuat data toko.", Toast.LENGTH_SHORT).show()
                allOutlets = emptyList()
                setupOutletSpinner()
                updateRestartButtonState()
            }
        }
    }

    // Fungsi untuk mengatur Spinner Outlet (Hanya untuk Owner)
    private fun setupOutletSpinner() {
        val outletNames = allOutlets.map { "${it.kodeOutlet} - ${it.namaOutlet}" }.toMutableList()
        outletNames.add(0, "Semua Toko/Outlet")

        val outletAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, outletNames)
        outletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOutlet.adapter = outletAdapter

        // Set nilai awal Spinner berdasarkan currentOutletFilter yang diterima
        if (currentOutletFilter == null) {
            spinnerOutlet.setSelection(0)
            selectedOutletId = null
        } else {
            val currentOutletIndex = allOutlets.indexOfFirst { it.idOutlet == currentOutletFilter }
            if (currentOutletIndex != -1) {
                spinnerOutlet.setSelection(currentOutletIndex + 1)
                selectedOutletId = currentOutletFilter
            } else {
                spinnerOutlet.setSelection(0)
                selectedOutletId = null
            }
        }

        // Listener HANYA dipasang jika Owner
        spinnerOutlet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedOutletId = if (position > 0) {
                    allOutlets.getOrNull(position - 1)?.idOutlet
                } else {
                    null
                }
                updateRestartButtonState()
            }
            override fun onNothingSelected(parent: AdapterView<*>) { selectedOutletId = null }
        }
    }

    // Fungsi untuk mengatur visibilitas View berdasarkan status pengguna
    private fun setupViewVisibility() {
        if (isOwner) {
            tvOutletTitle.visibility = View.VISIBLE
            spinnerOutlet.visibility = View.VISIBLE
            cbOwner.visibility = View.VISIBLE
        } else {
            // ✅ Non-Owner: Spinner dan Title HILANG
            tvOutletTitle.visibility = View.GONE
            spinnerOutlet.visibility = View.GONE
            cbOwner.visibility = View.GONE
        }
    }

    // Atur listener untuk memastikan hanya satu status/role yang terpilih
    private fun setupCheckboxListeners() {
        val statusGroup = listOf(cbAktif, cbNonAktif)
        val roleGroup = mutableListOf(cbKaryawan, cbAdmin).apply {
            if (isOwner) add(cbOwner)
        }

        // Status Group
        statusGroup.forEach { cb ->
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    statusGroup.filter { it != cb }.forEach { it.isChecked = false }
                }
                updateRestartButtonState()
            }
        }

        // Role Group
        roleGroup.forEach { cb ->
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    roleGroup.filter { it != cb }.forEach { it.isChecked = false }
                }
                updateRestartButtonState()
            }
        }
    }

    // Mengembalikan status CheckBox dan Spinner berdasarkan filter saat ini
    private fun restoreFilterState() {
        val defaultRole = if (isOwner) "Admin" else "Karyawan"

        // Status
        cbAktif.isChecked = currentStatusFilter == true
        cbNonAktif.isChecked = currentStatusFilter == false
        if (!cbAktif.isChecked && !cbNonAktif.isChecked) {
            cbAktif.isChecked = true
        }

        // Role
        cbKaryawan.isChecked = currentRoleFilter.equals("Karyawan", ignoreCase = true)
        cbAdmin.isChecked = currentRoleFilter.equals("Admin", ignoreCase = true)
        if (isOwner) {
            cbOwner.isChecked = currentRoleFilter.equals("Owner", ignoreCase = true)
        }
        if (!cbKaryawan.isChecked && !cbAdmin.isChecked && (!isOwner || !cbOwner.isChecked)) {
            when (defaultRole) {
                "Admin" -> cbAdmin.isChecked = true
                "Karyawan" -> cbKaryawan.isChecked = true
            }
        }

        // Catatan: selectedOutletId sudah diset di onViewCreated (jika non-Owner) atau di setupOutletSpinner (jika Owner)
    }

    // Menerapkan filter dan memanggil listener
    private fun applyFilter() {
        val statusFilter: Boolean? = when {
            cbAktif.isChecked -> true
            cbNonAktif.isChecked -> false
            else -> null // Semua Status
        }

        val roleFilter: String? = when {
            cbOwner.isChecked -> "Owner"
            cbAdmin.isChecked -> "Admin"
            cbKaryawan.isChecked -> "Karyawan"
            else -> null // Semua Role
        }

        // ✅ LOGIKA PENENTUAN ID OUTLET FINAL:
        // selectedOutletId HANYA digunakan jika Owner (karena hanya owner yang melihat spinner)
        // Jika bukan Owner, selectedOutletId sama dengan currentOutletFilter (ID pengguna) yang diset di onViewCreated
        val finalOutletId = selectedOutletId

        Log.d(TAG, "Filter Diterapkan - Final Outlet ID: $finalOutletId")

        filterAppliedListener?.onFilterApplied(statusFilter, roleFilter, finalOutletId)
    }

    // Memperbarui status tombol Restart
    private fun updateRestartButtonState() {
        val defaultRole = if (isOwner) "Admin" else "Karyawan"
        val defaultOutlet = if (isOwner) null else currentOutletFilter // Default outlet yang seharusnya

        // Cek role yang sedang terpilih
        val roleCheck = when {
            cbOwner.isChecked -> "Owner"
            cbAdmin.isChecked -> "Admin"
            cbKaryawan.isChecked -> "Karyawan"
            else -> null
        }

        // Cek ID Outlet yang sedang terpilih (menggunakan selectedOutletId yang sudah diset)
        val outletCheck = selectedOutletId

        // Cek apakah filter saat ini adalah filter default
        val isDefaultFilter = cbAktif.isChecked &&
                !cbNonAktif.isChecked &&
                roleCheck.equals(defaultRole, ignoreCase = true) &&
                outletCheck == defaultOutlet

        btnRestart.isEnabled = !isDefaultFilter
    }
}