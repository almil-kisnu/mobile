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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class OrderRepository private constructor(private val context: Context) {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = SupabaseClientProvider.client
    private val realtimeManager = RealtimeConnectionManager.getInstance(context)

    private var orderChannel: RealtimeChannel? = null
    private var detailOrderChannel: RealtimeChannel? = null
    private var orderRealtimeJob: Job? = null
    private var detailOrderRealtimeJob: Job? = null

    private var isOrderRealtimeActive = false
    private var isDetailOrderRealtimeActive = false

    private val database = AppDatabase.getDatabase(context)
    private val orderDao = database.orderDao()
    private val detailOrderDao = database.detailOrderDao()
    private val prefsManager = PreferencesManager(context)

    // In-memory cache untuk instant realtime updates
    private val _orderDetailCache = MutableStateFlow<List<OrderWithDetails>>(emptyList())

    init {
        Log.d(TAG, "🚀 OrderRepository initialized")

        applicationScope.launch {
            // Load from Room first (offline capability)
            loadFromRoomToCache()

            // Then sync if needed
            syncDataIfNeeded()

            // Setup realtime - SDK auto-reconnect
            delay(1000)
            setupRealtimeListeners()
        }
    }

    fun getSharedOrderDetail(): Flow<List<OrderWithDetails>> = _orderDetailCache.asStateFlow()

    private suspend fun loadFromRoomToCache() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📂 Loading data from Room to cache...")
            
            val orderList = orderDao.getAllOrdersOnce()
            val detailList = detailOrderDao.getAllDetailOrdersOnce()
            
            if (orderList.isNotEmpty()) {
                updateCache(
                    orderList.map { it.toModel() },
                    detailList.map { it.toModel() }
                )
                Log.d(TAG, "✅ Loaded ${orderList.size} orders from Room to cache")
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

            val isFirstLaunch = prefsManager.isFirstOrderLaunch()

            if (isFirstLaunch) {
                Log.d(TAG, "🔄 First launch sync")
                syncAllDataFromSupabase()
                prefsManager.setFirstOrderLaunch(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in syncDataIfNeeded", e)
        }
    }

    private suspend fun syncAllDataFromSupabase() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📥 Syncing from Supabase...")

            val orderList = client.from("view_orders")
                .select()
                .decodeList<Order>()

            val detailOrderList = client.from("view_detailorder")
                .select()
                .decodeList<DetailOrder>()

            Log.d(TAG, "📦 Fetched - Orders: ${orderList.size}, Details: ${detailOrderList.size}")

            // Update Room (untuk offline)
            orderDao.deleteAll()
            detailOrderDao.deleteAll()

            orderDao.insertAll(orderList.map { it.toEntity() })
            detailOrderDao.insertAll(detailOrderList.map { it.toEntity() })

            // Update in-memory cache (untuk realtime)
            updateCache(orderList, detailOrderList)

            prefsManager.setLastOrderSyncTime(System.currentTimeMillis())

            Log.d(TAG, "✅ Sync completed and cache updated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync error", e)
            throw e
        }
    }

    private suspend fun setupRealtimeListeners() {
        try {
            Log.d(TAG, "🚀 Setting up ORDER & DETAIL realtime...")

            setupOrderRealtimeListener()
            delay(500)
            setupDetailOrderRealtimeListener()

            Log.d(TAG, "✅ Realtime setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Setup error: ${e.message}", e)
        }
    }

    private suspend fun setupOrderRealtimeListener() {
        if (isOrderRealtimeActive) {
            Log.w(TAG, "⚠️ Order realtime already active")
            return
        }

        try {
            Log.d(TAG, "🔴 Setting up ORDER listener...")
            Log.d(TAG, "   📋 Table: orders")
            Log.d(TAG, "   📋 Schema: public")

            val channel = client.channel("orders_channel")

            val orderFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "orders"
            }
            
            Log.d(TAG, "   ✅ Flow created successfully")

            orderRealtimeJob = applicationScope.launch {
                try {
                    orderFlow.collect { change ->
                        Log.d(TAG, "🔔 Order change: ${change.javaClass.simpleName}")
                        handleOrderRealtimeChange(change)
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Order flow cancelled")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Order flow error: ${e.message}", e)
                    isOrderRealtimeActive = false

                    // Retry
                    delay(5000)
                    if (!isOrderRealtimeActive) {
                        Log.d(TAG, "🔄 Retrying order realtime...")
                        setupOrderRealtimeListener()
                    }
                }
            }

            // Subscribe langsung seperti DiskonRepository
            channel.subscribe()

            orderChannel = channel
            isOrderRealtimeActive = true

            // Hitung jumlah data orders
            val orderCount = orderDao.getOrderCount()
            Log.d(TAG, "✅ Order realtime ACTIVE (direct subscribe)")
            Log.d(TAG, "📊 Total Orders in Database: $orderCount")

        } catch (e: Exception) {
            isOrderRealtimeActive = false
            orderRealtimeJob?.cancel()
            orderRealtimeJob = null
            Log.e(TAG, "❌ Order setup error: ${e.message}", e)
        }
    }

    private suspend fun setupDetailOrderRealtimeListener() {
        if (isDetailOrderRealtimeActive) {
            Log.w(TAG, "⚠️ Detail realtime already active")
            return
        }

        try {
            Log.d(TAG, "🔴 Setting up DETAIL listener...")
            Log.d(TAG, "   📋 Table: detailorder")
            Log.d(TAG, "   📋 Schema: public")

            val channel = client.channel("details_channel")

            val detailOrderFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "detailorder"
            }

            Log.d(TAG, "   ✅ Flow created successfully")

            detailOrderRealtimeJob = applicationScope.launch {
                try {
                    detailOrderFlow.collect { change ->
                        Log.d(TAG, "🔔 Detail change: ${change.javaClass.simpleName}")
                        handleDetailOrderRealtimeChange(change)
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Detail flow cancelled")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Detail flow error: ${e.message}", e)
                    isDetailOrderRealtimeActive = false

                    // Retry
                    delay(5000)
                    if (!isDetailOrderRealtimeActive) {
                        Log.d(TAG, "🔄 Retrying detail realtime...")
                        setupDetailOrderRealtimeListener()
                    }
                }
            }

            // Subscribe langsung seperti DiskonRepository
            channel.subscribe()

            detailOrderChannel = channel
            isDetailOrderRealtimeActive = true
            Log.d(TAG, "✅ Detail realtime ACTIVE (direct subscribe)")

        } catch (e: Exception) {
            isDetailOrderRealtimeActive = false
            detailOrderRealtimeJob?.cancel()
            detailOrderRealtimeJob = null
            Log.e(TAG, "❌ Detail setup error: ${e.message}", e)
        }
    }

    private suspend fun handleOrderRealtimeChange(change: PostgresAction) = withContext(Dispatchers.IO) {
        try {
            when (change) {
                is PostgresAction.Insert -> {
                    val rawOrder = change.decodeRecord<RawOrder>()
                    Log.d(TAG, "   🆕 INSERT Order: #${rawOrder.idorder}")

                    // ✅ PERBAIKAN: Fetch dari view untuk mendapat data lengkap dengan JOIN
                    val fullOrder = client.from("view_orders")
                        .select {
                            filter { eq("idorder", rawOrder.idorder) }
                        }
                        .decodeSingle<Order>()

                    // Update Room (untuk offline)
                    orderDao.insert(fullOrder.toEntity())
                    // Update cache instantly (untuk UI)
                    updateCacheWithOrderChange(fullOrder)
                    Log.d(TAG, "   ✅ Order #${fullOrder.idorder} saved")
                }

                is PostgresAction.Update -> {
                    val rawOrder = change.decodeRecord<RawOrder>()
                    Log.d(TAG, "   🔄 UPDATE Order: #${rawOrder.idorder}")

                    // ✅ PERBAIKAN: Fetch dari view untuk mendapat data lengkap dengan JOIN
                    val fullOrder = client.from("view_orders")
                        .select {
                            filter { eq("idorder", rawOrder.idorder) }
                        }
                        .decodeSingle<Order>()

                    // Update Room (untuk offline)
                    orderDao.update(fullOrder.toEntity())
                    // Update cache instantly (untuk UI)
                    updateCacheWithOrderChange(fullOrder)
                    Log.d(TAG, "   ✅ Order #${fullOrder.idorder} updated")
                }

                is PostgresAction.Delete -> {
                    val deletedOrder = change.decodeOldRecord<RawOrder>()
                    if (deletedOrder != null) {
                        Log.d(TAG, "   🗑️ DELETE Order: #${deletedOrder.idorder}")
                        // Update Room (untuk offline)
                        orderDao.deleteById(deletedOrder.idorder)
                        // Update cache instantly (untuk UI)
                        removeCacheOrderItem(deletedOrder.idorder)
                        Log.d(TAG, "   ✅ Order #${deletedOrder.idorder} deleted")
                    }
                }

                else -> {
                    Log.d(TAG, "   ❓ Unknown: ${change.javaClass.simpleName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Handle order change: ${e.message}", e)
        }
    }

    private suspend fun handleDetailOrderRealtimeChange(change: PostgresAction) = withContext(Dispatchers.IO) {
        try {
            when (change) {
                is PostgresAction.Insert -> {
                    val rawDetail = change.decodeRecord<RawDetailOrder>()
                    Log.d(TAG, "   🆕 INSERT Detail: #${rawDetail.iddetail}")

                    // ✅ PERBAIKAN: Fetch dari view untuk mendapat data lengkap dengan JOIN
                    val fullDetail = client.from("view_detailorder")
                        .select {
                            filter { eq("iddetail", rawDetail.iddetail) }
                        }
                        .decodeSingle<DetailOrder>()

                    // Update Room (untuk offline)
                    detailOrderDao.insert(fullDetail.toEntity())
                    // Update cache instantly (untuk UI)
                    updateCacheWithDetailChange(fullDetail)
                    Log.d(TAG, "   ✅ Detail #${fullDetail.iddetail} saved")
                }

                is PostgresAction.Update -> {
                    val rawDetail = change.decodeRecord<RawDetailOrder>()
                    Log.d(TAG, "   🔄 UPDATE Detail: #${rawDetail.iddetail}")

                    // ✅ PERBAIKAN: Fetch dari view untuk mendapat data lengkap dengan JOIN
                    val fullDetail = client.from("view_detailorder")
                        .select {
                            filter { eq("iddetail", rawDetail.iddetail) }
                        }
                        .decodeSingle<DetailOrder>()

                    // Update Room (untuk offline)
                    detailOrderDao.update(fullDetail.toEntity())
                    // Update cache instantly (untuk UI)
                    updateCacheWithDetailChange(fullDetail)
                    Log.d(TAG, "   ✅ Detail #${fullDetail.iddetail} updated")
                }

                is PostgresAction.Delete -> {
                    val deletedDetail = change.decodeOldRecord<RawDetailOrder>()
                    if (deletedDetail != null) {
                        Log.d(TAG, "   🗑️ DELETE Detail: #${deletedDetail.iddetail}")
                        // Update Room (untuk offline)
                        detailOrderDao.deleteById(deletedDetail.iddetail)
                        // Update cache instantly (untuk UI)
                        removeCacheDetailItem(deletedDetail.iddetail)
                        Log.d(TAG, "   ✅ Detail #${deletedDetail.iddetail} deleted")
                    }
                }

                else -> {
                    Log.d(TAG, "   ❓ Unknown: ${change.javaClass.simpleName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Handle detail change: ${e.message}", e)
        }
    }
    private var lastSyncTime = 0L
    private val syncDebounceMs = 2000L

    suspend fun forceSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastSyncTime < syncDebounceMs) {
                return@withContext Result.failure(Exception("Tunggu sebentar"))
            }
            lastSyncTime = now

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

            orderDao.deleteAll()
            detailOrderDao.deleteAll()
            prefsManager.clearOrderSyncData()

            Log.d(TAG, "✅ Data cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Clear error", e)
            Result.failure(e)
        }
    }

    fun getConnectionStatus(): OrderConnectionStatus {
        return OrderConnectionStatus(
            isOnline = NetworkUtils.isNetworkAvailable(context),
            isOrderRealtimeActive = isOrderRealtimeActive,
            isDetailOrderRealtimeActive = isDetailOrderRealtimeActive
        )
    }

    private fun updateCache(
        orderList: List<Order>,
        detailList: List<DetailOrder>
    ) {
        val cache = orderList.map { order ->
            OrderWithDetails(
                order = order,
                details = detailList.filter { it.idorder == order.idorder }
            )
        }
        _orderDetailCache.value = cache
        Log.d(TAG, "✅ Cache updated with ${cache.size} orders")
    }

    private fun updateCacheWithOrderChange(order: Order) {
        val currentCache = _orderDetailCache.value.toMutableList()
        val orderIndex = currentCache.indexOfFirst { it.order.idorder == order.idorder }

        if (orderIndex != -1) {
            // Update existing order, keep details
            val existingDetails = currentCache[orderIndex].details
            currentCache[orderIndex] = OrderWithDetails(order = order, details = existingDetails)
            Log.d(TAG, "   🔄 Updated existing order in cache: ${order.idorder}")
        } else {
            // Add new order with empty details
            currentCache.add(0, OrderWithDetails(order = order, details = emptyList()))
            Log.d(TAG, "   ➕ Added new order to cache: ${order.idorder}")
        }

        _orderDetailCache.value = currentCache
        Log.d(TAG, "✅ Cache updated for order ${order.idorder}")
    }

    private fun updateCacheWithDetailChange(detail: DetailOrder) {
        val currentCache = _orderDetailCache.value.toMutableList()
        val orderIndex = currentCache.indexOfFirst { it.order.idorder == detail.idorder }

        if (orderIndex != -1) {
            val orderWithDetails = currentCache[orderIndex]
            val updatedDetails = orderWithDetails.details.toMutableList()

            val detailIndex = updatedDetails.indexOfFirst { it.iddetail == detail.iddetail }
            if (detailIndex != -1) {
                updatedDetails[detailIndex] = detail
                Log.d(TAG, "   🔄 Updated existing detail: ${detail.iddetail}")
            } else {
                updatedDetails.add(detail)
                Log.d(TAG, "   ➕ Added new detail: ${detail.iddetail}")
            }

            currentCache[orderIndex] = orderWithDetails.copy(details = updatedDetails)
            _orderDetailCache.value = currentCache
            Log.d(TAG, "✅ Cache updated with detail for order ${detail.idorder}")
        } else {
            Log.w(TAG, "⚠️ Order ${detail.idorder} not found in cache for detail update")
        }
    }

    private fun removeCacheOrderItem(idorder: Int) {
        val currentCache = _orderDetailCache.value.toMutableList()
        val removed = currentCache.removeAll { it.order.idorder == idorder }

        if (removed) {
            _orderDetailCache.value = currentCache
            Log.d(TAG, "✅ Order $idorder removed from cache")
        } else {
            Log.w(TAG, "⚠️ Order $idorder not found in cache")
        }
    }

    private fun removeCacheDetailItem(iddetail: Int) {
        val currentCache = _orderDetailCache.value.toMutableList()
        var removed = false

        currentCache.forEachIndexed { index, orderWithDetails ->
            val updatedDetails = orderWithDetails.details.filter { it.iddetail != iddetail }
            if (updatedDetails.size != orderWithDetails.details.size) {
                currentCache[index] = orderWithDetails.copy(details = updatedDetails)
                removed = true
                Log.d(TAG, "   🗑️ Removed detail from order ${orderWithDetails.order.idorder}")
            }
        }

        if (removed) {
            _orderDetailCache.value = currentCache
            Log.d(TAG, "✅ Detail $iddetail removed from cache")
        } else {
            Log.w(TAG, "⚠️ Detail $iddetail not found in cache")
        }
    }

    suspend fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up...")

        isOrderRealtimeActive = false
        isDetailOrderRealtimeActive = false

        orderRealtimeJob?.cancel()
        detailOrderRealtimeJob?.cancel()
        orderRealtimeJob = null
        detailOrderRealtimeJob = null

        delay(500)

        // Unsubscribe langsung
        try {
            orderChannel?.unsubscribe()
            detailOrderChannel?.unsubscribe()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Unsubscribe warning: ${e.message}")
        }

        orderChannel = null
        detailOrderChannel = null

        Log.d(TAG, "✅ Cleanup done")
    }

    companion object {
        private const val TAG = "OrderRepository"

        @Volatile
        private var INSTANCE: OrderRepository? = null

        fun getInstance(context: Context): OrderRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = OrderRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

@Serializable
data class RawOrder(
    @SerialName("idorder")
    val idorder: Int,

    @SerialName("namapelanggan")
    val namapelanggan: String? = null,

    @SerialName("grandtotal")
    val grandtotal: Double? = null,

    @SerialName("bayar")
    val bayar: Double? = null,

    @SerialName("kembalian")
    val kembalian: Double? = null,

    @SerialName("notelp")
    val notelp: String? = null,

    @SerialName("alamat")
    val alamat: String? = null,

    @SerialName("idkasir")
    val idkasir: Int? = null,

    @SerialName("tanggalorder")
    val tanggalorder: String? = null,

    @SerialName("idoutlet")
    val idoutlet: Int = 1,

    @SerialName("metode_pembayaran")
    val metode_pembayaran: String? = null,

    @SerialName("status")
    val status: String? = null
)

@Serializable
data class RawDetailOrder(
    @SerialName("iddetail") val iddetail: Int,
    @SerialName("idorder") val idorder: Int,
    @SerialName("idproduk") val idproduk: Int? = null,
    @SerialName("harga") val harga: Double? = null,
    @SerialName("jumlah") val jumlah: Int? = null,
    @SerialName("subtotal") val subtotal: Double? = null
)

data class OrderConnectionStatus(
    val isOnline: Boolean,
    val isOrderRealtimeActive: Boolean,
    val isDetailOrderRealtimeActive: Boolean
)