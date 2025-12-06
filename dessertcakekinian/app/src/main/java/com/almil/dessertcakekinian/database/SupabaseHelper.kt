package com.almil.dessertcakekinian.database

import android.os.Handler
import android.os.Looper
import com.almil.dessertcakekinian.model.RiwayatAbsen
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class SupabaseHelper {

    companion object {
        private const val SUPABASE_URL = "https://rujrwhtwkoferxhhnruq.supabase.co/rest/v1/"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ1anJ3aHR3a29mZXJ4aGhucnVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTkxNDYwODYsImV4cCI6MjA3NDcyMjA4Nn0.v7ALlCUbNBDy2TWQ0rhGc0tR0qgnR4J9ko8v-8Bwrww"

        private val executor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())

        private const val STATUS_HADIR = "Hadir"
        private const val STATUS_IZIN = "Izin"
        private const val STATUS_TERLAMBAT = "Terlambat"
        private const val STATUS_ALPHA = "Alpha"

        private fun getCurrentTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }

        private fun isTelat(jamMasuk: String, jamMulaiShift: String): Boolean {
            return try {
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                val masukTime = format.parse(jamMasuk)
                val shiftTime = format.parse(jamMulaiShift)

                val selisih = masukTime.time - shiftTime.time
                val selisihMenit = selisih / (60 * 1000)

                selisihMenit > 5
            } catch (e: Exception) {
                println("❌ Error parsing waktu: ${e.message}")
                false
            }
        }

        private fun getJamMulaiShift(shift: String): String {
            return when(shift.uppercase()) {
                "PAGI" -> "08:00"
                "SIANG" -> "13:00"
                "MALAM" -> "20:00"
                else -> "08:00"
            }
        }
    }

    interface SimpleCallback {
        fun onSuccess()
        fun onError(error: String)
    }

    interface RiwayatCallback {
        fun onSuccess(riwayatList: List<RiwayatAbsen>)
        fun onError(error: String)
    }

    interface SimpleRiwayatCallback {
        fun onSuccess(riwayatList: List<SimpleRiwayatData>)
        fun onError(error: String)
    }

    interface PenggunaMapCallback {
        fun onSuccess(userMap: Map<Int, String>)
        fun onError(error: String)
    }

    data class SimpleRiwayatData(
        val tanggal: String,
        val jamMasuk: String,
        val jamPulang: String,
        val status: String,
        val lokasi: String = "Database",
        val shift: String = "",
        val idPengguna: Int = 0
    )

    interface SimpanCallback {
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    // ==================== FUNGSI UTAMA ====================

    fun simpanMasuk(tanggal: String, jamMasuk: String, lokasiMasuk: String, username: String, shift: String, callback: SimpanCallback) {
        getUserIdFromUsername(username) { correctId ->
            executor.execute {
                try {
                    println("📝 SIMPAN MASUK: tanggal=$tanggal, jam=$jamMasuk, user=$username, id=$correctId, shift=$shift")

                    val jamMulaiShift = getJamMulaiShift(shift)
                    val status = if (isTelat(jamMasuk, jamMulaiShift)) STATUS_TERLAMBAT else STATUS_HADIR
                    println("🔍 Status otomatis: $status (Jam Masuk: $jamMasuk, Mulai Shift: $jamMulaiShift)")

                    val url = URL("${SUPABASE_URL}presensi")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("apikey", SUPABASE_KEY)
                        setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                        setRequestProperty("Prefer", "return=representation")
                        connectTimeout = 15000
                        readTimeout = 15000
                    }

                    val jsonData = JSONObject().apply {
                        put("tanggal", tanggal)
                        put("jam_masuk", jamMasuk)
                        put("jam_pulang", JSONObject.NULL)
                        put("status", status)
                        put("keterangan_izin", "")
                        put("pesan_owner", "")
                        put("created_at", getCurrentTimestamp())
                        put("id_pengguna", correctId)
                        put("shift", shift)
                    }

                    println("📄 Data yang dikirim: $jsonData")

                    val os: OutputStream = conn.outputStream
                    os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                    os.close()

                    val responseCode = conn.responseCode
                    println("📨 Response Code: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                        val br = BufferedReader(InputStreamReader(conn.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (br.readLine().also { line = it } != null) response.append(line)
                        br.close()

                        println("✅ RESPONSE SUCCESS: $response")
                        mainHandler.post { callback.onSuccess("✅ Absen masuk berhasil (Shift: $shift, Status: $status)") }
                    } else {
                        val errorStream = if (conn.errorStream != null) {
                            val br = BufferedReader(InputStreamReader(conn.errorStream))
                            val response = StringBuilder()
                            var line: String?
                            while (br.readLine().also { line = it } != null) response.append(line)
                            br.close()
                            response.toString()
                        } else {
                            "No error stream"
                        }

                        println("❌ ERROR Response: $responseCode - $errorStream")

                        if (responseCode == 409 && errorStream.contains("foreign key constraint")) {
                            mainHandler.post {
                                callback.onError("❌ Error: User tidak terdaftar di sistem. Hubungi admin.")
                            }
                        } else {
                            mainHandler.post {
                                callback.onError("❌ Gagal simpan masuk: $responseCode - $errorStream")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ EXCEPTION: ${e.message}")
                    mainHandler.post { callback.onError("❌ Error: ${e.message}") }
                }
            }
        }
    }

    fun simpanPulang(tanggal: String, jamPulang: String, lokasiPulang: String, username: String, shift: String, callback: SimpanCallback) {
        getUserIdFromUsername(username) { correctId ->
            executor.execute {
                try {
                    println("📝 SIMPAN PULANG: tanggal=$tanggal, jam=$jamPulang, user=$username, id=$correctId, shift=$shift")

                    val filter = "tanggal=eq.${URLEncoder.encode(tanggal, "UTF-8")}" +
                            "&id_pengguna=eq.$correctId" +
                            "&shift=eq.${URLEncoder.encode(shift, "UTF-8")}" +
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
                        setRequestProperty("Prefer", "return=representation")
                        connectTimeout = 15000
                        readTimeout = 15000
                    }

                    val jsonData = JSONObject().apply {
                        put("jam_pulang", jamPulang)
                    }

                    println("📄 Data pulang yang dikirim: $jsonData")

                    val os: OutputStream = conn.outputStream
                    os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                    os.close()

                    val responseCode = conn.responseCode
                    println("📨 Response Code Pulang: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                        mainHandler.post { callback.onSuccess("✅ Absen pulang berhasil disimpan (Shift: $shift)") }
                    } else {
                        val errorStream = if (conn.errorStream != null) {
                            val br = BufferedReader(InputStreamReader(conn.errorStream))
                            val response = StringBuilder()
                            var line: String?
                            while (br.readLine().also { line = it } != null) response.append(line)
                            br.close()
                            response.toString()
                        } else {
                            "No error stream"
                        }

                        println("❌ ERROR Response Pulang: $responseCode - $errorStream")
                        cariDanUpdatePulang(tanggal, correctId, jamPulang, shift, callback)
                    }
                } catch (e: Exception) {
                    println("❌ EXCEPTION Pulang: ${e.message}")
                    mainHandler.post { callback.onError("❌ Error: ${e.message}") }
                }
            }
        }
    }

    private fun cariDanUpdatePulang(tanggal: String, idPengguna: Int, jamPulang: String, shift: String, callback: SimpanCallback) {
        try {
            println("🔍 CARI DATA UNTUK PULANG: tanggal=$tanggal, id=$idPengguna, shift=$shift")

            val filter = "tanggal=eq.${URLEncoder.encode(tanggal, "UTF-8")}" +
                    "&id_pengguna=eq.$idPengguna" +
                    "&shift=eq.${URLEncoder.encode(shift, "UTF-8")}"

            val url = URL("${SUPABASE_URL}presensi?$filter&order=created_at.desc")
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
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
                println("👀 Data ditemukan: ${jsonArray.length()} records")

                if (jsonArray.length() > 0) {
                    val latestRecord = jsonArray.getJSONObject(0)
                    val recordId = latestRecord.getString("id")
                    updatePulangById(recordId, jamPulang, callback)
                } else {
                    mainHandler.post { callback.onError("❌ Tidak ada data absen masuk untuk shift $shift") }
                }
            } else {
                mainHandler.post { callback.onError("❌ Gagal mencari data: $responseCode") }
            }
        } catch (e: Exception) {
            mainHandler.post { callback.onError("❌ Error cari data: ${e.message}") }
        }
    }

    private fun updatePulangById(recordId: String, jamPulang: String, callback: SimpanCallback) {
        try {
            println("🔄 UPDATE PULANG BY ID: $recordId")

            val url = URL("${SUPABASE_URL}presensi?id=eq.$recordId")
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "PATCH"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                setRequestProperty("Prefer", "return=minimal")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val jsonData = JSONObject().apply {
                put("jam_pulang", jamPulang)
            }

            val os: OutputStream = conn.outputStream
            os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
            os.flush()
            os.close()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                mainHandler.post { callback.onSuccess("✅ Absen pulang berhasil (alternatif)") }
            } else {
                mainHandler.post { callback.onError("❌ Gagal update pulang: $responseCode") }
            }
        } catch (e: Exception) {
            mainHandler.post { callback.onError("❌ Error update by ID: ${e.message}") }
        }
    }

    fun updatePulangManual(username: String, tanggal: String, shift: String, jamPulang: String, callback: SimpanCallback) {
        getUserIdFromUsername(username) { correctId ->
            executor.execute {
                try {
                    println("🔄 UPDATE PULANG MANUAL: user=$username, tanggal=$tanggal, shift=$shift, jam=$jamPulang")

                    val filter = "tanggal=eq.${URLEncoder.encode(tanggal, "UTF-8")}" +
                            "&id_pengguna=eq.$correctId" +
                            "&shift=eq.${URLEncoder.encode(shift, "UTF-8")}" +
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
                        setRequestProperty("Prefer", "return=minimal")
                        connectTimeout = 10000
                        readTimeout = 10000
                    }

                    val jsonData = JSONObject().apply {
                        put("jam_pulang", "$jamPulang (Auto)")
                        put("pesan_owner", "Auto checkout system - Karyawan lupa absen pulang")
                    }

                    println("📄 Data auto checkout yang dikirim: $jsonData")

                    val os: OutputStream = conn.outputStream
                    os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                    os.close()

                    val responseCode = conn.responseCode
                    println("📨 Response Code Auto Checkout: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
                        mainHandler.post { callback.onSuccess("✅ Auto checkout berhasil: $jamPulang (Sistem)") }
                    } else {
                        val errorStream = if (conn.errorStream != null) {
                            val br = BufferedReader(InputStreamReader(conn.errorStream))
                            val response = StringBuilder()
                            var line: String?
                            while (br.readLine().also { line = it } != null) response.append(line)
                            br.close()
                            response.toString()
                        } else {
                            "No error stream"
                        }

                        println("❌ ERROR Auto Checkout: $responseCode - $errorStream")
                        mainHandler.post { callback.onError("❌ Gagal auto checkout: $responseCode") }
                    }

                } catch (e: Exception) {
                    println("❌ EXCEPTION Auto Checkout: ${e.message}")
                    mainHandler.post { callback.onError("❌ Error auto checkout: ${e.message}") }
                }
            }
        }
    }

    fun insertAlphaRecord(alphaData: Map<String, Any>, callback: SimpleCallback) {
        executor.execute {
            try {
                println("📝 INSERT ALPHA RECORD: $alphaData")

                val url = URL("${SUPABASE_URL}presensi")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Prefer", "return=minimal")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val jsonData = JSONObject(alphaData).toString()
                println("📄 Data alpha yang dikirim: $jsonData")

                val os: OutputStream = conn.outputStream
                os.write(jsonData.toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val responseCode = conn.responseCode
                println("📨 Response Code Alpha: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    println("✅ Alpha record berhasil disimpan")
                    mainHandler.post { callback.onSuccess() }
                } else {
                    val errorStream = if (conn.errorStream != null) {
                        val br = BufferedReader(InputStreamReader(conn.errorStream))
                        val response = StringBuilder()
                        var line: String?
                        while (br.readLine().also { line = it } != null) response.append(line)
                        br.close()
                        response.toString()
                    } else {
                        "No error stream"
                    }

                    println("❌ ERROR Insert Alpha: $responseCode - $errorStream")
                    mainHandler.post { callback.onError("Gagal simpan alpha: $responseCode") }
                }
            } catch (e: Exception) {
                println("❌ EXCEPTION Insert Alpha: ${e.message}")
                mainHandler.post { callback.onError("Error: ${e.message}") }
            }
        }
    }

    fun updateStatusToTerlambat(userId: Int, tanggal: String, shift: String, callback: SimpleCallback) {
        executor.execute {
            try {
                println("🔄 UPDATE STATUS KE TERLAMBAT: user=$userId, tanggal=$tanggal, shift=$shift")

                val filter = "tanggal=eq.${URLEncoder.encode(tanggal, "UTF-8")}" +
                        "&id_pengguna=eq.$userId" +
                        "&shift=eq.${URLEncoder.encode(shift, "UTF-8")}"

                val url = URL("${SUPABASE_URL}presensi?$filter")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "PATCH"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Prefer", "return=minimal")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val jsonData = JSONObject().apply {
                    put("status", STATUS_TERLAMBAT)
                }

                println("📄 Update status ke Terlambat: $jsonData")

                val os: OutputStream = conn.outputStream
                os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val responseCode = conn.responseCode
                println("📨 Response Code Update Terlambat: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
                    println("✅ Status updated to Terlambat in database")
                    mainHandler.post { callback.onSuccess() }
                } else {
                    val errorStream = if (conn.errorStream != null) {
                        val br = BufferedReader(InputStreamReader(conn.errorStream))
                        val response = StringBuilder()
                        var line: String?
                        while (br.readLine().also { line = it } != null) response.append(line)
                        br.close()
                        response.toString()
                    } else {
                        "No error stream"
                    }
                    println("❌ ERROR Update Terlambat: $responseCode - $errorStream")
                    mainHandler.post { callback.onError("Gagal update status terlambat: $responseCode") }
                }
            } catch (e: Exception) {
                println("❌ EXCEPTION Update Terlambat: ${e.message}")
                mainHandler.post { callback.onError("Error: ${e.message}") }
            }
        }
    }

    fun updateStatusToIzin(username: String, tanggal: String, shift: String, keterangan: String, callback: SimpanCallback) {
        getUserIdFromUsername(username) { userId ->
            executor.execute {
                try {
                    println("🔄 UPDATE STATUS TO IZIN: user=$userId, tanggal=$tanggal, shift=$shift, keterangan=$keterangan")

                    // Get current time as jam_pulang
                    val jamPulang = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                    val filter = "tanggal=eq.${URLEncoder.encode(tanggal, "UTF-8")}" +
                            "&id_pengguna=eq.$userId" +
                            "&shift=eq.${URLEncoder.encode(shift, "UTF-8")}" +
                            "&jam_pulang=is.null"

                    val url = URL("${SUPABASE_URL}presensi?$filter")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.apply {
                        requestMethod = "PATCH"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("apikey", SUPABASE_KEY)
                        setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                        setRequestProperty("Prefer", "return=minimal")
                        connectTimeout = 10000
                        readTimeout = 10000
                    }

                    val jsonData = JSONObject().apply {
                        put("status", STATUS_IZIN)
                        put("keterangan_izin", keterangan)
                        put("jam_pulang", jamPulang)
                    }

                    println("📄 Update status to Izin: $jsonData")

                    val os: OutputStream = conn.outputStream
                    os.write(jsonData.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                    os.close()

                    val responseCode = conn.responseCode
                    println("📨 Response Code Update Izin: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
                        println("✅ Status updated to Izin in database")
                        mainHandler.post { callback.onSuccess("✅ Izin berhasil dikirim") }
                    } else {
                        val errorStream = if (conn.errorStream != null) {
                            val br = BufferedReader(InputStreamReader(conn.errorStream))
                            val response = StringBuilder()
                            var line: String?
                            while (br.readLine().also { line = it } != null) response.append(line)
                            br.close()
                            response.toString()
                        } else {
                            "No error stream"
                        }
                        println("❌ ERROR Update Izin: $responseCode - $errorStream")
                        mainHandler.post { callback.onError("Gagal update status izin: $responseCode") }
                    }
                } catch (e: Exception) {
                    println("❌ EXCEPTION Update Izin: ${e.message}")
                    mainHandler.post { callback.onError("Error: ${e.message}") }
                }
            }
        }
    }

    fun checkAbsenStatusWithShift(username: String, tanggal: String, shift: String, callback: (sudahAbsen: Boolean, sudahCheckout: Boolean, status: String) -> Unit) {
        getUserIdFromUsername(username) { correctId ->
            executor.execute {
                try {
                    val filter = "tanggal=eq.${URLEncoder.encode(tanggal, "UTF-8")}" +
                            "&id_pengguna=eq.$correctId" +
                            "&shift=eq.${URLEncoder.encode(shift, "UTF-8")}"

                    val url = URL("${SUPABASE_URL}presensi?$filter&order=created_at.desc")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.apply {
                        requestMethod = "GET"
                        setRequestProperty("apikey", SUPABASE_KEY)
                        setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
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
                        val sudahAbsen = jsonArray.length() > 0
                        var sudahCheckout = false
                        var status = ""

                        if (sudahAbsen) {
                            val item = jsonArray.getJSONObject(0)
                            sudahCheckout = item.has("jam_pulang") && !item.isNull("jam_pulang")
                            status = item.optString("status", STATUS_HADIR)
                            println("🔍 Check status shift $shift: sudahAbsen=$sudahAbsen, sudahCheckout=$sudahCheckout, status=$status")
                        }

                        mainHandler.post { callback(sudahAbsen, sudahCheckout, status) }
                    } else {
                        println("🔍 Check status shift $shift: TIDAK ADA DATA (belum absen)")
                        mainHandler.post { callback(false, false, "") }
                    }

                } catch (e: Exception) {
                    println("❌ Error check status shift: ${e.message}")
                    mainHandler.post { callback(false, false, "") }
                }
            }
        }
    }

    fun getAbsenDetail(username: String, tanggal: String, shift: String, callback: (jamMasuk: String, jamPulang: String, status: String) -> Unit) {
        getUserIdFromUsername(username) { correctId ->
            executor.execute {
                try {
                    println("🔍 GET ABSEN DETAIL: user=$username, tanggal=$tanggal, shift=$shift, id=$correctId")

                    val filter = "tanggal=eq.${URLEncoder.encode(tanggal, "UTF-8")}" +
                            "&id_pengguna=eq.$correctId" +
                            "&shift=eq.${URLEncoder.encode(shift, "UTF-8")}" +
                            "&order=created_at.desc" +
                            "&limit=1"

                    val url = URL("${SUPABASE_URL}presensi?$filter")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.apply {
                        requestMethod = "GET"
                        setRequestProperty("apikey", SUPABASE_KEY)
                        setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
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
                        println("🔍 Data detail ditemukan: ${jsonArray.length()} records")

                        var jamMasuk = ""
                        var jamPulang = ""
                        var status = ""

                        if (jsonArray.length() > 0) {
                            val item = jsonArray.getJSONObject(0)
                            jamMasuk = if (item.has("jam_masuk") && !item.isNull("jam_masuk")) {
                                formatJam(item.getString("jam_masuk"))
                            } else {
                                ""
                            }
                            jamPulang = if (item.has("jam_pulang") && !item.isNull("jam_pulang")) {
                                formatJam(item.getString("jam_pulang"))
                            } else {
                                ""
                            }
                            status = if (item.has("status") && !item.isNull("status")) {
                                item.getString("status")
                            } else {
                                ""
                            }
                            println("✅ Detail absen: jamMasuk=$jamMasuk, jamPulang=$jamPulang, status=$status")
                        } else {
                            println("❌ Tidak ada data absen untuk tanggal $tanggal")
                        }

                        mainHandler.post { callback(jamMasuk, jamPulang, status) }
                    } else {
                        println("❌ Gagal ambil detail absen: $responseCode")
                        mainHandler.post { callback("", "", "") }
                    }

                } catch (e: Exception) {
                    println("❌ Exception ambil detail absen: ${e.message}")
                    mainHandler.post { callback("", "", "") }
                }
            }
        }
    }

    private fun getUserIdFromUsername(username: String, callback: (Int) -> Unit) {
        executor.execute {
            try {
                val encodedUsername = URLEncoder.encode(username, "UTF-8")
                val url = URL("${SUPABASE_URL}pengguna?username=eq.$encodedUsername&select=iduser")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
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
                    if (jsonArray.length() > 0) {
                        val userObj = jsonArray.getJSONObject(0)
                        val iduser = userObj.getInt("iduser")
                        println("✅ User ID ditemukan: $iduser untuk username: $username")
                        mainHandler.post { callback(iduser) }
                    } else {
                        println("❌ User tidak ditemukan: $username")
                        val fallbackId = Math.abs(username.hashCode()) % 1000000
                        println("⚠️ Menggunakan fallback ID: $fallbackId")
                        mainHandler.post { callback(fallbackId) }
                    }
                } else {
                    println("❌ Error get user ID: $responseCode")
                    val fallbackId = Math.abs(username.hashCode()) % 1000000
                    println("⚠️ Menggunakan fallback ID: $fallbackId")
                    mainHandler.post { callback(fallbackId) }
                }
            } catch (e: Exception) {
                println("❌ Exception get user ID: ${e.message}")
                val fallbackId = Math.abs(username.hashCode()) % 1000000
                println("⚠️ Menggunakan fallback ID: $fallbackId")
                mainHandler.post { callback(fallbackId) }
            }
        }
    }

    fun getRiwayatByUsername(username: String, callback: SimpleRiwayatCallback) {
        getUserIdFromUsername(username) { correctId ->
            executor.execute {
                var conn: HttpURLConnection? = null
                try {
                    val filter = "id_pengguna=eq.$correctId&order=tanggal.desc,created_at.desc&limit=200"
                    val url = URL("${SUPABASE_URL}presensi?$filter")

                    println("🔍 GET RIWAYAT HANYA UNTUK USER: $username (ID: $correctId)")
                    println("🔗 URL: $url")

                    conn = url.openConnection() as HttpURLConnection
                    conn.apply {
                        requestMethod = "GET"
                        setRequestProperty("apikey", SUPABASE_KEY)
                        setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Prefer", "return=representation")
                        connectTimeout = 15000
                        readTimeout = 15000
                        useCaches = false
                    }

                    val responseCode = conn.responseCode
                    println("📨 Response Code: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val br = BufferedReader(InputStreamReader(conn.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (br.readLine().also { line = it } != null) response.append(line)
                        br.close()

                        val jsonArray = JSONArray(response.toString())
                        val riwayatList = mutableListOf<SimpleRiwayatData>()

                        println("✅ Data berhasil diambil: ${jsonArray.length()} records HANYA untuk user $username")

                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)

                            val tanggal = item.optString("tanggal", "")
                            val jamMasuk = formatJam(item.optString("jam_masuk", "-"))
                            val jamPulang = formatJam(item.optString("jam_pulang", "-"))
                            val status = item.optString("status", STATUS_HADIR)
                            val shift = item.optString("shift", "Pagi")
                            val idPengguna = item.optInt("id_pengguna", 0)

                            val simpleRiwayat = SimpleRiwayatData(
                                tanggal = tanggal,
                                jamMasuk = jamMasuk,
                                jamPulang = jamPulang,
                                status = status,
                                shift = shift,
                                idPengguna = idPengguna
                            )
                            riwayatList.add(simpleRiwayat)
                        }

                        println("✅ Data HANYA user $username: ${riwayatList.size} records")
                        mainHandler.post { callback.onSuccess(riwayatList) }
                    } else {
                        val errorMsg = "HTTP Error: $responseCode - ${conn.responseMessage}"
                        println("❌ $errorMsg")
                        mainHandler.post { callback.onError(errorMsg) }
                    }
                } catch (e: SocketException) {
                    println("❌ SOCKET ERROR: ${e.message}")
                    mainHandler.post { callback.onError("Koneksi internet terputus: ${e.message}") }
                } catch (e: SocketTimeoutException) {
                    println("❌ TIMEOUT ERROR: ${e.message}")
                    mainHandler.post { callback.onError("Timeout koneksi ke server: ${e.message}") }
                } catch (e: Exception) {
                    println("❌ GENERAL ERROR: ${e.message}")
                    mainHandler.post { callback.onError("Error: ${e.message}") }
                } finally {
                    conn?.disconnect()
                    println("🔌 Koneksi ditutup")
                }
            }
        }
    }

    fun checkAndAutoCompleteYesterday(username: String, userShift: String, callback: (success: Boolean, message: String) -> Unit) {
        executor.execute {
            try {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DATE, -1)
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                println("🔍 CHECK AUTO COMPLETE YESTERDAY: user=$username, tanggal=$yesterday, shift=$userShift")

                checkAbsenStatusWithShift(username, yesterday, userShift) { sudahAbsen, sudahCheckout, status ->
                    if (sudahAbsen && !sudahCheckout) {
                        println("🔍 Ditemukan data kemarin yang lupa absen pulang, melakukan auto checkout...")

                        val jamPulang = when(userShift.uppercase()) {
                            "PAGI" -> "12:00"
                            "SIANG" -> "17:00"
                            "MALAM" -> "22:00"
                            else -> "23:59"
                        }

                        updatePulangManual(username, yesterday, userShift, jamPulang,
                            object : SimpanCallback {
                                override fun onSuccess(message: String) {
                                    println("✅ Auto checkout berhasil: $message")
                                    mainHandler.post { callback(true, "Auto checkout berhasil: $jamPulang (Status: $status)") }
                                }
                                override fun onError(error: String) {
                                    println("❌ Auto checkout gagal: $error")
                                    mainHandler.post { callback(false, "Auto checkout gagal: $error") }
                                }
                            })
                    } else {
                        val message = if (!sudahAbsen) {
                            "Tidak ada data absen kemarin"
                        } else {
                            "Data kemarin sudah lengkap (Status: $status)"
                        }
                        println("🔍 $message")
                        mainHandler.post { callback(true, message) }
                    }
                }
            } catch (e: Exception) {
                println("❌ EXCEPTION Check Auto Complete: ${e.message}")
                mainHandler.post { callback(false, "Error: ${e.message}") }
            }
        }
    }

    private fun formatJam(jam: String): String {
        return try {
            if (jam == "null" || jam.isEmpty() || jam == "-") {
                "-"
            } else {
                if (jam.length >= 5) {
                    jam.substring(0, 5)
                } else {
                    jam
                }
            }
        } catch (e: Exception) {
            "-"
        }
    }

    private fun parseRiwayatFromJson(item: JSONObject): RiwayatAbsen {
        throw NotImplementedError("Implementasi parseRiwayatFromJson memerlukan definisi kelas RiwayatAbsen")
    }

    private fun formatTanggalDisplay(tanggal: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            outputFormat.format(inputFormat.parse(tanggal) ?: Date())
        } catch (e: Exception) {
            tanggal
        }
    }
}