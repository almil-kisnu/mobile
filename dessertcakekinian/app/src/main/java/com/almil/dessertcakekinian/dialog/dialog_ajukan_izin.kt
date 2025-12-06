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
import com.almil.dessertcakekinian.database.SupabaseHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        // Ambil shift dari absen_data (sudah di-set di HomePageFragment)
        val absenPrefs = requireContext().getSharedPreferences("absen_data", Context.MODE_PRIVATE)
        userShift = absenPrefs.getString("user_shift", "Pagi") ?: "Pagi"
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
        
        // Format tanggal ke yyyy-MM-dd untuk database
        val tanggalFormatDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Hitung jam terhutang untuk full shift
        val jamTerhutang = calculateJamTerhutangFullShift(userShift)

        // Disable button saat proses
        btnKirim.isEnabled = false
        btnKirim.text = "Mengirim..."

        // Update status ke Izin di Supabase (record yang sudah ada)
        SupabaseHelper().updateStatusToIzin(
            username = nama,
            tanggal = tanggalFormatDb,
            shift = userShift,
            keterangan = alasan,
            callback = object : SupabaseHelper.SimpanCallback {
                override fun onSuccess(message: String) {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Permintaan izin berhasil dikirim!\nJam terhutang: $jamTerhutang",
                            Toast.LENGTH_LONG
                        ).show()
                        dismiss()
                    }
                }

                override fun onError(error: String) {
                    activity?.runOnUiThread {
                        btnKirim.isEnabled = true
                        btnKirim.text = "Kirim Permintaan"
                        Toast.makeText(
                            requireContext(),
                            "Gagal mengirim izin: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}