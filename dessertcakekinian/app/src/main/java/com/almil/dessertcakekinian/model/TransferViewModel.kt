package com.almil.dessertcakekinian.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransferViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransferRepository.getInstance(application)

    // All transfers dengan detail-detailnya (realtime)
    val allTransferWithDetails: StateFlow<List<TransferWithDetails>> =
        repository.transferWithDetailsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Filter by status
    val pendingTransfers: StateFlow<List<TransferStock>> =
        repository.transferStockFlow
            .map { transfers -> transfers.filter { it.status == "pending" } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val dikirimTransfers: StateFlow<List<TransferStock>> =
        repository.transferStockFlow
            .map { transfers -> transfers.filter { it.status == "dikirim" } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val diterimaTransfers: StateFlow<List<TransferStock>> =
        repository.transferStockFlow
            .map { transfers -> transfers.filter { it.status == "diterima" } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // All transfers
    val allTransfers: StateFlow<List<TransferStock>> =
        repository.transferStockFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Data state (Loading/Success/Error)
    val dataState: StateFlow<TransferDataState> =
        repository.dataState
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TransferDataState.Loading
            )

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        Log.d(TAG, "🚀 TransferViewModel initialized")

        // Monitor data changes
        viewModelScope.launch {
            allTransferWithDetails.collect { transfers ->
                val statusCount = transfers.groupingBy { it.transfer.status }.eachCount()
                val totalItems = transfers.sumOf { it.details.size }
                Log.d(TAG, "📊 Transfers: ${transfers.size}, Status: $statusCount, Total Items: $totalItems")
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

    // Get transfer by ID
    fun getTransferById(idtransfer: Int): Flow<TransferWithDetails?> {
        return repository.transferWithDetailsFlow.map { transfers ->
            transfers.find { it.transfer.idtransfer == idtransfer }
        }
    }

    // Get transfers by outlet asal
    fun getTransfersByOutletAsal(idoutlet: Int): Flow<List<TransferStock>> {
        return repository.transferStockFlow.map { transfers ->
            transfers.filter { it.idoutletAsal == idoutlet }
        }
    }

    // Get transfers by outlet tujuan
    fun getTransfersByOutletTujuan(idoutlet: Int): Flow<List<TransferStock>> {
        return repository.transferStockFlow.map { transfers ->
            transfers.filter { it.idoutletTujuan == idoutlet }
        }
    }

    // Get transfers by user pengirim
    fun getTransfersByPengirim(iduser: Int): Flow<List<TransferStock>> {
        return repository.transferStockFlow.map { transfers ->
            transfers.filter { it.iduserPengirim == iduser }
        }
    }

    // Get transfers by user penerima
    fun getTransfersByPenerima(iduser: Int): Flow<List<TransferStock>> {
        return repository.transferStockFlow.map { transfers ->
            transfers.filter { it.iduserPenerima == iduser }
        }
    }

    // Get detail transfer by transfer ID
    fun getDetailsByTransferId(idtransfer: Int): Flow<List<DetailTransfer>> {
        return repository.detailTransferFlow.map { details ->
            details.filter { it.idtransfer == idtransfer }
        }
    }

    // Calculate total items in transfer
    fun getTotalItemsInTransfer(idtransfer: Int): Flow<Int> {
        return getDetailsByTransferId(idtransfer).map { details ->
            details.sumOf { it.jumlah }
        }
    }

    // Calculate total items diterima in transfer
    fun getTotalDiterimaInTransfer(idtransfer: Int): Flow<Int> {
        return getDetailsByTransferId(idtransfer).map { details ->
            details.sumOf { it.jumlahDiterima ?: 0 }
        }
    }

    companion object {
        private const val TAG = "TransferViewModel"
    }
}