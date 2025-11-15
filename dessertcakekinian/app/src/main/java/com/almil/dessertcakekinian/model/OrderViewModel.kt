package com.almil.dessertcakekinian.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OrderRepository.getInstance(application)

    private val _allOrders = MutableStateFlow<OrderDataState>(OrderDataState.Loading)
    val allOrders: StateFlow<OrderDataState> = _allOrders

    init {
        // Observe data dari repository
        observeOrders()
    }

    private fun observeOrders() {
        viewModelScope.launch {
            repository.getSharedOrderDetail()
                .catch { e ->
                    // Handle error dari Flow
                    _allOrders.value = OrderDataState.Error(e.message ?: "Unknown error")
                }
                .collect { orderList ->
                    // Emit Success dengan data
                    _allOrders.value = OrderDataState.Success(orderList)
                }
        }
    }

    // 🔥 FIX: forceRefresh dengan proper error handling
    fun forceRefresh() {
        viewModelScope.launch {
            // Set loading HANYA jika belum ada data
            if (_allOrders.value is OrderDataState.Success &&
                (_allOrders.value as OrderDataState.Success).orders.isEmpty()) {
                _allOrders.value = OrderDataState.Loading
            }
            // Jika sudah ada data, biarkan SwipeRefreshLayout yang handle loading UI

            val result = repository.forceSync()

            result.onFailure { error ->
                // Emit error, tapi jangan overwrite data yang sudah ada
                val currentState = _allOrders.value
                if (currentState is OrderDataState.Success && currentState.orders.isNotEmpty()) {
                    // Sudah ada data, cukup emit error tanpa clear data
                    _allOrders.value = OrderDataState.Error(
                        error.message ?: "Gagal refresh",
                        currentState.orders // Kirim data lama
                    )
                } else {
                    // Tidak ada data, emit error biasa
                    _allOrders.value = OrderDataState.Error(error.message ?: "Gagal refresh")
                }
            }

            // onSuccess tidak perlu handle karena Flow otomatis emit data baru
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.cleanup()
        }
    }
}

// 🔥 FIX: OrderDataState dengan data lama saat error
sealed class OrderDataState {
    object Loading : OrderDataState()
    data class Success(val orders: List<OrderWithDetails>) : OrderDataState()
    data class Error(
        val message: String,
        val cachedOrders: List<OrderWithDetails> = emptyList() // Data lama jika ada
    ) : OrderDataState()
}