package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.MainActivity
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.model.produk
import com.almil.dessertcakekinian.model.KategoriProduk
import com.almil.dessertcakekinian.common.QuantitySelector
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class transactionFragment : Fragment() {

    private lateinit var listProduk: LinearLayout
    private lateinit var kategoriProduk: LinearLayout
    private lateinit var tvPesanan: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnPembayaran: Button
    private val produkList = mutableListOf<produk>()
    private val kategoriList = mutableListOf<KategoriProduk>()
    private val productQuantities = mutableMapOf<produk, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transaction, container, false)
        listProduk = view.findViewById(R.id.listProduk)
        kategoriProduk = view.findViewById(R.id.kategoriProduk)
        tvPesanan = view.findViewById(R.id.tvPesanan)
        tvTotal = view.findViewById(R.id.tvTotal)
        btnPembayaran = view.findViewById(R.id.btnPembayaran)

        // Implementasi navigasi ke paymentFragment
        btnPembayaran.setOnClickListener {
            val cart = getCartItems()
            if (cart.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Keranjang kosong, silakan pilih produk terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val total = calculateTotalPrice()
            val paymentFragment = paymentFragment.newInstance(cart, total)
            (activity as MainActivity).showFragment(paymentFragment)
        }

        // Load data dari Supabase
        loadDataFromSupabase()

        return view
    }

    private fun loadDataFromSupabase() {
        lifecycleScope.launch {
            try {
                // Fetch kategori dari database
                val categories = SupabaseClientProvider.client
                    .from("kategori")
                    .select()
                    .decodeList<KategoriProduk>()

                kategoriList.clear()
                kategoriList.addAll(categories)

                // Fetch semua produk dengan join ke kategori
                val products = SupabaseClientProvider.client
                    .from("produk")
                    .select {
                        filter {
                            // Anda bisa tambahkan filter di sini jika perlu
                        }
                    }
                    .decodeList<produk>()

                produkList.clear()
                produkList.addAll(products)

                // Setup kategori buttons setelah data dimuat
                setupKategoriButtons()

                // Tampilkan semua produk secara default
                displayProducts(produkList)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error loading data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupKategoriButtons() {
        kategoriProduk.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        var selectedButton: Button? = null

        // Ekstrak kategori unik dari produkList
        val categories = kategoriList.map { it.nkategori }.distinct()
        val allCategories = listOf("Semua") + categories

        // Inflate dan tambahkan button kategori
        for (cat in allCategories) {
            val btnView = inflater.inflate(R.layout.btn_kategori, kategoriProduk, false)
            val button = btnView.findViewById<Button>(R.id.b_kategori)
            button.text = cat

            // Atur status awal untuk tombol "Semua"
            if (cat == "Semua") {
                button.isSelected = true
                selectedButton = button
            }

            button.setOnClickListener {
                // Nonaktifkan status terpilih dari tombol sebelumnya
                selectedButton?.isSelected = false

                // Aktifkan status terpilih dari tombol yang baru diklik
                button.isSelected = true
                selectedButton = button

                // Filter produk berdasarkan kategori
                filterProductsByCategory(cat)
            }

            kategoriProduk.addView(btnView)
        }
    }

    private fun filterProductsByCategory(categoryName: String) {
        lifecycleScope.launch {
            try {
                val filtered = if (categoryName == "Semua") {
                    produkList
                } else {
                    // Cari idkategori berdasarkan nama kategori
                    val kategori = kategoriList.find { it.nkategori == categoryName }

                    if (kategori != null) {
                        // Filter produk yang memiliki idkategori yang sama
                        produkList.filter { it.idkategori == kategori.idkategori }
                    } else {
                        // Jika kategori tidak ditemukan, tampilkan semua
                        produkList
                    }
                }
                displayProducts(filtered)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error filtering products: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun displayProducts(products: List<produk>) {
        listProduk.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (product in products) {
            val productCard = inflater.inflate(R.layout.product_card, listProduk, false)

            // Bind data ke card
            val imgMenu = productCard.findViewById<ImageView>(R.id.imgMenu)
            val tvNamaMenu = productCard.findViewById<TextView>(R.id.tvNamaMenu)
            val tvDeskripsi = productCard.findViewById<TextView>(R.id.tvDeskripsi)
            val tvHarga = productCard.findViewById<TextView>(R.id.tvHarga)
            val tvStok = productCard.findViewById<TextView>(R.id.tvStok)
            val quantitySelector = productCard.findViewById<QuantitySelector>(R.id.quantitySelector)

            // Set default image karena database tidak menyimpan gambar
            imgMenu.setImageResource(R.drawable.ic_launcher_foreground)

            tvNamaMenu.text = product.namaproduk
            tvDeskripsi.text = product.deskripsi
            tvHarga.text = "Rp ${formatPrice(product.hargajual)}"
            tvStok.text = "Stok : ${product.stock}"

            // Set listener untuk quantity selector
            quantitySelector.setOnQuantityChangeListener { quantity ->
                productQuantities[product] = quantity
                onProductQuantityChanged(product, quantity)
            }

            // Restore quantity jika sudah ada sebelumnya
            productQuantities[product]?.let { savedQuantity ->
                quantitySelector.setQuantity(savedQuantity)
            }

            listProduk.addView(productCard)
        }

        // Update bottom bar setelah menampilkan produk
        updateBottomBar()
    }

    private fun onProductQuantityChanged(product: produk, quantity: Int) {
        // Validasi stok
        if (quantity > product.stock) {
            Toast.makeText(
                requireContext(),
                "Quantity melebihi stok tersedia!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Update bottom bar
        updateBottomBar()
    }

    private fun calculateTotalPrice(): Double {
        var total = 0.0
        productQuantities.forEach { (prod, quantity) ->
            if (quantity > 0) {
                total += prod.hargajual * quantity
            }
        }
        return total
    }

    private fun formatPrice(price: Double): String {
        return String.format("%,.0f", price).replace(",", ".")
    }

    private fun updateBottomBar() {
        val totalItems = productQuantities.values.sum()
        tvPesanan.text = "Pesanan: $totalItems"
        val totalPrice = calculateTotalPrice()
        tvTotal.text = "Rp ${formatPrice(totalPrice)}"
    }

    // Method public untuk mendapatkan data keranjang
    fun getCartItems(): Map<produk, Int> {
        return productQuantities.filter { it.value > 0 }
    }

    // Method untuk clear semua quantity
    fun clearCart() {
        productQuantities.clear()
        displayProducts(produkList)
    }

    // Method untuk refresh data dari database
    fun refreshData() {
        loadDataFromSupabase()
    }
}