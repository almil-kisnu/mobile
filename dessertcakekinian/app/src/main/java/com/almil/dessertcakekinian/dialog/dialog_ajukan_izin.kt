package com.almil.dessertcakekinian.dialog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
    private lateinit var etTanggalIzin: TextInputEditText
    private lateinit var etJamMulai: TextInputEditText
    private lateinit var etJamSelesai: TextInputEditText
    private lateinit var etAlasanIzin: TextInputEditText

    private lateinit var layoutTanggalIzin: TextInputLayout
    private lateinit var layoutJamMulai: TextInputLayout
    private lateinit var layoutJamSelesai: TextInputLayout
    private lateinit var layoutAlasanIzin: TextInputLayout

    private val calendar = Calendar.getInstance()
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
        etTanggalIzin = view.findViewById(R.id.etTanggalIzin)
        etJamMulai = view.findViewById(R.id.etJamMulai)
        etJamSelesai = view.findViewById(R.id.etJamSelesai)
        etAlasanIzin = view.findViewById(R.id.etAlasanIzin)

        layoutTanggalIzin = view.findViewById(R.id.layoutTanggalIzin)
        layoutJamMulai = view.findViewById(R.id.layoutJamMulai)
        layoutJamSelesai = view.findViewById(R.id.layoutJamSelesai)
        layoutAlasanIzin = view.findViewById(R.id.layoutAlasanIzin)
    }

    private fun setupAutoFillData() {
        val sharedPreferences = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("USER_NAME", "Karyawan") ?: "Karyawan"
        etNamaKaryawan.setText(userName)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        etTanggalIzin.setText(dateFormat.format(Date()))

        userShift = getShiftForDate(etTanggalIzin.text.toString())
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

    // FUNGSI BARU: Hitung jam terhutang berdasarkan shift
    private fun calculateJamTerhutang(jamMulai: String, jamSelesai: String, shift: String): String {
        if (jamMulai.isEmpty() || jamSelesai.isEmpty()) return "0j 0m"

        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTime = timeFormat.parse(jamMulai)
            val endTime = timeFormat.parse(jamSelesai)

            if (startTime != null && endTime != null) {
                // Tentukan jam shift normal
                val (shiftStart, shiftEnd) = when (shift) {
                    "Pagi" -> Pair(timeFormat.parse("07:00"), timeFormat.parse("12:00"))
                    "Siang" -> Pair(timeFormat.parse("13:00"), timeFormat.parse("17:00"))
                    "Malam" -> Pair(timeFormat.parse("18:00"), timeFormat.parse("22:00"))
                    else -> Pair(timeFormat.parse("07:00"), timeFormat.parse("12:00"))
                }

                if (shiftStart != null && shiftEnd != null) {
                    // Hitung overlap antara jam izin dan jam shift
                    val izinStart = maxOf(startTime.time, shiftStart.time)
                    val izinEnd = minOf(endTime.time, shiftEnd.time)

                    if (izinStart < izinEnd) {
                        val jamTerhutangMenit = (izinEnd - izinStart) / (60 * 1000)
                        val jam = jamTerhutangMenit / 60
                        val menit = jamTerhutangMenit % 60

                        return if (jam > 0 && menit > 0) "${jam}j ${menit}m"
                        else if (jam > 0) "${jam}j"
                        else "${menit}m"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "0j 0m"
    }

    private fun setupClickListeners() {
        btnClose.setOnClickListener {
            dismiss()
        }

        etTanggalIzin.setOnClickListener {
            showDatePicker()
        }

        etJamMulai.setOnClickListener {
            showTimePicker(true)
        }

        etJamSelesai.setOnClickListener {
            showTimePicker(false)
        }

        btnKirim.setOnClickListener {
            if (validasiInput()) {
                kirimPermintaanIzin()
            }
        }

        // Update shift ketika tanggal berubah
        etTanggalIzin.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                userShift = getShiftForDate(etTanggalIzin.text.toString())
            }
        }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
            etTanggalIzin.setText(dateFormat.format(calendar.time))

            // Update shift ketika tanggal dipilih
            userShift = getShiftForDate(etTanggalIzin.text.toString())
        }, year, month, day)

        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeText = timeFormat.format(calendar.time)

            if (isStartTime) {
                etJamMulai.setText(timeText)
            } else {
                etJamSelesai.setText(timeText)
            }
        }, hour, minute, true)

        timePicker.show()
    }

    private fun validasiInput(): Boolean {
        layoutTanggalIzin.error = null
        layoutJamMulai.error = null
        layoutJamSelesai.error = null
        layoutAlasanIzin.error = null

        val tanggal = etTanggalIzin.text.toString().trim()
        val jamMulai = etJamMulai.text.toString().trim()
        val jamSelesai = etJamSelesai.text.toString().trim()
        val alasan = etAlasanIzin.text.toString().trim()

        if (tanggal.isEmpty()) {
            layoutTanggalIzin.error = "Tanggal izin tidak boleh kosong"
            return false
        }
        if (jamMulai.isEmpty()) {
            layoutJamMulai.error = "Jam mulai tidak boleh kosong"
            return false
        }
        if (jamSelesai.isEmpty()) {
            layoutJamSelesai.error = "Jam selesai tidak boleh kosong"
            return false
        }
        if (alasan.isEmpty()) {
            layoutAlasanIzin.error = "Alasan tidak boleh kosong"
            return false
        }

        if (jamMulai.isNotEmpty() && jamSelesai.isNotEmpty()) {
            try {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val startTime = timeFormat.parse(jamMulai)
                val endTime = timeFormat.parse(jamSelesai)

                if (endTime != null && startTime != null && endTime.before(startTime)) {
                    layoutJamSelesai.error = "Jam selesai harus setelah jam mulai"
                    return false
                }
            } catch (e: Exception) {
                // Format time tidak valid
            }
        }

        return true
    }

    private fun kirimPermintaanIzin() {
        val nama = etNamaKaryawan.text.toString()
        val tanggal = etTanggalIzin.text.toString()
        val jamMulai = etJamMulai.text.toString()
        val jamSelesai = etJamSelesai.text.toString()
        val alasan = etAlasanIzin.text.toString()

        // Hitung jam terhutang berdasarkan shift
        val jamTerhutang = calculateJamTerhutang(jamMulai, jamSelesai, userShift)

        // Simpan ke SharedPreferences
        val prefs = requireContext().getSharedPreferences("izin_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val izinId = "izin_${System.currentTimeMillis()}"

        // Format: nama|tanggal|jamMulai|jamSelesai|alasan|status|jamTerhutang|shift
        val izinData = "$nama|$tanggal|$jamMulai|$jamSelesai|$alasan|PENDING|$jamTerhutang|$userShift"
        editor.putString(izinId, izinData)
        editor.apply()

        // Juga simpan flag izin untuk tanggal tersebut di absen_data
        val absenPrefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        val absenEditor = absenPrefs.edit()

        // Convert tanggal dari dd/MM/yyyy ke yyyy-MM-dd
        try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(tanggal)
            val formattedTanggal = outputFormat.format(date ?: Date())

            absenEditor.putBoolean("izin_$formattedTanggal", true)
            absenEditor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(requireContext(), "Permintaan izin berhasil dikirim!\nJam terhutang: $jamTerhutang", Toast.LENGTH_LONG).show()
        dismiss()
    }
}