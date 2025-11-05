package com.almil.dessertcakekinian.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.almil.dessertcakekinian.R
import com.almil.dessertcakekinian.model.Pengguna
import com.google.android.material.imageview.ShapeableImageView

// ✅ LANGKAH 1: DEFINISIKAN INTERFACE YANG HILANG DI SINI
interface OnUserActionListener {
    fun onEditClicked(pengguna: Pengguna)
}

interface OnUserItemClickListener {
    fun onUserItemClicked(pengguna: Pengguna)
}

// 🛑 Hapus interface OnUserClickListener yang lama jika masih ada

class UserAdapter(
    private var userList: List<Pengguna>,
    // ✅ Baris ini sekarang akan valid
    private val listener: OnUserItemClickListener
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivStoreImage: ShapeableImageView = itemView.findViewById(R.id.ivStoreImage)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvStoreCode: TextView = itemView.findViewById(R.id.tvStoreCode)
        val tvGmail: TextView = itemView.findViewById(R.id.tvGmail)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_card, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.ivStoreImage.setImageResource(R.drawable.ic_anonim)
        holder.tvUsername.text = currentUser.username
        holder.tvStoreCode.text = currentUser.outlet?.kodeOutlet ?: "-"
        holder.tvGmail.text = currentUser.phone

        // Status (sudah benar)
        if (currentUser.isActive) {
            holder.tvStatus.text = "Aktif"
            holder.tvStatus.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.status_active)
            )
        } else {
            holder.tvStatus.text = "Non-Aktif"
            holder.tvStatus.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.status_inactive)
            )
        }

        // ✅ Baris ini sekarang juga akan valid
        holder.itemView.setOnClickListener {
            listener.onUserItemClicked(currentUser)
        }
    }

    override fun getItemCount(): Int = userList.size

    fun updateData(newList: List<Pengguna>) {
        userList = newList
        notifyDataSetChanged()
    }
}