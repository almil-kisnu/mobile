package com.almil.dessertcakekinian.fragment

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.StrukrequestAdapter
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.CartItem
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class dialogRequestFragment : DialogFragment() {

    private var currentUserId: Int = -1
    private var currentUserOutletId: Int = -1
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rvProdukPembayaran: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var btnKirim: Button
    private lateinit var strukAdapter: StrukrequestAdapter

    private var cartItemsMap: Map<Int, CartItem> = emptyMap()

    companion object {
        private const val ARG_CART_ITEMS = "cart_items"

        fun newInstance(cartItems: Map<Int, CartItem>): dialogRequestFragment {
            val fragment = dialogRequestFragment()
            val args = Bundle().apply {
                putSerializable(ARG_CART_ITEMS, HashMap(cartItems))
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        getCurrentUserAndOutletIds()

        @Suppress("UNCHECKED_CAST")
        cartItemsMap = (arguments?.getSerializable(ARG_CART_ITEMS) as? HashMap<Int, CartItem>) ?: emptyMap()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }


    private fun initViews(view: View) {
        rvProdukPembayaran = view.findViewById(R.id.rvProdukPembayaran)
        btnBack = view.findViewById(R.id.btnBack)
        btnKirim = view.findViewById(R.id.btnPrint)
    }

    private fun setupRecyclerView() {
        strukAdapter = StrukrequestAdapter(cartItemsMap)
        rvProdukPembayaran.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = strukAdapter
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            dismiss()
        }

        btnKirim.setOnClickListener {
            if (cartItemsMap.isEmpty()) {
                Toast.makeText(requireContext(), "Keranjang kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            kirimRequestTransfer()
        }
    }

    private fun getCurrentUserAndOutletIds() {
        currentUserId = sharedPreferences.getInt("USER_ID", -1)
        currentUserOutletId = sharedPreferences.getInt("USER_OUTLET_ID", -1)

        Log.d("dialogRequestFragment", "User ID: $currentUserId, Outlet ID: $currentUserOutletId")
    }

    private fun kirimRequestTransfer() {
        if (currentUserId == -1 || currentUserOutletId == -1) {
            Toast.makeText(requireContext(), "Data user tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Tampilkan loading
        btnKirim.isEnabled = false
        btnKirim.text = "Mengirim..."

        lifecycleScope.launch {
            try {
                val supabase = SupabaseClientProvider.client

                // 1. Insert ke transfer_stock
                val transferData = buildJsonObject {
                    put("iduser_peminta", currentUserId)
                    put("idoutlet_tujuan", currentUserOutletId)
                }

                val transferResponse = supabase.from("transfer_stock")
                    .insert(transferData) {
                        select()
                    }
                    .decodeSingle<TransferStockResponse>()

                val idTransfer = transferResponse.idtransfer
                Log.d("dialogRequestFragment", "Transfer created with ID: $idTransfer")

                // 2. Insert ke detail_transfer untuk setiap item
                for (cartItem in cartItemsMap.values) {
                    val detailData = buildJsonObject {
                        put("idtransfer", idTransfer)
                        put("idproduk", cartItem.produkDetail.produk.idproduk)
                        put("jumlah", cartItem.quantity)
                    }

                    supabase.from("detail_transfer")
                        .insert(detailData)

                    Log.d("dialogRequestFragment",
                        "Detail inserted: Product ${cartItem.produkDetail.produk.idproduk}, Qty ${cartItem.quantity}")
                }

                // Berhasil
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Request berhasil dikirim!", Toast.LENGTH_SHORT).show()
                    dismiss()

                    // Kembali ke fragment sebelumnya atau refresh
                    parentFragmentManager.popBackStack()
                }

            } catch (e: Exception) {
                Log.e("dialogRequestFragment", "Error kirim request: ${e.message}", e)
                requireActivity().runOnUiThread {
                    btnKirim.isEnabled = true
                    btnKirim.text = "Kirim"
                    Toast.makeText(requireContext(), "Gagal mengirim request: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Serializable
    data class TransferStockResponse(
        val idtransfer: Int,
        val idoutlet_asal: Int? = null,
        val idoutlet_tujuan: Int,
        val tanggal_transfer: String? = null,
        val tanggal_terima: String? = null,
        val status: String? = null,
        val iduser_pengirim: Int? = null,
        val iduser_penerima: Int? = null,
        val catatan: String? = null,
        val iduser_peminta: Int? = null
    )
}