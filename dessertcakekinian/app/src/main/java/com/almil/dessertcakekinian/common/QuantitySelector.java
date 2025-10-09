package com.almil.dessertcakekinian.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.almil.dessertcakekinian.R;

public class QuantitySelector extends LinearLayout {
    private ImageButton btnPlus, btnMinus;
    private Button btnDelete; // Tombol hapus
    private TextView tvJumlah;
    private int quantity = 0;
    private OnQuantityChangeListener listener;

    // Interface untuk callback
    public interface OnQuantityChangeListener {
        void onQuantityChanged(int quantity);
    }

    public QuantitySelector(Context context) {
        super(context);
        init(context);
    }

    public QuantitySelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public QuantitySelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Inflate layout XML quantity_selector.xml
        LayoutInflater.from(context).inflate(R.layout.quantity_selector, this, true);

        // Inisialisasi view
        btnPlus = findViewById(R.id.btnPlus);
        btnMinus = findViewById(R.id.btnMinus);
        tvJumlah = findViewById(R.id.tvJumlah);
        btnDelete = findViewById(R.id.btnDelete); // Tombol baru

        // Set tampilan awal
        updateVisibility();

        // Tombol +
        btnPlus.setOnClickListener(v -> {
            quantity++;
            updateUI();
            if (listener != null) listener.onQuantityChanged(quantity);
        });

        // Tombol -
        btnMinus.setOnClickListener(v -> {
            if (quantity > 0) {
                quantity--;
                updateUI();
                if (listener != null) listener.onQuantityChanged(quantity);
            }
        });

        // Tombol hapus
        btnDelete.setOnClickListener(v -> {
            reset(); // kembalikan ke default
            if (listener != null) listener.onQuantityChanged(quantity);
        });
    }

    private void updateUI() {
        tvJumlah.setText(String.valueOf(quantity));
        updateVisibility();
    }

    private void updateVisibility() {
        if (quantity == 0) {
            // Mode default → hanya plus yang tampil
            btnPlus.setVisibility(VISIBLE);
            btnMinus.setVisibility(GONE);
            tvJumlah.setVisibility(GONE);
            btnDelete.setVisibility(GONE);
        } else {
            // Mode aktif → tampil semua
            btnPlus.setVisibility(VISIBLE);
            btnMinus.setVisibility(VISIBLE);
            tvJumlah.setVisibility(VISIBLE);
            btnDelete.setVisibility(VISIBLE);
        }
    }

    // ===== PUBLIC METHODS =====
    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
        updateUI();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.listener = listener;
    }

    public void reset() {
        quantity = 0;
        updateUI();
    }
}
