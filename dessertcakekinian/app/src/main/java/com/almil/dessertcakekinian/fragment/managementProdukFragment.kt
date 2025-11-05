package com.almil.dessertcakekinian.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.almil.dessertcakekinian.R

class managementProdukFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_management_produk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cari view berdasarkan ID yang sudah kita buat di XML
        val layoutDaftarProduk: RelativeLayout = view.findViewById(R.id.dafar_produk)
        val layoutRiwayatStok: RelativeLayout = view.findViewById(R.id.daftar_produk_rusak)

        // Atur OnClickListener untuk "Daftar Produk"
        layoutDaftarProduk.setOnClickListener {
            val daftarProdukFragment = DaftarProdukFragment()
            navigateToFragment(daftarProdukFragment)
        }

        // Atur OnClickListener untuk "Riwayat Stok"
        layoutRiwayatStok.setOnClickListener {
            val riwayatTransaksiFragment = RiwayatTransaksiFragment()
            navigateToFragment(riwayatTransaksiFragment)
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        // Gunakan parentFragmentManager untuk melakukan transaksi fragment
        parentFragmentManager.beginTransaction().apply {
            // Ganti fragment saat ini dengan fragment yang baru
            // GANTI R.id.fragment_container dengan ID container di Activity Anda
            replace(R.id.container, fragment)
            // Tambahkan ke back stack agar user bisa kembali dengan tombol back
            addToBackStack(null)
            // Lakukan transaksi
            commit()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = managementProdukFragment()
    }
}
