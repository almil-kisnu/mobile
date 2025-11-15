package com.almil.dessertcakekinian.model

import android.content.Context
import android.util.Log
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.decodeOldRecord
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TransferRepository private constructor(context: Context) {

    private val client = SupabaseClientProvider.client
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Flow untuk Transfer Stock
    private val _transferStockFlow = MutableStateFlow<List<TransferStock>>(emptyList())
    val transferStockFlow: StateFlow<List<TransferStock>> = _transferStockFlow.asStateFlow()

    // Flow untuk Detail Transfer
    private val _detailTransferFlow = MutableStateFlow<List<DetailTransfer>>(emptyList())
    val detailTransferFlow: StateFlow<List<DetailTransfer>> = _detailTransferFlow.asStateFlow()

    // Flow untuk data status (Success/Error/Loading)
    private val _dataState = MutableStateFlow<TransferDataState>(TransferDataState.Loading)
    val dataState: StateFlow<TransferDataState> = _dataState.asStateFlow()

    // Flow untuk Joined Data
    val transferWithDetailsFlow: StateFlow<List<TransferWithDetails>> = combine(
        _transferStockFlow,
        _detailTransferFlow
    ) { transfers, details ->
        transfers.map { transfer ->
            val transferDetails = details.filter { it.idtransfer == transfer.idtransfer }

            TransferWithDetails(
                transfer = transfer,
                details = transferDetails
            )
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        Log.d(TAG, "🚀 TransferRepository initialized")

        scope.launch {
            // Load data pertama kali
            loadAllData()

            // Setup realtime
            delay(1000)
            setupRealtimeListeners()
        }
    }

    // Load semua data dari Supabase
    suspend fun loadAllData() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📥 Loading data from Supabase...")
            _dataState.value = TransferDataState.Loading

            // Fetch transfer stock
            val transfers = client.from("transfer_stock")
                .select()
                .decodeList<TransferStock>()

            Log.d(TAG, "✅ Transfer Stock fetched: ${transfers.size} records")

            // Log detail per status
            if (transfers.isNotEmpty()) {
                val statusCount = transfers.groupingBy { it.status }.eachCount()
                statusCount.forEach { (status, count) ->
                    Log.d(TAG, "   └─ Status '$status': $count transfer(s)")
                }
            } else {
                Log.w(TAG, "⚠️ Tidak ada data transfer_stock ditemukan di database")
            }

            // Fetch detail transfer
            val details = client.from("detail_transfer")
                .select()
                .decodeList<DetailTransfer>()

            Log.d(TAG, "✅ Detail Transfer fetched: ${details.size} records")

            // Log detail per transfer
            if (details.isNotEmpty()) {
                val detailsByTransfer = details.groupingBy { it.idtransfer }.eachCount()
                Log.d(TAG, "   └─ Details grouped by ${detailsByTransfer.size} transfer(s)")
                detailsByTransfer.forEach { (idTransfer, count) ->
                    val totalItems = details.filter { it.idtransfer == idTransfer }.sumOf { it.jumlah }
                    Log.d(TAG, "      • Transfer #$idTransfer: $count item(s), Total qty: $totalItems")
                }
            } else {
                Log.w(TAG, "⚠️ Tidak ada data detail_transfer ditemukan di database")
            }

            // Update flows
            _transferStockFlow.value = transfers
            _detailTransferFlow.value = details

            // Summary logging
            val pendingCount = transfers.count { it.status == "pending" }
            val dikirimCount = transfers.count { it.status == "dikirim" }
            val diterimaCount = transfers.count { it.status == "diterima" }
            val dibatalkanCount = transfers.count { it.status == "dibatalkan" }

            Log.d(TAG, "📊 SUMMARY:")
            Log.d(TAG, "   ├─ Total Transfers: ${transfers.size}")
            Log.d(TAG, "   ├─ Pending: $pendingCount")
            Log.d(TAG, "   ├─ Dikirim: $dikirimCount")
            Log.d(TAG, "   ├─ Diterima: $diterimaCount")
            Log.d(TAG, "   ├─ Dibatalkan: $dibatalkanCount")
            Log.d(TAG, "   └─ Total Detail Items: ${details.size}")

            _dataState.value = TransferDataState.Success(transfers.size, details.size)
            Log.d(TAG, "✅ Data loaded successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Load error: ${e.message}", e)
            Log.e(TAG, "   └─ Error type: ${e.javaClass.simpleName}")
            _dataState.value = TransferDataState.Error(e.message ?: "Unknown error")
        }
    }

    // Setup realtime untuk kedua tabel
    private suspend fun setupRealtimeListeners() {
        try {
            Log.d(TAG, "🔴 Setting up realtime listeners...")

            // Channel untuk transfer_stock
            val transferChannel = client.channel("transfer_events")
            val transferFlow = transferChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "transfer_stock"
            }

            scope.launch {
                transferFlow.collect { change ->
                    handleTransferChange(change)
                }
            }

            transferChannel.subscribe()
            Log.d(TAG, "✅ Realtime listener 'transfer_events' subscribed")

            // Channel untuk detail_transfer
            val detailChannel = client.channel("detail_transfer_events")
            val detailFlow = detailChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "detail_transfer"
            }

            scope.launch {
                detailFlow.collect { change ->
                    handleDetailChange(change)
                }
            }

            detailChannel.subscribe()
            Log.d(TAG, "✅ Realtime listener 'detail_transfer_events' subscribed")

            Log.d(TAG, "🎉 All realtime listeners active!")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Realtime setup error: ${e.message}", e)
        }
    }

    // Handle perubahan transfer_stock
    private suspend fun handleTransferChange(change: PostgresAction) {
        try {
            val currentList = _transferStockFlow.value.toMutableList()

            when (change) {
                is PostgresAction.Insert -> {
                    val newTransfer = change.decodeRecord<TransferStock>()
                    currentList.add(newTransfer)
                    Log.d(TAG, "🆕 INSERT Transfer: ID=${newTransfer.idtransfer}, Status=${newTransfer.status}")
                    Log.d(TAG, "   └─ From Outlet ${newTransfer.idoutletAsal} → Outlet ${newTransfer.idoutletTujuan}")
                }
                is PostgresAction.Update -> {
                    val updated = change.decodeRecord<TransferStock>()
                    val index = currentList.indexOfFirst { it.idtransfer == updated.idtransfer }
                    if (index != -1) {
                        val oldStatus = currentList[index].status
                        currentList[index] = updated
                        Log.d(TAG, "🔄 UPDATE Transfer: ID=${updated.idtransfer}")
                        Log.d(TAG, "   └─ Status changed: '$oldStatus' → '${updated.status}'")
                    }
                }
                is PostgresAction.Delete -> {
                    val deleted = change.decodeOldRecord<TransferStock>()
                    if (deleted != null) {
                        currentList.removeAll { it.idtransfer == deleted.idtransfer }
                        Log.d(TAG, "🗑️ DELETE Transfer: ID=${deleted.idtransfer}")
                    }
                }
                else -> {
                    Log.w(TAG, "⚠️ Received unhandled PostgresAction type: ${change::class.simpleName}")
                }
            }

            _transferStockFlow.value = currentList
            Log.d(TAG, "📊 Current total transfers: ${currentList.size}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Handle transfer error: ${e.message}", e)
        }
    }

    // Handle perubahan detail_transfer
    private suspend fun handleDetailChange(change: PostgresAction) {
        try {
            val currentList = _detailTransferFlow.value.toMutableList()

            when (change) {
                is PostgresAction.Insert -> {
                    val newDetail = change.decodeRecord<DetailTransfer>()
                    currentList.add(newDetail)
                    Log.d(TAG, "🆕 INSERT Detail: TransferID=${newDetail.idtransfer}, ProdukID=${newDetail.idproduk}, Qty=${newDetail.jumlah}")
                }
                is PostgresAction.Update -> {
                    val updated = change.decodeRecord<DetailTransfer>()
                    val index = currentList.indexOfFirst { it.iddetailTransfer == updated.iddetailTransfer }
                    if (index != -1) {
                        val old = currentList[index]
                        currentList[index] = updated
                        Log.d(TAG, "🔄 UPDATE Detail: ID=${updated.iddetailTransfer}")
                        Log.d(TAG, "   └─ Jumlah: ${old.jumlah} → ${updated.jumlah}, Diterima: ${old.jumlahDiterima} → ${updated.jumlahDiterima}")
                    }
                }
                is PostgresAction.Delete -> {
                    val deleted = change.decodeOldRecord<DetailTransfer>()
                    if (deleted != null) {
                        currentList.removeAll { it.iddetailTransfer == deleted.iddetailTransfer }
                        Log.d(TAG, "🗑️ DELETE Detail: ID=${deleted.iddetailTransfer}")
                    }
                }
                else -> {
                    Log.w(TAG, "⚠️ Received unhandled PostgresAction type: ${change::class.simpleName}")
                }
            }

            _detailTransferFlow.value = currentList
            Log.d(TAG, "📊 Current total detail items: ${currentList.size}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Handle detail error: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "TransferRepository"

        @Volatile
        private var INSTANCE: TransferRepository? = null

        fun getInstance(context: Context): TransferRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TransferRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

// Data State untuk monitoring
sealed class TransferDataState {
    object Loading : TransferDataState()
    data class Success(val transferCount: Int, val detailCount: Int) : TransferDataState()
    data class Error(val message: String) : TransferDataState()
}