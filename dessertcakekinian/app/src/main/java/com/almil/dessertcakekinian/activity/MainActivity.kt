package com.almil.dessertcakekinian.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.fragment.transactionFragment
import com.almil.dessertcakekinian.fragment.riwayatPesananFragment

class MainActivity : AppCompatActivity() {

    private lateinit var iconMenu: ImageView
    private var popupWindow: PopupWindow? = null
    private var isMenuVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        iconMenu = findViewById(R.id.iconMenu)

        // Klik hamburger menu
        iconMenu.setOnClickListener {
            if (isMenuVisible) hidePopupMenu() else showPopupMenu(it)
        }

        // Tampilkan fragment default (misal halaman produk)
        // replaceFragment(ProductFragment()) kalau udah ada
    }

    private fun showPopupMenu(anchorView: View) {
        val view = LayoutInflater.from(this).inflate(R.layout.menu, null)

        popupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 20f
        }

        val produk = view.findViewById<LinearLayout>(R.id.produk)
        val transaksi = view.findViewById<LinearLayout>(R.id.transaksi)

        // klik produk
        produk.setOnClickListener {
            // TODO: ganti fragment produk
            replaceFragment(riwayatPesananFragment())
            popupWindow?.dismiss()
        }

        // klik transaksi
        transaksi.setOnClickListener {
            replaceFragment(transactionFragment())
            popupWindow?.dismiss()
        }

        popupWindow?.showAsDropDown(anchorView, -150, 20)
        isMenuVisible = true

        popupWindow?.setOnDismissListener {
            isMenuVisible = false
        }
    }

    private fun hidePopupMenu() {
        popupWindow?.dismiss()
        isMenuVisible = false
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
