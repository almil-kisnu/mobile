package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.TransferAdapter
import com.almil.dessertcakekinian.model.TransferStock
import com.almil.dessertcakekinian.model.TransferViewModel
import com.almil.dessertcakekinian.model.TransferDataState
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.ImageButton

class transferFragment : Fragment() {

    private val viewModel: TransferViewModel by viewModels()
    private lateinit var transferAdapter: TransferAdapter

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipSemua: Chip
    private lateinit var chipPending: Chip
    private lateinit var chipDikirim: Chip
    private lateinit var chipDiterima: Chip
    private lateinit var chipDibatalkan: Chip
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnBack: ImageButton
    private lateinit var textViewError: TextView

    // Filter state
    private var currentFilter: FilterType = FilterType.ALL
    private var searchQuery: String = ""

    enum class FilterType {
        ALL, PENDING, DIKIRIM, DITERIMA, DIBATALKAN
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transfer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_produk)
        editTextSearch = view.findViewById(R.id.edit_text_search)
        chipGroup = view.findViewById(R.id.chip_group_filter)
        chipSemua = view.findViewById(R.id.chipSemua)
        chipPending = view.findViewById(R.id.chipPending)
        chipDikirim = view.findViewById(R.id.chipDikirim)
        chipDiterima = view.findViewById(R.id.chipDiterima)
        chipDibatalkan = view.findViewById(R.id.chipDibatalkan)
        fabAdd = view.findViewById(R.id.fab_add)
        btnBack = view.findViewById(R.id.btnBack)
        textViewError = view.findViewById(R.id.textViewError)
    }

    private fun setupRecyclerView() {
        transferAdapter = TransferAdapter(
            penggunaList = emptyList(),
            outletList = emptyList(),
            onItemClick = { transfer ->
                onTransferClick(transfer)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transferAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        // Back button
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Search functionality
        editTextSearch.addTextChangedListener { text ->
            searchQuery = text.toString()
            applyFilters()
        }

        // Chip filters
        chipSemua.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = FilterType.ALL
                applyFilters()
            }
        }

        chipPending.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = FilterType.PENDING
                applyFilters()
            }
        }

        chipDikirim.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = FilterType.DIKIRIM
                applyFilters()
            }
        }

        chipDiterima.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = FilterType.DITERIMA
                applyFilters()
            }
        }

        chipDibatalkan.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = FilterType.DIBATALKAN
                applyFilters()
            }
        }

        // FAB Add
        fabAdd.setOnClickListener {
            onFabAddClicked()
        }
    }

    private fun onFabAddClicked() {
        val fragment = RqTransferFragment.newInstance()

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe data state
                launch {
                    viewModel.dataState.collectLatest { state ->
                        when (state) {
                            is TransferDataState.Loading -> {
                                showLoading(true)
                                textViewError.visibility = View.GONE
                            }
                            is TransferDataState.Success -> {
                                showLoading(false)
                                textViewError.visibility = View.GONE
                            }
                            is TransferDataState.Error -> {
                                showLoading(false)
                                textViewError.visibility = View.VISIBLE
                                textViewError.text = state.message
                            }
                        }
                    }
                }

                // Observe transfers
                launch {
                    viewModel.allTransfers.collectLatest { transfers ->
                        applyFilters()
                    }
                }

                // Observe pengguna list
                launch {
                    viewModel.allPengguna.collectLatest { penggunaList ->
                        updateAdapterData()
                    }
                }

                // Observe outlet list
                launch {
                    viewModel.allOutlet.collectLatest { outletList ->
                        updateAdapterData()
                    }
                }
            }
        }
    }

    private fun applyFilters() {
        viewLifecycleOwner.lifecycleScope.launch {
            val allTransfers = viewModel.allTransfers.value

            // Filter by status
            val filteredByStatus = when (currentFilter) {
                FilterType.ALL -> allTransfers
                FilterType.PENDING -> allTransfers.filter { it.status.equals("pending", ignoreCase = true) }
                FilterType.DIKIRIM -> allTransfers.filter { it.status.equals("dikirim", ignoreCase = true) }
                FilterType.DITERIMA -> allTransfers.filter { it.status.equals("diterima", ignoreCase = true) }
                FilterType.DIBATALKAN -> allTransfers.filter { it.status.equals("dibatalkan", ignoreCase = true) }
            }

            // Filter by search query (hanya kode toko pengirim)
            val filteredBySearch = if (searchQuery.isBlank()) {
                filteredByStatus
            } else {
                val query = searchQuery.lowercase()
                filteredByStatus.filter { transfer ->
                    // Search hanya by kode outlet pengirim
                    val outletAsal = viewModel.allOutlet.value.find { it.idoutlet == transfer.idoutletAsal }
                    outletAsal?.kodeOutlet?.lowercase()?.contains(query) == true
                }
            }

            transferAdapter.submitList(filteredBySearch)
        }
    }

    private fun updateAdapterData() {
        // Create new adapter with updated data
        transferAdapter = TransferAdapter(
            penggunaList = viewModel.allPengguna.value,
            outletList = viewModel.allOutlet.value,
            onItemClick = { transfer ->
                onTransferClick(transfer)
            }
        )
        recyclerView.adapter = transferAdapter
        applyFilters()
    }

    private fun showLoading(show: Boolean) {
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun onTransferClick(transfer: TransferStock) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Ambil TransferWithDetails dari ViewModel
            val transferWithDetails = viewModel.allTransferWithDetails.value
                .find { it.transfer.idtransfer == transfer.idtransfer }

            if (transferWithDetails != null) {
                // Navigate dengan data lengkap
                if (transfer.status.equals("dikirim", ignoreCase = true)) {
                    // Navigate ke ResTransferFragment untuk terima barang
                    val fragment = ResTransferFragment.newInstance(transfer.idtransfer)

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                } else {
                    // Navigate ke DtTransferFragment untuk lihat detail saja
                    val fragment = dtTransferFragment.newInstance(transfer.idtransfer)

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data when fragment resumes
        viewModel.reload()
    }
}