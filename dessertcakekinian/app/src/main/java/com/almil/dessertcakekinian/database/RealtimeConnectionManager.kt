package com.almil.dessertcakekinian.database

import android.content.Context
import android.util.Log
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.*

class RealtimeConnectionManager private constructor(private val context: Context) {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = SupabaseClientProvider.client

    // Tracking channels yang aktif
    private val activeChannels = mutableMapOf<String, RealtimeChannel>()
    private val channelJobs = mutableMapOf<String, Job>()

    init {
        Log.d(TAG, "🚀 RealtimeConnectionManager initialized (SDK auto-reconnect mode)")
    }

    /**
     * Subscribe ke channel - SDK otomatis handle websocket
     */
    suspend fun subscribeChannel(
        channelId: String,
        channel: RealtimeChannel,
        job: Job
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check jika channel sudah ada
            synchronized(activeChannels) {
                if (activeChannels.containsKey(channelId)) {
                    Log.w(TAG, "⚠️ Channel $channelId already active")
                    return@withContext true
                }
            }

            Log.d(TAG, "📡 Subscribing channel: $channelId")

            // Subscribe - SDK otomatis connect websocket jika belum
            channel.subscribe()

            // Wait untuk subscribe complete (max 5 detik)
            var retries = 0
            while (channel.status.value.toString() != "SUBSCRIBED" && retries < 25) {
                delay(200)
                retries++
            }

            val finalStatus = channel.status.value.toString()
            if (finalStatus != "SUBSCRIBED") {
                Log.e(TAG, "❌ Failed to subscribe $channelId. Status: $finalStatus")
                return@withContext false
            }

            synchronized(activeChannels) {
                activeChannels[channelId] = channel
                channelJobs[channelId] = job
            }

            Log.d(TAG, "✅ Channel $channelId SUBSCRIBED. Active: ${activeChannels.size}")
            Log.d(TAG, "   Websocket: ${client.realtime.status.value}")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error subscribing $channelId: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Unsubscribe channel
     */
    suspend fun unsubscribeChannel(channelId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔌 Unsubscribing: $channelId")

            // Cancel job
            synchronized(channelJobs) {
                channelJobs[channelId]?.cancel()
                channelJobs.remove(channelId)
            }

            // Unsubscribe channel
            val channel = synchronized(activeChannels) {
                activeChannels.remove(channelId)
            }

            channel?.let {
                try {
                    val status = it.status.value.toString()
                    if (status == "SUBSCRIBED" || status == "SUBSCRIBING") {
                        it.unsubscribe()
                        delay(300)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Unsubscribe warning: ${e.message}")
                }
            }

            Log.d(TAG, "✅ $channelId removed. Remaining: ${activeChannels.size}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unsubscribing $channelId", e)
        }
    }

    /**
     * Cleanup all channels
     */
    suspend fun cleanupAllChannels() {
        try {
            Log.d(TAG, "🧹 Cleaning up all channels...")

            // Cancel all jobs
            synchronized(channelJobs) {
                channelJobs.values.forEach {
                    try { it.cancel() } catch (_: Exception) { }
                }
                channelJobs.clear()
            }

            delay(500)

            // Unsubscribe all channels
            val channelsToClean = synchronized(activeChannels) {
                activeChannels.toMap()
            }

            channelsToClean.forEach { (channelId, channel) ->
                try {
                    val status = channel.status.value.toString()
                    if (status == "SUBSCRIBED" || status == "SUBSCRIBING") {
                        channel.unsubscribe()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Cleanup warning for $channelId: ${e.message}")
                }
            }

            synchronized(activeChannels) {
                activeChannels.clear()
            }

            Log.d(TAG, "✅ All channels cleaned")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cleanup error", e)
        }
    }

    fun getActiveChannelCount(): Int = synchronized(activeChannels) { activeChannels.size }

    suspend fun cleanup() {
        Log.d(TAG, "🧹 Final cleanup...")
        cleanupAllChannels()
    }

    companion object {
        private const val TAG = "RealtimeConnectionMgr"

        @Volatile
        private var INSTANCE: RealtimeConnectionManager? = null

        fun getInstance(context: Context): RealtimeConnectionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RealtimeConnectionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}