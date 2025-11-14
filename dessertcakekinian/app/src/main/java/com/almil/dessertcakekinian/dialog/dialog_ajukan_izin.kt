package com.almil.dessertcakekinian.dialog

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

// 1. UBAH DARI 'Fragment()' MENJADI 'DialogFragment()'
class dialog_ajukan_izin : DialogFragment() {

    // 2. Deklarasikan View di sini agar bisa diakses di semua fungsi
    private lateinit var btnClose: ImageButton
    private lateinit var btnKirim: MaterialButton
    private lateinit var etNamaKaryawan: TextInputEditText
    private lateinit var etTanggalIzin: TextInputEditText
    private lateinit var etJamMulai: TextInputEditText
    private lateinit var etJamSelesai: TextInputEditText
    private lateinit var etAlasanIzin: TextInputEditText

    // Layout untuk menampilkan error (opsional namun disarankan)
    private lateinit var layoutTanggalIzin: TextInputLayout
    private lateinit var layoutJamMulai: TextInputLayout
    private lateinit var layoutJamSelesai: TextInputLayout
    private lateinit var layoutAlasanIzin: TextInputLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 3. GANTI LAYOUT DENGAN NAMA FILE XML DIALOG ANDA
        return inflater.inflate(R.layout.dialog_ajukan_izin, container, false)
    }

    // 4. TAMBAHKAN FUNGSI onViewCreated UNTUK SEMUA LOGIKA
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 5. WAJIB: Membuat background dialog transparan agar sudut bulat XML terlihat
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 6. Inisialisasi semua View dari XML
        btnClose = view.findViewById(R.id.btnClose)
        btnKirim = view.findViewById(R.id.btnKirimPermintaan)
        etNamaKaryawan = view.findViewById(R.id.etNamaKaryawan)

        etTanggalIzin = view.findViewById(R.id.etTanggalIzin)
        etJamMulai = view.findViewById(R.id.etJamMulai) // <-- BARU
        etJamSelesai = view.findViewById(R.id.etJamSelesai) // <-- BARU
        etAlasanIzin = view.findViewById(R.id.etAlasanIzin)

        // Inisialisasi Layout (untuk error)
        layoutTanggalIzin = view.findViewById(R.id.layoutTanggalIzin)
        layoutJamMulai = view.findViewById(R.id.layoutJamMulai) // <-- BARU
        layoutJamSelesai = view.findViewById(R.id.layoutJamSelesai) // <-- BARU
        layoutAlasanIzin = view.findViewById(R.id.layoutAlasanIzin)


        // --- Tambahkan Logika Anda Di Sini ---

        // TODO: Isi nama karyawan secara otomatis (misalnya dari SharedPreferences)
        // etNamaKaryawan.setText("Almil (Otomatis)")

        // 7. Logika tombol "Tutup" (X)
        btnClose.setOnClickListener {
            dismiss() // Menutup dialog
        }

        // 8. Logika tombol "Tanggal Izin" (untuk memunculkan kalender)
        etTanggalIzin.setOnClickListener {
            // TODO: Tampilkan DatePickerDialog (dialog kalender) di sini
            // Contoh implementasi:
            // showDatePicker()
            Toast.makeText(context, "Buka Kalender...", Toast.LENGTH_SHORT).show()
        }

        // 9. Logika "Jam Mulai" (untuk memunculkan TimePicker) <-- BARU
        etJamMulai.setOnClickListener {
            // TODO: Tampilkan TimePickerDialog (dialog jam) di sini
            // Contoh implementasi:
            // showTimePicker(isSelectingStartTime = true)
            Toast.makeText(context, "Buka Jam Mulai...", Toast.LENGTH_SHORT).show()
        }

        // 10. Logika "Jam Selesai" (untuk memunculkan TimePicker) <-- BARU
        etJamSelesai.setOnClickListener {
            // TODO: Tampilkan TimePickerDialog (dialog jam) di sini
            // Contoh implementasi:
            // showTimePicker(isSelectingStartTime = false)
            Toast.makeText(context, "Buka Jam Selesai...", Toast.LENGTH_SHORT).show()
        }

        // 11. Logika tombol "Kirim Permintaan" (Validasi diperbarui)
        btnKirim.setOnClickListener {
            if (validasiInput()) {
                // TODO: Tambahkan logika kirim data ke database/API
                // Ambil datanya:
                // val tanggal = etTanggalIzin.text.toString()
                // val jamMulai = etJamMulai.text.toString()
                // val jamSelesai = etJamSelesai.text.toString()
                // val alasan = etAlasanIzin.text.toString()

                Toast.makeText(context, "Permintaan Izin Terkirim", Toast.LENGTH_SHORT).show()
                dismiss() // Tutup dialog setelah kirim
            }
        }
    }

    // 12. Fungsi validasi terpisah agar lebih rapi <-- BARU
    private fun validasiInput(): Boolean {
        // Reset error sebelumnya
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

        return true // Semua validasi lolos
    }
}