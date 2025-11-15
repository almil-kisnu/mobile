package com.almil.dessertcakekinian.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.OrderHistoryAdapter
import com.almil.dessertcakekinian.model.OrderDataState
import com.almil.dessertcakekinian.model.OrderViewModel
import com.almil.dessertcakekinian.model.OrderWithDetails
import kotlinx.coroutines.launch
import java.util.*
import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.util.Log

class RiwayatTransaksiFragment : Fragment() {

    companion object {
        private const val TAG = "RiwayatTransaksi"

        @JvmStatic
        fun newInstance() = RiwayatTransaksiFragment()
    }

    private val viewModel: OrderViewModel by viewModels()
    private lateinit var orderHistoryAdapter: OrderHistoryAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnFilter: ImageButton
    private lateinit var btnBack: ImageButton

    // Filter states
    private var selectedDate: String? = null
    private var selectedHour: String? = null
    private var allOrders: List<OrderWithDetails> = emptyList()
    private var isFilterActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_riwayat_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Debug: Cek SharedPreferences (HARUS SAMA DENGAN LOGIN!)
        val sharedPreferences = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val outletKode = sharedPreferences.getString("OUTLET_KODE", null)
        Log.d(TAG, "SharedPreferences OUTLET_KODE: $outletKode")

        initViews(view)
        setupRecyclerView()
        setupListeners()
        setupObservers()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvRiwayat)
        etSearch = view.findViewById(R.id.etSearchRiwayat)
        btnFilter = view.findViewById(R.id.btnFilterRiwayat)
        btnBack = view.findViewById(R.id.btnBackRiwayat)
    }

    private fun setupRecyclerView() {
        orderHistoryAdapter = OrderHistoryAdapter { orderWithDetails ->
            navigateToDetail(orderWithDetails)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderHistoryAdapter
        }
    }

    private fun setupListeners() {
        // Back button
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Search functionality
        etSearch.addTextChangedListener { text ->
            filterOrders(text.toString())
        }

        // Tambahkan listener untuk tombol search di keyboard
        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Sembunyikan keyboard
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                // Hilangkan focus dari EditText
                v.clearFocus()

                true // Return true untuk consume event
            } else {
                false
            }
        }

        btnFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allOrders.collect { state ->
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: OrderDataState) {
        when (state) {
            is OrderDataState.Loading -> {
                // Loading handled by ViewModel
            }

            is OrderDataState.Success -> {
                Log.d(TAG, "Orders received: ${state.orders.size}")
                allOrders = state.orders
                filterOrders(etSearch.text.toString())
            }

            is OrderDataState.Error -> {
                if (state.cachedOrders.isNotEmpty()) {
                    Log.d(TAG, "Using cached orders: ${state.cachedOrders.size}")
                    allOrders = state.cachedOrders
                    filterOrders(etSearch.text.toString())
                }
            }
        }
    }

    private fun showFilterDialog() {
        val calendar = Calendar.getInstance()

        // Date Picker
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

                // After selecting date, show time picker
                showTimePicker(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Reset") { _, _ ->
            clearFilters()
        }

        datePickerDialog.show()
    }

    private fun showTimePicker(calendar: Calendar) {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, _ ->
                selectedHour = String.format("%02d", hourOfDay)
                applyFilters()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            0,
            true
        )

        timePickerDialog.setButton(TimePickerDialog.BUTTON_NEUTRAL, "Skip Jam") { _, _ ->
            selectedHour = null
            applyFilters()
        }

        timePickerDialog.show()
    }

    private fun applyFilters() {
        isFilterActive = selectedDate != null || selectedHour != null
        updateFilterIcon()
        filterOrders(etSearch.text.toString())
    }

    private fun clearFilters() {
        selectedDate = null
        selectedHour = null
        isFilterActive = false
        updateFilterIcon()
        filterOrders(etSearch.text.toString())
    }

    private fun updateFilterIcon() {
        if (isFilterActive) {
            btnFilter.setImageResource(R.drawable.ic_filter_on)
        } else {
            btnFilter.setImageResource(R.drawable.ic_filter_off)
        }
    }

    private fun filterOrders(searchQuery: String) {
        Log.d(TAG, "===== FILTER ORDERS =====")
        Log.d(TAG, "All orders count: ${allOrders.size}")

        var filtered = allOrders

        // Filter by date
        if (selectedDate != null) {
            filtered = filtered.filter { it.order.tanggal == selectedDate }
            Log.d(TAG, "After date filter: ${filtered.size}")
        }

        // Filter by hour
        if (selectedHour != null) {
            filtered = filtered.filter { orderWithDetails ->
                val hour = orderWithDetails.order.jam.split(":").firstOrNull()
                hour == selectedHour
            }
            Log.d(TAG, "After hour filter: ${filtered.size}")
        }

        // Filter by search query (order ID or product name)
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { orderWithDetails ->
                val pelangganMatch = orderWithDetails.order.namapelanggan.contains(searchQuery, ignoreCase = true)
                val productMatch = orderWithDetails.details.any {
                    it.namaproduk.contains(searchQuery, ignoreCase = true)
                }
                pelangganMatch || productMatch
            }
            Log.d(TAG, "After search filter: ${filtered.size}")
        }

        Log.d(TAG, "Filtered orders sebelum ke adapter: ${filtered.size}")

        // Update RecyclerView dengan filter outlet
        orderHistoryAdapter.submitFilteredList(filtered, requireContext())
    }

    private fun navigateToDetail(orderWithDetails: OrderWithDetails) {
        val detailFragment = dtRiwayatFragment.newInstance(orderWithDetails)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }
}