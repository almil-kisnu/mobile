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
import java.util.concurrent.Executors

class PenggunaApi {

    companion object {
        private const val SUPABASE_URL = "https://rujrwhtwkoferxhhnruq.supabase.co/rest/v1/"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ1anJ3aHR3a29mZXJ4aGhucnVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTkxNDYwODYsImV4cCI6MjA3NDcyMjA4Nn0.v7ALlCUbNBDy2TWQ0rhGc0tR0qgnR4J9ko8v-8Bwrww"

        private val executor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())
    }

    class Pengguna(
        val iduser: Int,
        val username: String,
        val phone: String,
        val role: String,
        val isActive: Boolean,
        val idoutlet: Int?,
        val nik: String
    )

    interface PenggunaCallback {
        fun onSuccess(pengguna: Pengguna)
        fun onError(error: String)
    }

    interface PenggunaListCallback {
        fun onSuccess(list: List<Pengguna>)
        fun onError(error: String)
    }

    interface SimpanCallback {
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    fun getById(iduser: Int, callback: PenggunaCallback) {
        executor.execute {
            try {
                val filter = "iduser=eq.$iduser&limit=1"
                val url = URL(SUPABASE_URL + "pengguna?$filter")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    val br = BufferedReader(InputStreamReader(conn.inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    val arr = JSONArray(sb.toString())
                    if (arr.length() == 0) {
                        postError(callback, "Data tidak ditemukan")
                    } else {
                        val o = arr.getJSONObject(0)
                        val p = mapPengguna(o)
                        mainHandler.post { callback.onSuccess(p) }
                    }
                } else {
                    val br = BufferedReader(InputStreamReader(conn.errorStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    postError(callback, "Error: $code - $sb")
                }
            } catch (e: Exception) {
                postError(callback, "Error: ${e.message}")
            }
        }
    }

    fun getByUsername(username: String, callback: PenggunaCallback) {
        executor.execute {
            try {
                val col = URLEncoder.encode("username", "UTF-8")
                val uname = username ?: ""
                val filter = "$col=eq.${URLEncoder.encode(uname.trim(), "UTF-8")}&limit=1"
                val fullUrl = SUPABASE_URL + "pengguna?$filter"
                println("[PenggunaApi] URL: $fullUrl")

                val url = URL(fullUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val code = conn.responseCode
                println("[PenggunaApi] code=$code")

                if (code == HttpURLConnection.HTTP_OK) {
                    val br = BufferedReader(InputStreamReader(conn.inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    println("[PenggunaApi] body=$sb")
                    val arr = JSONArray(sb.toString())
                    if (arr.length() == 0) {
                        postError(callback, "Data tidak ditemukan")
                    } else {
                        val o = arr.getJSONObject(0)
                        val p = mapPengguna(o)
                        mainHandler.post { callback.onSuccess(p) }
                    }
                } else {
                    val br = BufferedReader(InputStreamReader(conn.errorStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    println("[PenggunaApi] errorBody=$sb")
                    postError(callback, "Error: $code - $sb")
                }
            } catch (e: Exception) {
                postError(callback, "Error: ${e.message}")
            }
        }
    }

    fun getByNamaBelakang(namaBelakang: String, callback: PenggunaCallback) {
        getByUsername(namaBelakang, callback)
    }

    fun getByTelepon(telepon: String, callback: PenggunaCallback) {
        executor.execute {
            try {
                val filter = "phone=eq.${URLEncoder.encode(telepon ?: "", "UTF-8")}&limit=1"
                val url = URL(SUPABASE_URL + "pengguna?$filter")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    val br = BufferedReader(InputStreamReader(conn.inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    val arr = JSONArray(sb.toString())
                    if (arr.length() == 0) {
                        postError(callback, "Data tidak ditemukan")
                    } else {
                        val o = arr.getJSONObject(0)
                        val p = mapPengguna(o)
                        mainHandler.post { callback.onSuccess(p) }
                    }
                } else {
                    val br = BufferedReader(InputStreamReader(conn.errorStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    postError(callback, "Error: $code - $sb")
                }
            } catch (e: Exception) {
                postError(callback, "Error: ${e.message}")
            }
        }
    }

    fun listAll(callback: PenggunaListCallback) {
        executor.execute {
            try {
                val url = URL(SUPABASE_URL + "pengguna?order=iduser.asc")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    val br = BufferedReader(InputStreamReader(conn.inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    val arr = JSONArray(sb.toString())
                    val list = mutableListOf<Pengguna>()
                    for (i in 0 until arr.length()) {
                        list.add(mapPengguna(arr.getJSONObject(i)))
                    }
                    mainHandler.post { callback.onSuccess(list) }
                } else {
                    val br = BufferedReader(InputStreamReader(conn.errorStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    postError(callback, "Error: $code - $sb")
                }
            } catch (e: Exception) {
                postError(callback, "Error: ${e.message}")
            }
        }
    }

    fun createPengguna(pengguna: Pengguna, callback: SimpanCallback) {
        executor.execute {
            try {
                val url = URL(SUPABASE_URL + "pengguna")
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

                val json = JSONObject().apply {
                    put("iduser", pengguna.iduser)
                    if (pengguna.username != null) put("username", pengguna.username)
                    if (pengguna.phone != null) put("phone", pengguna.phone)
                    if (pengguna.role != null) put("role", pengguna.role)
                    put("is_active", pengguna.isActive)
                    if (pengguna.idoutlet != null) put("idoutlet", pengguna.idoutlet)
                    if (pengguna.nik != null) put("nik", pengguna.nik)
                }

                val os: OutputStream = conn.outputStream
                os.write(json.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_NO_CONTENT) {
                    mainHandler.post { callback.onSuccess("OK") }
                } else {
                    val br = BufferedReader(InputStreamReader(conn.errorStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    postError(callback, "Error: $code - $sb")
                }
            } catch (e: Exception) {
                postError(callback, "Error: ${e.message}")
            }
        }
    }

    private fun mapPengguna(o: JSONObject): Pengguna {
        val iduser = o.optInt("iduser", 0)
        val username = o.optString("username", "")
        val phone = o.optString("phone", "")
        val role = o.optString("role", "")
        val isActive = o.optBoolean("is_active", true)
        val idoutlet = if (o.has("idoutlet") && !o.isNull("idoutlet")) o.optInt("idoutlet") else null
        val nik = o.optString("nik", "")
        return Pengguna(iduser, username, phone, role, isActive, idoutlet, nik)
    }

    private fun postError(cb: PenggunaCallback, msg: String) {
        mainHandler.post { cb.onError(msg) }
    }

    private fun postError(cb: PenggunaListCallback, msg: String) {
        mainHandler.post { cb.onError(msg) }
    }

    private fun postError(cb: SimpanCallback, msg: String) {
        mainHandler.post { cb.onError(msg) }
    }
}