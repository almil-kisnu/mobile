package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.DiskonAdapter
import com.almil.dessertcakekinian.model.DiskonViewModel
import com.almil.dessertcakekinian.model.EventDiskon
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import android.widget.TextView

class DiskonFragment : Fragment() {

    private val viewModel: DiskonViewModel by viewModels()
    private lateinit var diskonAdapter: DiskonAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipSemua: Chip
    private lateinit var chipAktif: Chip
    private lateinit var chipBerakhir: Chip
    private lateinit var btnBack: ImageButton

    // Menyimpan data asli
    private var allDiskonList: List<EventDiskon> = emptyList()

    // Filter state
    private var currentFilterStatus: FilterStatus = FilterStatus.SEMUA
    private var currentSearchQuery: String = ""

    enum class FilterStatus {
        SEMUA, AKTIF, BERAKHIR
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diskon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupSearchBar()
        setupChipFilter()
        observeData()

        // Load data saat fragment dibuka
        viewModel.reload()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_diskon)
        editTextSearch = view.findViewById(R.id.edit_text_search)
        chipGroup = view.findViewById(R.id.chip_group_filter)
        chipSemua = view.findViewById(R.id.chipSemua)
        chipAktif = view.findViewById(R.id.chipAktif)
        chipBerakhir = view.findViewById(R.id.chipBerakhir)
        btnBack = view.findViewById(R.id.btnBack)

        // Back button handler
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        // Setup adapter dengan click listener
        diskonAdapter = DiskonAdapter { diskon ->
            onDiskonItemClicked(diskon)
        }

        // Setup RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = diskonAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchBar() {
        editTextSearch.addTextChangedListener { editable ->
            currentSearchQuery = editable?.toString()?.trim() ?: ""
            applyFilters()
        }
        editTextSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Sembunyikan keyboard
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
                true // Return true untuk consume event
            } else {
                false
            }
        }
    }

    private fun setupChipFilter() {
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipSemua) -> {
                    currentFilterStatus = FilterStatus.SEMUA
                }
                checkedIds.contains(R.id.chipAktif) -> {
                    currentFilterStatus = FilterStatus.AKTIF
                }
                checkedIds.contains(R.id.chipBerakhir) -> {
                    currentFilterStatus = FilterStatus.BERAKHIR
                }
            }
            applyFilters()
        }
    }

    private fun observeData() {
        // Observe all events
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allEvents.collect { diskonList ->
                allDiskonList = diskonList
                applyFilters()

                // Optional: tampilkan pesan jika list kosong
                if (diskonList.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Belum ada event diskon",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Optional: Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Bisa tambahkan ProgressBar dan show/hide di sini
                // progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun applyFilters() {
        var filteredList = allDiskonList

        // Filter berdasarkan status
        filteredList = when (currentFilterStatus) {
            FilterStatus.SEMUA -> filteredList
            FilterStatus.AKTIF -> filteredList.filter { it.isActive }
            FilterStatus.BERAKHIR -> filteredList.filter { !it.isActive }
        }

        // Filter berdasarkan search query
        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter { diskon ->
                diskon.namaDiskon.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // Submit filtered list
        diskonAdapter.submitList(filteredList)

        // Show/hide empty state
        val tvEmptyState = view?.findViewById<TextView>(R.id.tv_empty_state)
        if (filteredList.isEmpty() && allDiskonList.isNotEmpty()) {
            tvEmptyState?.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState?.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun onDiskonItemClicked(diskon: EventDiskon) {
        // Navigate ke dtDiskonFragment dengan data diskon
        val fragment = dtDiskonFragment.newInstance(diskon)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // Sesuaikan dengan ID container Anda
            .addToBackStack(null)
            .commit()
    }

    companion object {
        @JvmStatic
        fun newInstance() = DiskonFragment()
    }
}