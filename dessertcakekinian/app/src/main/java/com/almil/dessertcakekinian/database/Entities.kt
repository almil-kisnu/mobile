package com.almil.dessertcakekinian.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "produk_kategori")
data class ProdukKategoriEntity(
    @PrimaryKey
    @ColumnInfo(name = "idproduk")
    val idproduk: Int,
    @ColumnInfo(name = "namaproduk")
    val namaproduk: String,
    @ColumnInfo(name = "kategori")
    val kategori: String?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "barcode")
    val barcode: String?,
    @ColumnInfo(name = "harga_eceran")
    val hargaEceran: Double?
)

@Entity(tableName = "detail_stok")
data class DetailStokEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_detail_stock")
    val idDetailStock: Int,
    @ColumnInfo(name = "idproduk")
    val idproduk: Int,
    @ColumnInfo(name = "idoutlet")
    val idoutlet: Int,
    @ColumnInfo(name = "stok")
    val stok: Int,
    @ColumnInfo(name = "harga_beli")
    val hargaBeli: Double,
    @ColumnInfo(name = "tgl_kadaluarsa")
    val tglKadaluarsa: String?
)

@Entity(tableName = "harga_grosir")
data class HargaGrosirEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_harga")
    val idHarga: Int,

    @ColumnInfo(name = "idproduk")
    val idproduk: Int,

    @ColumnInfo(name = "min_qty")
    val minQty: Int,

    @ColumnInfo(name = "harga_jual")
    val hargaJual: Double
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val idorder: Int,
    val namapelanggan: String,
    val grandtotal: Double,
    val bayar: Double,
    val kembalian: Double,
    val tanggal: String,
    val jam: String,
    val metode_pembayaran: String,
    val status: String,
    val username: String?,
    val kode_outlet: String?
)

@Entity(tableName = "detail_orders")
data class DetailOrderEntity(
    @PrimaryKey
    val iddetail: Int,
    val idorder: Int,
    val namaproduk: String,
    val harga: Double,
    val jumlah: Int,
    val subtotal: Double
)

