package com.almil.dessertcakekinian.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiskonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DiskonRepository.getInstance(application)

    // All diskons dengan produk-produknya (realtime)
    val allDiskonWithProducts: StateFlow<List<DiskonWithProducts>> =
        repository.diskonWithProductsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Active diskons only
    val activeDiskon: StateFlow<List<EventDiskon>> =
        repository.eventDiskonFlow
            .map { events -> events.filter { it.isActive } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // All events
    val allEvents: StateFlow<List<EventDiskon>> =
        repository.eventDiskonFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        Log.d(TAG, "🚀 DiskonViewModel initialized")

        // Monitor data changes
        viewModelScope.launch {
            allDiskonWithProducts.collect { diskons ->
                val activeCount = diskons.count { it.diskon.isActive }
                val totalProduk = diskons.sumOf { it.produkIds.size }
                Log.d(TAG, "📊 Diskons: ${diskons.size}, Active: $activeCount, Total Produk: $totalProduk")
            }
        }
    }

    // Reload data dari Supabase
    fun reload() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.loadAllData()
            _isLoading.value = false
        }
    }

    // Get diskon untuk produk tertentu
    fun getDiskonForProduk(idproduk: Int): Flow<List<EventDiskon>> {
        return combine(
            repository.eventDiskonFlow,
            repository.detailDiskonFlow
        ) { events, details ->
            val diskonIds = details
                .filter { it.idproduk == idproduk }
                .map { it.idDiskon }

            events.filter { it.idDiskon in diskonIds && it.isActive }
        }
    }

    // Hitung harga setelah diskon
    fun hitungHargaDiskon(hargaAsli: Double, idproduk: Int): Flow<Double> {
        return getDiskonForProduk(idproduk).map { diskons ->
            val totalDiskonPersen = diskons.sumOf { it.nilaiDiskon }
            val totalDiskon = hargaAsli * (totalDiskonPersen / 100)
            (hargaAsli - totalDiskon).coerceAtLeast(0.0)
        }
    }

    companion object {
        private const val TAG = "DiskonViewModel"
    }
}