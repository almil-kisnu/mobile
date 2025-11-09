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
        // Menggunakan ID tvJumlah, tetapi di-cast ke EditText
        etJumlah = findViewById(R.id.tvJumlah);

        // --- LOGIKA TOMBOL DELETE DIHAPUS SESUAI PERMINTAAN ---

        // Set tampilan awal
        updateUI();

        // Tombol +
        btnPlus.setOnClickListener(v -> {
            quantity++;
            updateUI();
            notifyQuantityChange();
        });

        // Tombol -
        btnMinus.setOnClickListener(v -> {
            if (quantity > 0) {
                quantity--;
                updateUI();
                notifyQuantityChange();
            }
            // Tombol minus akan otomatis dinonaktifkan ketika quantity = 0 (dikelola di updateButtonState)
        });

        // Listener untuk Input Manual pada EditText
        etJumlah.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Kosong */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Jika teks dihapus, ini akan di-trigger
                if (s.length() == 0) {
                    quantity = 0;
                    updateButtonState(); // Nonaktifkan minus segera
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (!text.isEmpty()) {
                    try {
                        int newQuantity = Integer.parseInt(text);
                        // Cek apakah ada perubahan. Jika ya, update quantity.
                        if (newQuantity != quantity) {
                            quantity = Math.max(0, newQuantity); // Pastikan tidak negatif
                            notifyQuantityChange();
                        }
                    } catch (NumberFormatException e) {
                        // Jika input bukan angka valid, biarkan saja (harusnya dicegah oleh inputType="number" di XML)
                    }
                } else {
                    // Ketika input dikosongkan, kirim notifikasi 0
                    if (quantity != 0) {
                        quantity = 0;
                        notifyQuantityChange();
                    }
                }
                updateButtonState(); // Pastikan state tombol diperbarui
            }
        });

        // Listener untuk menyelesaikan input (optional, tapi membantu)
        etJumlah.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Pastikan nilai di EditText dikonversi dan diset lagi
                if (v.getText().toString().isEmpty()) {
                    setQuantity(0);
                } else {
                    setQuantity(Integer.parseInt(v.getText().toString()));
                }
                return false; // Membiarkan keyboard ditutup
            }
            return false;
        });
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
        if (listener != null) {
            listener.onQuantityChanged(quantity);
        }
    }

    // ===== PUBLIC METHODS =====
    public void setQuantity(int quantity) {
        // Pastikan kuantitas tidak negatif
        this.quantity = Math.max(0, quantity);
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