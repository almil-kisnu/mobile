package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.almil.dessertcakekinian.activity.MainActivity
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.produk
import com.almil.dessertcakekinian.model.OrderId
import com.almil.dessertcakekinian.model.Order // Diambil dari package model
import com.almil.dessertcakekinian.model.DetailOrder // Diambil dari package model
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale

class paymentFragment : Fragment() {

    private lateinit var etNamaPemesan: EditText
    private lateinit var etCatatan: EditText // Digunakan untuk catatan order keseluruhan
    private lateinit var listProdukTerpilih: LinearLayout
    private lateinit var radioGroupMetode: RadioGroup
    private lateinit var tvSubTotal: TextView
    private lateinit var btnBayar: Button
    private lateinit var btnBack: ImageButton

    private var cartItems: Map<produk, Int>? = null
    private var totalPrice: Double = 0.0 // Ini adalah total/grandtotal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Memastikan penggunaan tipe yang benar untuk Map
            @Suppress("UNCHECKED_CAST")
            cartItems = it.getSerializable("cartItems") as? Map<produk, Int>
            totalPrice = it.getDouble("totalPrice", 0.0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_payment, container, false)
        etNamaPemesan = view.findViewById(R.id.etNamaPemesan)
        etCatatan = view.findViewById(R.id.etCatatan)
        listProdukTerpilih = view.findViewById(R.id.listProdukTerpilih)
        radioGroupMetode = view.findViewById(R.id.radioGroupMetode)
        tvSubTotal = view.findViewById(R.id.tvSubTotal)
        btnBayar = view.findViewById(R.id.btnBayar)
        btnBack = view.findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            (activity as MainActivity).showFragment((activity as MainActivity).getTransactionFragment())
        }

        // Logic untuk tombol bayar
        btnBayar.setOnClickListener {
            checkAndProceedPayment()
        }

        displayCartItems()
        tvSubTotal.text = formatPrice(totalPrice)
        return view
    }

    private fun checkAndProceedPayment() {
        val namaPemesan = etNamaPemesan.text.toString().trim()
        val selectedMethodId = radioGroupMetode.checkedRadioButtonId

        if (namaPemesan.isEmpty()) {
            Toast.makeText(requireContext(), "Nama pemesan tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedMethodId == -1) {
            Toast.makeText(requireContext(), "Pilih metode pembayaran.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadioButton = view?.findViewById<RadioButton>(selectedMethodId)
        val metodeBayar = selectedRadioButton?.text.toString() ?: "Cash"

        if (metodeBayar == "Cash") {
            showCashPaymentDialog()
        } else {
            // Untuk metode non-tunai (Transfer, dll.), anggap uang pas.
            processPaymentAndInsertOrder(totalPrice, totalPrice, 0.0, metodeBayar)
        }
    }

    private fun showCashPaymentDialog() {
        // Asumsi R.layout.dialog_cash_payment sudah dibuat (sesuai instruksi sebelumnya)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cash_payment, null)
        val tvTotalHarga = dialogView.findViewById<TextView>(R.id.tvTotalHarga)
        val etUangBayar = dialogView.findViewById<EditText>(R.id.etUangBayar)
        val tvKembalian = dialogView.findViewById<TextView>(R.id.tvKembalian)

        tvTotalHarga.text = formatPrice(totalPrice)

        etUangBayar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val uangBayar = s.toString().toDoubleOrNull() ?: 0.0
                val kembalian = uangBayar - totalPrice
                tvKembalian.text = formatPrice(kembalian)
                // Ganti R.color.white dan R.color.design_default_color_error dengan warna yang sesuai di proyek Anda
                val colorId = if (kembalian >= 0) android.R.color.white else android.R.color.holo_red_dark
                tvKembalian.setTextColor(resources.getColor(colorId, null))
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        AlertDialog.Builder(requireContext())
            .setTitle("Pembayaran Tunai")
            .setView(dialogView)
            .setPositiveButton("Proses", null) // Set null di sini, dan override di show
            .setNegativeButton("Batal", null)
            .create()
            .apply {
                show()
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val uangBayarStr = etUangBayar.text.toString().trim()
                    val uangBayar = uangBayarStr.toDoubleOrNull() ?: 0.0
                    val kembalian = uangBayar - totalPrice
                    val metodeBayar = "Cash"

                    if (uangBayarStr.isEmpty() || uangBayar <= 0) {
                        Toast.makeText(requireContext(), "Masukkan jumlah uang yang dibayarkan.", Toast.LENGTH_SHORT).show()
                    } else if (kembalian < 0) {
                        Toast.makeText(requireContext(), "Jumlah bayar kurang dari total harga!", Toast.LENGTH_SHORT).show()
                    } else {
                        processPaymentAndInsertOrder(uangBayar, totalPrice, kembalian, metodeBayar)
                        dismiss()
                    }
                }
            }
    }

    private fun processPaymentAndInsertOrder(uangBayar: Double, totalHarga: Double, kembalian: Double, metodeBayar: String) {
        val namaPemesan = etNamaPemesan.text.toString().trim()
        val catatanOrder = etCatatan.text.toString().trim()
        val idKasir = 1 // Placeholder: Ganti dengan ID kasir yang sebenarnya (misalnya dari Auth)

        val newOrder = Order(
            namapelanggan = namaPemesan,
            total = totalHarga,
            diskon = 0.0,
            grandtotal = totalHarga, // Sama dengan total karena diskon 0
            bayar = uangBayar,
            kembalian = kembalian,
            metodebayar = metodeBayar,
            idkasir = idKasir
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // A. Insert ke tabel 'order' dan SELECT idorder
                // Kunci perbaikan: Menggunakan OrderId untuk decoding agar sesuai dengan SELECT {idorder}
                val insertedOrders = SupabaseClientProvider.client.postgrest["orders"]
                    .insert(newOrder) {
                        select(Columns.raw("idorder")) // Meminta server hanya mengembalikan idorder
                    }
                    .decodeList<OrderId>() // <-- DECODING MENGGUNAKAN OrderId (Penyimpanan Sekali Pakai)

                // Ambil idorder dari hasil decode
                val idOrder: Int? = insertedOrders.firstOrNull()?.idorder

                if (idOrder != null) {
                    println("ID Order yang berhasil di-insert: $idOrder") // Print ke Logcat (Debug)
                }

                if (idOrder != null && cartItems != null) {
                    // B. Insert ke tabel 'detailorder'
                    val detailsToInsert = cartItems!!.mapNotNull { (product, quantity) ->
                        // Pastikan idproduk tidak null sebelum insert
                        if (quantity > 0 && product.idproduk != null) {
                            DetailOrder(
                                idorder = idOrder,
                                idproduk = product.idproduk,
                                harga = product.hargajual,
                                jumlah = quantity,
                                subtotal = product.hargajual * quantity,
                                catatan = if (catatanOrder.isNotEmpty()) catatanOrder else null
                            )
                        } else null
                    }

                    if (detailsToInsert.isNotEmpty()) {
                        SupabaseClientProvider.client.postgrest["detailorder"].insert(detailsToInsert)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Transaksi berhasil! Kembalian: ${formatPrice(kembalian)}", Toast.LENGTH_LONG).show()
                        // Navigasi atau reset UI setelah sukses
                        parentFragmentManager.popBackStack() // Kembali ke layar sebelumnya
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Gagal mendapatkan ID Order atau cart kosong.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                println("ERROR TRANSAKSI: ${e.message}") // Print pesan error ke Logcat
                withContext(Dispatchers.Main) {
                    // Sekarang Toast akan menampilkan pesan error yang benar (dari e.message)
                    Toast.makeText(requireContext(), "Transaksi Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }
    private fun displayCartItems() {
        listProdukTerpilih.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        cartItems?.forEach { (product, quantity) ->
            if (quantity > 0) {
                val productCard = inflater.inflate(R.layout.product_card_selected, listProdukTerpilih, false)
                // Bind data ke card
                val tvNamaMenu = productCard.findViewById<TextView>(R.id.tvNamaMenu)
                val tvHarga = productCard.findViewById<TextView>(R.id.tvHarga)
                val tvQuantity = productCard.findViewById<TextView>(R.id.tvQuantity)

                tvNamaMenu.text = product.namaproduk
                tvHarga.text = "Rp ${formatPrice(product.hargajual)}"
                tvQuantity.text = quantity.toString()
                listProdukTerpilih.addView(productCard)
            }
        }
    }

    private fun formatPrice(price: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return "Rp ${formatter.format(price)}"
    }

    companion object {
        fun newInstance(cartItems: Map<produk, Int>, totalPrice: Double) =
            paymentFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("cartItems", cartItems as Serializable)
                    putDouble("totalPrice", totalPrice)
                }
            }
    }
}