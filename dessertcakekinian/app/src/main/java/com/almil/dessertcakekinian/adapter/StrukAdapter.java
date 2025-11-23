package com.almil.dessertcakekinian.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.almil.dessertcakekinian.R; // Pastikan R.java Anda di package ini
import com.almil.dessertcakekinian.model.CartItem;
import com.almil.dessertcakekinian.model.ProdukKategori;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StrukAdapter extends RecyclerView.Adapter<StrukAdapter.StrukViewHolder> {

    private List<CartItem> cartItemList;
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0");
    public StrukAdapter(Map<Integer, CartItem> cartItemsMap) {
        // Konversi Map menjadi List untuk memudahkan penggunaan RecyclerView
        this.cartItemList = new ArrayList<>(cartItemsMap.values());
    }

    public void updateData(Map<Integer, CartItem> newCartItemsMap) {
        this.cartItemList = new ArrayList<>(newCartItemsMap.values());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StrukViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_struk, parent, false); // Ganti dengan nama file XML Anda jika berbeda
        return new StrukViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StrukViewHolder holder, int position) {
        CartItem cartItem = cartItemList.get(position);
        ProdukKategori produk = cartItem.getProdukDetail().getProduk();
        int quantity = cartItem.getQuantity();
        Double hargaSatuan = cartItem.getHargaSatuan();

        if (hargaSatuan == null) {
            hargaSatuan = 0.0;
        }

        // 1. Nomor Urut + Nama Produk
        holder.tvNumber.setText((position + 1) + ".");
        holder.tvNama.setText(produk.getNamaproduk());

        // Hitung Harga Total
        double hargaTotal = quantity * hargaSatuan;

        String qtyText = quantity + " x " + decimalFormat.format(hargaSatuan);
        holder.tvQty.setText(qtyText);

        String hargaTotalText = "Rp " + decimalFormat.format(hargaTotal);
        holder.tvHarga.setText(hargaTotalText);
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    // --- ViewHolder Class ---
    public static class StrukViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNumber;
        final TextView tvNama;
        final TextView tvQty;
        final TextView tvHarga;

        public StrukViewHolder(@NonNull View itemView) {
            super(itemView);
            // Inisialisasi View dari layout XML
            tvNumber = itemView.findViewById(R.id.tv_number);
            tvNama = itemView.findViewById(R.id.tv_nama);
            tvQty = itemView.findViewById(R.id.tv_qty);
            tvHarga = itemView.findViewById(R.id.tv_harga);
        }
    }
}