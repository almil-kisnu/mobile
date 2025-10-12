package com.almil.dessertcakekinian.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.parcelize.Parcelize // Import untuk @Parcelize
import android.os.Parcelable // Import untuk Parcelable

// Pastikan Anda telah menambahkan plugin 'kotlin-parcelize' di build.gradle (modul level)

// WAJIB PARCELABLE JIKA INGIN DITRANSFER MELALUI BUNDLE/INTENT
@Parcelize
@Serializable
data class produk(
    @SerialName("idproduk")
    val idproduk: Int? = null,

    @SerialName("kodeproduk")
    val kodeproduk: String? = null,

    @SerialName("namaproduk")
    val namaproduk: String,

    @SerialName("idkategori")
    val idkategori: Int? = null,

    @SerialName("hargamodal")
    val hargamodal: Double? = null,

    @SerialName("hargajual")
    val hargajual: Double,

    @SerialName("stock")
    val stock: Int,

    @SerialName("gambar")
    val gambar: String? = null,

    @SerialName("deskripsi")
    val deskripsi: String,

    @SerialName("status")
    val status: String
) : Parcelable // <-- DITAMBAHKAN

// WAJIB PARCELABLE JIKA INGIN DITRANSFER MELALUI BUNDLE/INTENT
@Parcelize
@Serializable
data class KategoriProduk(
    @SerialName("idkategori")
    val idkategori: Int? = null,

    @SerialName("nkategori")
    val nkategori: String
) : Parcelable // <-- DITAMBAHKAN

// WAJIB PARCELABLE JIKA INGIN DITRANSFER MELALUI BUNDLE/INTENT
@Parcelize
@Serializable
data class ProdukWithKategori(
    @SerialName("idproduk")
    val idproduk: Int? = null,

    @SerialName("kodeproduk")
    val kodeproduk: String? = null,

    @SerialName("namaproduk")
    val namaproduk: String,

    @SerialName("hargajual")
    val hargajual: Double,

    @SerialName("stock")
    val stock: Int,

    @SerialName("gambar")
    val gambar: String? = null,

    @SerialName("deskripsi")
    val deskripsi: String,

    @SerialName("kategori")
    val kategori: KategoriProduk? = null
) : Parcelable // <-- DITAMBAHKAN

// Data class Order tidak terlalu sering dikirim antar komponen, tapi tetap bisa dijadikan Parcelable jika dibutuhkan.
// Saya tidak mengubahnya menjadi Parcelable karena fokusnya adalah pada objek 'produk' yang menyebabkan error.
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
data class Pengguna(
    @SerialName("iduser")
    val idUser: Int,

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

    // --- TAMBAHKAN BARIS INI ---
    @SerialName("updated_at")
    val updatedAt: String? = null // Kolom baru yang menyebabkan error
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