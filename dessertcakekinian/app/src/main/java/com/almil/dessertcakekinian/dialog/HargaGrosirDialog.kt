// File: dialog/HargaGrosirDialog.kt
package com.almil.dessertcakekinian.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.HargaJualAdapter
import com.almil.dessertcakekinian.model.HargaGrosir
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class HargaGrosirDialog : DialogFragment() {

    private var hargaGrosirList: List<HargaGrosir> = emptyList()

    // Json instance yang aman
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    companion object {
        private const val ARG_HARGA_GROSIR = "harga_grosir_list"

        @JvmStatic
        fun newInstance(hargaGrosirList: List<HargaGrosir>): HargaGrosirDialog {
            return HargaGrosirDialog().apply {
                arguments = Bundle().apply {
                    try {
                        val jsonString = Json.encodeToString(hargaGrosirList)
                        putString(ARG_HARGA_GROSIR, jsonString)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            val jsonString = bundle.getString(ARG_HARGA_GROSIR)
            Log.d("HargaGrosirDialog", "Received JSON: $jsonString")
            if (jsonString != null && jsonString.isNotBlank()) {
                try {
                    hargaGrosirList = json.decodeFromString<List<HargaGrosir>>(jsonString)
                    Log.d("HargaGrosirDialog", "Decoded ${hargaGrosirList.size} items")
                } catch (e: Exception) {
                    Log.e("HargaGrosirDialog", "Decode failed", e)
                    hargaGrosirList = emptyList()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.harga_grosir_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_harga_jual)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = HargaJualAdapter(hargaGrosirList)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
}