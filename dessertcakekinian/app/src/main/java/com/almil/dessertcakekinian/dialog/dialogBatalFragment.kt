package com.almil.dessertcakekinian.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.database.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import android.widget.EditText
import android.widget.ImageButton

class dialogBatalFragment : DialogFragment() {

    private var idTransfer: Int = 0
    private var onBatalListener: ((Boolean) -> Unit)? = null
    private var onDismissParent: (() -> Unit)? = null

    companion object {
        private const val ARG_ID_TRANSFER = "arg_id_transfer"

        fun newInstance(idTransfer: Int): dialogBatalFragment {
            return dialogBatalFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_TRANSFER, idTransfer)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            idTransfer = it.getInt(ARG_ID_TRANSFER, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog_batal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }


    private fun initViews(view: View) {
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        val etCatatan = view.findViewById<EditText>(R.id.et_catatan)
        val btnKirim = view.findViewById<Button>(R.id.btnKirim)

        // Setup back button
        btnBack.setOnClickListener {
            dismiss()
        }

        // Setup kirim button
        btnKirim.setOnClickListener {
            val catatan = etCatatan.text.toString().trim()

            if (catatan.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Catatan tidak boleh kosong",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            batalkanTransfer(catatan)
        }
    }


    // Hapus method setupListeners() yang lama

    private fun batalkanTransfer(catatan: String) {
        // Disable button
        view?.findViewById<Button>(R.id.btnKirim)?.isEnabled = false

        lifecycleScope.launch {
            try {
                // Update status dan catatan ke 'dibatalkan' di Supabase
                SupabaseClientProvider.client.from("transfer_stock")
                    .update(
                        mapOf(
                            "status" to "dibatalkan",
                            "catatan" to catatan
                        )
                    ) {
                        filter {
                            eq("idtransfer", idTransfer)
                        }
                    }

                // Berhasil
                Toast.makeText(
                    requireContext(),
                    "Transfer berhasil dibatalkan",
                    Toast.LENGTH_SHORT
                ).show()

                // Callback ke fragment parent
                onBatalListener?.invoke(true)
                onDismissParent?.invoke()
                dismiss()

            } catch (e: Exception) {
                // Gagal
                view?.findViewById<Button>(R.id.btnKirim)?.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Gagal membatalkan transfer: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                onBatalListener?.invoke(false)
            }
        }
    }

    fun setOnDismissParent(listener: () -> Unit) {
        onDismissParent = listener
    }

    // Setter untuk callback
    fun setOnBatalListener(listener: (Boolean) -> Unit) {
        onBatalListener = listener
    }
}