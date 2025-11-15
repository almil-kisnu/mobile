package com.almil.dessertcakekinian.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CartViewModel(application: Application) : AndroidViewModel(application) {
    private val _cartItems = MutableLiveData<MutableMap<Int, CartItem>>()
    val cartItems: LiveData<MutableMap<Int, CartItem>> = _cartItems

    private val diskonRepository = DiskonRepository.getInstance(application)

    // List untuk menyimpan data diskon dan detail diskon produk
    private var eventDiskonList: List<EventDiskon> = emptyList()
    private var detailDiskonProdukList: List<DetailDiskonProduk> = emptyList()

    init {
        _cartItems.value = mutableMapOf()

        // 🔥 OTOMATIS subscribe ke DiskonRepository
        observeDiskonData()
    }

    /**
     * 🎯 Fungsi untuk observe data diskon dari repository
     * Data akan otomatis update setiap ada perubahan
     */
    private fun observeDiskonData() {
        viewModelScope.launch {
            // Combine event diskon dan detail diskon
            combine(
                diskonRepository.eventDiskonFlow,
                diskonRepository.detailDiskonFlow
            ) { events, details ->
                Pair(events, details)
            }.collect { (events, details) ->
                // Update data lokal
                eventDiskonList = events
                detailDiskonProdukList = details

                Log.d("CartViewModel", "✅ Data diskon updated from repository")
                Log.d("CartViewModel", "   - Events: ${events.size}")
                Log.d("CartViewModel", "   - Details: ${details.size}")
                Log.d("CartViewModel", "   - Active: ${events.count { it.isActive }}")

                // Refresh semua harga di cart setelah diskon berubah
                refreshCartPrices()
            }
        }
    }

    /**
     * 🔄 Refresh harga semua item di cart
     * Dipanggil otomatis ketika data diskon berubah
     */
    private fun refreshCartPrices() {
        val currentMap = _cartItems.value ?: return

        if (currentMap.isEmpty()) return

        Log.d("CartViewModel", "🔄 Refreshing ${currentMap.size} cart items...")

        currentMap.forEach { (idproduk, cartItem) ->
            val (newHargaSatuan) = getAdjustedPrice(
                cartItem.produkDetail,
                cartItem.quantity
            )

            cartItem.hargaSatuan = newHargaSatuan
        }

        // Trigger LiveData update
        _cartItems.value = currentMap

        Log.d("CartViewModel", "✅ Cart prices refreshed")
    }

    // Fungsi untuk set data diskon dari Supabase (opsional, fallback)
    @Deprecated("Data diskon sudah otomatis dari DiskonRepository")
    fun setDiskonData(eventDiskon: List<EventDiskon>, detailDiskonProduk: List<DetailDiskonProduk>) {
        this.eventDiskonList = eventDiskon
        this.detailDiskonProdukList = detailDiskonProduk
        Log.w("CartViewModel", "⚠️ setDiskonData() deprecated - use DiskonRepository instead")
    }

    private fun getAdjustedPrice(produkDetail: ProdukDetail, quantity: Int): Pair<Double, EventDiskon?> {
        val hargaGrosirList = produkDetail.hargaGrosir

        // PRIORITAS 1: Cek Harga Grosir
        val matchedGrosir = hargaGrosirList
            .filter { it.minQty > 0 && quantity >= it.minQty }
            .maxByOrNull { it.minQty }

        if (matchedGrosir != null) {
            return Pair(matchedGrosir.hargaJual, null)
        }

        // PRIORITAS 2: Cek Diskon Aktif
        val diskonResult = getActiveDiskonForProduct(produkDetail.produk.idproduk)

        if (diskonResult != null) {
            val (diskonPersen, eventDiskon) = diskonResult
            val hargaEceran = produkDetail.produk.harga_eceran ?: 0.0
            val hargaSetelahDiskon = hargaEceran - (hargaEceran * diskonPersen / 100)
            return Pair(hargaSetelahDiskon, eventDiskon)
        }

        // PRIORITAS 3: Harga Eceran
        val hargaEceran = produkDetail.produk.harga_eceran ?: 0.0
        return Pair(hargaEceran, null)
    }

    private fun getActiveDiskonForProduct(idproduk: Int): Pair<Double, EventDiskon>? {
        Log.d("CartViewModel", "========== START getActiveDiskonForProduct ==========")
        Log.d("CartViewModel", "idproduk yang dicari: $idproduk")
        Log.d("CartViewModel", "eventDiskonList.size: ${eventDiskonList.size}")

        if (eventDiskonList.isEmpty()) {
            Log.d("CartViewModel", "❌ eventDiskonList KOSONG!")
            return null
        }

        val result = eventDiskonList.firstOrNull { diskon ->
            Log.d("CartViewModel", "--- Checking diskon: ${diskon.namaDiskon} ---")
            Log.d("CartViewModel", "isActive: ${diskon.isActive}")
            Log.d("CartViewModel", "berlakuUntuk: '${diskon.berlakuUntuk}'")

            if (!diskon.isActive) {
                Log.d("CartViewModel", "❌ Diskon tidak aktif")
                return@firstOrNull false
            }

            val matches = when (diskon.berlakuUntuk) {
                "semua_produk" -> {
                    Log.d("CartViewModel", "✅ Match: semua_produk")
                    true
                }
                "produk_spesifik" -> {
                    val found = detailDiskonProdukList.any {
                        it.idDiskon == diskon.idDiskon && it.idproduk == idproduk
                    }
                    Log.d("CartViewModel", "Produk spesifik found: $found")
                    found
                }
                else -> {
                    Log.d("CartViewModel", "❌ berlakuUntuk tidak dikenali: '${diskon.berlakuUntuk}'")
                    false
                }
            }

            matches
        }

        if (result != null) {
            Log.d("CartViewModel", "✅ Diskon ditemukan: ${result.namaDiskon} (${result.nilaiDiskon}%)")
            Log.d("CartViewModel", "========== END getActiveDiskonForProduct ==========")
            return Pair(result.nilaiDiskon, result)
        }

        Log.d("CartViewModel", "❌ Tidak ada diskon yang cocok")
        Log.d("CartViewModel", "========== END getActiveDiskonForProduct ==========")
        return null
    }

    fun updateCart(idproduk: Int, produkDetail: ProdukDetail, quantity: Int) {
        val currentMap = _cartItems.value ?: mutableMapOf()

        if (quantity > 0) {
            val (newHargaSatuan) = getAdjustedPrice(produkDetail, quantity)

            val existingItem = currentMap[idproduk]

            if (existingItem != null) {
                existingItem.quantity = quantity
                existingItem.hargaSatuan = newHargaSatuan
            } else {
                currentMap[idproduk] = CartItem(produkDetail, quantity, newHargaSatuan)
            }
        } else {
            currentMap.remove(idproduk)
        }

        _cartItems.value = currentMap
    }

    fun clearCart() {
        _cartItems.value = mutableMapOf()
    }

    fun getCartQuantitiesMap(): Map<Int, Int> {
        return _cartItems.value?.mapValues { it.value.quantity } ?: emptyMap()
    }
}