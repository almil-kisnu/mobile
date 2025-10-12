package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.OtpType
import kotlinx.coroutines.launch

class confirmOtpFragment : Fragment() {
    private var userEmail: String? = null

    // Referensi ke Views
    private lateinit var etOtp1: EditText
    private lateinit var etOtp2: EditText
    private lateinit var etOtp3: EditText
    private lateinit var etOtp4: EditText
    private lateinit var etOtp5: EditText
    private lateinit var etOtp6: EditText
    private lateinit var btnVerify: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userEmail = it.getString(ARG_EMAIL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_confirm_otp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Views
        etOtp1 = view.findViewById(R.id.etOtp1)
        etOtp2 = view.findViewById(R.id.etOtp2)
        etOtp3 = view.findViewById(R.id.etOtp3)
        etOtp4 = view.findViewById(R.id.etOtp4)
        etOtp5 = view.findViewById(R.id.etOtp5)
        etOtp6 = view.findViewById(R.id.etOtp6)
        btnVerify = view.findViewById(R.id.btnVerify)

        btnVerify.setOnClickListener {
            // 1. Gabungkan semua input OTP menjadi satu string
            val otpCode = "${etOtp1.text}${etOtp2.text}${etOtp3.text}${etOtp4.text}${etOtp5.text}${etOtp6.text}"

            // Validasi sederhana
            if (otpCode.length == 6 && !userEmail.isNullOrBlank()) {
                verifyOtpCode(userEmail!!, otpCode)
            } else {
                Toast.makeText(requireContext(), "Harap isi 6 digit OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyOtpCode(email: String, token: String) {
        btnVerify.isEnabled = false
        btnVerify.text = "Memverifikasi..."

        val supabase = SupabaseClientProvider.client
        // 2. Jalankan di Coroutine
        lifecycleScope.launch {
            try {
                // 3. Panggil fungsi verifikasi OTP dari Supabase
                supabase.auth.verifyEmailOtp(
                    type = OtpType.Email.SIGNUP, // Sesuaikan tipenya, SIGNUP untuk pendaftaran
                    email = email,
                    token = token
                )

                // 4. Jika berhasil, navigasi ke RegisterFragment
                Toast.makeText(requireContext(), "Verifikasi Berhasil!", Toast.LENGTH_SHORT).show()
                navigateToRegisterFragment()

            } catch (e: Exception) {
                // 5. Jika gagal, catat error dan tampilkan pesan
                Log.e(TAG, "Gagal verifikasi OTP: ", e)
                Toast.makeText(requireContext(), "Kode OTP salah atau telah kedaluwarsa.", Toast.LENGTH_LONG).show()

                btnVerify.isEnabled = true
                btnVerify.text = "Verifikasi"
            }
        }
    }

    private fun navigateToRegisterFragment() {
        val registerFragment = registerFragment() // Ganti dengan nama fragment register Anda

        // PENTING: Ganti R.id.fragment_container_view dengan ID container fragment di Activity utama Anda
        parentFragmentManager.beginTransaction()
            .replace(R.id.registerFragment, registerFragment)
            .addToBackStack(null) // Agar user bisa kembali
            .commit()
    }

    companion object {
        private const val TAG = "ConfirmOtpFragment"
        private const val ARG_EMAIL = "user_email"

        @JvmStatic
        fun newInstance(email: String) =
            confirmOtpFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                }
            }
    }
}
