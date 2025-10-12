package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.OnUserActionListener
import com.almil.dessertcakekinian.adapter.UserAdapter
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.Pengguna
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

class ManagementUsersFragment : Fragment(), UserAddedListener, OnUserActionListener, UserUpdatedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var etSearch: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var spinnerRole: Spinner

    // ✅ LANGKAH 1: Buat 2 list. Satu untuk menyimpan data asli, satu untuk yang ditampilkan.
    private var allUsersList = mutableListOf<Pengguna>()
    private var filteredUserList = mutableListOf<Pengguna>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_management_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi semua view
        recyclerView = view.findViewById(R.id.recyclerViewUsers)
        etSearch = view.findViewById(R.id.etSearch)
        spinnerStatus = view.findViewById(R.id.spinnerStatus)
        spinnerRole = view.findViewById(R.id.spinnerRole)

        // Setup RecyclerView dengan list yang akan difilter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        userAdapter = UserAdapter(filteredUserList, this)
        recyclerView.adapter = userAdapter

        // Setup komponen UI lainnya
        setupSpinners()
        setupSearch()

        val fabAddUser = view.findViewById<FloatingActionButton>(R.id.fabAddUser)
        fabAddUser.setOnClickListener {
            val bottomSheet = AddUserBottomSheetFragment.newInstance()
            bottomSheet.setUserAddedListener(this)
            bottomSheet.show(childFragmentManager, AddUserBottomSheetFragment.TAG)
        }

        fetchUsers()
    }

    private fun setupSpinners() {
        // Setup Spinner Status
        val statusAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.status_filter_options, android.R.layout.simple_spinner_item
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = statusAdapter
        // Set default ke "Aktif"
        spinnerStatus.setSelection(1)

        // Setup Spinner Role
        val roleAdapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.role_filter_options, android.R.layout.simple_spinner_item
        )
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = roleAdapter
        // Set default ke "Karyawan"
        spinnerRole.setSelection(2)


        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFiltersAndSearch() // Panggil filter setiap kali pilihan spinner berubah
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerStatus.onItemSelectedListener = listener
        spinnerRole.onItemSelectedListener = listener
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener {
            applyFiltersAndSearch() // Panggil filter setiap kali teks pencarian berubah
        }
    }

    // ✅ LANGKAH 2: Buat fungsi inti untuk memfilter dan mencari
    private fun applyFiltersAndSearch() {
        val searchQuery = etSearch.text.toString().trim().lowercase()
        val selectedStatus = spinnerStatus.selectedItem.toString()
        val selectedRole = spinnerRole.selectedItem.toString()

        // Mulai dengan semua data, lalu filter secara bertahap
        var currentList = allUsersList.toList()

        // 1. Filter berdasarkan Status
        if (selectedStatus != "Semua Status") {
            val isActiveFilter = selectedStatus == "Aktif"
            currentList = currentList.filter { it.isActive == isActiveFilter }
        }

        // 2. Filter berdasarkan Role
        if (selectedRole != "Semua Role") {
            currentList = currentList.filter { it.role.equals(selectedRole, ignoreCase = true) }
        }

        // 3. Filter berdasarkan Pencarian (Username atau Telepon)
        if (searchQuery.isNotEmpty()) {
            currentList = currentList.filter {
                it.username.lowercase().contains(searchQuery) || it.phone.contains(searchQuery)
            }
        }

        // Update list di adapter dengan hasil akhir
        filteredUserList.clear()
        filteredUserList.addAll(currentList)
        userAdapter.notifyDataSetChanged()
    }

    private fun fetchUsers() {
        lifecycleScope.launch {
            try {
                val supabase = SupabaseClientProvider.client
                val response = supabase.from("pengguna").select {
                    order("createdat", order = Order.DESCENDING)
                }
                val fetchedUsers = response.decodeList<Pengguna>()

                // ✅ LANGKAH 3: Simpan data asli ke allUsersList
                allUsersList.clear()
                allUsersList.addAll(fetchedUsers)

                // Setelah data didapat, langsung terapkan filter default
                applyFiltersAndSearch()

            } catch (e: Exception) {
                Log.e("ManagementUsersFragment", "Gagal mengambil data: ", e)
                Toast.makeText(requireContext(), "Gagal mengambil data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Implementasi dari Listener tidak berubah ---
    override fun onEditClicked(pengguna: Pengguna) {
        val dialog = EditUserDialogFragment.newInstance(pengguna)
        dialog.setUserUpdatedListener(this)
        dialog.show(childFragmentManager, EditUserDialogFragment.TAG)
    }

    override fun onUserUpdated() {
        Log.d("ManagementUsersFragment", "Sinyal update diterima, memuat ulang data...")
        fetchUsers()
    }

    override fun onUserAdded() {
        Log.d("ManagementUsersFragment", "Sinyal tambah pengguna diterima, memuat ulang data...")
        fetchUsers()
    }
}