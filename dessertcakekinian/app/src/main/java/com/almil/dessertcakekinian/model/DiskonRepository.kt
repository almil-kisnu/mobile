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

class DiskonRepository private constructor(context: Context) {

    private val client = SupabaseClientProvider.client
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Flow untuk Event Diskon
    private val _eventDiskonFlow = MutableStateFlow<List<EventDiskon>>(emptyList())
    val eventDiskonFlow: StateFlow<List<EventDiskon>> = _eventDiskonFlow.asStateFlow()

    // Flow untuk Detail Diskon Produk
    private val _detailDiskonFlow = MutableStateFlow<List<DetailDiskonProduk>>(emptyList())
    val detailDiskonFlow: StateFlow<List<DetailDiskonProduk>> = _detailDiskonFlow.asStateFlow()

    // Flow untuk Joined Data
    val diskonWithProductsFlow: StateFlow<List<DiskonWithProducts>> = combine(
        _eventDiskonFlow,
        _detailDiskonFlow
    ) { events, details ->
        events.map { event ->
            val produkIds = details
                .filter { it.idDiskon == event.idDiskon }
                .map { it.idproduk }

            DiskonWithProducts(
                diskon = event,
                produkIds = produkIds
            )
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        Log.d(TAG, "🚀 DiskonRepository initialized")

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

            // Fetch event diskon
            val events = client.from("event_diskon")
                .select()
                .decodeList<EventDiskon>()

            // Fetch detail diskon produk
            val details = client.from("detail_diskon_produk")
                .select()
                .decodeList<DetailDiskonProduk>()

            Log.d(TAG, "📦 Loaded: ${events.size} events, ${details.size} details")

            // Update flows
            _eventDiskonFlow.value = events
            _detailDiskonFlow.value = details

            val activeCount = events.count { it.isActive }
            Log.d(TAG, "✅ Data loaded - Active: $activeCount/${events.size}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Load error: ${e.message}", e)
        }
    }

    // Setup realtime untuk event_diskon
    private suspend fun setupRealtimeListeners() {
        try {
            Log.d(TAG, "🔴 Setting up realtime...")

            // Channel untuk event_diskon
            val eventChannel = client.channel("diskon_events")
            val eventFlow = eventChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "event_diskon"
            }

            scope.launch {
                eventFlow.collect { change ->
                    handleEventChange(change)
                }
            }

            eventChannel.subscribe()

            // Channel untuk detail_diskon_produk
            val detailChannel = client.channel("diskon_details")
            val detailFlow = detailChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "detail_diskon_produk"
            }

            scope.launch {
                detailFlow.collect { change ->
                    handleDetailChange(change)
                }
            }

            detailChannel.subscribe()

            Log.d(TAG, "✅ Realtime active")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Realtime error: ${e.message}", e)
        }
    }

    // Handle perubahan event_diskon
    private suspend fun handleEventChange(change: PostgresAction) {
        try {
            val currentList = _eventDiskonFlow.value.toMutableList()

            when (change) {
                is PostgresAction.Insert -> {
                    val newEvent = change.decodeRecord<EventDiskon>()
                    currentList.add(newEvent)
                    Log.d(TAG, "🆕 INSERT Event: ${newEvent.namaDiskon}")
                }
                is PostgresAction.Update -> {
                    val updated = change.decodeRecord<EventDiskon>()
                    val index = currentList.indexOfFirst { it.idDiskon == updated.idDiskon }
                    if (index != -1) {
                        currentList[index] = updated
                        Log.d(TAG, "🔄 UPDATE Event: ${updated.namaDiskon}")
                    }
                }
                is PostgresAction.Delete -> {
                    val deleted = change.decodeOldRecord<EventDiskon>()
                    if (deleted != null) {
                        currentList.removeAll { it.idDiskon == deleted.idDiskon }
                        Log.d(TAG, "🗑️ DELETE Event: ${deleted.namaDiskon}")
                    }
                }else -> {
                Log.w(TAG, "⚠️ Received unhandled PostgresAction type: ${change::class.simpleName}")
            }
            }

            _eventDiskonFlow.value = currentList
        } catch (e: Exception) {
            Log.e(TAG, "❌ Handle event error: ${e.message}", e)
        }
    }

    // Handle perubahan detail_diskon_produk
    private suspend fun handleDetailChange(change: PostgresAction) {
        try {
            val currentList = _detailDiskonFlow.value.toMutableList()

            when (change) {
                is PostgresAction.Insert -> {
                    val newDetail = change.decodeRecord<DetailDiskonProduk>()
                    currentList.add(newDetail)
                    Log.d(TAG, "🆕 INSERT Detail: DiskonID=${newDetail.idDiskon}")
                }
                is PostgresAction.Update -> {
                    val updated = change.decodeRecord<DetailDiskonProduk>()
                    val index = currentList.indexOfFirst { it.idDetailDiskonProduk == updated.idDetailDiskonProduk }
                    if (index != -1) {
                        currentList[index] = updated
                        Log.d(TAG, "🔄 UPDATE Detail")
                    }
                }
                is PostgresAction.Delete -> {
                    val deleted = change.decodeOldRecord<DetailDiskonProduk>()
                    if (deleted != null) {
                        currentList.removeAll { it.idDetailDiskonProduk == deleted.idDetailDiskonProduk }
                        Log.d(TAG, "🗑️ DELETE Detail")
                    }
                }else -> {
                Log.w(TAG, "⚠️ Received unhandled PostgresAction type: ${change::class.simpleName}")
            }
            }

            _detailDiskonFlow.value = currentList
        } catch (e: Exception) {
            Log.e(TAG, "❌ Handle detail error: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "DiskonRepository"

        @Volatile
        private var INSTANCE: DiskonRepository? = null

        fun getInstance(context: Context): DiskonRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DiskonRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}