// File: com.almil.dessertcakekinian.model/ProductViewModel.kt (PERBAIKAN ERROR 'Unresolved reference')

package com.almil.dessertcakekinian.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

sealed class ProductDataState {
    data object Loading : ProductDataState()
    data class Success(val produkDetails: List<ProdukDetail>) : ProductDataState()
    data class Error(val message: String) : ProductDataState()
}

class ProductViewModel : ViewModel() {

    // Akses Singleton
    private val repository: ProductRepository = ProductRepository

    // STATE UTAMA - Mengamati Shared Flow dari Repository
    val allProducts: StateFlow<ProductDataState> = repository.getSharedProdukDetail()
        .map<List<ProdukDetail>, ProductDataState> { ProductDataState.Success(it) }
        .catch { e ->
            val errorMessage = "Gagal memuat atau streaming data dari Repository: ${e.message}"
            Log.e("ProductViewModel", errorMessage, e)
            emit(ProductDataState.Error(errorMessage))
        }
        // 💡 BARIS BERMASALAH DIHAPUS. Kita biarkan initialValue = Loading yang menangani state awal.
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProductDataState.Loading
        )

    /**
     * Fungsi Reload (tetap seperti sebelumnya, hanya logging)
     */
    fun reloadData() {
        Log.w("ProductViewModel", "reloadData() dipanggil, tetapi SharedFlow yang menangani koneksi.")
    }

    // Fungsi getRawProductList tetap sama (Mengakses StateFlow internal ViewModel)
    fun getRawProductList(): List<ProdukDetail> {
        return when (val state = allProducts.value) {
            is ProductDataState.Success -> state.produkDetails
            else -> emptyList()
        }
    }
}