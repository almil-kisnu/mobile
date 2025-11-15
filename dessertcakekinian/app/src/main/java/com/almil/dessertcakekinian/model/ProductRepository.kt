package com.almil.dessertcakekinian.model

import android.content.Context
import android.util.Log
import com.almil.dessertcakekinian.database.AppDatabase
import com.almil.dessertcakekinian.database.RealtimeConnectionManager
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import com.almil.dessertcakekinian.utils.NetworkUtils
import com.almil.dessertcakekinian.utils.PreferencesManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.decodeOldRecord
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ProductRepository private constructor(private val context: Context) {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = SupabaseClientProvider.client
    private val realtimeManager = RealtimeConnectionManager.getInstance(context)

    private var stockChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null
    private var isRealtimeActive = false

    private val database = AppDatabase.getDatabase(context)
    private val produkDao = database.produkDao()
    private val detailStokDao = database.detailStokDao()
    private val hargaGrosirDao = database.hargaGrosirDao()

    private val prefsManager = PreferencesManager(context)

    private val sharedProdukDetailFlow: Flow<List<ProdukDetail>> = combine(
        produkDao.getAllProduk(),
        detailStokDao.getAllStok(),
        hargaGrosirDao.getAllHarga()
    ) { produkList, stokList, hargaList ->
        Log.d(TAG, "📊 Combining: ${produkList.size} products")

        produkList.map { produkEntity ->
            val produk = produkEntity.toModel()
            val hargaGrosirProduk = hargaList
                .filter { it.idproduk == produk.idproduk }
                .map { it.toModel() }
            val stokProduk = stokList
                .filter { it.idproduk == produk.idproduk }
                .map { it.toModel() }

            ProdukDetail(
                produk = produk,
                hargaGrosir = hargaGrosirProduk,
                detailStok = stokProduk
            )
        }
    }.shareIn(
        scope = applicationScope,
        started = SharingStarted.WhileSubscribed(5000L),
        replay = 1
    )

    init {
        Log.d(TAG, "🚀 ProductRepository initialized")

        applicationScope.launch {
            // Sync data first
            syncDataIfNeeded()

            // Setup realtime - SDK will auto-reconnect on network changes
            delay(1000)
            setupRealtimeListener()
        }
    }

    fun getSharedProdukDetail(): Flow<List<ProdukDetail>> = sharedProdukDetailFlow

    private suspend fun syncDataIfNeeded() = withContext(Dispatchers.IO) {
        try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                Log.w(TAG, "⚠️ OFFLINE - using cache")
                return@withContext
            }

            val isFirstLaunch = prefsManager.isFirstLaunch()

            if (isFirstLaunch) {
                Log.d(TAG, "🔄 First launch sync")
                syncAllDataFromSupabase()
                prefsManager.setFirstLaunch(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in syncDataIfNeeded", e)
        }
    }

    private suspend fun syncAllDataFromSupabase() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📥 Syncing from Supabase...")

            val produkList = client.from("v_produk_kategori")
                .select()
                .decodeList<ProdukKategori>()

            val hargaList = client.from("harga_grosir")
                .select()
                .decodeList<HargaGrosir>()

            val stokList = client.from("detail_stock")
                .select()
                .decodeList<DetailStok>()

            Log.d(TAG, "📦 Fetched - Produk: ${produkList.size}, Harga: ${hargaList.size}, Stok: ${stokList.size}")

            produkDao.insertAll(produkList.map { it.toEntity() })
            hargaGrosirDao.insertAll(hargaList.map { it.toEntity() })
            detailStokDao.insertAll(stokList.map { it.toEntity() })

            prefsManager.setLastSyncTime(System.currentTimeMillis())

            Log.d(TAG, "✅ Sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync error", e)
            throw e
        }
    }

    private suspend fun setupRealtimeListener() {
        if (isRealtimeActive) {
            Log.w(TAG, "⚠️ Realtime already active")
            return
        }

        try {
            Log.d(TAG, "🔴 Setting up STOCK realtime...")

            // Create channel - unique name bukan masalah, SDK track by reference
            val channel = client.channel("product_stock")

            // Setup postgres change flow
            val stockFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "detail_stock"
            }

            // Job untuk collect changes - SDK auto-reconnect saat network berubah
            realtimeJob = applicationScope.launch {
                try {
                    stockFlow.collect { change ->
                        Log.d(TAG, "🔔 Stock change: ${change.javaClass.simpleName}")
                        handleRealtimeChange(change)
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Stock flow cancelled")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Stock flow error: ${e.message}", e)
                    isRealtimeActive = false

                    // Retry setelah 5 detik jika error
                    delay(5000)
                    if (!isRealtimeActive) {
                        Log.d(TAG, "🔄 Retrying realtime setup...")
                        setupRealtimeListener()
                    }
                }
            }

            // Subscribe - SDK handle websocket otomatis
            val subscribed = realtimeManager.subscribeChannel(
                channelId = CHANNEL_ID_STOCK,
                channel = channel,
                job = realtimeJob!!
            )

            if (subscribed) {
                stockChannel = channel
                isRealtimeActive = true
                Log.d(TAG, "✅ Stock realtime ACTIVE - SDK will auto-reconnect")
            } else {
                realtimeJob?.cancel()
                realtimeJob = null
                Log.e(TAG, "❌ Failed to subscribe")
            }

        } catch (e: Exception) {
            isRealtimeActive = false
            realtimeJob?.cancel()
            realtimeJob = null
            Log.e(TAG, "❌ Setup error: ${e.message}", e)
        }
    }

    private suspend fun handleRealtimeChange(change: PostgresAction) = withContext(Dispatchers.IO) {
        try {
            when (change) {
                is PostgresAction.Insert -> {
                    val newStock = change.decodeRecord<DetailStok>()
                    detailStokDao.insert(newStock.toEntity())
                    Log.d(TAG, "   🆕 INSERT: ID=${newStock.idDetailStock}, Qty=${newStock.stok}")
                }

                is PostgresAction.Update -> {
                    val updatedStock = change.decodeRecord<DetailStok>()
                    detailStokDao.update(updatedStock.toEntity())
                    Log.d(TAG, "   🔄 UPDATE: ID=${updatedStock.idDetailStock}, Qty=${updatedStock.stok}")
                }

                is PostgresAction.Delete -> {
                    val deletedStock = change.decodeOldRecord<DetailStok>()
                    if (deletedStock != null) {
                        detailStokDao.deleteById(deletedStock.idDetailStock)
                        Log.d(TAG, "   🗑️ DELETE: ID=${deletedStock.idDetailStock}")
                    }
                }

                else -> {
                    Log.d(TAG, "   ❓ Unknown: ${change.javaClass.simpleName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Handle change error: ${e.message}", e)
        }
    }

    suspend fun forceSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return@withContext Result.failure(Exception("Tidak ada koneksi"))
            }

            Log.d(TAG, "🔄 Force sync...")
            syncAllDataFromSupabase()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Force sync failed", e)
            Result.failure(e)
        }
    }

    suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ Clearing all data...")

            produkDao.deleteAll()
            hargaGrosirDao.deleteAll()
            detailStokDao.deleteAll()
            prefsManager.clearSyncData()

            Log.d(TAG, "✅ Data cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Clear error", e)
            Result.failure(e)
        }
    }

    fun getConnectionStatus(): ConnectionStatus {
        return ConnectionStatus(
            isOnline = NetworkUtils.isNetworkAvailable(context),
            isRealtimeActive = isRealtimeActive
        )
    }

    suspend fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up...")

        isRealtimeActive = false

        realtimeJob?.cancel()
        realtimeJob = null

        delay(500)

        stockChannel?.let {
            realtimeManager.unsubscribeChannel(CHANNEL_ID_STOCK)
        }
        stockChannel = null

        Log.d(TAG, "✅ Cleanup done")
    }

    companion object {
        private const val TAG = "ProductRepository"
        private const val CHANNEL_ID_STOCK = "product_stock"

        @Volatile
        private var INSTANCE: ProductRepository? = null

        fun getInstance(context: Context): ProductRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ProductRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

data class ConnectionStatus(
    val isOnline: Boolean,
    val isRealtimeActive: Boolean
)