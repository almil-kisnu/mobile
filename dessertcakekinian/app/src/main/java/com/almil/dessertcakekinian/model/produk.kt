package com.almil.dessertcakekinian.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProdukKategori(
    val idproduk: Int,
    val namaproduk: String,
    val kategori: String? = null,
    val status: String,
    val barcode: String? = null,
    val harga_eceran: Double? = null
)
@Serializable
data class DetailStok(
    @SerialName("id_detail_stock") val idDetailStock: Int,
    val idproduk: Int,
    val idoutlet: Int,
    val stok: Int,
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
    val detailStok: List<DetailStok> = emptyList(),
)
data class CartItem(
    val produkDetail: ProdukDetail,
    var quantity: Int,
    var hargaSatuan: Double = 0.0
)

@Serializable
data class EventDiskon(
    @SerialName("id_diskon")
    val idDiskon: Int = 0,

    @SerialName("nama_diskon")
    val namaDiskon: String,

    @SerialName("deskripsi")
    val deskripsi: String? = null,

    @SerialName("tanggal_mulai")
    val tanggalMulai: String,

    @SerialName("jam_mulai")
    val jamMulai: String,

    @SerialName("tanggal_selesai")
    val tanggalSelesai: String,

    @SerialName("jam_selesai")
    val jamSelesai: String,

    @SerialName("nilai_diskon")
    val nilaiDiskon: Double,

    @SerialName("berlaku_untuk")
    val berlakuUntuk: String,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("created_at")
    val createdAt: String? = null
): java.io.Serializable

// Model 2: Detail Diskon Produk
@Serializable
data class DetailDiskonProduk(
    @SerialName("id_detail_diskon_produk")
    val idDetailDiskonProduk: Int = 0,

    @SerialName("id_diskon")
    val idDiskon: Int,

    @SerialName("idproduk")
    val idproduk: Int
)

// Model 3: Joined Data (Diskon dengan Produk-produknya)
data class DiskonWithProducts(
    val diskon: EventDiskon,
    val produkIds: List<Int>
)

@Serializable
data class Order(
    @SerialName("idorder")
    val idorder: Int,

    @SerialName("namapelanggan")
    val namapelanggan: String,

    @SerialName("grandtotal")
    val grandtotal: Double,

    @SerialName("bayar")
    val bayar: Double,

    @SerialName("kembalian")
    val kembalian: Double,

    @SerialName("notelp")
    val notelp: String? = null,

    @SerialName("alamat")
    val alamat: String? = null,

    @SerialName("idkasir")          // ✅ TAMBAHKAN INI
    val idkasir: Int? = null,       // ✅ TAMBAHKAN INI

    @SerialName("tanggal")
    val tanggal: String,

    @SerialName("jam")
    val jam: String,

    @SerialName("tanggalorder")     // ✅ Tambahkan juga karena ada di view
    val tanggalorder: String? = null, // ✅ Tambahkan juga karena ada di view

    @SerialName("idoutlet")         // ✅ Tambahkan juga
    val idoutlet: Int? = null,      // ✅ Tambahkan juga

    @SerialName("metode_pembayaran")
    val metode_pembayaran: String,

    @SerialName("status")
    val status: String,

    @SerialName("username")
    val username: String? = null,

    @SerialName("kode_outlet")
    val kode_outlet: String? = null
)

@Serializable
data class DetailOrder(
    @SerialName("iddetail")
    val iddetail: Int,

    @SerialName("idorder")
    val idorder: Int,

    @SerialName("namaproduk")
    val namaproduk: String,

    @SerialName("harga")
    val harga: Double,

    @SerialName("jumlah")
    val jumlah: Int,

    @SerialName("subtotal")
    val subtotal: Double
)


@Serializable
data class OrderWithDetails(
    val order: Order,
    val details: List<DetailOrder>
)


// digunakan sebagai insert ke database
@Serializable // Wajib ada
data class TransaksiOrder(
    val grandtotal: Double,
    val bayar: Double,
    val kembalian: Double,
    val idkasir: Int,
    val idoutlet: Int,
    val metode_pembayaran: String
)
@Serializable
data class TransaksiDetailOrder(
    val idorder: Int,
    val idproduk: Int,
    val harga: Double,
    val jumlah: Int,
    val subtotal: Double
)

@Serializable
data class TransferStock(
    @SerialName("idtransfer")
    val idtransfer: Int = 0,

    @SerialName("idoutlet_asal")
    val idoutletAsal: Int,

    @SerialName("idoutlet_tujuan")
    val idoutletTujuan: Int,

    @SerialName("tanggal_transfer")
    val tanggalTransfer: String? = null,

    @SerialName("tanggal_terima")
    val tanggalTerima: String? = null,

    @SerialName("status")
    val status: String = "pending", // pending, dikirim, diterima, dibatalkan

    @SerialName("iduser_pengirim")
    val iduserPengirim: Int,

    @SerialName("iduser_penerima")
    val iduserPenerima: Int? = null,

    @SerialName("catatan")
    val catatan: String? = null
)

// Model 2: Detail Transfer
@Serializable
data class DetailTransfer(
    @SerialName("iddetail_transfer")
    val iddetailTransfer: Int = 0,

    @SerialName("idtransfer")
    val idtransfer: Int,

    @SerialName("idproduk")
    val idproduk: Int,

    @SerialName("jumlah")
    val jumlah: Int,

    @SerialName("jumlah_diterima")
    val jumlahDiterima: Int? = null,

    @SerialName("catatan")
    val catatan: String? = null
)

// Model 3: Transfer dengan Details (Joined)
data class TransferWithDetails(
    val transfer: TransferStock,
    val details: List<DetailTransfer>
)