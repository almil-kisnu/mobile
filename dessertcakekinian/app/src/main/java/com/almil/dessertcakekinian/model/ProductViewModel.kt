// File: com.almil.dessertcakekinian.model/ProductViewModel.kt (UPDATED)

package com.almil.dessertcakekinian.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class ProductDataState {
    data object Loading : ProductDataState()
    data class Success(val produkDetails: List<ProdukDetail>) : ProductDataState()
    data class Error(val message: String) : ProductDataState()
}

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository = ProductRepository.getInstance(application)

    val allProducts: StateFlow<ProductDataState> = repository.getSharedProdukDetail()
        .map<List<ProdukDetail>, ProductDataState> {
            if (it.isEmpty()) {
                ProductDataState.Loading
            } else {
                ProductDataState.Success(it)
            }
        }
        .catch { e ->
            val errorMessage = "Gagal memuat data: ${e.message}"
            Log.e("ProductViewModel", errorMessage, e)
            emit(ProductDataState.Error(errorMessage))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProductDataState.Loading
        )

    /**
     * Force refresh data dari server
     * Dipanggil saat user melakukan pull-to-refresh
     */
    fun forceRefresh() {
        viewModelScope.launch {
            try {
                Log.d("ProductViewModel", "Force refresh dimulai...")
                repository.forceSync()
                Log.d("ProductViewModel", "Force refresh selesai")
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error saat force refresh: ${e.message}", e)
            }
        }
    }

    /**
     * Get raw product list untuk keperluan internal
     */
    fun getRawProductList(): List<ProdukDetail> {
        return when (val state = allProducts.value) {
            is ProductDataState.Success -> state.produkDetails
            else -> emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.cleanup()
        }
    }
}