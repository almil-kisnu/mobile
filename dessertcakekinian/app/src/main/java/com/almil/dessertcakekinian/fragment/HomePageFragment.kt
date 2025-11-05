package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.content.Intent // Import Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout // Import LinearLayout
import androidx.fragment.app.Fragment
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.presensiActivity // Import presensiActivity

class HomePageFragment : Fragment() {

    // ... (Variabel lain)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Cari LinearLayout dengan ID menuPresensi
        val menuPresensi: LinearLayout = view.findViewById(R.id.menuPresensi)

        // 2. Tambahkan Click Listener
        menuPresensi.setOnClickListener {
            // Membuat Intent untuk membuka presensiActivity
            val intent = Intent(requireContext(), presensiActivity::class.java)
            startActivity(intent)

            // !!! Hapus baris ini: requireActivity().finish()
            // Activity host akan tetap ada di back stack dan akan di-resume
            // ketika presensiActivity ditutup.
        }

        // ... (Kode inisialisasi lain)
    }
}