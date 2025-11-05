package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.EditUserActivity
import com.almil.dessertcakekinian.adapter.OnUserActionListener
import com.almil.dessertcakekinian.adapter.OnUserItemClickListener
import com.almil.dessertcakekinian.adapter.UserAdapter
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.Pengguna
import com.almil.dessertcakekinian.BottomSheet.FilterAppliedListener
import com.almil.dessertcakekinian.BottomSheet.SearchUserFragment
import com.almil.dessertcakekinian.dialog.UserAddedListener
import com.almil.dessertcakekinian.dialog.dialogUserDetailFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import com.almil.dessertcakekinian.activity.addUserActivity

class ManagementUsersFragment : Fragment(), UserAddedListener, OnUserItemClickListener,
    FilterAppliedListener, OnUserActionListener {

    // --- ARGUMENTS DAN STATE ROLE (Diambil Langsung dari SharedPreferences) ---
    private var isUserOwner: Boolean = false
    private var isUserAdmin: Boolean = false
    private var userRole: String? = null
    private var currentUserOutletId: Int? = null
    private var currentUserName: String? = null
    // --------------------------------

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var etSearch: EditText
    private lateinit var btnFilter: ImageButton
    private lateinit var btnReload: MaterialButton
    private lateinit var tvError: TextView

    private var allUsersList = mutableListOf<Pengguna>()
    private var filteredUserList = mutableListOf<Pengguna>()

    // Filter yang sedang diterapkan
    private var currentStatusFilter: Boolean? = true
    private var currentRoleFilter: String? = null
    private var currentOutletFilter: Int? = null


    private val editUserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == EditUserActivity.RESULT_USER_UPDATED) {
            onUserUpdated()
        }
    }

    private val addUserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Cek apakah hasil berasal dari AddUserActivity dan sukses
        if (result.resultCode == addUserActivity.RESULT_USER_ADDED) {
            onUserAdded() // Panggil fungsi untuk memuat ulang data
        }
    }

    private val onEditActionListener = object : OnUserActionListener {
        override fun onEditClicked(pengguna: Pengguna) {
            this@ManagementUsersFragment.onEditClicked(pengguna)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ PERUBAHAN UTAMA: AMBIL DATA ROLE DAN ID OUTLET DARI SHARED PREFERENCES
        val sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)

        userRole = sharedPreferences.getString("USER_ROLE", null)
        currentUserName = sharedPreferences.getString("USER_NAME", null)
        currentUserOutletId = sharedPreferences.getInt("USER_OUTLET_ID", -1).let {
            if (it == -1) null else it // -1 adalah nilai default jika tidak ada
        }

        // --- Logcat Informasi Pengguna ---
        Log.d("UserManagementInfo", "Pengguna saat ini: ${currentUserName}")
        Log.d("UserManagementInfo", "Role Pengguna: ${userRole}")
        Log.d("UserManagementInfo", "ID Outlet Pengguna: ${currentUserOutletId ?: "Tidak Ada/Global"}")
        // ---------------------------------

        // Tentukan status boolean dari role
        isUserOwner = userRole.equals("owner", ignoreCase = true)
        isUserAdmin = userRole.equals("admin", ignoreCase = true)

        // Tentukan Default Role Filter n Outletnya
        currentStatusFilter = true
        currentRoleFilter = if (isUserOwner) "Admin" else "Karyawan"
        currentOutletFilter = if (isUserOwner) {
            null // Owner: Semua Toko/Outlet
        } else {
            currentUserOutletId // Admin/Karyawan: Outletnya sendiri
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_management_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Logcat Tambahan di onViewCreated (Untuk Debugging UI/State) ---
        Log.d("UserManagementInfo", "onViewCreated: Role Owner? $isUserOwner, Role Admin? $isUserAdmin")
        Log.d("UserManagementInfo", "onViewCreated: Default Filter Outlet ID: $currentOutletFilter")
        // -------------------------------------------------------------------

        recyclerView = view.findViewById(R.id.recyclerViewUsers)
        etSearch = view.findViewById(R.id.edit_text_search)
        btnFilter = view.findViewById(R.id.button_filter)
        btnReload = view.findViewById(R.id.button_reload)
        tvError = view.findViewById(R.id.textViewError)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        userAdapter = UserAdapter(filteredUserList, this)
        recyclerView.adapter = userAdapter

        setupFilterButton()
        setupSearch()
        setupReloadButton()

        val fabAddUser = view.findViewById<FloatingActionButton>(R.id.fabAddUser)
        // Atur visibilitas FAB berdasarkan role yang sudah dibaca
        fabAddUser.visibility = if (isUserOwner || isUserAdmin) View.VISIBLE else View.GONE

        // ✅ Logika FAB Add User
        fabAddUser.setOnClickListener {
            if (isUserOwner || isUserAdmin) {
                val intent = Intent(requireContext(), addUserActivity::class.java)
                addUserLauncher.launch(intent) // Gunakan launcher yang baru
            }
        }
        fetchUsers()
    }

    private fun setupReloadButton() {
        btnReload.setOnClickListener {
            // Sembunyikan pesan error/reload
            showReloadUI(false)
            // Muat ulang data
            fetchUsers()
        }
    }

    private fun showReloadUI(show: Boolean, errorMessage: String? = null) {
        if (show) {
            recyclerView.visibility = View.GONE
            btnReload.visibility = View.VISIBLE
            tvError.visibility = View.VISIBLE
            tvError.text = errorMessage ?: "Gagal mengambil data. Coba muat ulang."
        } else {
            recyclerView.visibility = View.VISIBLE
            btnReload.visibility = View.GONE
            tvError.visibility = View.GONE
        }
    }

    private fun setupFilterButton() {
        btnFilter.setOnClickListener {
            // ✅ Mengirim currentOutletFilter ke BottomSheet
            val bottomSheet = SearchUserFragment.newInstance(
                currentStatusFilter,
                currentRoleFilter,
                currentOutletFilter,
                isUserOwner,
                isUserAdmin
            )
            bottomSheet.setFilterAppliedListener(this)
            bottomSheet.show(childFragmentManager, SearchUserFragment.TAG)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener {
            applyFiltersAndSearch()
        }
    }

    private fun applyFiltersAndSearch() {
        val searchQuery = etSearch.text.toString().trim().lowercase()
        val selectedStatus = currentStatusFilter
        val selectedRole = currentRoleFilter
        val selectedOutlet = currentOutletFilter

        var currentList = allUsersList.toList()

        // 1. Filter Status
        if (selectedStatus != null) {
            val isActiveFilter = selectedStatus
            currentList = currentList.filter { it.isActive == isActiveFilter }
        }

        // 2. Filter Role
        if (selectedRole != null) {
            currentList = currentList.filter { it.role.equals(selectedRole, ignoreCase = true) }
        }

        // 3. Filter Outlet (Logika filter disesuaikan agar selalu mengecek idOutlet)
        if (!isUserOwner && currentUserOutletId != null) {
            // Admin/Karyawan: Batasi hanya pada outletnya sendiri
            currentList = currentList.filter { it.idOutlet == currentUserOutletId }
            Log.d("UserManagementFilter", "Filter Outlet: Dibatasi ke ID Outlet $currentUserOutletId (Admin/Karyawan)")
        } else if (isUserOwner && selectedOutlet != null) {
            // Owner dengan Filter Outlet yang dipilih
            currentList = currentList.filter { it.idOutlet == selectedOutlet }
            Log.d("UserManagementFilter", "Filter Outlet: Diterapkan ID Outlet $selectedOutlet (Owner/Filter)")
        } else if (isUserOwner && selectedOutlet == null) {
            // Owner tanpa Filter Outlet (Tampilkan semua)
            Log.d("UserManagementFilter", "Filter Outlet: Tidak ada batasan (Owner)")
        } else {
            // Admin/Karyawan tanpa ID Outlet (Tidak seharusnya terjadi jika sistem login benar)
            Log.d("UserManagementFilter", "Filter Outlet: Tidak ada data pengguna karena ID Outlet null dan bukan Owner")
        }


        // 4. Search Query
        if (searchQuery.isNotEmpty()) {
            currentList = currentList.filter {
                it.username.lowercase().contains(searchQuery) ||
                        it.nik?.contains(searchQuery) == true ||
                        it.phone.contains(searchQuery) ||
                        it.outlet?.kodeOutlet?.lowercase()?.contains(searchQuery) == true
            }
        }

        Log.d("UserManagementFilter", "Hasil Filter/Search: ${currentList.size} pengguna ditemukan")

        filteredUserList.clear()
        filteredUserList.addAll(currentList)
        userAdapter.notifyDataSetChanged()

        // Cek filter default untuk tombol Filter
        val defaultRole = if (isUserOwner) "Admin" else "Karyawan"
        val defaultOutlet = if (isUserOwner) null else currentUserOutletId

        val filterIsDefault = currentStatusFilter == true &&
                currentRoleFilter.equals(defaultRole, ignoreCase = true) &&
                currentOutletFilter == defaultOutlet

        if (filterIsDefault) {
            btnFilter.setImageResource(R.drawable.ic_filter_off)
        } else {
            btnFilter.setImageResource(R.drawable.ic_filter_on)
        }
    }

    private fun fetchUsers() {
        // Pastikan UI error disembunyikan sebelum memulai fetch baru
        showReloadUI(false)

        lifecycleScope.launch {
            try {
                val supabase = SupabaseClientProvider.client

                // Ambil data pengguna dan gabungkan (join) dengan data outlet
                val response = supabase.from("pengguna").select(
                    Columns.raw("*, outlet(idoutlet, kode_outlet, nama_outlet)")
                ) {
                    order("createdat", order = Order.DESCENDING)
                }

                val fetchedUsers = response.decodeList<Pengguna>()

                Log.d("UserManagementData", "Data pengguna berhasil diambil: ${fetchedUsers.size} total")

                allUsersList.clear()
                allUsersList.addAll(fetchedUsers)
                applyFiltersAndSearch()

                // Pastikan RecyclerView terlihat jika fetch berhasil
                showReloadUI(false)

            } catch (e: Exception) {
                Log.e("ManagementUsersFragment", "Gagal mengambil data: ", e)
                // Tampilkan UI reload/error jika terjadi kegagalan
                val errorMessage = "Gagal memuat data: ${e.message}. Coba lagi."
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                showReloadUI(true, errorMessage)
            }
        }
    }

    // --- IMPLEMENTASI INTERFACE ---

    override fun onUserItemClicked(pengguna: Pengguna) {
        val dialog = dialogUserDetailFragment.newInstance(pengguna, onEditActionListener)
        dialog.show(childFragmentManager, dialogUserDetailFragment.TAG)
    }

    override fun onEditClicked(pengguna: Pengguna) {
        val intent = Intent(requireContext(), EditUserActivity::class.java)
        intent.putExtra(EditUserActivity.EXTRA_USER_JSON, Json.encodeToString(pengguna))
        editUserLauncher.launch(intent)
    }

    override fun onFilterApplied(filterStatus: Boolean?, filterRole: String?, filterOutletId: Int?) {
        Log.d("UserManagementFilter", "Filter Diterapkan: Status=$filterStatus, Role=$filterRole, Outlet ID=$filterOutletId")
        currentStatusFilter = filterStatus
        currentRoleFilter = filterRole
        currentOutletFilter = filterOutletId
        applyFiltersAndSearch()
    }

    fun onUserUpdated() {
        Log.d("ManagementUsersFragment", "Sinyal update diterima, memuat ulang data...")
        fetchUsers()
    }

    override fun onUserAdded() {
        Log.d("ManagementUsersFragment", "Sinyal tambah pengguna diterima, memuat ulang data...")
        fetchUsers()
    }
}