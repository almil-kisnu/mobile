package com.almil.dessertcakekinian.common;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;

import com.almil.dessertcakekinian.R;

// Custom View untuk mengontrol kuantitas, dengan input yang dapat diedit secara manual.
public class QuantitySelector extends LinearLayout {
    private Button btnPlus, btnMinus; // Sekarang MaterialButton (di-cast sebagai Button)
    private EditText etJumlah; // Sekarang EditText
    private int quantity = 0;
    private OnQuantityChangeListener listener;
    private int maxQuantity = Integer.MAX_VALUE;
    private boolean isUpdatingText = false;
    private boolean shouldNotifyExternal = true; // Tambahkan setelah maxQuantity


    public void setMaxQuantity(int max) {
        this.maxQuantity = max;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

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
        etJumlah = findViewById(R.id.tvJumlah);
        etJumlah.setFocusable(false);
        etJumlah.setFocusableInTouchMode(false);
        etJumlah.setClickable(false);
        etJumlah.setCursorVisible(false);

        // Set tampilan awal
        updateUI();

        // Tombol +
        btnPlus.setOnClickListener(v -> {
            if (quantity < maxQuantity) {
                quantity++;
                updateUI();
                notifyQuantityChange();
            } else {
                android.widget.Toast.makeText(context,
                        "Stok maksimal: " + maxQuantity,
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Tombol -
        btnMinus.setOnClickListener(v -> {
            if (quantity > 0) {
                quantity--;
                updateUI();
                notifyQuantityChange();
            }
        });

        // TAMBAHKAN: OnFocusChangeListener untuk select all saat diklik
//        etJumlah.setOnFocusChangeListener((v, hasFocus) -> {
//            if (hasFocus) {
//                // Saat mendapat focus, select semua teks agar langsung terganti saat user mengetik
//                etJumlah.post(() -> etJumlah.selectAll());
//            }
//        });

        // TAMBAHKAN: OnClickListener untuk select all saat diklik (jika sudah fokus)
//        etJumlah.setOnClickListener(v -> {
//            etJumlah.selectAll();
//        });

//        // Listener untuk Input Manual pada EditText
//        etJumlah.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Kosong */ }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                if (s.length() == 0) {
//                    quantity = 0;
//                    updateButtonState();
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                if (isUpdatingText) {
//                    return;
//                }
//
//                String text = s.toString();
//
//                if (text.isEmpty()) {
//                    quantity = 0;
//                    updateButtonState();
//                    // JANGAN notify saat user sedang ngetik
//                    shouldNotifyExternal = false;
//                    notifyQuantityChange();
//                    shouldNotifyExternal = true;
//                    return;
//                }
//
//                try {
//                    int newQuantity = Integer.parseInt(text);
//
//                    if (newQuantity > maxQuantity) {
//                        isUpdatingText = true;
//                        s.replace(0, s.length(), String.valueOf(maxQuantity));
//                        isUpdatingText = false;
//
//                        quantity = maxQuantity;
//                        android.widget.Toast.makeText(context,
//                                "Stok maksimal: " + maxQuantity,
//                                android.widget.Toast.LENGTH_SHORT).show();
//                        // Notify karena ada koreksi otomatis
//                        notifyQuantityChange();
//                    } else if (newQuantity >= 0 && newQuantity != quantity) {
//                        quantity = newQuantity;
//                        // JANGAN notify saat user sedang ngetik
//                        shouldNotifyExternal = false;
//                        notifyQuantityChange();
//                        shouldNotifyExternal = true;
//                    }
//                } catch (NumberFormatException e) {
//                    // Biarkan user melanjutkan input
//                }
//
//                updateButtonState();
//            }
//        });
//
//        // UBAH: Listener untuk menyelesaikan input - tambahkan clearFocus
//        etJumlah.setOnEditorActionListener((v, actionId, event) -> {
//            if (actionId == EditorInfo.IME_ACTION_DONE) {
//                String text = v.getText().toString();
//
//                if (text.isEmpty()) {
//                    setQuantity(0);
//                } else {
//                    try {
//                        int value = Integer.parseInt(text);
//                        // Set tanpa notify dulu
//                        shouldNotifyExternal = false;
//                        setQuantity(value);
//                        shouldNotifyExternal = true;
//                    } catch (NumberFormatException e) {
//                        setQuantity(0);
//                    }
//                }
//
//                // BARU: Notify setelah user selesai edit
//                notifyQuantityChange();
//
//                etJumlah.clearFocus();
//                android.view.inputmethod.InputMethodManager imm =
//                        (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
//                if (imm != null) {
//                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
//                }
//
//                return true;
//            }
//            return false;
//        });
    }

    private void updateUI() {
        // Tampilkan kuantitas saat ini di EditText
        // Perhatian: text watcher dapat memicu loop, jadi pastikan kita hanya mengubah jika teks berbeda.
        String currentText = etJumlah.getText().toString();
        String newText = String.valueOf(quantity);
        if (!currentText.equals(newText)) {
            etJumlah.setText(newText);
            // Pindahkan kursor ke akhir agar mudah dilihat
            etJumlah.setSelection(etJumlah.getText().length());
        }

        // Update status tombol minus
        updateButtonState();
    }

    private void updateButtonState() {
        // Tombol minus hanya aktif jika quantity > 0
        btnMinus.setEnabled(quantity > 0);

        // Tidak ada lagi logika visibility, semua kontrol selalu terlihat
    }

    private void notifyQuantityChange() {
        if (listener != null && shouldNotifyExternal) {
            listener.onQuantityChanged(quantity);
        }
    }

    // ===== PUBLIC METHODS =====
    public void setQuantity(int quantity) {
        // Pastikan kuantitas tidak negatif dan tidak melebihi max
        if (quantity > maxQuantity) {
            this.quantity = maxQuantity;
            android.widget.Toast.makeText(getContext(),
                    "Stok maksimal: " + maxQuantity,
                    android.widget.Toast.LENGTH_SHORT).show();
        } else {
            this.quantity = Math.max(0, quantity);
        }
        updateUI();
        notifyQuantityChange();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.listener = listener;
    }

    public void reset() {
        setQuantity(0);
    }
}