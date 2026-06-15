package com.example.glyphtrainer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()   // ← obrigatório para Theme.SplashScreen

        super.onCreate(savedInstanceState)

        AppMode.currentMode = AppMode.Mode.PLAY
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
