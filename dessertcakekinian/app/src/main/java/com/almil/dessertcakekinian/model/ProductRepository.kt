// File: com.almil.dessertcakekinian.model/ProductRepository.kt (Singleton DENGAN PERBAIKAN)

package com.almil.dessertcakekinian.model

import android.util.Log
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.decodeOldRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

// 💡 UBAH CLASS MENJADI OBJECT (SINGLETON)
object ProductRepository {

    // Gunakan ApplicationScope untuk menjaga Flow tetap aktif selama aplikasi berjalan
    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client = SupabaseClientProvider.client

    // Channel Realtime untuk memantau perubahan pada tabel detail_stock
    private val stockChannel = client.channel("stock_updates")

    // 💡 DEFINISIKAN FLOW HANYA SEKALI DAN SHARE KE SELURUH APLIKASI
    private val sharedProdukDetailFlow: Flow<List<ProdukDetail>> = flow {
        Log.d("ProductRepository", "STARTING NEW REALTIME STREAM (HANYA SEKALI)")
        // 1. Ambil data awal (Snapshot)
        var produkDetails = fetchInitialData()
        emit(produkDetails) // Keluarkan data awal

        // 2. Setup Realtime Listener
        val stockFlow = stockChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "detail_stock"
        }
        stockChannel.subscribe()

        // 3. Memproses pembaruan Realtime
        stockFlow.collect { change ->
            when (change) {
                is PostgresAction.Insert -> {
                    val newStock = change.decodeRecord<DetailStok>()
                    produkDetails = updateStokInList(produkDetails, newStock)
                }
                is PostgresAction.Update -> {
                    val updatedStock = change.decodeRecord<DetailStok>()
                    produkDetails = updateStokInList(produkDetails, updatedStock)
                }
                is PostgresAction.Delete -> {
                    val deletedStock = change.decodeOldRecord<DetailStok>()
                    if (deletedStock != null) {
                        produkDetails = removeStokFromList(produkDetails, deletedStock)
                    }
                }
                else -> { /* Ignore other actions */ }
            }
            emit(produkDetails)
        }
    }
        .shareIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(5000L),
            replay = 1 // Replay data terakhir segera ke observer baru
        )

    // 💡 FUNGSI PUBLIC BARU: Semua ViewModel akan memanggil fungsi ini
    fun getSharedProdukDetail(): Flow<List<ProdukDetail>> = sharedProdukDetailFlow


    private suspend fun fetchInitialData(): List<ProdukDetail> = withContext(Dispatchers.IO) {
        val produkList = client.from("v_produk_kategori").select().decodeList<ProdukKategori>()
        val hargaList = client.from("harga_grosir").select().decodeList<HargaGrosir>()
        val stokList = client.from("detail_stock").select().decodeList<DetailStok>()

        return@withContext produkList.map { produk ->
            val hargaGrosirProduk = hargaList.filter { it.idproduk == produk.idproduk }
            val stokProduk = stokList.filter { it.idproduk == produk.idproduk }

            ProdukDetail(
                produk = produk,
                hargaGrosir = hargaGrosirProduk,
                detailStok = stokProduk
            )
        }
    }

    private fun updateStokInList(currentList: List<ProdukDetail>, updatedStock: DetailStok): List<ProdukDetail> {
        return currentList.map { produkDetail ->
            if (produkDetail.produk.idproduk == updatedStock.idproduk) {
                val newStokList = produkDetail.detailStok.map { stok ->
                    if (stok.idDetailStock == updatedStock.idDetailStock) {
                        updatedStock
                    } else {
                        stok
                    }
                }.toMutableList().apply {
                    if (!any { it.idDetailStock == updatedStock.idDetailStock }) {
                        add(updatedStock)
                    }
                }
                produkDetail.copy(detailStok = newStokList.toList())
            } else {
                produkDetail
            }
        }
    }

    private fun removeStokFromList(currentList: List<ProdukDetail>, deletedStock: DetailStok): List<ProdukDetail> {
        return currentList.map { produkDetail ->
            if (produkDetail.produk.idproduk == deletedStock.idproduk) {
                val newStokList = produkDetail.detailStok.filter { it.idDetailStock != deletedStock.idDetailStock }
                produkDetail.copy(detailStok = newStokList)
            } else {
                produkDetail
            }
        }
    }

    suspend fun closeRealtimeChannel() {
        stockChannel.unsubscribe()
        Log.d("ProductRepository", "Realtime channel ditutup (Repository Singleton).")
    }
}