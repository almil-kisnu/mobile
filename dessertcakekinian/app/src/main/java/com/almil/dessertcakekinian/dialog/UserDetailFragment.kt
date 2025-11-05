package com.almil.dessertcakekinian.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.adapter.OnUserActionListener
import com.almil.dessertcakekinian.model.Pengguna
import com.google.android.material.button.MaterialButton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.google.android.material.imageview.ShapeableImageView

// ✅ Ubah dari Fragment menjadi DialogFragment
class dialogUserDetailFragment : DialogFragment() {
    private lateinit var pengguna: Pengguna
    private var listener: OnUserActionListener? = null

    companion object {
        const val TAG = "UserDetailDialog"
        private const val ARG_PENGGUNA = "pengguna_object"

        // ✅ Ganti newInstance untuk menerima objek Pengguna (Parcelable) dan OnUserActionListener
        @JvmStatic
        fun newInstance(pengguna: Pengguna, listener: OnUserActionListener) =
            dialogUserDetailFragment().apply {
                this.listener = listener // Simpan listener
                arguments = Bundle().apply {
                    putString(ARG_PENGGUNA, Json.encodeToString(pengguna))
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCancelable = true

        // ... (kode style dialog)
        setStyle(STYLE_NORMAL, R.style.CustomDialogTheme)

        arguments?.let {
            val jsonString = it.getString(ARG_PENGGUNA)
            if (jsonString != null) {
                // Ambil sebagai String dan Dekode kembali ke objek Pengguna
                pengguna = Json.decodeFromString(jsonString)
            } else {
                // Lempar exception jika string-nya null
                throw IllegalArgumentException("Pengguna is required")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout detail pengguna
        return inflater.inflate(R.layout.fragment_user_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mapping View

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvRole = view.findViewById<TextView>(R.id.tvRole)
        val tvStoreCodeValue = view.findViewById<TextView>(R.id.tvStoreCodeValue)
        val tvNikValue = view.findViewById<TextView>(R.id.tvNikValue)
        val tvRegisteredValue = view.findViewById<TextView>(R.id.tvRegisteredValue)
        val tvPhoneValue = view.findViewById<TextView>(R.id.tvPhoneValue)
        val tvStatusValue = view.findViewById<TextView>(R.id.tvStatusValue)
        val tvReasonLabel = view.findViewById<TextView>(R.id.tvReasonLabel)
        val tvReasonValue = view.findViewById<TextView>(R.id.tvReasonValue)
        val btnEdit = view.findViewById<MaterialButton>(R.id.btnEdit)
        val ivProfile = view.findViewById<ShapeableImageView>(R.id.ivProfile) // Digunakan jika ada implementasi gambar

        // Binding Data
        ivProfile.setImageResource(R.drawable.ic_anonim)
        tvName.text = pengguna.username
        tvRole.text = pengguna.role ?: "-"
        tvStoreCodeValue.text = pengguna.outlet?.kodeOutlet ?: "-"
        tvNikValue.text = pengguna.nik ?: "-"
        tvRegisteredValue.text =
            pengguna.hiredDate // Asumsi field ini ada dan format tanggal sudah diurus
        tvPhoneValue.text = pengguna.phone
        tvReasonValue.text = pengguna.deactivatedReason ?: "-"

        // Status (Logika sama seperti di adapter)
        if (pengguna.isActive) {
            tvStatusValue.text = "aktif"
            tvStatusValue.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.status_active
                )
            ) // Ganti dengan warna yang sesuai
            tvReasonLabel.visibility = View.GONE
            tvReasonValue.visibility = View.GONE
        } else {
            tvStatusValue.text = "non-aktif"
            tvStatusValue.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.status_inactive
                )
            ) // Ganti dengan warna yang sesuai
        }



        // ✅ LOGIKA TOMBOL "UBAH" (Memanggil Edit BottomSheet)
        btnEdit.setOnClickListener {
            listener?.onEditClicked(pengguna) // Panggil logika edit melalui listener
            dismiss() // Tutup dialog detail setelah memanggil edit
        }

    }
}