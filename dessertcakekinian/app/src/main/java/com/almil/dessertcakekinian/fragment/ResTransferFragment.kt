package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.ResponAdapter
import com.almil.dessertcakekinian.model.DetailTransfer
import com.almil.dessertcakekinian.model.TransferViewModel
import com.almil.dessertcakekinian.model.ProductViewModel
import com.almil.dessertcakekinian.model.TransferWithDetails
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import java.text.SimpleDateFormat
import android.content.Context
import android.content.SharedPreferences
import java.util.Date
import java.util.*
import io.github.jan.supabase.postgrest.from

class ResTransferFragment : Fragment(), ResponAdapter.OnItemActionListener {

    private val viewModel: TransferViewModel by viewModels()
    private val ProductviewModel: ProductViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences

    // Views
    private lateinit var backArrow: ImageView
    private lateinit var tvOutletPeminta: TextView
    private lateinit var tvOutletPengirim: TextView
    private lateinit var rvProdukTransfer: RecyclerView
    private lateinit var btnSendResponse: Button

    // Adapter
    private lateinit var responAdapter: ResponAdapter

    // Data
    private var idTransfer: Int = 0
    private var currentTransferWithDetails: TransferWithDetails? = null
    private val detailList = mutableListOf<DetailTransfer>()

    companion object {
        private const val ARG_ID_TRANSFER = "arg_id_transfer"

        fun newInstance(idTransfer: Int): ResTransferFragment {
            return ResTransferFragment().apply {
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
        return inflater.inflate(R.layout.fragment_res_transfer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun initViews(view: View) {
        backArrow = view.findViewById(R.id.back_arrow)
        tvOutletPeminta = view.findViewById(R.id.tv_outlet_peminta)
        tvOutletPengirim = view.findViewById(R.id.tv_outlet_pengirim)
        rvProdukTransfer = view.findViewById(R.id.rvProdukTransfer)
        btnSendResponse = view.findViewById(R.id.btn_send_response)
    }

    private fun setupRecyclerView() {
        responAdapter = ResponAdapter(
            itemActionListener = this,
            viewModel = viewModel ,
            productViewModel = ProductviewModel,  // ✅ Tambah ini
            lifecycleOwner = viewLifecycleOwner
        )

        rvProdukTransfer.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = responAdapter
            setHasFixedSize(true)
            android.util.Log.d("ResTransfer", "RecyclerView setup complete")
        }
    }

    private fun setupListeners() {
        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSendResponse.setOnClickListener {
            sendResponse()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe transfer dengan details
                launch {
                    viewModel.getTransferById(idTransfer).collectLatest { transferWithDetails ->
                        if (transferWithDetails != null) {
                            currentTransferWithDetails = transferWithDetails
                            displayTransferData(transferWithDetails)
                        }
                    }
                }
            }
        }
    }

    private fun displayTransferData(transferWithDetails: TransferWithDetails) {
        val transfer = transferWithDetails.transfer

        // Update RecyclerView PERTAMA (supaya data langsung muncul)
        detailList.clear()
        detailList.addAll(transferWithDetails.details)
        responAdapter.updateData(detailList.toList())
        android.util.Log.d("ResTransfer", "RecyclerView updated: ${detailList.size} items")

        viewLifecycleOwner.lifecycleScope.launch {
            // Get Outlet Peminta (Tujuan)
            launch {
                viewModel.getOutletById(transfer.idoutletTujuan).collect { outletTujuan ->
                    if (outletTujuan != null) {
                        transfer.iduserPeminta?.let { idPenerima ->
                            viewModel.getPenggunaById(idPenerima).collect { penerima ->
                                tvOutletPeminta.text = if (penerima != null) {
                                    "${penerima.username} (${penerima.role ?: "Staff"}) - ${outletTujuan.kodeOutlet}"
                                } else {
                                    "Belum ditentukan - ${outletTujuan.kodeOutlet}"
                                }
                            }
                        } ?: run {
                            tvOutletPeminta.text = "Belum ditentukan - ${outletTujuan.kodeOutlet}"
                        }
                    }
                }
            }

            // Get Outlet Pengirim (Asal)
            launch {
                transfer.idoutletAsal?.let { idAsal ->
                    viewModel.getOutletById(idAsal).collect { outletAsal ->
                        if (outletAsal != null) {
                            transfer.iduserPengirim?.let { idPengirim ->
                                viewModel.getPenggunaById(idPengirim).collect { pengirim ->
                                    tvOutletPengirim.text = if (pengirim != null) {
                                        "${pengirim.username} (${pengirim.role ?: "Staff"}) - ${outletAsal.kodeOutlet}"
                                    } else {
                                        "Belum ditentukan - ${outletAsal.kodeOutlet}"
                                    }
                                }
                            } ?: run {
                                tvOutletPengirim.text = "Belum ditentukan - ${outletAsal.kodeOutlet}"
                            }
                        }
                    }
                } ?: run {
                    tvOutletPengirim.text = "Outlet Asal tidak tersedia"
                }
            }
        }
    }

    private fun formatTanggal(tanggal: String?): String {
        if (tanggal.isNullOrEmpty()) return "-"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

            val date = inputFormat.parse(tanggal)
            date?.let { outputFormat.format(it) } ?: tanggal
        } catch (e: Exception) {
            try {
                val inputFormat2 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

                val date = inputFormat2.parse(tanggal)
                date?.let { outputFormat.format(it) } ?: tanggal
            } catch (e2: Exception) {
                tanggal
            }
        }
    }

    private fun sendResponse() {

        // Validasi: pastikan semua item sudah diisi jumlah diterimanya
        val hasInvalidQuantity = detailList.any {
            val qty = it.jumlahDiterima ?: it.jumlah
            qty < 0
        }

        if (hasInvalidQuantity) {
            Toast.makeText(
                requireContext(),
                "Harap isi jumlah yang diterima untuk semua produk",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // ✅ Ambil USER_ID dari SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("USER_ID", -1)

        if (userId == -1) {
            Toast.makeText(
                requireContext(),
                "Error: User tidak ditemukan. Silakan login kembali.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // ✅ Kirim ke backend
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val supabase = SupabaseClientProvider.client

                // 1. Update semua detail_transfer
                for (detail in detailList) {
                    android.util.Log.d("ResTransfer", "Detail: id=${detail.iddetailTransfer}, produk=${detail.idproduk}, qty=${detail.jumlahDiterima}")

                    if (detail.iddetailTransfer > 0) {  // ✅ Pastikan ID valid
                        val result = supabase.from("detail_transfer")
                            .update({
                                set("jumlah_diterima", detail.jumlahDiterima ?: detail.jumlah)
                                set("catatan", detail.catatan ?: "")
                            }) {
                                filter {
                                    eq("iddetail_transfer", detail.iddetailTransfer)
                                }
                            }

                        android.util.Log.d("ResTransfer", "Update result: $result")
                    } else {
                        android.util.Log.e("ResTransfer", "Invalid iddetail_transfer: ${detail.iddetailTransfer}")
                    }
                }

                // 2. Update transfer_stock
                val currentDateTime = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())

                supabase.from("transfer_stock")
                    .update({
                        set("tanggal_terima", currentDateTime)
                        set("iduser_penerima", userId)
                        set("status", "diterima")
                    }) {
                        filter {
                            eq("idtransfer", idTransfer)
                        }
                    }

                // 3. Success
                Toast.makeText(
                    requireContext(),
                    "Respon berhasil dikirim",
                    Toast.LENGTH_SHORT
                ).show()


                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                android.util.Log.e("ResTransfer", "Error sending response: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Gagal mengirim respon: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Implement ResponAdapter.OnItemActionListener
    override fun onQuantityChanged(position: Int, newQuantity: Int) {
        if (position >= 0 && position < detailList.size) {
            detailList[position] = detailList[position].copy(jumlahDiterima = newQuantity)
            android.util.Log.d("ResTransfer", "Quantity changed at $position: $newQuantity")
        }
    }

    override fun onCatatanChanged(position: Int, newCatatan: String) {
        if (position >= 0 && position < detailList.size) {
            detailList[position] = detailList[position].copy(catatan = newCatatan)
        }
    }
}