package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.RadioGroup
import android.widget.ImageButton
import android.widget.Button // Import Button
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.StrukAdapter
import com.almil.dessertcakekinian.dialog.dialog_struk
import com.almil.dessertcakekinian.model.CartViewModel
import com.google.android.material.textfield.TextInputEditText
import android.text.TextWatcher
import android.text.Editable
import android.util.Log
import com.almil.dessertcakekinian.model.Order
import java.util.Locale
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
//database
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.DetailOrder
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import io.github.jan.supabase.postgrest.query.Columns

enum class MetodePembayaran {
    cash,
    transfer
}

private const val ARG_TOTAL_PRICE = "total_price"

class PembayaranFragment : Fragment() {

    private val cartViewModel: CartViewModel by activityViewModels()
    private var totalHargaString: String? = null
    private var totalHargaLong: Long = 0L
    private lateinit var btnBack: ImageButton
    private lateinit var tvTotalTagihan: TextView
    private lateinit var rgMetodePembayaran: RadioGroup
    private lateinit var tilUangDibayar: TextInputLayout
    private lateinit var etUangDibayar: TextInputEditText
    private lateinit var tvKembalian: TextView
    private lateinit var rvProdukPembayaran: RecyclerView
    private lateinit var strukAdapter: StrukAdapter
    private lateinit var btnKonfirmasiPembayaran: Button
    private var currentUserId: Int = -1
    private var currentUserOutletId: Int = -1
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Ambil data total harga dari Bundle
            totalHargaString = it.getString(ARG_TOTAL_PRICE)
            // Konversi totalHargaString ke Long untuk perhitungan
            totalHargaLong = totalHargaString.toLongFromRupiahString()
        }
        sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pembayaran, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi Komponen UI
        btnBack = view.findViewById(R.id.btnBack)
        tvTotalTagihan = view.findViewById(R.id.tvTotalTagihan)
        rgMetodePembayaran = view.findViewById(R.id.rgMetodePembayaran)
        tilUangDibayar = view.findViewById(R.id.tilUangDibayar)
        etUangDibayar = view.findViewById(R.id.etUangDibayar)
        tvKembalian = view.findViewById(R.id.tvKembalian)
        rvProdukPembayaran = view.findViewById(R.id.rvProdukPembayaran)
        btnKonfirmasiPembayaran = view.findViewById(R.id.btnKonfirmasiPembayaran)
        totalHargaString?.let {
            tvTotalTagihan.text = totalHargaLong.toSimpleRupiahString()
        }
        getCurrentUserAndOutletIds()
        setupStrukRecyclerView()
        observeCartData()
        setupPaymentMethodListener(view)

        tilUangDibayar.visibility = View.VISIBLE

        setupBackButton()
        setupUangDibayarDefault()
        setupKembalianCalculator()
        setupKonfirmasiButton()
    }

    private fun showStrukDialog(orderId: Int, cartItems: Map<Int, com.almil.dessertcakekinian.model.CartItem>) {
        val metode = getSelectedMetodePembayaran().name
        val namaKasir = sharedPreferences.getString("USER_NAME", "Kasir") ?: "Kasir"

        val dialog = dialog_struk.newInstance(
            orderId = orderId,
            grandTotal = totalHargaLong.toSimpleRupiahString(),
            bayar = getUangDibayar().toSimpleRupiahString(),
            kembalian = getKembalian().toSimpleRupiahString(),
            metodePembayaran = metode,
            namaKasir = namaKasir,
            cartItems = cartItems
        )

        dialog.show(parentFragmentManager, "DialogStruk")

        // Kosongkan keranjang setelah dialog ditampilkan
        cartViewModel.clearCart()
    }

    private fun getCurrentUserAndOutletIds() {
        currentUserId = sharedPreferences.getInt("USER_ID", -1)
        currentUserOutletId = sharedPreferences.getInt("USER_OUTLET_ID", -1)
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupUangDibayarDefault() {
        if (totalHargaLong > 0) {
            etUangDibayar.setText(totalHargaLong.toString())
        }
    }


    private fun setupKembalianCalculator() {
        etUangDibayar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateChange()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        calculateChange()
    }

    private fun calculateChange() {
        val uangDibayar = getUangDibayar()
        val tagihan = totalHargaLong
        val kembalian = uangDibayar - tagihan

        val formattedKembalian = kembalian.toSimpleRupiahString()

        // Set teks kembalian dan atur warna/pesan
        when {
            kembalian < 0 -> {
                tvKembalian.text = formattedKembalian
                tvKembalian.setTextColor(resources.getColor(R.color.status_inactive))
                tilUangDibayar.error = "Uang kurang Rp ${kembalian.toSimpleRupiahString().replace("-Rp", "")}"
            }
            else -> {
                tvKembalian.text = formattedKembalian
                tvKembalian.setTextColor(resources.getColor(android.R.color.black))
                tilUangDibayar.error = null
            }
        }
    }

    private fun getUangDibayar(): Long {
        return etUangDibayar.text.toString().trim().toLongOrNull() ?: 0L
    }

    private fun getKembalian(): Long {
        val uangDibayar = getUangDibayar()
        return uangDibayar - totalHargaLong
    }

    private fun setupStrukRecyclerView() {
        val initialCartData = cartViewModel.cartItems.value ?: emptyMap()
        strukAdapter = StrukAdapter(initialCartData)

        rvProdukPembayaran.layoutManager = LinearLayoutManager(context)
        rvProdukPembayaran.adapter = strukAdapter
    }

    private fun observeCartData() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItemsMap ->
            strukAdapter.updateData(cartItemsMap)
        }
    }

    private fun setupPaymentMethodListener(view: View) {
        rgMetodePembayaran.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rbTunai -> {
                    tilUangDibayar.visibility = View.VISIBLE
                    setupUangDibayarDefault()
                    calculateChange()
                }
                R.id.rbQris -> {
                    tilUangDibayar.visibility = View.GONE
                    etUangDibayar.setText(totalHargaLong.toString()) // Set Uang Dibayar = Tagihan untuk Non Tunai
                    tvKembalian.text = "Rp 0"
                    tvKembalian.setTextColor(resources.getColor(android.R.color.black))
                    tilUangDibayar.error = null
                }
            }
        }
    }

    // --- Logika Supabase Insert ---

    private fun getSelectedMetodePembayaran(): MetodePembayaran {
        return when (rgMetodePembayaran.checkedRadioButtonId) {
            R.id.rbTunai -> MetodePembayaran.cash
            R.id.rbQris -> MetodePembayaran.transfer
            else -> MetodePembayaran.cash
        }
    }

    private fun setupKonfirmasiButton() {
        btnKonfirmasiPembayaran.setOnClickListener {
            val metode = getSelectedMetodePembayaran().name
            val kembalian = getKembalian()

            // 1. Validasi Uang Bayar (khusus Tunai)
            if (metode == MetodePembayaran.cash.name && kembalian < 0) {
                showErrorMessage("Uang yang dibayar kurang! Tidak dapat melanjutkan.")
                return@setOnClickListener
            }

            // 2. Validasi ID Kasir dan Outlet
            if (currentUserId == -1 || currentUserOutletId == -1) {
                showErrorMessage("Data Kasir atau Outlet tidak ditemukan. Mohon Login ulang.")
                return@setOnClickListener
            }

            val grandTotalDb = totalHargaLong.toDouble()
            val bayarDb = getUangDibayar().toDouble()
            val kembalianDb = if (metode == MetodePembayaran.cash.name) kembalian.toDouble() else 0.0 // Kembalian 0 untuk Non Tunai

            // Lakukan INSERT ke database
            insertOrder(
                grandTotal = grandTotalDb,
                bayar = bayarDb,
                kembalian = kembalianDb,
                idKasir = currentUserId,
                idOutlet = currentUserOutletId,
                metodePembayaran = metode
            )
        }
    }
    private fun insertDetailOrders(orderId: Int) {
        val cartItems = cartViewModel.cartItems.value ?: return

        if (cartItems.isEmpty()) {
            Log.w("SupabaseInsert", "Cart kosong, tidak ada detail order untuk disimpan")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val detailOrders = cartItems.map { (_, cartItem) ->
                    val subtotal = cartItem.hargaSatuan * cartItem.quantity
                    DetailOrder(
                        idorder = orderId,
                        idproduk = cartItem.produkDetail.produk.idproduk,
                        harga = cartItem.hargaSatuan,
                        jumlah = cartItem.quantity,
                        subtotal = subtotal
                    )
                }

                // Insert semua detail order sekaligus
                SupabaseClientProvider.client
                    .from("detailorder")
                    .insert(detailOrders)

                Log.d("SupabaseInsert", "Detail order berhasil disimpan: ${detailOrders.size} item")

                // Tampilkan dialog struk setelah berhasil insert
                showStrukDialog(orderId, cartItems)

            } catch (e: Exception) {
                showErrorMessage("Error saat menyimpan detail order: ${e.message}")
                Log.e("SupabaseInsert", "Error saat insert detail order", e)
                e.printStackTrace()
            }
        }
    }

    private fun insertOrder(
        grandTotal: Double,
        bayar: Double,
        kembalian: Double,
        idKasir: Int,
        idOutlet: Int,
        metodePembayaran: String
    ) {
        val order = Order(
            grandtotal = grandTotal,
            bayar = bayar,
            kembalian = kembalian,
            idkasir = idKasir,
            idoutlet = idOutlet,
            metode_pembayaran = metodePembayaran
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Insert Order dan dapatkan ID
                val response = SupabaseClientProvider.client
                    .from("orders")
                    .insert(order) {
                        select(columns = Columns.list("idorder"))
                    }
                    .decodeList<JsonObject>()

                val newOrder = response.firstOrNull()

                if (newOrder != null) {
                    val orderId = newOrder["idorder"]?.jsonPrimitive?.int

                    if (orderId != null) {
                        Log.d("SupabaseInsert", "Order berhasil disimpan dengan ID: $orderId")

                        // 2. Insert Detail Order menggunakan orderId
                        insertDetailOrders(orderId)

                    } else {
                        showErrorMessage("Order ID tidak ditemukan")
                    }
                } else {
                    Log.w("SupabaseInsert", "Order berhasil disimpan tapi tidak ada data yang dikembalikan")
                    showErrorMessage("Transaksi gagal, tidak ada data yang dikembalikan.")
                }

            } catch (e: Exception) {
                showErrorMessage("Error saat insert order: ${e.message}")
                Log.e("SupabaseInsert", "Error saat insert order", e)
                e.printStackTrace()
            }
        }
    }


    private fun showSuccessMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    // Fungsi ekstensi untuk mengkonversi String Rupiah ("Rp 150.000") menjadi Long (150000)
    private fun String?.toLongFromRupiahString(): Long {
        return this?.replace(Regex("[^\\d]"), "")?.toLongOrNull() ?: 0L
    }

    // Fungsi MODIFIKASI: Mengkonversi Long menjadi String Rupiah (Rp150.000)
    private fun Long.toSimpleRupiahString(): String {
        val symbols = DecimalFormatSymbols(Locale("in", "ID"))
        symbols.currencySymbol = "Rp"
        symbols.groupingSeparator = '.'
        val format = DecimalFormat("¤#,##0", symbols)
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 0
        format.roundingMode = RoundingMode.HALF_UP

        val absValue = kotlin.math.abs(this)
        val formatted = format.format(absValue).replace("Rp", "Rp")

        // Tambahkan tanda minus jika nilainya negatif
        return if (this < 0) "-$formatted" else formatted
    }

    companion object {
        @JvmStatic
        fun newInstance(totalPrice: String) =
            PembayaranFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TOTAL_PRICE, totalPrice)
                }
            }
    }
}