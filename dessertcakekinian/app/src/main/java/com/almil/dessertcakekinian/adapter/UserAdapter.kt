package com.almil.dessertcakekinian.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.Pengguna

interface OnUserActionListener {
    fun onEditClicked(pengguna: Pengguna)
}

class UserAdapter(
    private var userList: List<Pengguna>,
    private val listener: OnUserActionListener
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvPhone: TextView = itemView.findViewById(R.id.tvGmail)
        val tvPassword: TextView = itemView.findViewById(R.id.tvPassword)
        val tvRole: TextView = itemView.findViewById(R.id.tvRole)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvDateLabel: TextView = itemView.findViewById(R.id.tvDateLabel)
        val tvReason: TextView = itemView.findViewById(R.id.tvReason)
        val tvReasonLabel: TextView = itemView.findViewById(R.id.tvReasonLabel)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_card, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]

        holder.tvUsername.text = currentUser.username
        holder.tvPhone.text = currentUser.phone
        holder.tvPassword.text = "••••••••"
        holder.tvRole.text = currentUser.role

        // ✅ --- LOGIKA BARU UNTUK STATUS ---
        if (currentUser.isActive) {
            holder.tvStatus.text = "AKTIF"
            // Atur background hijau dan teks hijau terang
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_aktif)
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green)) // Asumsi kamu punya color green

            // Set lebar tvStatus agar konsisten
            val params = holder.tvStatus.layoutParams
            holder.tvStatus.layoutParams = params

            // Tampilkan tanggal masuk
            holder.tvDateLabel.text = "Tanggal Masuk:"
            holder.tvDate.text = currentUser.hiredDate ?: "N/A"

            // Sembunyikan field alasan
            holder.tvReasonLabel.visibility = View.GONE
            holder.tvReason.visibility = View.GONE
        } else {
            holder.tvStatus.text = "NON-AKTIF"
            // Atur background merah dan teks merah terang
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_nonaktif)
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red)) // Asumsi kamu punya color red

            // Set lebar tvStatus menjadi wrap_content agar pas dengan teks "NON-AKTIF"
            val params = holder.tvStatus.layoutParams
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
            holder.tvStatus.layoutParams = params

            // Tampilkan tanggal keluar
            holder.tvDateLabel.text = "Tanggal Keluar:"
            holder.tvDate.text = currentUser.deactivatedAt ?: "N/A"

            // Tampilkan field alasan
            holder.tvReasonLabel.visibility = View.VISIBLE
            holder.tvReason.visibility = View.VISIBLE
            holder.tvReason.text = currentUser.deactivatedReason ?: "Tidak ada alasan."
        }
        // ------------------------------------

        holder.btnEdit.setOnClickListener {
            listener.onEditClicked(currentUser)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }
}