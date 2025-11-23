package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.dtResponAdapter
import com.almil.dessertcakekinian.model.ProductViewModel
import com.almil.dessertcakekinian.model.TransferViewModel
import com.almil.dessertcakekinian.model.TransferWithDetails
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.almil.dessertcakekinian.dialog.dialogBatalFragment
class dtTransferFragment : Fragment(), dtResponAdapter.OnItemActionListener {

    private val viewModel: TransferViewModel by viewModels()
    private val productViewModel: ProductViewModel by viewModels()

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var rvPermintaan: RecyclerView
    private lateinit var tvTokoPenerima: TextView
    private lateinit var tvTokoPengirim: TextView
    private lateinit var tvUserPenerima: TextView
    private lateinit var tvUserPengirim: TextView
    private lateinit var tvTanggalDiterima: TextView
    private lateinit var tvTanggalDikirim: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCatatan: TextView

    // Adapter
    private lateinit var dtResponAdapter: dtResponAdapter

    // Data
    private var idTransfer: Int = 0

    companion object {
        private const val ARG_ID_TRANSFER = "arg_id_transfer"

        fun newInstance(idTransfer: Int): dtTransferFragment {
            return dtTransferFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_TRANSFER, idTransfer)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            idTransfer = it.getInt(ARG_ID_TRANSFER, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dt_transfer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        rvPermintaan = view.findViewById(R.id.rvPermintaan)
        tvTokoPenerima = view.findViewById(R.id.tvTokoPenerima)
        tvTokoPengirim = view.findViewById(R.id.tvTokoPengirim)
        tvUserPenerima = view.findViewById(R.id.tvUserPenerima)
        tvUserPengirim = view.findViewById(R.id.tvUserPengirim)
        tvTanggalDiterima = view.findViewById(R.id.tvTanggalDiterima)
        tvTanggalDikirim = view.findViewById(R.id.tvTanggalDikirim)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvCatatan = view.findViewById(R.id.tvCatatan)
        val bottomBar = view.findViewById<View>(R.id.bottomBar)
        val btnAksi = view.findViewById<Button>(R.id.btnAksi)
    }
    private fun showBatalDialog() {
        val dialog = dialogBatalFragment.newInstance(idTransfer)
        dialog.setOnBatalListener { success ->
            if (success) {
                // Refresh data setelah berhasil dibatalkan
                observeData()
            }
        }
        dialog.setOnDismissParent {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        dialog.show(childFragmentManager, "DialogBatal")
    }

    private fun setupRecyclerView() {
        dtResponAdapter = dtResponAdapter(
            itemActionListener = this,
            viewModel = viewModel,
            productViewModel = productViewModel,
            lifecycleOwner = viewLifecycleOwner
        )

        rvPermintaan.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = dtResponAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        view?.findViewById<Button>(R.id.btnAksi)?.setOnClickListener {
            showBatalDialog()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getTransferById(idTransfer).collectLatest { transferWithDetails ->
                    if (transferWithDetails != null) {
                        displayTransferData(transferWithDetails)
                    }
                }
            }
        }
    }

    private fun displayTransferData(transferWithDetails: TransferWithDetails) {
        val transfer = transferWithDetails.transfer
        val details = transferWithDetails.details

        // Update RecyclerView dengan detail transfer
        dtResponAdapter.updateData(details)

        // Display status
        tvStatus.text = when (transfer.status.lowercase()) {
            "pending" -> "Pending"
            "dikirim" -> "Dikirim"
            "diterima" -> "Diterima"
            "dibatalkan" -> "Dibatalkan"
            else -> transfer.status
        }
        val bottomBar = view?.findViewById<View>(R.id.bottomBar)
        if (transfer.status.lowercase() == "pending") {
            bottomBar?.visibility = View.VISIBLE
        } else {
            bottomBar?.visibility = View.GONE
        }

        // Display catatan
        tvCatatan.text = if (transfer.catatan.isNullOrEmpty()) "-" else transfer.catatan

        // Display tanggal (✅ Sesuai nama field di TransferStock)
        tvTanggalDikirim.text = formatTanggal(transfer.tanggalTransfer)
        tvTanggalDiterima.text = formatTanggal(transfer.tanggalTerima)

        // Get dan display data outlet & user (✅ Handle nullable idoutletAsal)
        viewLifecycleOwner.lifecycleScope.launch {
            // Outlet Pengirim
            if (transfer.idoutletAsal != null) {
                viewModel.getOutletById(transfer.idoutletAsal).collectLatest { outlet ->
                    tvTokoPengirim.text = outlet?.kodeOutlet ?: "-"
                }
            } else {
                tvTokoPengirim.text = "-"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Outlet Penerima
            viewModel.getOutletById(transfer.idoutletTujuan).collectLatest { outlet ->
                tvTokoPenerima.text = outlet?.kodeOutlet ?: "-"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // User Pengirim (✅ Handle nullable iduserPengirim)
            if (transfer.iduserPengirim != null) {
                viewModel.getPenggunaById(transfer.iduserPengirim).collectLatest { pengguna ->
                    tvUserPengirim.text = pengguna?.username ?: "-"
                }
            } else {
                tvUserPengirim.text = "-"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // User Penerima (✅ Handle nullable iduserPenerima)
            if (transfer.iduserPenerima != null) {
                viewModel.getPenggunaById(transfer.iduserPenerima).collectLatest { pengguna ->
                    tvUserPenerima.text = pengguna?.username ?: "-"
                }
            } else {
                tvUserPenerima.text = "-"
            }
        }
    }

    private fun formatTanggal(tanggal: String?): String {
        if (tanggal.isNullOrEmpty()) return "-"

        return try {
            // Format: yyyy-MM-dd'T'HH:mm:ss atau yyyy-MM-dd HH:mm:ss
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

            val date = inputFormat.parse(tanggal)
            date?.let { outputFormat.format(it) } ?: tanggal
        } catch (e: Exception) {
            try {
                // Coba format alternatif
                val inputFormat2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

                val date = inputFormat2.parse(tanggal)
                date?.let { outputFormat.format(it) } ?: tanggal
            } catch (e2: Exception) {
                try {
                    // Coba format tanpa jam
                    val inputFormat3 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

                    val date = inputFormat3.parse(tanggal)
                    date?.let { outputFormat.format(it) } ?: tanggal
                } catch (e3: Exception) {
                    tanggal
                }
            }
        }
    }

    // Interface implementation (tidak digunakan di sini karena read-only)
    override fun onCatatanChanged(position: Int, newCatatan: String) {
        // Read-only mode, tidak perlu implement
    }
}