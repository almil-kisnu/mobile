package com.almil.dessertcakekinian.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.almil.dessertcakekinian.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class dialog_ajukan_izin : DialogFragment() {

    private lateinit var btnClose: ImageButton
    private lateinit var btnKirim: MaterialButton
    private lateinit var etNamaKaryawan: TextInputEditText
    private lateinit var etAlasanIzin: TextInputEditText

    private lateinit var layoutAlasanIzin: TextInputLayout

    private var userShift: String = "Pagi"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_ajukan_izin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        initViews(view)
        setupAutoFillData()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        btnClose = view.findViewById(R.id.btnClose)
        btnKirim = view.findViewById(R.id.btnKirimPermintaan)
        etNamaKaryawan = view.findViewById(R.id.etNamaKaryawan)
        etAlasanIzin = view.findViewById(R.id.etAlasanIzin)

        layoutAlasanIzin = view.findViewById(R.id.layoutAlasanIzin)
    }

    private fun setupAutoFillData() {
        val sharedPreferences = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("USER_NAME", "Karyawan") ?: "Karyawan"
        etNamaKaryawan.setText(userName)

        // Set shift default untuk hari ini
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        val today = dateFormat.format(Date())
        userShift = getShiftForDate(today)
    }

    private fun getShiftForDate(tanggal: String): String {
        val userSession = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = userSession.getString("USER_NAME", "") ?: ""

        if (userName.isEmpty()) return "Pagi"

        try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = dateFormat.parse(tanggal) ?: return "Pagi"

            val calendar = Calendar.getInstance()
            calendar.time = date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            return when (dayOfWeek) {
                Calendar.SATURDAY, Calendar.SUNDAY -> "Siang"
                else -> "Pagi"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Pagi"
        }
    }

    // Fungsi untuk menghitung jam terhutang berdasarkan shift
    private fun calculateJamTerhutangFullShift(shift: String): String {
        return when (shift) {
            "Pagi" -> "5j 0m"  // 07:00 - 12:00 = 5 jam
            "Siang" -> "4j 0m"  // 13:00 - 17:00 = 4 jam
            "Malam" -> "4j 0m"  // 18:00 - 22:00 = 4 jam
            else -> "5j 0m"
        }
    }

    private fun setupClickListeners() {
        btnClose.setOnClickListener {
            dismiss()
        }

        btnKirim.setOnClickListener {
            if (validasiInput()) {
                kirimPermintaanIzin()
            }
        }
    }

    private fun validasiInput(): Boolean {
        layoutAlasanIzin.error = null

        val alasan = etAlasanIzin.text.toString().trim()

        if (alasan.isEmpty()) {
            layoutAlasanIzin.error = "Alasan tidak boleh kosong"
            return false
        }

        return true
    }

    private fun kirimPermintaanIzin() {
        val nama = etNamaKaryawan.text.toString()
        val alasan = etAlasanIzin.text.toString()
        val tanggalHariIni = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date())

        // Hitung jam terhutang untuk full shift
        val jamTerhutang = calculateJamTerhutangFullShift(userShift)

        // Simpan ke SharedPreferences
        val prefs = requireContext().getSharedPreferences("izin_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val izinId = "izin_${System.currentTimeMillis()}"

        // Format: nama|tanggal|jamMulai|jamSelesai|alasan|status|jamTerhutang|shift
        val (jamMulai, jamSelesai) = when (userShift) {
            "Pagi" -> Pair("07:00", "12:00")
            "Siang" -> Pair("13:00", "17:00")
            "Malam" -> Pair("18:00", "22:00")
            else -> Pair("07:00", "12:00")
        }

        val izinData = "$nama|$tanggalHariIni|$jamMulai|$jamSelesai|$alasan|PENDING|$jamTerhutang|$userShift"
        editor.putString(izinId, izinData)
        editor.apply()

        // Juga simpan flag izin untuk tanggal tersebut di absen_data
        val absenPrefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val absenEditor = absenPrefs.edit()

        // Convert tanggal dari dd/MM/yyyy ke yyyy-MM-dd
        try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(tanggalHariIni)
            val formattedTanggal = outputFormat.format(date ?: Date())

            absenEditor.putBoolean("izin_$formattedTanggal", true)
            absenEditor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(
            requireContext(),
            "Permintaan izin berhasil dikirim!\nJam terhutang: $jamTerhutang",
            Toast.LENGTH_LONG
        ).show()
        dismiss()
    }
}