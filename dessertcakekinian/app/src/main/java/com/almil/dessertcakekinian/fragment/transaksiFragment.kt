package com.almil.dessertcakekinian.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import com.almil.dessertcakekinian.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param1"

class transaksiFragment : Fragment() {

    // Hapus BSTransaksiFragment import/panggilan karena kita tidak menggunakannya lagi

    // ... (properti onCreate dan onCreateView tetap sama) ...

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout CoordinatorLayout yang baru
        return inflater.inflate(R.layout.fragment_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dapatkan referensi ke View Bottom Sheet permanen
        val bottomComponent = view.findViewById<LinearLayout>(R.id.permanentBottomComponent)
        val headerLayout = view.findViewById<LinearLayout>(R.id.headerLayout)
        val handleIndicator = view.findViewById<View>(R.id.handleIndicator)
        val dividerView = view.findViewById<View>(R.id.dividerView)

        // 1. Dapatkan BottomSheetBehavior
        val behavior = BottomSheetBehavior.from(bottomComponent)

        // 2. Setting permanen: TIDAK BISA DIHILANGKAN (dismissed)
        behavior.isHideable = false

        // 3. Hitung dan Atur Peek Height
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                if (handleIndicator != null && headerLayout != null && dividerView != null) {

                    val handleMarginParams = handleIndicator.layoutParams as ViewGroup.MarginLayoutParams
                    val handleHeight = handleIndicator.height + handleMarginParams.topMargin

                    val headerHeight = headerLayout.height

                    val dividerMarginParams = dividerView.layoutParams as ViewGroup.MarginLayoutParams
                    val dividerHeight = dividerView.height + dividerMarginParams.topMargin

                    val calculatedPeekHeight = handleHeight + headerHeight + dividerHeight

                    // Set peekHeight
                    val finalPeekHeight = calculatedPeekHeight + dpToPx(requireContext(), 8)
                    behavior.peekHeight = finalPeekHeight

                    // Atur padding bottom RecyclerView agar konten tidak tertutup
                    val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
                    recyclerView.setPadding(
                        recyclerView.paddingLeft,
                        recyclerView.paddingTop,
                        recyclerView.paddingRight,
                        finalPeekHeight + dpToPx(requireContext(), 16) // Tambah sedikit margin
                    )
                }

                // 4. Atur state ke COLLAPSED (peek) saat pertama kali muncul
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        })

        // ... (Listener klik menuIcon/filterIcon jika masih diperlukan) ...
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            transaksiFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}