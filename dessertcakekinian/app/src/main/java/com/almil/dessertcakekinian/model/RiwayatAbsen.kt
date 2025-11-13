package com.almil.dessertcakekinian.model

data class RiwayatAbsen(
    val id: String,
    val namaPengguna: String,
    val role: String,
    val tanggal: String,
    val jamMasuk: String,
    val jamPulang: String?,
    val status: String?,
    val keteranganIzin: String?,
    val pesanOwner: String?,
    val dibuatPada: String?
) {
    // Constructor untuk data dari SharedPreferences
    constructor(tanggal: String, jamMasuk: String, jamPulang: String, status: String) :
            this("", "Kasir", "Karyawan", tanggal, jamMasuk, jamPulang, status, null, null, null)
}