package com.example.glyphtrainer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppMode.currentMode = AppMode.Mode.PLAY
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
