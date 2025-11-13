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
import java.util.concurrent.Executors

class ShiftDefinitionApi {

    companion object {
        private const val SUPABASE_URL = "https://rujrwhtwkoferxhhnruq.supabase.co/rest/v1/"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ1anJ3aHR3a29mZXJ4aGhucnVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTkxNDYwODYsImV4cCI6MjA3NDcyMjA4Nn0.v7ALlCUbNBDy2TWQ0rhGc0tR0qgnR4J9ko8v-8Bwrww"

        private val executor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())
    }

    class ShiftDefinition(
        val id_shift_def: Int,
        val nama_shift: String,
        val jam_mulai: String,
        val jam_selesai: String
    )

    interface ShiftCallback {
        fun onSuccess(shift: ShiftDefinition)
        fun onError(error: String)
    }

    interface ShiftListCallback {
        fun onSuccess(list: List<ShiftDefinition>)
        fun onError(error: String)
    }

    interface CreateCallback {
        fun onSuccess(created: ShiftDefinition)
        fun onError(error: String)
    }

    fun getById(id: Int, callback: ShiftCallback) {
        executor.execute {
            try {
                val url = URL("${SUPABASE_URL}shift?id_shift_def=eq.$id&limit=1")
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
                    if (arr.length() == 0) postError(callback, "Data tidak ditemukan")
                    else {
                        val o = arr.getJSONObject(0)
                        val s = mapShift(o)
                        mainHandler.post { callback.onSuccess(s) }
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

    fun listAll(callback: ShiftListCallback) {
        executor.execute {
            try {
                val url = URL("${SUPABASE_URL}shift?order=id_shift_def.asc")
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
                    val list = mutableListOf<ShiftDefinition>()
                    for (i in 0 until arr.length()) {
                        list.add(mapShift(arr.getJSONObject(i)))
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

    fun create(nameShift: String, jsmMulai: String, jsmSalesai: String, callback: CreateCallback) {
        executor.execute {
            try {
                val url = URL("${SUPABASE_URL}shift")
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Prefer", "return=representation")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val json = JSONObject().apply {
                    put("nama_shift", nameShift)
                    put("jam_mulai", jsmMulai)
                    put("jam_selesai", jsmSalesai)
                }

                val os: OutputStream = conn.outputStream
                os.write(json.toString().toByteArray(Charsets.UTF_8))
                os.flush()
                os.close()

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
                    val br = BufferedReader(InputStreamReader(conn.inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()
                    val arr = JSONArray(sb.toString())
                    val o = arr.getJSONObject(0)
                    val s = mapShift(o)
                    mainHandler.post { callback.onSuccess(s) }
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

    private fun mapShift(o: JSONObject): ShiftDefinition {
        val id = o.optInt("id_shift_def", 0)
        val nama = o.optString("nama_shift", "")
        val mulai = o.optString("jam_mulai", "")
        val selesai = o.optString("jam_selesai", "")
        return ShiftDefinition(id, nama, mulai, selesai)
    }

    private fun postError(cb: ShiftCallback, msg: String) {
        mainHandler.post { cb.onError(msg) }
    }

    private fun postError(cb: ShiftListCallback, msg: String) {
        mainHandler.post { cb.onError(msg) }
    }

    private fun postError(cb: CreateCallback, msg: String) {
        mainHandler.post { cb.onError(msg) }
    }
}