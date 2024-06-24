package com.example.uasml2_gender

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 1000 // Waktu jeda splash screen dalam milidetik

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            // Intent untuk pindah ke ActivityMasuk setelah SPLASH_TIME_OUT
            val intent = Intent(this@SplashActivity, ActivityMasuk::class.java)
            startActivity(intent)
            finish() // Menutup SplashActivity agar tidak kembali ke sini saat tombol back ditekan
        }, SPLASH_TIME_OUT)
    }
}
