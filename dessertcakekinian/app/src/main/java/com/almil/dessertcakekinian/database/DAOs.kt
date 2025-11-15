package com.almil.dessertcakekinian.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdukDao {
    @Query("SELECT * FROM produk_kategori")
    fun getAllProduk(): Flow<List<ProdukKategoriEntity>>
    @Query("SELECT * FROM produk_kategori WHERE idproduk = :id")
    suspend fun getProdukById(id: Int): ProdukKategoriEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(produk: List<ProdukKategoriEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produk: ProdukKategoriEntity)
    @Update
    suspend fun update(produk: ProdukKategoriEntity)
    @Delete
    suspend fun delete(produk: ProdukKategoriEntity)
    @Query("DELETE FROM produk_kategori")
    suspend fun deleteAll()
}

@Dao
interface DetailStokDao {
    @Query("SELECT * FROM detail_stok WHERE idproduk = :idproduk")
    fun getStokByProdukId(idproduk: Int): Flow<List<DetailStokEntity>>
    @Query("SELECT * FROM detail_stok")
    fun getAllStok(): Flow<List<DetailStokEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stok: List<DetailStokEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stok: DetailStokEntity)
    @Update
    suspend fun update(stok: DetailStokEntity)
    @Delete
    suspend fun delete(stok: DetailStokEntity)
    @Query("DELETE FROM detail_stok WHERE id_detail_stock = :idDetailStock")
    suspend fun deleteById(idDetailStock: Int)
    @Query("DELETE FROM detail_stok")
    suspend fun deleteAll()
}

@Dao
interface HargaGrosirDao {
    @Query("SELECT * FROM harga_grosir WHERE idproduk = :idproduk")
    fun getHargaByProdukId(idproduk: Int): Flow<List<HargaGrosirEntity>>
    @Query("SELECT * FROM harga_grosir")
    fun getAllHarga(): Flow<List<HargaGrosirEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(harga: List<HargaGrosirEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(harga: HargaGrosirEntity)
    @Update
    suspend fun update(harga: HargaGrosirEntity)
    @Delete
    suspend fun delete(harga: HargaGrosirEntity)
    @Query("DELETE FROM harga_grosir")
    suspend fun deleteAll()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY idorder DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>
    @Query("SELECT * FROM orders WHERE idorder = :orderId")
    suspend fun getOrderById(orderId: Int): OrderEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)
    @Update
    suspend fun update(order: OrderEntity)
    @Delete
    suspend fun delete(order: OrderEntity)
    @Query("DELETE FROM orders WHERE idorder = :orderId")
    suspend fun deleteById(orderId: Int)
    @Query("DELETE FROM orders")
    suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int
}

@Dao
interface DetailOrderDao {
    @Query("SELECT * FROM detail_orders ORDER BY iddetail DESC")
    fun getAllDetailOrders(): Flow<List<DetailOrderEntity>>
    @Query("SELECT * FROM detail_orders WHERE idorder = :orderId")
    suspend fun getDetailsByOrderId(orderId: Int): List<DetailOrderEntity>
    @Query("SELECT * FROM detail_orders WHERE iddetail = :detailId")
    suspend fun getDetailById(detailId: Int): DetailOrderEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: DetailOrderEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<DetailOrderEntity>)
    @Update
    suspend fun update(detail: DetailOrderEntity)
    @Delete
    suspend fun delete(detail: DetailOrderEntity)
    @Query("DELETE FROM detail_orders WHERE iddetail = :detailId")
    suspend fun deleteById(detailId: Int)
    @Query("DELETE FROM detail_orders WHERE idorder = :orderId")
    suspend fun deleteByOrderId(orderId: Int)
    @Query("DELETE FROM detail_orders")
    suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM detail_orders")
    suspend fun getDetailCount(): Int
}

