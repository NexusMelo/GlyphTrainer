package com.example.glyphtrainer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ProgramActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ativa modo programação
        AppMode.currentMode = AppMode.Mode.PROGRAM

        // abre o sistema atual (igual ao jogar)
        startActivity(Intent(this, MainActivity::class.java))

        // fecha esta activity
        finish()
    }
}