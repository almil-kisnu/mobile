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

    // In-memory cache untuk instant realtime updates
    private val _productDetailCache = MutableStateFlow<List<ProdukDetail>>(emptyList())

    init {
        Log.d(TAG, "🚀 ProductRepository initialized")

        applicationScope.launch {
            // Load from Room first (offline capability)
            loadFromRoomToCache()

            // Then sync if needed
            syncDataIfNeeded()

            // Setup realtime - SDK will auto-reconnect on network changes
            delay(1000)
            setupRealtimeListener()
        }
    }

    fun getSharedProdukDetail(): Flow<List<ProdukDetail>> = _productDetailCache.asStateFlow()

    private suspend fun loadFromRoomToCache() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📂 Loading data from Room to cache...")
            
            val produkList = produkDao.getAllProdukOnce()
            val stokList = detailStokDao.getAllStokOnce()
            val hargaList = hargaGrosirDao.getAllHargaOnce()
            
            if (produkList.isNotEmpty()) {
                updateCache(
                    produkList.map { it.toModel() },
                    hargaList.map { it.toModel() },
                    stokList.map { it.toModel() }
                )
                Log.d(TAG, "✅ Loaded ${produkList.size} products from Room to cache")
            } else {
                Log.d(TAG, "⚠️ Room database is empty, will sync from server")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading from Room: ${e.message}", e)
        }
    }

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

            // Update Room (untuk offline)
            produkDao.insertAll(produkList.map { it.toEntity() })
            hargaGrosirDao.insertAll(hargaList.map { it.toEntity() })
            detailStokDao.insertAll(stokList.map { it.toEntity() })

            // Update in-memory cache (untuk realtime)
            updateCache(produkList, hargaList, stokList)

            prefsManager.setLastSyncTime(System.currentTimeMillis())

            Log.d(TAG, "✅ Sync completed and cache updated")
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
                    // Update Room (untuk offline)
                    detailStokDao.insert(newStock.toEntity())
                    // Update cache instantly (untuk UI)
                    updateCacheWithStockChange(newStock)
                    Log.d(TAG, "   🆕 INSERT: ID=${newStock.idDetailStock}, Qty=${newStock.stok}")
                }

                is PostgresAction.Update -> {
                    val updatedStock = change.decodeRecord<DetailStok>()
                    // Update Room (untuk offline)
                    detailStokDao.update(updatedStock.toEntity())
                    // Update cache instantly (untuk UI)
                    updateCacheWithStockChange(updatedStock)
                    Log.d(TAG, "   🔄 UPDATE: ID=${updatedStock.idDetailStock}, Qty=${updatedStock.stok}")
                }

                is PostgresAction.Delete -> {
                    val deletedStock = change.decodeOldRecord<DetailStok>()
                    if (deletedStock != null) {
                        // Update Room (untuk offline)
                        detailStokDao.deleteById(deletedStock.idDetailStock)
                        // Update cache instantly (untuk UI)
                        removeCacheStockItem(deletedStock.idDetailStock)
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

    private fun updateCache(
        produkList: List<ProdukKategori>,
        hargaList: List<HargaGrosir>,
        stokList: List<DetailStok>
    ) {
        val cache = produkList.map { produk ->
            ProdukDetail(
                produk = produk,
                hargaGrosir = hargaList.filter { it.idproduk == produk.idproduk },
                detailStok = stokList.filter { it.idproduk == produk.idproduk }
            )
        }
        _productDetailCache.value = cache
        Log.d(TAG, "✅ Cache updated with ${cache.size} products")
    }

    private fun updateCacheWithStockChange(stock: DetailStok) {
        val currentCache = _productDetailCache.value.toMutableList()
        val productIndex = currentCache.indexOfFirst { it.produk.idproduk == stock.idproduk }
        
        if (productIndex != -1) {
            val product = currentCache[productIndex]
            val updatedStocks = product.detailStok.toMutableList()
            
            val stockIndex = updatedStocks.indexOfFirst { it.idDetailStock == stock.idDetailStock }
            if (stockIndex != -1) {
                updatedStocks[stockIndex] = stock
                Log.d(TAG, "   🔄 Updated existing stock: ${stock.idDetailStock}, qty=${stock.stok}")
            } else {
                updatedStocks.add(stock)
                Log.d(TAG, "   ➕ Added new stock: ${stock.idDetailStock}, qty=${stock.stok}")
            }
            
            currentCache[productIndex] = product.copy(detailStok = updatedStocks)
            _productDetailCache.value = currentCache
            Log.d(TAG, "✅ Cache updated for product ${stock.idproduk}")
        } else {
            Log.w(TAG, "⚠️ Product ${stock.idproduk} not found in cache")
        }
    }

    private fun removeCacheStockItem(idDetailStock: Int) {
        val currentCache = _productDetailCache.value.toMutableList()
        var removed = false
        
        currentCache.forEachIndexed { index, product ->
            val updatedStocks = product.detailStok.filter { it.idDetailStock != idDetailStock }
            if (updatedStocks.size != product.detailStok.size) {
                currentCache[index] = product.copy(detailStok = updatedStocks)
                removed = true
                Log.d(TAG, "   🗑️ Removed stock from product ${product.produk.idproduk}")
            }
        }
        
        if (removed) {
            _productDetailCache.value = currentCache
            Log.d(TAG, "✅ Cache updated - stock item $idDetailStock removed")
        } else {
            Log.w(TAG, "⚠️ Stock item $idDetailStock not found in cache")
        }
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