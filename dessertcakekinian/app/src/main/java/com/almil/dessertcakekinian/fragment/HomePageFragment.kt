package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.activity.DaftarProdukActivity
import com.almil.dessertcakekinian.activity.TransaksiActivity
import com.almil.dessertcakekinian.activity.DiskonActivity
import com.almil.dessertcakekinian.activity.RiwayatActivity
import com.almil.dessertcakekinian.activity.presensiActivity

class HomePageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuPresensi: LinearLayout = view.findViewById(R.id.menuPresensi)
        val menuProduk: LinearLayout = view.findViewById(R.id.menuProduk)
        val menuTransaksi: LinearLayout = view.findViewById(R.id.menuTransaksi)
        val menuRiwayat: LinearLayout = view.findViewById(R.id.menuRiwayat)
        val menuDiskon: LinearLayout = view.findViewById(R.id.menuDiskon)

        menuPresensi.setOnClickListener {
            val intent = Intent(requireContext(), presensiActivity::class.java)
            startActivity(intent)
        }

        menuProduk.setOnClickListener {
            val intent = Intent(requireContext(), DaftarProdukActivity::class.java)
            startActivity(intent)
        }

        menuTransaksi.setOnClickListener {
            val intent = Intent(requireContext(), TransaksiActivity::class.java)
            startActivity(intent)
        }

        menuRiwayat.setOnClickListener {
            val intent = Intent(requireContext(), RiwayatActivity::class.java)
            startActivity(intent)
        }

        menuDiskon.setOnClickListener {
            val intent = Intent(requireContext(), DiskonActivity::class.java)
            startActivity(intent)
        }
    }
}