package com.almil.dessertcakekinian.database

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class JadwalMingguanApi {

    companion object {
        private const val SUPABASE_URL = "https://rujrwhtwkoferxhhnruq.supabase.co/rest/v1/"
        private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ1anJ3aHR3a29mZXJ4aGhucnVxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTkxNDYwODYsImV4cCI6MjA3NDcyMjA4Nn0.v7ALlCUbNBDy2TWQ0rhGc0tR0qgnR4J9ko8v-8Bwrww"

        private val executor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())
    }

    class JadwalMingguan(
        val id_siklus: Int,
        val id_pengguna: Int,
        val id_shift_senin: Int?,
        val id_shift_selasa: Int?,
        val id_shift_rabu: Int?,
        val id_shift_kamis: Int?,
        val id_shift_jumat: Int?,
        val id_shift_sabtu: Int?,
        val id_shift_minggu: Int?,
        val nama_pengguna: String = "",
        val shift_senin: String = "",
        val shift_selasa: String = "",
        val shift_rabu: String = "",
        val shift_kamis: String = "",
        val shift_jumat: String = "",
        val shift_sabtu: String = "",
        val shift_minggu: String = ""
    )

    interface JadwalListCallback {
        fun onSuccess(list: List<JadwalMingguan>)
        fun onError(error: String)
    }

    fun listAllWithDetails(callback: JadwalListCallback) {
        executor.execute {
            try {
                // Ambil semua data jadwal mingguan
                val urlJadwal = URL("${SUPABASE_URL}jadwal_mingguan?order=id_siklus.asc")
                val connJadwal = urlJadwal.openConnection() as HttpURLConnection
                connJadwal.apply {
                    requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val codeJadwal = connJadwal.responseCode
                if (codeJadwal == HttpURLConnection.HTTP_OK) {
                    val br = BufferedReader(InputStreamReader(connJadwal.inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) sb.append(line)
                    br.close()

                    val arrJadwal = JSONArray(sb.toString())
                    val listJadwal = mutableListOf<JadwalMingguan>()

                    // Ambil semua data pengguna untuk mapping
                    val listPengguna = getAllPengguna()
                    // Ambil semua data shift untuk mapping
                    val listShift = getAllShift()

                    for (i in 0 until arrJadwal.length()) {
                        val jadwalObj = arrJadwal.getJSONObject(i)
                        val jadwal = mapJadwalMingguan(jadwalObj)

                        // Cari nama pengguna berdasarkan id_pengguna
                        val pengguna = listPengguna.find { it.iduser == jadwal.id_pengguna }
                        val namaPengguna = pengguna?.username ?: "User ${jadwal.id_pengguna}"

                        // Map shift IDs ke nama shift
                        val shiftSenin = getNamaShiftById(jadwal.id_shift_senin, listShift)
                        val shiftSelasa = getNamaShiftById(jadwal.id_shift_selasa, listShift)
                        val shiftRabu = getNamaShiftById(jadwal.id_shift_rabu, listShift)
                        val shiftKamis = getNamaShiftById(jadwal.id_shift_kamis, listShift)
                        val shiftJumat = getNamaShiftById(jadwal.id_shift_jumat, listShift)
                        val shiftSabtu = getNamaShiftById(jadwal.id_shift_sabtu, listShift)
                        val shiftMinggu = getNamaShiftById(jadwal.id_shift_minggu, listShift)

                        val jadwalDetail = JadwalMingguan(
                            id_siklus = jadwal.id_siklus,
                            id_pengguna = jadwal.id_pengguna,
                            id_shift_senin = jadwal.id_shift_senin,
                            id_shift_selasa = jadwal.id_shift_selasa,
                            id_shift_rabu = jadwal.id_shift_rabu,
                            id_shift_kamis = jadwal.id_shift_kamis,
                            id_shift_jumat = jadwal.id_shift_jumat,
                            id_shift_sabtu = jadwal.id_shift_sabtu,
                            id_shift_minggu = jadwal.id_shift_minggu,
                            nama_pengguna = namaPengguna,
                            shift_senin = shiftSenin,
                            shift_selasa = shiftSelasa,
                            shift_rabu = shiftRabu,
                            shift_kamis = shiftKamis,
                            shift_jumat = shiftJumat,
                            shift_sabtu = shiftSabtu,
                            shift_minggu = shiftMinggu
                        )

                        listJadwal.add(jadwalDetail)
                    }

                    mainHandler.post { callback.onSuccess(listJadwal) }
                } else {
                    postError(callback, "Error: $codeJadwal")
                }
            } catch (e: Exception) {
                postError(callback, "Error: ${e.message}")
            }
        }
    }

    private fun getNamaShiftById(idShift: Int?, listShift: List<ShiftDefinitionApi.ShiftDefinition>): String {
        if (idShift == null) return "Libur"
        val shift = listShift.find { it.id_shift_def == idShift }
        return shift?.nama_shift ?: "Shift $idShift"
    }

    private fun getAllPengguna(): List<PenggunaApi.Pengguna> {
        return try {
            val url = URL("${SUPABASE_URL}pengguna")
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val br = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) sb.append(line)
                br.close()

                val arr = JSONArray(sb.toString())
                val list = mutableListOf<PenggunaApi.Pengguna>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(mapPengguna(o))
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getAllShift(): List<ShiftDefinitionApi.ShiftDefinition> {
        return try {
            val url = URL("${SUPABASE_URL}shift")
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val br = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) sb.append(line)
                br.close()

                val arr = JSONArray(sb.toString())
                val list = mutableListOf<ShiftDefinitionApi.ShiftDefinition>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(ShiftDefinitionApi().mapShift(o))
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapPengguna(o: JSONObject): PenggunaApi.Pengguna {
        val iduser = o.optInt("iduser", 0)
        val username = o.optString("username", "")
        val phone = o.optString("phone", "")
        val role = o.optString("role", "")
        val isActive = o.optBoolean("is_active", true)
        val idoutlet = if (o.has("idoutlet") && !o.isNull("idoutlet")) o.optInt("idoutlet") else null
        val nik = o.optString("nik", "")
        return PenggunaApi.Pengguna(iduser, username, phone, role, isActive, idoutlet, nik)
    }

    private fun mapJadwalMingguan(o: JSONObject): JadwalMingguan {
        return JadwalMingguan(
            id_siklus = o.optInt("id_siklus", 0),
            id_pengguna = o.optInt("id_pengguna", 0),
            id_shift_senin = if (o.has("id_shift_senin") && !o.isNull("id_shift_senin")) o.optInt("id_shift_senin") else null,
            id_shift_selasa = if (o.has("id_shift_selasa") && !o.isNull("id_shift_selasa")) o.optInt("id_shift_selasa") else null,
            id_shift_rabu = if (o.has("id_shift_rabu") && !o.isNull("id_shift_rabu")) o.optInt("id_shift_rabu") else null,
            id_shift_kamis = if (o.has("id_shift_kamis") && !o.isNull("id_shift_kamis")) o.optInt("id_shift_kamis") else null,
            id_shift_jumat = if (o.has("id_shift_jumat") && !o.isNull("id_shift_jumat")) o.optInt("id_shift_jumat") else null,
            id_shift_sabtu = if (o.has("id_shift_sabtu") && !o.isNull("id_shift_sabtu")) o.optInt("id_shift_sabtu") else null,
            id_shift_minggu = if (o.has("id_shift_minggu") && !o.isNull("id_shift_minggu")) o.optInt("id_shift_minggu") else null
        )
    }

    private fun postError(cb: JadwalListCallback, msg: String) {
        mainHandler.post { cb.onError(msg) }
    }
}