package com.almil.dessertcakekinian.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Pastikan Anda telah menambahkan plugin 'kotlin-parcelize' di build.gradle (modul level)

@Serializable
data class ProdukKategori(
    val idproduk: Int,
    val namaproduk: String,
    val kategori: String? = null,
    val status: String,
    val barcode: String? = null
)

// products.kt (Atau file tempat data class berada)
@Serializable
data class DetailStok(
    // PERBAIKAN: Mapping 'id_detail_stock' ke 'idDetailStock'
    @SerialName("id_detail_stock") val idDetailStock: Int,

    val idproduk: Int,
    // Kolom ini tidak perlu @SerialName jika namanya sama/sesuai
    val idoutlet: Int,
    val stok: Int,

    // PERBAIKAN: Gunakan penamaan properti Kotlin yang standar (camelCase)
    @SerialName("harga_beli") val hargaBeli: Double,
    @SerialName("tgl_kadaluarsa") val tglKadaluarsa: String? = null
)

@Serializable
data class HargaGrosir(
    @SerialName("id_harga") val idHarga: Int,

    val idproduk: Int,
    @SerialName("min_qty") val minQty: Int,
    @SerialName("harga_jual") val hargaJual: Double
)

@Serializable
data class ProdukDetail(
    val produk: ProdukKategori,
    val hargaGrosir: List<HargaGrosir> = emptyList(),
    val detailStok: List<DetailStok> = emptyList()
)

@Serializable
data class Outlet(
    @SerialName("idoutlet")
    val idOutlet: Int, // Digunakan untuk menyimpan ke database pengguna

    @SerialName("kode_outlet")
    val kodeOutlet: String, // Digunakan untuk tampilan di Spinner

    @SerialName("nama_outlet")
    val namaOutlet: String, // Digunakan untuk tampilan di Spinner

    @SerialName("alamat")
    val alamat: String? = null,

    @SerialName("telepon")
    val telepon: String? = null,

    @SerialName("is_active")
    val isActive: Boolean? = true,

    @SerialName("created_at")
    val createdAt: String? = null
)
@Serializable
data class OutletInfo(
    @SerialName("idoutlet")
    val idOutlet: Int,
    @SerialName("kode_outlet")
    val kodeOutlet: String,

    @SerialName("nama_outlet")
    val namaOutlet: String
)

@Serializable
data class Pengguna(
    @SerialName("iduser")
    val idUser: Int,

    @SerialName("nik")
    val nik: String? = null,

    @SerialName("username")
    val username: String,

    @SerialName("password")
    val password: String,

    @SerialName("role")
    val role: String,

    @SerialName("phone")
    val phone: String,

    @SerialName("createdat")
    val createdAt: String,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("deactivated_at")
    val deactivatedAt: String? = null,

    @SerialName("deactivated_reason")
    val deactivatedReason: String? = null,

    @SerialName("hired_date")
    val hiredDate: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("idoutlet")
    val idOutlet: Int? = null,

    @SerialName("outlet")
    val outlet: OutletInfo? = null
)

@Serializable
data class Order(
    @SerialName("namapelanggan")
    val namapelanggan: String,

    @SerialName("total")
    val total: Double, // total before diskon (same as subtotal)

    @SerialName("diskon")
    val diskon: Double = 0.0,

    @SerialName("grandtotal")
    val grandtotal: Double, // final total after diskon (same as subtotal)

    @SerialName("bayar")
    val bayar: Double, // assuming cash payment or a placeholder

    @SerialName("kembalian")
    val kembalian: Double,

    @SerialName("metodebayar")
    val metodebayar: String,

    @SerialName("idkasir")
    val idkasir: Int
    // tanggalorder will be handled by the database
)

@Serializable
data class OrderId(
    @SerialName("idorder")
    val idorder: Int? = null
)
// DetailOrder biasanya hanya digunakan untuk keperluan database/API, tidak perlu Parcelable
@Serializable
data class DetailOrder(
    val idorder: Int,
    val idproduk: Int,
    val harga: Double, // price of the product
    val jumlah: Int, // quantity
    val subtotal: Double, // price * quantity
    val catatan: String? // product-specific note
)