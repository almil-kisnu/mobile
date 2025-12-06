package com.almil.dessertcakekinian.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
// Import Class Animasi yang diperlukan
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import com.almil.dessertcakekinian.R
import java.util.concurrent.atomic.AtomicBoolean

class splashActivity : AppCompatActivity() {

    // Gunakan AtomicBoolean untuk mengontrol pengulangan secara aman
    private val isRunning = AtomicBoolean(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Panggil fungsi animasi dot
        startDotAnimation()

        // Delay 3 detik lalu pindah ke LoginActivity
        Handler(Looper.getMainLooper()).postDelayed({
            // Hentikan pengulangan animasi sebelum pindah Activity
            isRunning.set(false)
            startActivity(Intent(this, loginActivity::class.java))
            finish()
        }, 3000) // 3000 ms = 3 detik
    }

    // FUNGSI UNTUK MENGANIMAISKAN TITIK-TITIK DENGAN EFEK GESER KE KANAN
    private fun startDotAnimation() {
        // Mendapatkan referensi View dari XML
        val dots = listOf(
            findViewById<View>(R.id.dot1),
            findViewById<View>(R.id.dot2),
            findViewById<View>(R.id.dot3),
            findViewById<View>(R.id.dot4),
            findViewById<View>(R.id.dot5)
        )

        // --- PENGATURAN GERAK ---
        val distance = 20f // Jarak total titik bergeser ke kanan (dalam pixel)
        val durationPerDot = 300L // Kecepatan geser satu arah
        val delayStep = 80L // Jeda antar titik untuk efek gelombang

        dots.forEachIndexed { index, dot ->

            // 1. Animasi Geser Kanan (0f ke distance)
            val slideRight = ObjectAnimator.ofFloat(dot, View.TRANSLATION_X, 0f, distance).apply {
                duration = durationPerDot
                startDelay = index * delayStep
            }

            // 2. Animasi Geser Kiri (distance ke 0f)
            val slideLeft = ObjectAnimator.ofFloat(dot, View.TRANSLATION_X, distance, 0f).apply {
                duration = durationPerDot
                // Gelombang kembali dimulai dari titik paling kanan
                startDelay = (dots.size - 1 - index) * delayStep
            }

            // Gabungkan kedua animasi (kanan -> kiri)
            val sequence = AnimatorSet().apply {
                playSequentially(slideRight, slideLeft)
            }

            // Tambahkan listener untuk pengulangan
            sequence.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    // Hanya ulangi jika Activity masih berjalan
                    if (isRunning.get()) {
                        animation.start()
                    }
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })

            // Langsung mulai setiap sequence. Karena playTogether tidak diperlukan
            // jika setiap titik memiliki logikanya sendiri yang mengulang
            sequence.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Pastikan untuk menghentikan pengulangan jika Activity dihancurkan
        isRunning.set(false)
    }
}