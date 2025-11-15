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

    private val sharedOrderDetailFlow: Flow<List<OrderWithDetails>> = combine(
        orderDao.getAllOrders(),
        detailOrderDao.getAllDetailOrders()
    ) { orderList, detailOrderList ->
        Log.d(TAG, "📊 Combining: ${orderList.size} orders")
        orderList.map { orderEntity ->
            val order = orderEntity.toModel()
            val details = detailOrderList
                .filter { it.idorder == order.idorder }
                .map { it.toModel() }
            OrderWithDetails(order = order, details = details)
        }
    }.shareIn(
        scope = applicationScope,
        started = SharingStarted.WhileSubscribed(5000L),
        replay = 1
    )

    init {
        Log.d(TAG, "🚀 OrderRepository initialized")

        applicationScope.launch {
            // Sync data first
            syncDataIfNeeded()

            // Setup realtime - SDK auto-reconnect
            delay(1000)
            setupRealtimeListeners()
        }
    }

    fun getSharedOrderDetail(): Flow<List<OrderWithDetails>> = sharedOrderDetailFlow

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

            orderDao.deleteAll()
            detailOrderDao.deleteAll()

            orderDao.insertAll(orderList.map { it.toEntity() })
            detailOrderDao.insertAll(detailOrderList.map { it.toEntity() })

            prefsManager.setLastOrderSyncTime(System.currentTimeMillis())

            Log.d(TAG, "✅ Sync completed")
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

            val channel = client.channel("orders_channel")

            val orderFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "orders"
            }

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

            val subscribed = realtimeManager.subscribeChannel(
                channelId = CHANNEL_ID_ORDER,
                channel = channel,
                job = orderRealtimeJob!!
            )

            if (subscribed) {
                orderChannel = channel
                isOrderRealtimeActive = true
                Log.d(TAG, "✅ Order realtime ACTIVE")
            } else {
                orderRealtimeJob?.cancel()
                orderRealtimeJob = null
                Log.e(TAG, "❌ Failed to subscribe order")
            }

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

            val channel = client.channel("details_channel")

            val detailOrderFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "detailorder"
            }

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

            val subscribed = realtimeManager.subscribeChannel(
                channelId = CHANNEL_ID_DETAIL,
                channel = channel,
                job = detailOrderRealtimeJob!!
            )

            if (subscribed) {
                detailOrderChannel = channel
                isDetailOrderRealtimeActive = true
                Log.d(TAG, "✅ Detail realtime ACTIVE")
            } else {
                detailOrderRealtimeJob?.cancel()
                detailOrderRealtimeJob = null
                Log.e(TAG, "❌ Failed to subscribe detail")
            }

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

                    val fullOrder = client.from("view_orders")
                        .select {
                            filter { eq("idorder", rawOrder.idorder) }
                        }
                        .decodeSingle<Order>()

                    orderDao.insert(fullOrder.toEntity())
                    Log.d(TAG, "   ✅ Order #${fullOrder.idorder} saved")
                }

                is PostgresAction.Update -> {
                    val rawOrder = change.decodeRecord<RawOrder>()
                    Log.d(TAG, "   🔄 UPDATE Order: #${rawOrder.idorder}")

                    val fullOrder = client.from("view_orders")
                        .select {
                            filter { eq("idorder", rawOrder.idorder) }
                        }
                        .decodeSingle<Order>()

                    orderDao.update(fullOrder.toEntity())
                    Log.d(TAG, "   ✅ Order #${fullOrder.idorder} updated")
                }

                is PostgresAction.Delete -> {
                    val deletedOrder = change.decodeOldRecord<RawOrder>()
                    if (deletedOrder != null) {
                        orderDao.deleteById(deletedOrder.idorder)
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

                    val fullDetail = client.from("view_detailorder")
                        .select {
                            filter { eq("iddetail", rawDetail.iddetail) }
                        }
                        .decodeSingle<DetailOrder>()

                    detailOrderDao.insert(fullDetail.toEntity())
                    Log.d(TAG, "   ✅ Detail #${fullDetail.iddetail} saved")
                }

                is PostgresAction.Update -> {
                    val rawDetail = change.decodeRecord<RawDetailOrder>()
                    Log.d(TAG, "   🔄 UPDATE Detail: #${rawDetail.iddetail}")

                    val fullDetail = client.from("view_detailorder")
                        .select {
                            filter { eq("iddetail", rawDetail.iddetail) }
                        }
                        .decodeSingle<DetailOrder>()

                    detailOrderDao.update(fullDetail.toEntity())
                    Log.d(TAG, "   ✅ Detail #${fullDetail.iddetail} updated")
                }

                is PostgresAction.Delete -> {
                    val deletedDetail = change.decodeOldRecord<RawDetailOrder>()
                    if (deletedDetail != null) {
                        detailOrderDao.deleteById(deletedDetail.iddetail)
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

    suspend fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up...")

        isOrderRealtimeActive = false
        isDetailOrderRealtimeActive = false

        orderRealtimeJob?.cancel()
        detailOrderRealtimeJob?.cancel()
        orderRealtimeJob = null
        detailOrderRealtimeJob = null

        delay(500)

        orderChannel?.let {
            realtimeManager.unsubscribeChannel(CHANNEL_ID_ORDER)
        }
        detailOrderChannel?.let {
            realtimeManager.unsubscribeChannel(CHANNEL_ID_DETAIL)
        }

        orderChannel = null
        detailOrderChannel = null

        Log.d(TAG, "✅ Cleanup done")
    }

    companion object {
        private const val TAG = "OrderRepository"
        private const val CHANNEL_ID_ORDER = "order_channel"
        private const val CHANNEL_ID_DETAIL = "detail_channel"

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
    @SerialName("idorder") val idorder: Int,
    @SerialName("namapelanggan") val namapelanggan: String? = null,
    @SerialName("grandtotal") val grandtotal: Double? = null,
    @SerialName("bayar") val bayar: Double? = null,
    @SerialName("kembalian") val kembalian: Double? = null,
    @SerialName("idkasir") val idkasir: Int? = null,
    @SerialName("tanggalorder") val tanggalorder: String? = null,
    @SerialName("idoutlet") val idoutlet: Int = 1,
    @SerialName("metode_pembayaran") val metode_pembayaran: String? = null,
    @SerialName("status") val status: String? = null
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