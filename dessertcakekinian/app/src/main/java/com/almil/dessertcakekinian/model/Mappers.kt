package com.almil.dessertcakekinian.model

import com.almil.dessertcakekinian.database.ProdukKategoriEntity
import com.almil.dessertcakekinian.database.DetailStokEntity
import com.almil.dessertcakekinian.database.HargaGrosirEntity
import com.almil.dessertcakekinian.database.OrderEntity
import com.almil.dessertcakekinian.database.DetailOrderEntity

// Mapper dari Entity ke Model
fun ProdukKategoriEntity.toModel() = ProdukKategori(
    idproduk = idproduk,
    namaproduk = namaproduk,
    kategori = kategori,
    status = status,
    barcode = barcode,
    harga_eceran = hargaEceran
)

fun DetailStokEntity.toModel() = DetailStok(
    idDetailStock = idDetailStock,
    idproduk = idproduk,
    idoutlet = idoutlet,
    stok = stok,
    hargaBeli = hargaBeli,
    tglKadaluarsa = tglKadaluarsa
)

fun HargaGrosirEntity.toModel() = HargaGrosir(
    idHarga = idHarga,
    idproduk = idproduk,
    minQty = minQty,
    hargaJual = hargaJual
)

// Mapper dari Model ke Entity
fun ProdukKategori.toEntity() = ProdukKategoriEntity(
    idproduk = idproduk,
    namaproduk = namaproduk,
    kategori = kategori,
    status = status,
    barcode = barcode,
    hargaEceran = harga_eceran
)

fun DetailStok.toEntity() = DetailStokEntity(
    idDetailStock = idDetailStock,
    idproduk = idproduk,
    idoutlet = idoutlet,
    stok = stok,
    hargaBeli = hargaBeli,
    tglKadaluarsa = tglKadaluarsa
)

fun HargaGrosir.toEntity() = HargaGrosirEntity(
    idHarga = idHarga,
    idproduk = idproduk,
    minQty = minQty,
    hargaJual = hargaJual
)

fun Order.toEntity() = OrderEntity(
    idorder = idorder,
    namapelanggan = namapelanggan,
    grandtotal = grandtotal,
    bayar = bayar,
    kembalian = kembalian,
    tanggal = tanggal,
    jam = jam,
    metode_pembayaran = metode_pembayaran,
    status = status,
    username = username,
    kode_outlet = kode_outlet
)

fun OrderEntity.toModel() = Order(
    idorder = idorder,
    namapelanggan = namapelanggan,
    grandtotal = grandtotal,
    bayar = bayar,
    kembalian = kembalian,
    tanggal = tanggal,
    jam = jam,
    metode_pembayaran = metode_pembayaran,
    status = status,
    username = username,
    kode_outlet = kode_outlet
)

// DetailOrder conversions
fun DetailOrder.toEntity() = DetailOrderEntity(
    iddetail = iddetail,
    idorder = idorder,
    namaproduk = namaproduk,
    harga = harga,
    jumlah = jumlah,
    subtotal = subtotal
)

fun DetailOrderEntity.toModel() = DetailOrder(
    iddetail = iddetail,
    idorder = idorder,
    namaproduk = namaproduk,
    harga = harga,
    jumlah = jumlah,
    subtotal = subtotal
)