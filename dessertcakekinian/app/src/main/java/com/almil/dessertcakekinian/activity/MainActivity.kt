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
import com.almil.dessertcakekinian.fragment.paymentFragment
import com.almil.dessertcakekinian.fragment.riwayatPesananFragment
import com.almil.dessertcakekinian.fragment.ManagementUsersFragment

class MainActivity : AppCompatActivity() {

    private lateinit var iconMenu: ImageView
    private var popupWindow: PopupWindow? = null
    private var isMenuVisible = false
    private var activeFragment: Fragment? = null
    private val transactionFragment = transactionFragment()

    private val riwayatPesananFragment = riwayatPesananFragment()
    private val managemenUsersFragment = ManagementUsersFragment()
    private val paymentFragment = paymentFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        iconMenu = findViewById(R.id.iconMenu)

        iconMenu.setOnClickListener {
            if (isMenuVisible) hidePopupMenu() else showPopupMenu(it)
        }

        if (savedInstanceState == null) {
            showFragment(transactionFragment, "TransactionFragment")
        }
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

        produk.setOnClickListener {
            showFragment(managemenUsersFragment)
            popupWindow?.dismiss()
        }

        transaksi.setOnClickListener {
            showFragment(transactionFragment)
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

    fun showFragment(fragment: Fragment, tag: String = fragment.javaClass.simpleName) {
        val transaction = supportFragmentManager.beginTransaction()
        if (activeFragment != null && activeFragment != fragment) {
            transaction.hide(activeFragment!!)
        }
        if (!fragment.isAdded) {
            transaction.add(R.id.container, fragment, tag)
        } else {
            transaction.show(fragment)
        }
        findViewById<View>(R.id.customToolbar).visibility =
            if (fragment is paymentFragment) View.GONE else View.VISIBLE
        transaction.commit()
        activeFragment = fragment
    }

    fun getTransactionFragment(): Fragment {
        return transactionFragment
    }

    fun getPaymentFragment(): Fragment {
        return paymentFragment
    }

    fun resetTransactionFragment() {
        if (transactionFragment.isAdded) {
            transactionFragment.clearCart()
            transactionFragment.refreshData()
        }
        showFragment(transactionFragment)
    }


}