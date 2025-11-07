package com.almil.dessertcakekinian.BottomSheet

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import com.almil.dessertcakekinian.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.annotation.StyleRes

class BSTransaksiFragment : BottomSheetDialogFragment() {

    private var param1: String? = null
    private var param2: String? = null

    @StyleRes
    private val customTheme: Int = R.style.CustomBottomSheetDialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // **PENTING:** Menerapkan tema Bottom Sheet kustom untuk memastikan visibilitas
        setStyle(STYLE_NORMAL, customTheme)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_b_s_transaksi, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val headerLayout = view.findViewById<LinearLayout>(R.id.headerLayout)
        val handleIndicator = view.findViewById<View>(R.id.handleIndicator)
        val dividerView = view.findViewById<View>(R.id.dividerView)

        val behavior = (dialog as BottomSheetDialog).behavior

        // Menghitung peekHeight setelah layout diukur untuk akurasi (menggunakan ViewTreeObserver)
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

                    // Set peekHeight dan pastikan ada sedikit space tambahan di bawah divider
                    behavior.peekHeight = calculatedPeekHeight + dpToPx(requireContext(), 8)
                } else {
                    // Fallback
                    behavior.peekHeight = dpToPx(requireContext(), 250)
                }

                // **PENTING:** Atur state awal ke COLLAPSED
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        })
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BSTransaksiFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}