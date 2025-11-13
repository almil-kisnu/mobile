package com.almil.dessertcakekinian.database

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import com.almil.dessertcakekinian.model.RiwayatAbsen

class SupabaseHelper {

    companion object {
        private const val SUPABASE_URL = "https://rujrwhtwkoferxhhnruq.supabase.co/rest/v1/"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ1anJ3aHR3a29mZXJ4aGhucnVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTkxNDYwODYsImV4cCI6MjA3NDcyMjA4Nn0.v7ALlCUbNBDy2TWQ0rhGc0tR0qgnR4J9ko8v-8Bwrww"

        private val executor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())

        private fun getCurrentTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }
    }

    interface RiwayatCallback {
        fun onSuccess(riwayatList: List<RiwayatAbsen>)
        fun onError(error: String)
    }

    interface SimpanCallback {
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    fun simpanMasuk(tanggal: String, jamMasuk: String, lokasiMasuk: String, namaPengguna: String, status: String, keteranganIzin: String, callback: SimpanCallback) {
        executeSimpanMasuk(tanggal, jamMasuk, lokasiMasuk, namaPengguna, status, keteranganIzin, callback)
    }

    fun simpanMasuk(tanggal: String, jamMasuk: String, lokasiMasuk: String, idPengguna: Int, status: String?, keteranganIzin: String?, callback: SimpanCallback) {
        executeSimpanMasukWithId(tanggal, jamMasuk, lokasiMasuk, idPengguna, status, keteranganIzin, callback)
    }

    fun simpanPulang(tanggal: String, jamPulang: String, lokasiPulang: String, namaPengguna: String, status: String, keteranganIzin: String, callback: SimpanCallback) {
        executeSimpanPulang(tanggal, jamPulang, lokasiPulang, namaPengguna, status, keteranganIzin, callback)
    }

    fun simpanPulang(tanggal: String, jamPulang: String, lokasiPulang: String, idPengguna: Int, status: String?, keteranganIzin: String?, callback: SimpanCallback) {
        executeSimpanPulangWithId(tanggal, jamPulang, lokasiPulang, idPengguna, status, keteranganIzin, callback)
    }

    fun getRiwayat(callback: RiwayatCallback) {
        executeGetRiwayatTask(callback)
    }

    fun getRiwayatByIdPengguna(idPengguna: Int, callback: RiwayatCallback) {
        executeGetRiwayatTaskFiltered(idPengguna, callback)
    }

    private fun executeSimpanMasuk(tanggal: String, jamMasuk: String, lokasiMasuk: String, namaPengguna: String, status: String, keteranganIzin: String, callback: SimpanCallback) {
        executor.execute {
            try {
                val url = URL("${SUPABASE_URL}presensi")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Prefer", "return=minimal")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val jsonData = JSONObject().apply {
                    put("tanggal", tanggal)
                    put("jam_masuk", jamMasuk)
                    put("jam_pulang", JSONObject.NULL)
                    put("status", status)
                    put("keterangan_izin", keteranganIzin)
                    put("pesan_owner", "")
                    put("created_at", getCurrentTimestamp())
                    put("id_pengguna", Math.abs(namaPengguna.hashCode()))
                }

                val os: OutputStream = conn.outputStream
                os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val responseCode = conn.responseCode
                val result = if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    "success"
                } else {
                    val br = BufferedReader(InputStreamReader(conn.errorStream))
                    val response = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) response.append(line)
                    br.close()
                    "Error: $responseCode - $response"
                }

                mainHandler.post {
                    if (result == "success") {
                        callback.onSuccess("Absen masuk berhasil disimpan ke cloud")
                    } else {
                        callback.onError(result)
                    }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError("Error: ${e.message}") }
            }
        }
    }

    private fun executeSimpanMasukWithId(tanggal: String, jamMasuk: String, lokasiMasuk: String, idPengguna: Int, status: String?, keteranganIzin: String?, callback: SimpanCallback) {
        executor.execute {
            try {
                val url = URL("${SUPABASE_URL}presensi")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Prefer", "return=minimal")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val jsonData = JSONObject().apply {
                    put("tanggal", tanggal)
                    put("jam_masuk", jamMasuk)
                    put("jam_pulang", JSONObject.NULL)
                    put("status", status ?: "Hadir")
                    put("keterangan_izin", keteranganIzin ?: "")
                    put("pesan_owner", "")
                    put("created_at", getCurrentTimestamp())
                    put("id_pengguna", idPengguna)
                }

                val os: OutputStream = conn.outputStream
                os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val responseCode = conn.responseCode
                val result = if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    "success"
                } else {
                    "Error: $responseCode"
                }

                mainHandler.post {
                    if (result == "success") {
                        callback.onSuccess("Absen masuk berhasil disimpan ke cloud")
                    } else {
                        callback.onError(result)
                    }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError("Error: ${e.message}") }
            }
        }
    }

    private fun executeSimpanPulang(tanggal: String, jamPulang: String, lokasiPulang: String, namaPengguna: String, status: String, keteranganIzin: String, callback: SimpanCallback) {
        executor.execute {
            try {
                val idPengguna = Math.abs(namaPengguna.hashCode())
                val filter = "tanggal=eq.${URLEncoder.encode(tanggal, "UTF-8")}" +
                        "&id_pengguna=eq.$idPengguna" +
                        "&jam_masuk=not.is.null" +
                        "&jam_pulang=is.null"

                val url = URL("${SUPABASE_URL}presensi?$filter")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "PATCH"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Prefer", "return=minimal")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val jsonData = JSONObject().apply {
                    put("jam_pulang", jamPulang)
                    put("status", status)
                    put("keterangan_izin", keteranganIzin)
                }

                val os: OutputStream = conn.outputStream
                os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val responseCode = conn.responseCode
                val result = if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    "success"
                } else {
                    val br = BufferedReader(InputStreamReader(conn.errorStream))
                    val response = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) response.append(line)
                    br.close()
                    "Error: $responseCode - $response"
                }

                mainHandler.post {
                    if (result == "success") {
                        callback.onSuccess("Absen pulang berhasil disimpan ke cloud")
                    } else {
                        callback.onError("Gagal update absen pulang: $result\nPastikan sudah absen masuk hari ini dengan nama yang sama")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError("Error: ${e.message}") }
            }
        }
    }

    private fun executeSimpanPulangWithId(tanggal: String, jamPulang: String, lokasiPulang: String, idPengguna: Int, status: String?, keteranganIzin: String?, callback: SimpanCallback) {
        executor.execute {
            try {
                val filter = "tanggal=eq.${URLEncoder.encode(tanggal, "UTF-8")}" +
                        "&id_pengguna=eq.$idPengguna" +
                        "&jam_masuk=not.is.null" +
                        "&jam_pulang=is.null"

                val url = URL("${SUPABASE_URL}presensi?$filter")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "PATCH"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Prefer", "return=minimal")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val jsonData = JSONObject().apply {
                    put("jam_pulang", jamPulang)
                    put("status", status ?: "Hadir")
                    put("keterangan_izin", keteranganIzin ?: "")
                }

                val os: OutputStream = conn.outputStream
                os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val responseCode = conn.responseCode
                val result = if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    "success"
                } else {
                    val br = BufferedReader(InputStreamReader(conn.errorStream))
                    val response = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) response.append(line)
                    br.close()
                    "Error: $responseCode - $response"
                }

                mainHandler.post {
                    if (result == "success") {
                        callback.onSuccess("Absen pulang berhasil disimpan ke cloud")
                    } else {
                        callback.onError("Gagal update absen pulang: $result\nPastikan sudah absen masuk hari ini dengan ID yang sama")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError("Error: ${e.message}") }
            }
        }
    }

    private fun executeGetRiwayatTask(callback: RiwayatCallback) {
        executor.execute {
            try {
                val url = URL("${SUPABASE_URL}presensi?order=created_at.desc")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val br = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) response.append(line)
                    br.close()

                    val jsonArray = JSONArray(response.toString())
                    val riwayatList = mutableListOf<RiwayatAbsen>()

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val riwayatAbsen = parseRiwayatFromJson(item)
                        riwayatList.add(riwayatAbsen)
                    }

                    mainHandler.post { callback.onSuccess(riwayatList) }
                } else {
                    mainHandler.post { callback.onError("Error: $responseCode") }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError("Error: ${e.message}") }
            }
        }
    }

    private fun executeGetRiwayatTaskFiltered(idPengguna: Int, callback: RiwayatCallback) {
        executor.execute {
            try {
                val filter = "id_pengguna=eq.$idPengguna&order=created_at.desc"
                val url = URL("${SUPABASE_URL}presensi?$filter")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val br = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) response.append(line)
                    br.close()

                    val jsonArray = JSONArray(response.toString())
                    val riwayatList = mutableListOf<RiwayatAbsen>()

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val riwayatAbsen = parseRiwayatFromJson(item)
                        riwayatList.add(riwayatAbsen)
                    }

                    mainHandler.post { callback.onSuccess(riwayatList) }
                } else {
                    mainHandler.post { callback.onError("Error: $responseCode") }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError("Error: ${e.message}") }
            }
        }
    }

    private fun parseRiwayatFromJson(item: JSONObject): RiwayatAbsen {
        val tanggal = item.optString("tanggal", "")
        val jamMasuk = if (item.has("jam_masuk") && !item.isNull("jam_masuk")) item.getString("jam_masuk") else "-"
        val jamPulang = if (item.has("jam_pulang") && !item.isNull("jam_pulang")) item.getString("jam_pulang") else "-"
        val status = item.optString("status", "Hadir")
        val keteranganIzin = if (item.has("keterangan_izin") && !item.isNull("keterangan_izin")) item.getString("keterangan_izin") else null
        val pesanOwner = if (item.has("pesan_owner") && !item.isNull("pesan_owner")) item.getString("pesan_owner") else null
        val dibuatPada = item.optString("created_at", "")

        return RiwayatAbsen(
            id = item.optString("id", ""),
            namaPengguna = "Kasir",
            role = "Karyawan",
            tanggal = formatTanggalDisplay(tanggal),
            jamMasuk = jamMasuk,
            jamPulang = jamPulang,
            status = status,
            keteranganIzin = keteranganIzin,
            pesanOwner = pesanOwner,
            dibuatPada = dibuatPada
        )
    }

    private fun formatTanggalDisplay(tanggal: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            outputFormat.format(inputFormat.parse(tanggal)!!)
        } catch (e: Exception) {
            tanggal
        }
    }
}